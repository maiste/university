/* This module implements the interpreter of the Javix language. */

package trac.javix

object Interp {

import trac._
import trac.javix.AST._

// Values 

case class VInt (n:Int)                          extends Value
case class VString (s:String)                    extends Value
case class VBox (n:Int)                          extends Value
case class VArray (a:Array[Value])               extends Value
case object VNull                                extends Value

sealed abstract class Value {
  override def toString : String =
    this match {
      case VInt(n) => n.toString
      case VString(s) => "\""+s+"\""
      case VBox(n) => "<" + n.toString + ">"
      case VArray(a) => a.mkString("[",",","]")
      case VNull => "."
    }
}

sealed class State (p : Program, tr:Boolean) {

  type JumpTbl = Map[Label,Int]

  val code : Array[Instruction] = p.code.toArray
  val trace : Boolean = tr
  val jumpTbl : JumpTbl = mkJumpTbl(p.code,0,Map.empty)
  var stack : List[Value] = List()
  val vars : Array[Value] = Array.fill(p.varsize)(VNull)
  var pc : Int = 0
  var time : Int = 0
  var printed : List[String] = List()

  def mkJumpTbl (code:List[Instruction],i:Int,m:JumpTbl) : JumpTbl =
    code match {
      case Nil => m
      case Labelize(lab) :: code => mkJumpTbl(code,i+1,m+(lab->i))
      case _ :: code => mkJumpTbl(code,i+1,m)
    }

  def goto(lab:Label) : Unit = { pc = jumpTbl(lab) }

  def push(v:Value) : Unit = { stack = v :: stack }

  def pop(msg:String) : Value =
    stack match {
      case Nil => throw new Invalid("Not enough stack for " + msg)
      case v :: l => stack = l; v
    }

  def popInt(msg:String) : Int =
    pop(msg) match {
      case VInt(i) => i
      case _ => throw new Invalid("Invalid stack head for " + msg)
    }

  def popStr(msg:String) : String =
    pop(msg) match {
      case VString(s) => s
      case _ => throw new Invalid("Invalid stack head for " + msg)
    }

  def printStack (d:Int,l:List[Value]) : String =
    (if (l.length > d) "...," else "") + l.take(d).reverse.mkString(",")

  def printVars (a:Array[Value]) : String = {
    a.zipWithIndex.filter{case (v,_) => v != VNull}
    .map{case (v,i) => s"v$i=$v"}.mkString(" ")
  }

  override def toString : String = {
    val vs = printVars(vars)
    val stk = printStack(10,stack)
    s" vars:$vs\n stk:$stk\ntime:$time pc:$pc ${code(pc)}"
  }

}

def eval(p:Program,trace:Boolean=false) : String = {
  val st = new State(p,trace)
  run(st)
  StringContext.processEscapes(st.printed.reverse.mkString)
}

def run(st:State) : Unit = {
  var stop = false
  while (!stop) {
    assert (0 <= st.pc && st.pc < st.code.length)
    if (st.trace) println(st)
    st.time += 1
    val instr = st.code(st.pc)
    st.pc += 1
    instr match {
      case Return =>
        stop = true
        if (st.stack.nonEmpty)
          println("Warning: final Return discards some stack")
      case Labelize(_) => ()
      case Comment(_) => ()
      case Box =>
        val i = st.popInt("Box")
        st.push(VBox(i))
      case Unbox =>
        st.pop("Unbox") match {
          case VBox(i) => st.push (VInt(i))
          case _ => throw new Invalid ("Incorrect stack head for Unbox")
        }
      case IPrint =>
        st.printed = st.popInt("IPrint").toString :: st.printed
      case SPrint =>
        st.printed = st.popStr("SPrint") :: st.printed
      case Ldc(s) => st.push(VString(s))
      case SCat =>
        val s2 = st.popStr("SCat")
        val s1 = st.popStr("SCat")
        st.push(VString(s1+s2))
      case Push(i) => st.push (VInt(i))
      case Pop => st.pop("Pop")
      case Swap =>
        val v2 = st.pop("Swap")
        val v1 = st.pop("Swap")
        st.push(v2); st.push(v1)
      case Dup =>
        val v = st.pop("Dup")
        st.push(v); st.push(v)
      case IOp(o) =>
        val n2 = st.popInt("Binop")
        val n1 = st.popInt("Binop")
        st.push(VInt(IntOp.eval(o,n1,n2)))
      case AStore(k) =>
        st.pop("AStore") match {
          case VInt(_) => throw new Invalid("Astore of a non-boxed int")
          case v => st.vars(k) = v
        }
      case ALoad(k) => st.push(st.vars(k))
      case IStore(k) =>
        st.pop("IStore") match {
          case v@VInt(_) => st.vars(k) = v
          case _ => throw new Invalid("Istore of a non-int")
        }
      case ILoad(k) => st.push(st.vars(k)) /* TODO checks */
      case Goto(lab) => st.goto(lab)
      case Ificmp(cmp,lab) =>
        val n2 = st.popInt("Ificmp")
        val n1 = st.popInt("Ificmp")
        if (CompOp.eval(cmp,n1,n2)) st.goto(lab)
      case If(cmp,lab) =>
        val n = st.popInt("If")
        if (CompOp.eval(cmp,n,0)) st.goto(lab)
      case ANewarray =>
        val n = st.popInt("ANewArray")
        st.push(VArray(Array.fill(n)(VNull)))
      case AAStore =>
        val v = st.pop("AAStore")
        val i = st.popInt("AAStore")
        val a = st.pop("AAStore")
        (v,a) match {
          case (VInt(_),_) => throw new Invalid("AAStore of a non-boxed int")
          case (_,VArray(a)) => a(i) = v
          case (_,_) => throw new Invalid("AAStore on a a non-VArray")
        }
      case AALoad =>
        val i = st.popInt("AALoad")
        st.pop("AALoad") match {
          case VArray(a) => st.push(a(i))
          case _ => throw new Invalid("AALoad on a a non-VArray")
        }
      case Tableswitch(offset,tab,default) =>
        val n = st.popInt("Tableswitch") - offset
        if (0 <= n && n < tab.length) st.goto(tab(n)) else st.goto(default)
      case Checkarray =>
        val a = st.pop("Checkarray")
        a match {
          case VArray(_) => st.push(a)
          case _ => throw new Invalid("Checkarray on a non-VArray")
        }
      case Checkstring =>
        val a = st.pop("Checkstring")
        a match {
          case VString(_) => st.push(a)
          case _ => throw new Invalid("Checkstring on a non-VString")
        }
    }
  }
}

}

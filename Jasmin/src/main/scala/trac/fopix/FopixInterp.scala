
// Interpretation of Fopix programs

// TODO : To finish !

package trac.fopix

object Interp {

import scala.annotation.tailrec
import scala.collection.mutable.{Map => MutableMap}
import trac._
import trac.fopix.AST._
import trac.PrimOp._

type Address = Int

type Block = Array[Result]
type Memory = MutableMap[Address,Block]

type Env = Map[Ident,Result]  // immutable map of variables
type FunDef = (List[Ident], Expr)
type FunEnv = MutableMap[FunIdent, FunDef]

sealed abstract class Result {
  // Customized pretty-print of interpretation results
  override def toString : String =
    this match {
      case RUnit => "()"
      case RInt(n) => n.toString
      case RStr(s) => "\""+s+"\""
      case RBool(b) => b.toString
      case RBlk(a) => "@" + a.toString
      case RFun(f,args) => f + args.mkString("(", ",", ")")
    }
}

case object RUnit                                extends Result
case class RInt (n:Int)                          extends Result
case class RStr (s:String)                       extends Result
case class RBool (b:Boolean)                     extends Result
case class RBlk (a:Address)                      extends Result
case class RFun (f:FunIdent, args:List[Result])  extends Result

def getInt (r:Result,msg:String) : Int =
 r match {
   case RInt(n) => n
   case _ => throw new Invalid(msg + " is not an integer")
 }

// NB: in RFun, args is empty unless you try to accept partial application
// and over-applications

// Global mutable elements for interpretation : 

var allPrints : List[String] = List()
var memsize = 0
val mem : Memory = MutableMap.empty
val functions : FunEnv = MutableMap.empty

// Global table of functions, giving their parameter lists and their bodies
type TableFun = MutableMap[FunIdent,(List[Ident],Expr)]
val tblfun : TableFun = MutableMap.empty

def reset () : Unit = {
  allPrints = List()
  memsize = 0
  mem.clear()
  tblfun.clear()
  functions.clear()
}

def loadFuns(p:Program) : Unit = {
    p match {
      case Nil => ()
      case Val(_, _) :: p => loadFuns(p)
      case Def(fid, args, e) :: p => {
        functions += (fid -> (args, e))
        loadFuns(p)
      }
    }
}

def eval (p:Program) : String = {
  reset()
  /* TODO: remplir tblfun avec tous les Def de fonctions du program */
  val initEnv : Env = Map.empty
  loadFuns(p)
  eval(initEnv,p)
  StringContext.processEscapes(allPrints.reverse.mkString)
}

def eval (env:Env,p:Program) : Env =
  p match {
    case Nil => env
    case Val(x,e) :: p => eval(env + (x -> eval(env,e)), p)
    case Def(_,_,_) :: p => eval(env,p)
  }

def eval(env:Env,e:Expr) : Result =
  e match {
    case Num(n) => RInt(n)
    case Str(s) => RStr(s)
    case Var(v) => env(v)
    case Fun(f) => RFun(f,List())
    case Op(o,e1,e2) => binop(o,eval(env,e1),eval(env,e2))    
    case Let(x,e1,e2) => eval(env + (x -> eval(env, e1)), e2)
    case If(e1,e2,e3) => if_eval(eval(env, e1), env, e2, e3)
    case Prim(p,l) => prim(p,l.map(eval(env,_)))
    case Call(e,l) => call_eval(e, l, env)
  }

def if_eval(c: Result, env: Env, e1: Expr, e2: Expr) : Result = {
  c match {
    case RBool(b) =>
      if (b) eval(env, e1)
      else eval(env, e2)
    case _ => throw new Invalid("Comparison in if is not a boolean")
  }
}

def call_eval(f: Expr, args: List[Expr], env: Env) : Result = {
  val id = eval(env, f) match {
    case RFun(f,_) => f
    case _ => throw new Invalid("Type not supported")
  }
  val (names : List[Ident], body : Expr) = functions(id)
  val mapArgs : List[(Ident, Expr)]=  names zip args
  val newEnv  : Env = mapArgs.foldLeft(Map.empty : Env){
    (acc : Env, value : (Ident, Expr)) =>  {
      val (name, expr) = value
      (acc + (name -> eval(env, expr)))
    }
  }
  eval(newEnv, body)
}

def binop(o:BinOp.T,r1:Result,r2:Result) : Result = {
  val msg = "Binop argument"
  val n1 = getInt(r1,msg)
  val n2 = getInt(r2,msg)
  if (BinOp.isArith(o))
    RInt(IntOp.eval(BinOp.toArith(o),n1,n2))
  else
    RBool(CompOp.eval(BinOp.toCmp(o),n1,n2))
}

def prim(p:PrimOp.T,args:List[Result]) : Result =
 (p,args) match {
   case (New, List(RInt(n))) => new_eval(n)
   case (Get, List(RBlk(addr), RInt(idx))) => mem(addr)(idx)
   case (Set, List(RBlk(addr), RInt(idx), value)) => set_eval(addr, idx, value)
   case (Tuple, args) => tuple_eval(args)
   case (Printint,List(RInt(n))) => allPrints = n.toString :: allPrints; RUnit
   case (Printstr,List(RStr(s))) => allPrints = s :: allPrints; RUnit
   case (Cat, List(RStr(s1), RStr(s2))) => RStr(s1 + s2)
   case _ => throw new Invalid("Unsupported primitive call (TODO ? bad arg ?)")
 }

def new_eval(n : Integer) : Result = {
  mem += (memsize -> new Array[Result](n));
  memsize += 1;
  RBlk(memsize-1)
}

def set_eval(addr : Integer, idx : Integer, value : Result) : Result = {
  mem(addr)(idx) = value;
  RUnit
}

def tuple_eval(args : List[Result]) : Result = {
  val blck = new_eval(args.length);
  val addr = blck match {
    case RBlk(addr) => addr
    case _ => throw new Invalid("This case shoudln't append!")
  };
  args.indices.foreach (
    i => set_eval(addr, i, args(i))
  )
  blck
}

}

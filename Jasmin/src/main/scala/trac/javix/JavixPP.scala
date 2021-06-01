
// Pretty-printing of Javix

package trac.javix

object PP {

import trac._
import trac.IntOp._
import trac.CompOp._
import trac.javix.AST._

def header (p:Program) =
s""".class public ${p.classname}
.super java/lang/Object

.method public static main([Ljava/lang/String;)V
   .limit stack 2
   invokestatic ${p.classname}/code()V
   return
.end method

;;; the compiled code

.method public static code()V
.limit locals ${p.varsize}
.limit stack ${p.stacksize}
"""

def box =
     """   iconst_1 ;;;;;;BOX
	   newarray int ;;BOX
	   dup_x1 ;;;;;;;;BOX
	   swap ;;;;;;;;;;BOX
	   iconst_0 ;;;;;;BOX
	   swap ;;;;;;;;;;BOX
	   iastore ;;;;;;;BOX"""

def unbox =
     """   checkcast [I ;;UNBOX
	   iconst_0 ;;;;;;UNBOX
	   iaload ;;;;;;;;UNBOX"""

def iprint =
     """getstatic java/lang/System/out Ljava/io/PrintStream;  ;IPRINT
	swap                                                  ;IPRINT
	invokevirtual java/io/PrintStream/print(I)V           ;IPRINT"""

def sprint =
     """getstatic java/lang/System/out Ljava/io/PrintStream;           ;SPRINT
	swap                                                           ;SPRINT
	invokevirtual java/io/PrintStream/print(Ljava/lang/String;)V   ;SPRINT"""

def pp (p:Program) : String =
  header(p) + pp_code(p) + "\n.end method\n"

def pp_code (p:Program) : String =
  p.code.map(labelled_instr).mkString("\n")

def labelled_instr (i:Instruction) : String =
  i match {
    case Labelize(l) => l + ":"
    case _ => "\t" + instr(i)
  }

def instr (i:Instruction) : String =
  i match {
    case Labelize(l) => l + ":"
    case Comment(s) => ";; " + s
    case Box => box
    case Unbox => unbox
    case Push(n) => push(n)
    case Pop => "pop"
    case Swap => "swap"
    case Dup => "dup"
    case IOp(o) => op(o)
    case AStore(v) => s"astore $v"
    case ALoad(v) => s"aload $v"
    case IStore(v) => s"istore $v"
    case ILoad(v) => s"iload $v"
    case Goto(l) => s"goto $l"
    case Ificmp(c,l) => s"if_icmp${cmp(c)} $l"
    case If(c,l) => s"if${cmp(c)} $l"
    case ANewarray => "anewarray java/lang/Object"
    case Checkarray => "checkcast [Ljava/lang/Object;"
    case AAStore => "aastore"
    case AALoad => "aaload"
    case Return => "return"
    case Tableswitch(o,labs,dft) =>
      s"tableswitch $o" +labs.mkString("\n\t","\n\t","\n\t")+"default: "+dft
    case IPrint => iprint
    case SPrint => sprint
    case Ldc(s) => "ldc \""+s+"\""
    case SCat =>
      "invokevirtual java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;"
    case Checkstring => "checkcast java/lang/String"
  }

def push(n:Int) : String =
  if (0 <= n && n <= 5)
    s"iconst_$n"
  else if (-(1<<7) <= n && n < (1<<7))
    s"bipush $n"
  else if (-(1<<15) <= n && n < (1<<15))
    s"sipush $n"
  else
    s"ldc $n"

def op : IntOp.T => String = {
  case Add => "iadd"
  case Sub => "isub"
  case Mul => "imul"
  case Div => "idiv"
  case Mod => "irem"
}

def cmp : CompOp.T => String = {
  case Eq => "eq"
  case Ne => "ne"
  case Le => "le"
  case Lt => "lt"
  case Ge => "ge"
  case Gt => "gt"
}

}

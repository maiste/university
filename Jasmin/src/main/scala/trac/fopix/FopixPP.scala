
// Pretty-printing of Fopix

package trac.fopix {
object PP {

import trac._
import trac.PrimOp._
import AST._


def pp (p:Program) : String =
 p.map(pp).mkString("","\n","\n")

def pp (d:Definition) : String =
 d match {
   case Val(id,e) => "val " + id + " = " + pp(1,e)
   case Def(f,args,e) => "def " + f + tuple(args) + " = " + pp(1,e)
 }

// TODO: priority of operations

// i : how much indentation when putting a newline

def pp (i:Int,e:Expr) : String =
  e match {
    case Num(n) => n.toString
    case Str(s) => "\""+s+"\""
    case Var(v) => v
    case Fun(f) => "&"+f
    case Let(id,e1,e2) =>
      nl(i) + "let " + id + " = " + pp(i+1,e1) + nl(i) + "in " + pp(i,e2)
    case If(e1,e2,e3) =>
      nl(i) + "if " + pp(i+1,e1) +
      nl(i) + "then " + pp(i+1,e2) +
      nl(i) + "else " + pp(i+1,e3)
    case Op(o,e1,e2) => paren(pp(i,e1) + o.toString + pp(i,e2))
    case Call(Fun(f), l) => f + tuple (l.map(pp(i,_)))
    case Call(e,l) => "?"+paren (pp(i,e)) + tuple (l.map(pp(i,_)))
    case Prim(New,List(size)) => New + bracket (pp(i,size))
    case Prim(Get,List(a,idx)) => pp(i,a) + bracket (pp(i,idx))
    case Prim(Set,List(a,idx,v)) =>
      pp(i,a) + bracket (pp(i,idx)) + ":=" + pp(i,v)
    case Prim(Printint,List(e)) => Printint + paren(pp(i,e))
    case Prim(Printstr,List(e)) => Printstr + paren(pp(i,e))
    case Prim(Cat,List(e1,e2)) => paren(pp(i,e1)) + "^" + paren(pp(i,e2))
    case Prim(Tuple,el) => array (el.map(pp(i,_)))
    case Prim(p,_) => throw new Invalid("Bad Primitive " + p)
  }

def nl(i:Int) : String = "\n" + " "*i

def paren(s:String) : String = "("+s+")"

def bracket(s:String) : String = "["+s+"]"

def tuple(l:List[String]) : String = l.mkString("(", ",", ")")

def array(l:List[String]) : String = l.mkString("[", ",", "]")

}}

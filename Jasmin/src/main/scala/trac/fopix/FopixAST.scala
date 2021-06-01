
// The abstract syntax tree for fopix programs.

package trac.fopix

object AST {

import trac._

type Program = List[Definition]

type Ident = String
type FunIdent = String

sealed abstract class Definition
case class Val (id:Ident, e:Expr)                       extends Definition
case class Def (fid:FunIdent, args:List[Ident], e:Expr) extends Definition

sealed abstract class Expr
case class Num (n:Int)                          extends Expr
case class Str (s:String)                       extends Expr
case class Fun (fid:FunIdent)                   extends Expr
case class Var (id:Ident)                       extends Expr
case class Let (id:Ident, e1:Expr, e2:Expr)     extends Expr
case class If (e1:Expr, e2:Expr, e3:Expr)       extends Expr
case class Op (o:BinOp.T, e1:Expr, e2:Expr)     extends Expr
case class Prim (p:PrimOp.T, args:List[Expr])   extends Expr
case class Call (f:Expr, args:List[Expr])       extends Expr

// Extra Prim and Call constructors with variadic arguments

object Prim {
  def apply(p:PrimOp.T,args:Expr*) = new Prim(p,args.toList)
}

object Call {
  def apply(f:Expr,args:Expr*) = new Call(f,args.toList)
}

// Some preudo-constructors for primitive operations

import trac.PrimOp._

def mkNew (e:Expr) : Expr = Prim(New,e)
def mkGet (a:Expr,i:Expr) : Expr = Prim(Get,a,i)
def mkSet (a:Expr,i:Expr,v:Expr) : Expr = Prim(Set,a,i,v)
def mkPrintint (e:Expr) : Expr = Prim(Printint,e)
def mkPrintstr (e:Expr) : Expr = Prim(Printstr,e)
def mkCat (e1:Expr,e2:Expr) : Expr = Prim(Cat,e1,e2)
def mkTuple (el:List[Expr]) : Expr = Prim(Tuple,el)

}

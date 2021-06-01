
// The abstract syntax tree for anfix programs.

/* Anfix is the same as Fopix, except that:
   - function call arguments must be "simple" (variables or constants)
   - "if" conditions are immediate comparisons between "simple"
      expressions

   NB: To represent boolean constants true and false,
   the numbers 1 and 0 could be used.
*/

package trac.anfix

import trac._

object AST {

type Program = List[Definition]

type Ident = String
type FunIdent = String

sealed abstract class Definition
case class Val (id:Ident, e:Expr)                       extends Definition
case class Def (fid:FunIdent, args:List[Ident], e:Expr) extends Definition

sealed abstract class Expr
case class Simple (e:SimplExpr)                              extends Expr
case class Let (id:Ident, e1:Expr, e2:Expr)                  extends Expr
case class If (c:Comparison, e2:Expr, e3:Expr)               extends Expr
case class Op (o:IntOp.T, e1:SimplExpr, e2:SimplExpr)        extends Expr
case class Prim (o:PrimOp.T, args:List[SimplExpr])           extends Expr
case class Call (f:SimplExpr, args:List[SimplExpr])          extends Expr

sealed abstract class SimplExpr
case class Num (n:Int)                                  extends SimplExpr
case class Str (s:String)                               extends SimplExpr
case class Fun (fid:FunIdent)                           extends SimplExpr
case class Var (id:Ident)                               extends SimplExpr

type Comparison = (CompOp.T,SimplExpr,SimplExpr)

}

object PP {
  def pp (p:AST.Program) : String =
    fopix.PP.pp(transl.Anfix2Fopix.trans(p))
}

object Interp {
  def eval(p:AST.Program) : String =
    fopix.Interp.eval(transl.Anfix2Fopix.trans(p))
}

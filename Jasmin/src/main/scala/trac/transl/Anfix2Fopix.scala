
/* This module implements a translation back from Anfix to Fopix
 * (for printing or interpretation) */

package trac.transl

object Anfix2Fopix {

import trac._
import trac.anfix.{AST=>S}
import trac.fopix.{AST=>T}

def trans(p:S.Program) : T.Program = p.map(trans)

def trans(d:S.Definition) : T.Definition =
  d match {
    case S.Val(x,e) => T.Val(x,trans(e))
    case S.Def(f,a,e) => T.Def(f,a,trans(e))
  }

def trans (e:S.Expr) : T.Expr =
  e match {
    case S.Simple(e) => trans(e)
    case S.Let(id,e1,e2) => T.Let(id,trans(e1),trans(e2))
    case S.If(c,e1,e2) => T.If(transCmp(c),trans(e1),trans(e2))
    case S.Op(o,e1,e2) => T.Op(BinOp.ofArith(o),trans(e1),trans(e2))
    case S.Prim(p,a) => T.Prim(p,a.map(trans))
    case S.Call(e,a) => T.Call(trans(e),a.map(trans))
  }

def trans (e:S.SimplExpr) : T.Expr =
  e match {
    case S.Num(n) => T.Num(n)
    case S.Str(s) => T.Str(s)
    case S.Fun(f) => T.Fun(f)
    case S.Var(v) => T.Var(v)
  }

def transCmp : S.Comparison => T.Expr =
  { case (o,e1,e2) => T.Op(BinOp.ofCmp(o),trans(e1),trans(e2)) }

}


/* This module implements a translation back from Kontix to Fopix
 * (for printing or interpretation) */

// Note : this code uses the reserved identifiers "__K" "__E" and
// reserved function name "__RET"

package trac.transl

object Kontix2Fopix {

import scala.annotation.tailrec
import trac._
import trac.kontix.{AST=>S}
import trac.fopix.{AST=>T}

val E : String = "__E"
val K : String = "__K"
val R : String = "__RET"

def trans(p:S.Program) : T.Program =
  p.defs.map(trans) ++
  List(
    T.Def(R,List(E,"r"),T.Num(0)),
    T.Val("_",
          T.Let(K,T.Fun(R),
           T.Let(E,T.mkNew(T.Num(0)),
            trans(p.main)))))

def trans(d:S.Definition) : T.Definition =
  d match {
    case S.DefFun(f,a,e) => T.Def (f,K::E::a,trans(e))
    case S.DefCont(f,ids,x,e) =>
      val body =
        untuple (T.Var(E),ids,2,
                 T.Let(K,T.mkGet(T.Var(E),T.Num(0)),
                       T.Let(E,T.mkGet(T.Var(E),T.Num(1)),
                             trans(e))))
      T.Def (f,List(E,x),body)
  }

def untuple(a:T.Expr,ids:List[T.Ident],offset:Int,e:T.Expr) : T.Expr = {
  @tailrec def loop(n:Int,ids:List[T.Ident],e:T.Expr) : T.Expr =
    ids match {
      case Nil => e
      case id::ids =>
        loop(n-1,ids,T.Let(id,T.mkGet(a,T.Num(n)),e))
    }
  loop(offset+ids.length-1,ids.reverse,e)
}

def trans (e:S.TailExpr) : T.Expr =
 e match {
   case S.Let (x,e1,e2) => T.Let (x, trans(e1), trans(e2))
   case S.If (c,e1,e2) => T.If (transCmp(c), trans(e1), trans(e2))
   case S.Call(e,el) => T.Call (trans(e), T.Var(K)::T.Var(E)::el.map(trans))
   case S.Ret(e) => T.Call (T.Var(K), List(T.Var(E),trans(e)))
   case S.PushCont(c,ids,e) =>
     T.Let (E, T.mkTuple((K::E::ids).map(T.Var)),
       T.Let (K, T.Fun(c), trans(e)))
 }

def transCmp : S.Comparison => T.Expr = {
  case (o,e1,e2) => T.Op(BinOp.ofCmp(o),trans(e1),trans(e2))
}

def trans(e:S.BasicExpr) : T.Expr =
  e match {
    case S.Num(n) => T.Num(n)
    case S.Str(s) => T.Str(s)
    case S.Fun(f) => T.Fun(f)
    case S.Var(x) => T.Var(x)
    case S.BLet (x,e1,e2) => T.Let (x,trans(e1),trans(e2))
    case S.BIf (c,e1,e2) => T.If (transCmp(c),trans(e1),trans(e2))
    case S.Op (o,e1,e2) => T.Op (BinOp.ofArith(o),trans(e1),trans(e2))
    case S.Prim(p,el) => T.Prim(p,el.map(trans))
  }
}

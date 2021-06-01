
/* This module implements the compilation from Anfix to Kontix */

package trac.transl

object Anfix2Kontix {

  import trac._
  import trac.anfix.{AST=>S}
  import trac.kontix.{AST=>T}
  import scala.collection.immutable._

  type vars = Set[T.Ident]

  /******** UTILITIES ********/

  /******** KONT UTILITIES ********/
  var konts : List[T.DefCont] = List()
  var kont_id = 0

  def fresh_kontname () = {
    val kontname = "kont_" + kont_id.toString
    kont_id += 1
    kontname
  }

  /******** VARS UTILITIES ********/
  def get_vars_comp (c: S.Comparison) : vars = {
    c match {
      case (_, S.Var (id1), S.Var (id2)) => Set(id1, id2)
      case (_, S.Var (id), _) => Set(id)
      case (_, _, S.Var (id)) => Set(id)
      case _ => Set()
    }
  }

  def get_vars_simplexpr (e:S.SimplExpr) : vars = {
    e match {
      case S.Num (n) => Set()
      case S.Str (s) => Set()
      case S.Fun (f) => Set()
      case S.Var (id) => Set(id)
    }
  }

  def get_vars_simplexprs (l:List[S.SimplExpr]) : vars = {
    l match {
      case Nil => Set()
      case x :: xs => get_vars_simplexpr(x).union(get_vars_simplexprs(xs))
    }
  }

  def get_vars_expr (e:S.Expr) : vars = {
    e match {
      case S.Simple (e) => get_vars_simplexpr(e)
      case S.Let (x, e1, e2) =>
        get_vars_expr(e1).union(get_vars_expr(e2)) - x
      case S.If (c, e1, e2) =>
        get_vars_expr(e1).union(get_vars_expr(e2)).union(get_vars_comp(c))
      case S.Op (_, e1, e2) =>
        get_vars_simplexpr(e1).union(get_vars_simplexpr(e2))
      case S.Prim (_, args) =>
        get_vars_simplexprs(args)
      case S.Call (f, args) =>
        get_vars_simplexprs(args).union(get_vars_simplexpr(f))
    }
  }

  /******** TRANSLATE ANFIX TO KONTIX ********/
  def trans (p:S.Program) : T.Program = {
    val defs = trans_def(p)
    val main = trans_expr(merge_main(p))
    T.Program(defs ++ konts, main)
  }

  def merge_main (p:S.Program) : S.Expr = {
    p match {
      case Nil => S.Simple (S.Num (0))
      case S.Val (id, e) :: xs => S.Let (id, e, merge_main(xs))
      case S.Def (_, _, _) :: xs => merge_main(xs)
    }
  }

  def trans_def (d:List[S.Definition]) : List[T.Definition] = {
    d match {
      case Nil =>
        List()
      case S.Val (_, _) :: xs =>
        List() ++ trans_def(xs)
      case S.Def (fid, args, e) :: xs =>
        List(T.DefFun(fid, args, trans_expr(e))) ++ trans_def(xs)
    }
  }

  def trans_comp(c:S.Comparison) : T.Comparison = {
    c match {
      case (op, e1, e2) => (op, trans_simplexpr(e1), trans_simplexpr(e2))
    }
  }

  def trans_expr (e:S.Expr) : T.TailExpr = {
    e match {
      case S.Simple (e) =>
        T.Ret (trans_simplexpr(e))
      case S.Let (id, e1, e2) =>
        if (call_in_expr(e1)) {
          trans_let(id, e1, e2)
        } else {
          trans_simpl_let(id, e1, e2)
        }
      case S.If (c, e1, e2) =>
        T.If (trans_comp(c), trans_expr(e1), trans_expr(e2))
      case S.Op (o, e1, e2) =>
        T.Ret (T.Op (o, trans_simplexpr(e1), trans_simplexpr(e2)))
      case S.Prim (p, args) =>
        T.Ret (T.Prim (p, args.map(x => trans_simplexpr(x))))
      case S.Call (f, args) =>
        T.Call (trans_simplexpr(f), args.map(x => trans_simplexpr(x)))
    }
  }


  def trans_let (id:S.Ident, e1:S.Expr, e2:S.Expr) : T.TailExpr = {
    val saves = (get_vars_expr(e2) - id).toList

    /* PushCont */
    val kont_name = fresh_kontname()
    val expr_e1 = trans_expr(e1)
    val push = T.PushCont (kont_name, saves, expr_e1)

    /* DefKont */
    val expr_e2 = trans_expr(e2)
    val defkont = T.DefCont (kont_name, saves, id, expr_e2)
    konts = defkont :: konts

    push
  }

  def trans_simpl_let(id:S.Ident, e1:S.Expr, e2:S.Expr) : T.TailExpr = {
    T.Let (id, trans_expr_to_be(e1), trans_expr(e2))
  }

  def call_in_expr(e:S.Expr) : Boolean = {
    e match {
      case S.Simple (_) => false
      case S.Let (_, e1, e2) => call_in_expr(e1) || call_in_expr(e2)
      case S.If (c, e1, e2) => call_in_expr(e1) || call_in_expr(e2)
      case S.Op (_, _, _) => false
      case S.Prim (_, _) => false
      case S.Call (_, _) => true
    }
  }

  def trans_expr_to_be(e:S.Expr) : T.BasicExpr = {
     e match {
       case S.Simple (e) =>
         trans_simplexpr(e)
       case S.Let (id, e1, e2) =>
         T.BLet (id, trans_expr_to_be(e1), trans_expr_to_be(e2))
       case S.If (c, e1, e2) =>
         T.BIf (trans_comp(c), trans_expr_to_be(e1), trans_expr_to_be(e2))
       case S.Op (o, e1, e2) =>
         T.Op (o, trans_simplexpr(e1), trans_simplexpr(e2))
      case S.Prim (p, args) =>
         T.Prim (p, args.map(x => trans_simplexpr(x)))
       case S.Call (f, args) =>
         throw new Invalid("Not supposed to be here")
    }
  }

  def trans_simplexpr (e:S.SimplExpr) : T.BasicExpr = {
    e match {
      case S.Num (n) =>
        T.Num (n)
      case S.Str (s) =>
        T.Str (s)
      case S.Fun (f) =>
        T.Fun (f)
      case S.Var (id) =>
        T.Var (id)
    }
  }
}


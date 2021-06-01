
/* This module implements a translation from Fopix to Anfix */

package trac.transl

object Fopix2Anfix {

import trac._
import trac.fopix.{AST=>S}
import trac.anfix.{AST=>T}


/****** Utilities *******/

var label_counter = 0

/* Generate a fresh label for splitting let ... in */
def fresh_id () = {
  val res = "_x" + label_counter.toString()
  label_counter += 1
  res
}

/* Return true if it's translated into a simple expr */
def is_simple_expr(e : S.Expr) : Boolean = {
  e match {
    case S.Num(_) | S.Str (_) | S.Var (_) | S.Fun(_) => true
    case _ => false
  }
}


/***** Anfix transformation *****/

/* Translate a program */
def trans(p:S.Program) : T.Program = {
  p match {
    case Nil => List ()
    case S.Val (id, e) :: p =>
      val trans_val = T.Val (id,trans_expr(e))
      trans_val :: trans(p)
    case S.Def (fid, args, e) :: p =>
      val trans_def = T.Def(fid, args, trans_expr(e))
      trans_def :: trans(p)
  }
}

/* Translate an expression */
def trans_expr(e: S.Expr) : T.Expr = {
  e match {
    case S.Num(n) => T.Simple(T.Num(n))
    case S.Str(s) => T.Simple(T.Str(s))
    case S.Fun(fid) => T.Simple(T.Fun(fid))
    case S.Var(id) => T.Simple(T.Var(id))
    case S.Let(id, e1, e2) => trans_let(id, e1, e2)
    case S.If(e1, e2, e3) => trans_if(e1, e2, e3)
    case S.Op (o, e1, e2) => trans_op(o,e1,e2)
    case S.Prim(p, args) => trans_prim(p, args)
    case S.Call(f, args) => trans_call(f, args)
  }
}

/* Translate operator */
def trans_op(o: BinOp.T, e1: S.Expr, e2: S.Expr) : T.Expr = {
  val trans_e1 = trans_expr(e1)
  val trans_e2 = trans_expr(e2)
  (trans_e1, trans_e2) match {
    case (T.Simple(s1), T.Simple(s2)) => trans_op_aux(o, s1, s2)
    case (T.Simple(s1), e) => {
      val let_id = fresh_id ()
      val let_var = T.Var(let_id)
      val operation = trans_op_aux(o, s1, let_var)
      T.Let(let_id, e, operation)
    }
    case (e, T.Simple(s2)) => {
      val let_id = fresh_id ()
      val let_var = T.Var(let_id)
      val operation = trans_op_aux(o, let_var, s2)
      T.Let(let_id, e, operation)
    }
    case (e1, e2) => {
      val let_id_e1 = fresh_id ()
      val let_var_e1 = T.Var(let_id_e1)
      val let_id_e2 = fresh_id ()
      val let_var_e2 = T.Var(let_id_e2)
      val operation = trans_op_aux(o, let_var_e1, let_var_e2)
      T.Let (let_id_e1, e1,
        T.Let(let_id_e2, e2, operation))
    }
  }
}

def trans_op_aux (o: BinOp.T, s1: T.SimplExpr, s2: T.SimplExpr) : T.Expr = {
  if(BinOp.isArith(o)) {
    T.Op(BinOp.toArith(o), s1, s2)
  } else {
    T.If((BinOp.toCmp(o), s1, s2), T.Simple(T.Num(1)), T.Simple(T.Num(0)))
  }
}


/* Translate let */
def trans_let(id : S.Ident, e1: S.Expr, e2: S.Expr) : T.Expr = {
  val trans_e1 = trans_expr(e1)
  val trans_e2 = trans_expr(e2)
  T.Let(id, trans_e1, trans_e2)
}


/* Translate if */
def trans_if(e1: S.Expr, e2: S.Expr, e3: S.Expr) : T.Expr = {
  e1 match {
    case S.Op(o, left, right) =>
      if(BinOp.isCmp(o)) {
        if (is_simple_expr(left) && is_simple_expr(right)) {
          translate_simple_if(o, left, right, e2, e3)
        } else if(is_simple_expr(left) || is_simple_expr(right)) {
          trans_one_side_if(o, left, right,e2,e3)
        } else {
          trans_if_aux(e1,e2,e3)
        }
      } else {
        trans_if_aux(e1,e2,e3)
      }
    case _ => trans_if_aux(e1,e2, e3)
  }
}

def translate_simple_if(
  o: BinOp.T,
  left: S.Expr,
  right: S.Expr,
  e2: S.Expr,
  e3: S.Expr) : T.Expr = {
   (trans_expr(left), trans_expr(right)) match {
        case (T.Simple(s1), T.Simple(s2)) =>
          val e2_trans = trans_expr(e2)
          val e3_trans = trans_expr(e3)
          T.If((BinOp.toCmp(o), s1, s2), e2_trans, e3_trans)
        case _ => throw new Invalid("It's broken...")
   }
}

def trans_one_side_if(
  o: BinOp.T,
  left: S.Expr,
  right: S.Expr,
  e2: S.Expr,
  e3: S.Expr) : T.Expr = {
  (trans_expr(left), trans_expr(right)) match {
      case (T.Simple(s1), e) => {
        val let_id = fresh_id()
        val let_var = T.Var(let_id)
        val e2_trans = trans_expr(e2)
        val e3_trans = trans_expr(e3)
        T.Let(let_id, e,
          T.If((BinOp.toCmp(o), s1, let_var), e2_trans, e3_trans))
      }
      case (e, T.Simple(s2)) => {
          val let_id = fresh_id()
          val let_var = T.Var(let_id)
          val e2_trans = trans_expr(e2)
          val e3_trans = trans_expr(e3)
          T.Let(let_id, e,
            T.If((BinOp.toCmp(o), let_var, s2), e2_trans, e3_trans))
        }
      case _ => throw new Invalid("It's broken...")
   }

}

def trans_if_aux(e1 : S.Expr, e2: S.Expr, e3: S.Expr) : T.Expr = {
  val trans_e1 = trans_expr(e1)
  val trans_e2 = trans_expr(e2)
  val trans_e3 = trans_expr(e3)
  val let_id = fresh_id()
  val let_var = T.Var(let_id)
  T.Let (let_id, trans_e1,
    T.If((CompOp.Eq, let_var, T.Num(1)), trans_e2, trans_e3))
}


/* Translate args */
def trans_args_list(args : List[S.Expr]) : (List[(T.Ident, T.Expr)], List[T.SimplExpr]) = {
  args.foldRight((List(), List()) : (List[(T.Ident, T.Expr)], List[T.SimplExpr])) {
    (expr, acc) => {
      val (let_expr, simple) = acc
      val trans_e = trans_expr(expr)
      trans_e match {
        case T.Simple(s) => (let_expr, s::simple)
        case _ => {
          val let_id = fresh_id ()
          val let_var = T.Var(let_id)
          val new_let_expr = (let_id, trans_e)::let_expr
          val new_simple = let_var::simple
          (new_let_expr, new_simple)
        }
      }
    }
  }
}

/* Translate Prim */
def trans_prim (o: PrimOp.T, args : List[S.Expr]) : T.Expr = {
  val (let, new_args) = trans_args_list(args)
  val new_prim : T.Expr = T.Prim(o, new_args)
  let.foldRight(new_prim) {
    (let_value, acc) => {
      val (id, expr) = let_value
      T.Let (id, expr, acc)
    }
  }
}

/* Translate Call */
def trans_call(f : S.Expr, args : List[S.Expr]) : T.Expr = {
  val (let, new_args) = trans_args_list(args)
  val trans_f = trans_expr(f)
  val new_call = trans_f match {
    case T.Simple(s) =>  T.Call(s, new_args)
    case _ => {
      val let_id = fresh_id()
      val let_var = T.Var(let_id)
      T.Let(let_id, trans_f,
        T.Call (let_var, new_args))
    }
  }
  let.foldRight(new_call) {
    (let_value, acc) => {
      val (id, expr) = let_value
      T.Let(id, expr, acc)
    }
  }
}

}

/* This module implements a compiler from Fopix to Javix. */

package trac.transl

object Fopix2Javix {

import scala.collection.mutable.{Map => MutableMap}
import trac._
import trac.PrimOp._
import trac.BinOp._
import trac.fopix.{AST=>S}
import trac.javix.{AST=>T}

type Env = Map[S.Ident, T.Var]
type Funs = MutableMap[S.FunIdent, (List[S.Ident], S.Expr, T.Label)]
type Switchs = (Int, List[(Int, T.Label)])

var label_count = 0
var max_var = 0
val label_dispatch = "lab_dispatch"
val label_dispatch_no_swap = "lab_disptach_ns"
val label_error = "lab_error"

var funs : Funs = MutableMap.empty
var switchs : Switchs = (1000, List())


/******** UTILITIES ********/

def fresh_lab () = {
  val res_lab = label_count.toString
  label_count += 1
  res_lab
}


def fresh_id (env : Env) = {
  var max = -1
  env foreach {
    case (_, v) =>
      if (v >= max) {
        max = v
      }
  }
  max + 1
}


def get_switch_index() : Int = {
  switchs._1
}


def add_label_to_switch(index:Int, label:T.Label) : Unit = {
  val label_list = switchs._2 ++ List((index, label))
  switchs = (index+1, label_list)
}

def get_labels_from_switch() : List[T.Label] = {
  switchs._2.map {
    label => label._2
  }
}

def get_label_index_from_id(f: S.FunIdent) : Int = {
  val (_,_,search_label) = funs(f)
  val ids = switchs._2.filter {
    (elt) => {
      val (_, label) = elt
      search_label == label
    }
  }
  if (ids.length != 1) {
    throw new Invalid("Wrong size : " + ids.length)
  } else {
    ids.head._1
  }
}

/************************/


def assign_in_env (id: S.Ident, env : Env) : (List[T.Instruction], Env) = {
  val number = fresh_id(env)
  val instr = List(T.AStore(number))
  val newEnv = env + (id -> number)
  (instr, newEnv)
}


def get_funs (p: List[S.Definition]) {
  p match {
    case Nil => List ()
    case S.Val(_, _) :: p => get_funs(p)
    case S.Def(fid, args, e) :: p =>
      val label = fid + "_" + fresh_lab()
      val index = get_switch_index()
      add_label_to_switch(index, label)
      funs += (fid -> (args, e,  label))
      get_funs(p)
  }
}


def compile (prog_name:String,p:S.Program) : T.Program = {
  val varsize = 100
  val stacksize = 10000
  val initEnv : Env = Map.empty

  get_funs(p)

  val instrs =
    compile_definitions(initEnv, p) ++
    List(T.Return) ++
    compile_funs() ++
    compile_tableswitch()

  val reduce_instrs = reduce_instructions(instrs)

  T.Program(prog_name,reduce_instrs,varsize,stacksize)
}


def compile_funs() : List[T.Instruction]  = {
  def assign_args(args: List[S.Ident], index: Int, env: Env) : Env = {
    args match {
      case Nil => env
      case v :: vars => assign_args(vars, index + 1, env + (v -> index))
    }
  }

  var funs_instr : List[T.Instruction] = List()

  for ((_,fun_env) <- funs) {
    val (args, expr, label) = fun_env
    val env : Env = assign_args(args, 0, Map.empty)
    funs_instr =
      funs_instr ++
      List(T.Labelize(label)) ++
      compile_expr_term(env, expr, true)
  }
   funs_instr
}


def compile_definitions (env:Env,p:List[S.Definition]) : List[T.Instruction] = {
  p match {
    case Nil => List()
    case S.Val("_", e) :: p => {
      val instr = compile_expr(env, e)
      instr ++ List(T.Pop) ++ compile_definitions(env, p)
    }
    case S.Val(x,e) :: p => {
      val instr = compile_expr(env,e)
      val (assignement, newEnv) = assign_in_env(x, env)
      instr ++ assignement ++ compile_definitions(newEnv, p)
    }
    case S.Def(_,_,_) :: p => compile_definitions(env, p)
  }
}


def compile_tableswitch () : List[T.Instruction] = {
  val labels = get_labels_from_switch()

  val error_instr = List(
    T.Labelize(label_error),
    T.Push(0),
    T.IPrint,
    T.Return
  )

  val table_instr = List(
    T.Labelize (label_dispatch),
    T.Swap,
    T.Labelize(label_dispatch_no_swap),
    T.Tableswitch (1000, labels, label_error)
  )

  table_instr ++ error_instr
}


def compile_if(env:Env, e1: S.Expr, e2: S.Expr, e3: S.Expr) : List[T.Instruction] = {
  val cmp = compile_expr(env, e1)
  val v1 = compile_expr(env, e2)
  val v2 = compile_expr(env, e3)

  val lab_else = "else_" + fresh_lab()
  var lab_end = "endif_" + fresh_lab()

  cmp ++ List(T.Unbox) ++
  List(T.If(BinOp.toCmp(BinOp.Eq), lab_else)) ++ v1 ++
  List(T.Goto(lab_end), T.Labelize(lab_else)) ++ v2 ++
  List(T.Labelize(lab_end))
}


def compile_op(env : Env, o: BinOp.T, e1: S.Expr, e2: S.Expr) : List[T.Instruction] = {
  val cmp =
    if (BinOp.isArith(o)) {
      List(T.IOp(BinOp.toArith(o)))
    } else {
      val lab1 = "ifcmp_" + fresh_lab()
      var lab2 = "endifcmp_" + fresh_lab()
      List(
        T.Ificmp(BinOp.toCmp(o), lab1),
        T.Push(0),
        T.Goto(lab2),
        T.Labelize(lab1),
        T.Push(1),
        T.Labelize(lab2)
      )
    }
  val instr_1 = compile_expr(env, e1)
  val instr_2 = compile_expr(env, e2)

  instr_1 ++ List(T.Unbox) ++
  instr_2++ List(T.Unbox) ++
  cmp ++ List(T.Box)
}


def compile_let(env:Env, id:S.Ident, e1:S.Expr, e2:S.Expr) : List[T.Instruction] = {
  val (i, newEnv) = assign_in_env(id, env)
  val i1 = compile_expr(env, e1)
  val i2 = compile_expr(newEnv, e2)
  i1 ++ i ++ i2
}


def compile_prim(env:Env, p:PrimOp.T, args:List[S.Expr]) : List[T.Instruction] = {
  p match {
    case New => compile_prim_new(env, args)
    case Get => compile_prim_get(env, args)
    case Set => compile_prim_set(env, args)
    case Tuple => compile_prim_tuple(env, args)
    case Printint =>
      compile_expr(env, args.head) ++ List(T.Unbox, T.IPrint, T.Push(0), T.Box)
    case Printstr =>
      compile_expr(env, args.head) ++ List(T.Checkstring, T.SPrint, T.Push(0), T.Box)
    case Cat =>
      val e1 :: e2 :: tl = args
      compile_expr(env, e1) ++ compile_expr(env, e2) ++ List(T.Checkstring, T.SCat)
    case _ => List()
  }
}


def compile_prim_new(env : Env, args : List[S.Expr]) : List[T.Instruction] = {
  if (args.length != 1) {
    throw new Invalid("Invalid array setter exception!")
  } else {
    val instr = compile_expr(env, args(0))
    val build_instr = List (
      T.Unbox,
      T.ANewarray
    )
    instr ++ build_instr
  }
}

def compile_prim_get(env: Env, args : List[S.Expr]) : List[T.Instruction] = {
  if (args.length != 2) {
    throw new Invalid("Invalid array getter exception!")
  } else {
    val addr = compile_expr(env, args(0))
    val index = compile_expr(env, args(1))
    addr ++ List(T.Checkarray) ++ index ++ List(T.Unbox, T.AALoad)
  }
}

def compile_prim_set(env: Env, args : List[S.Expr]) : List[T.Instruction] = {
  if (args.length != 3) {
    throw new Invalid("Invalid array setter exception!")
  } else {
    val addr = compile_expr(env, args(0))
    val index = compile_expr(env, args(1))
    val value = compile_expr(env, args(2))
    addr ++ List(T.Checkarray) ++
    index ++ List(T.Unbox) ++
    value ++ List(T.AAStore, T.Push(0), T.Box)
  }
}

def build_set_for_tuple(env : Env, exprs : List[S.Expr]) : List[T.Instruction] = {
  var pos = 0
  exprs.foldLeft(List() : List[T.Instruction]) {
    (acc, expr) => {
      val instr = compile_expr(env, expr)
      val set_instr = List (
        T.Dup,
        T.Checkarray,
        T.Push(pos),
      ) ++ instr ++ List (
        T.AAStore
      )
      pos += 1
      acc ++ set_instr
    }
  }
}

def compile_prim_tuple(env : Env, args : List[S.Expr]) : List[T.Instruction] = {
  val size = args.length
  val init = List (
    T.Push(size),
    T.ANewarray
  )
  val set_instrs = build_set_for_tuple(env, args)
  init ++ set_instrs
}


def compile_exprs(env:Env, exprs:List[S.Expr]) : List[T.Instruction] = {
  exprs match {
    case Nil => List()
    case expr :: exprs =>
      compile_expr(env, expr) ++ compile_exprs(env, exprs)
  }
}


def store_args_in_heap(arg_idx : Int) : List[T.Instruction] = {
  if  (arg_idx >= 0) {
    T.AStore(arg_idx) :: store_args_in_heap(arg_idx - 1)
  } else {
    List()
  }
}


def restore_heap_from_stack(arg_idx : Int, args_size : Int) : List[T.Instruction] = {
  if (arg_idx >= args_size) {
    List()
  } else {
    T.Swap :: T.AStore(arg_idx) :: restore_heap_from_stack(arg_idx+1, args_size)
  }
}


def save_args_in_stack(arg_idx : Int) : List[T.Instruction] = {
  if (arg_idx < 0) {
    List()
  } else {
    T.ALoad(arg_idx) :: save_args_in_stack(arg_idx-1)
  }
}


def compile_call(env:Env, fid:S.FunIdent, args:List[S.Expr]) : List[T.Instruction] = {
  val (_, _, call_label) = funs(fid)
  val index = get_switch_index()
  val args_size = args.length
  val nb_to_save = env.size.min(args_size)
  val return_label = "rc_" + index.toString

  add_label_to_switch(index, return_label)

  val save_heap = save_args_in_stack(nb_to_save-1)
  val compiled_args = compile_exprs(env, args)
  val pushed_args = store_args_in_heap(args_size-1)
  val restore_heap = restore_heap_from_stack(0, nb_to_save)
  val call = List(
    T.Push(index),
    T.Goto (call_label),
    T.Labelize (return_label)
  )

  save_heap ++ compiled_args ++ pushed_args ++ call ++ restore_heap
}


def compile_call_expr(env:Env, e: S.Expr, args:List[S.Expr]) : List[T.Instruction] = {
  val index = get_switch_index()
  val args_size = args.length
  val nb_to_save = env.size.min(args_size)
  val return_label = "rc_" + index.toString

  // Sauvegarde du tas
  val save_heap = save_args_in_stack(nb_to_save-1)

  // Push du code de retour
  add_label_to_switch(index, return_label)
  val push_ret_lab = List(T.Push(index))

  // Dispatch sur la valeur calculée
  val call_instr = compile_expr(env, e) ++ List(
    T.Unbox
  )

  // Compilation et sauvegarde des arguments
  val compiled_args = compile_exprs(env, args)
  val pushed_args = store_args_in_heap(args_size-1)

  val goto_return = List (
    T.Goto(label_dispatch_no_swap),
    T.Labelize(return_label)
  )

  // Restauration du tas
  val restore_heap = restore_heap_from_stack(0, nb_to_save)

  save_heap ++ push_ret_lab ++
  call_instr ++ compiled_args ++
  pushed_args ++ goto_return ++ restore_heap
}


def compile_fun(env : Env, f: S.FunIdent) : List[T.Instruction] = {
  val index = get_label_index_from_id(f)
  List (
    T.Push(index),
    T.Box
  )
}


def compile_expr(env:Env, e:S.Expr) : List[T.Instruction] = {
  e match {
    case S.Num(n) => List(T.Push(n), T.Box)
    case S.Str(s) => List(T.Ldc(s))
    case S.Var(v) => List(T.ALoad(env(v)))
    case S.Fun(f) => compile_fun(env, f)
    case S.Op(o,e1,e2) => compile_op(env, o, e1, e2)
    case S.Let(id, e1, e2) => compile_let(env, id, e1, e2)
    case S.Prim(p, args) => compile_prim(env, p, args)
    case S.If(e1, e2, e3) => compile_if(env, e1, e2, e3)
    case S.Call(S.Fun(f), args) => compile_call(env, f, args)
    case S.Call(e, args) => compile_call_expr(env, e, args)
    case _ => List()
  }
}


/********* Clean Box ***********/

def reduce_instructions(instrs : List[T.Instruction]) : List[T.Instruction] = {
  instrs match {
    case Nil => List()
    case T.Box :: T.Unbox :: instrs => reduce_instructions(instrs)
    case instr::instrs => instr :: reduce_instructions(instrs)
  }
}

/* Ugly version of compile_expr when we wan't terminal call */
def compile_expr_term(env:Env, e:S.Expr, term:Boolean) : List[T.Instruction] = {
  e match {
    case S.Call(S.Fun(f), args) =>
      if (term) {
        compile_call_term(env, f, args)
      }
      else {
        compile_call(env, f, args)
      }
    case S.Let(id, e1, e2) =>
      val (i, newEnv) = assign_in_env(id, env)
      val i1 = compile_expr(env, e1)
      val i2 = if (term) {
        compile_expr_term(newEnv, e2, true)
      } else {
        compile_expr(newEnv, e2)
      }
      i1 ++ i ++ i2
    case S.If(e1, e2, e3) =>
      val cmp = compile_expr(env, e1)
      val (v1, v2) = if (term) {
        (compile_expr_term(env, e2, true), compile_expr_term(env, e3, true))
      } else {
        (compile_expr(env, e2), compile_expr(env, e3))
      }

      val lab_else = "else_" + fresh_lab()
      var lab_end = "endif_" + fresh_lab()

      cmp ++ List(T.Unbox) ++
      List(T.If(BinOp.toCmp(BinOp.Eq), lab_else)) ++ v1 ++
      List(T.Goto(lab_end), T.Labelize(lab_else)) ++ v2 ++
      List(T.Labelize(lab_end))
    // Others cases contains no term expressions, we go back to the original function
    case _ => compile_expr(env, e) ++ List(T.Goto (label_dispatch))
  }
}

def compile_call_term(env:Env, fid:S.FunIdent, args:List[S.Expr]) : List[T.Instruction] = {
  // Compilation et sauvegarde des arguments
  val args_size = args.length
  val compiled_args = compile_exprs(env, args)
  val pushed_args = store_args_in_heap(args_size-1)

  val (_, _, label) = funs(fid)
  val recursive_call = List(T.Goto (label))

  compiled_args ++ pushed_args ++ recursive_call
}

}

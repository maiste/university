/* This module implements a compiler from Kontix to Javix. */

package trac.transl

object Kontix2Javix {

  import trac._
  import trac.PrimOp._
  import trac.kontix.{AST => S}
  import trac.javix.{AST => T}
  import trac.javix.PP
  import scala.collection.mutable.{Map => MutableMap}

  type Env = Map[S.Ident, T.Var]
  type Funs = MutableMap[S.Ident, T.Label]
  type Switch = List[(Int, T.Label)]



  /** ****** UTILITIES *************/

  // Dev

  val todo = List(T.Comment("/!\\ TODO /!\\"))

  // Labels

  val label_dispatch = "_dispatch"
  val label_error = "_error"
  val lab_ret = "_ret"
  var lab_count = 0

  def fresh_label() = {
    val res = lab_count.toString
    lab_count += 1
    res
  }

  val goto_dispatch = List(T.Goto(label_dispatch))

  // Variables

  def fresh_id(env: Env) = {
    var max = 1 // Already v0 and v1 inside
    env foreach {
      case (_, v) =>
        if (v >= max) {
          max = v
        }
    }
    max + 1
  }

  // Functions + Continuations

  var env_fun: Funs = MutableMap.empty

  def print_funs(): Unit = {
    println("--- Fun & Cont ---")
    env_fun foreach { f =>
      val (id, _) = f
      println(s"Fun $id")
    }
    println("------------------")
  }

  // Switch

  var switch_id: Int = 1000
  var switch: Switch = List()

  def add_to_switch(lab: T.Label): Integer = {
    switch = switch ++ List((switch_id, lab))
    val switch_to_push = switch_id
    switch_id += 1
    switch_to_push
  }

  def get_labels_from_switch(): List[T.Label] = {
    switch.map(_._2)
  }

  def get_label_index_from_id(f: S.Ident): Int = {
    val search_label = env_fun(f)
    val idxs = switch.filter(_._2 == search_label)
    if (idxs.length != 1) {
      throw new Invalid("Wrong size : " + idxs.length)
    } else {
      idxs.head._1
    }
  }

  // Env

  def init_env(): Env = {
    val env: Env = Map.empty
    val env_v0 = env + ("v0" -> 0)
    val env_v1 = env_v0 + ("v1" -> 1)
    env_v1
  }

  def assign_in_env(env: Env, id: S.Ident): (List[T.Instruction], Env) = {
    val nb = fresh_id(env)
    if (id == "_") {
      (List(T.Pop), env)
    } else {
      val instr = List(T.AStore(nb))
      val new_env = env + (id -> nb)
      (instr, new_env)
    }
  }

  def print_env(env: Env): Unit = {
    println("---Env---")
    env foreach { m =>
      val (id, v) = m
      println(s"$id -> $v")
    }
    println("---------")
  }

  // Instruction

  val load_v0 = List(T.ILoad(0))
  val store_v0 = List(T.IStore(0))
  val load_v1 = List(T.ALoad(1))
  val store_v1 = List(T.AStore(1))

  def init_instr() = {
    val init_code = add_to_switch(lab_ret)
    val init_instrs =
      List(
        T.Push(init_code)
      ) ++ store_v0 ++ List(
        T.Push(0),
        T.ANewarray,
        T.AStore(1)
      )

    init_instrs
  }

  def print_instrs(i: List[T.Instruction]): Unit = {
    println("--- Instructions ---")
    i foreach { i =>
      println(" -" + PP.instr(i))
    }
    println("--------------------")
  }



  /******** Translation **********/

  def compile(progname: String, p: S.Program): T.Program = {
    val S.Program(defs, main) = p
    val init = init_instr()
    get_defs_label(defs) // Ajoute les labels
    val defs_translated = trans_defs(defs)
    val virgin_env = init_env()
    val main_translated = trans_tailexpr(virgin_env, main)

    val main_instrs =
      init ++
        main_translated
    val instrs = main_instrs ++ defs_translated ++ trans_tableswitch()
    val clean_instrs = reduce_instructions(instrs)

    val stacksize = compute_stack_size(0,0, clean_instrs)
    val varsize = local_var_size(clean_instrs) + 1

    T.Program(progname, clean_instrs, varsize, stacksize)
  }



  /*********** Definitions **********/

  def fill_def(f: S.Ident): Unit = {
    val fun_label = f + "_" + fresh_label()
    val label_index = add_to_switch(fun_label)
    env_fun += (f -> fun_label)
  }

  def get_defs_label(defs: List[S.Definition]): Unit = {
    defs match {
      case Nil => ()
      case S.DefFun(f, args, e) :: defs => {
        fill_def(f)
        get_defs_label(defs)
      }
      case S.DefCont(f, env, args, e) :: defs => {
        fill_def(f)
        get_defs_label(defs)
      }
    }
  }

  // --- DEF_FUN ---

  def assign_args(args: S.Formals, env: Env): Env = {
    args match {
      case Nil => env
      case arg :: args => {
        val (_, new_env) = assign_in_env(env, arg)
        assign_args(args, new_env)
      }
    }
  }

  def trans_deffun(
      f: S.FunName,
      args: S.Formals,
      e: S.TailExpr
  ): List[T.Instruction] = {
    val fun_label = env_fun(f)
    val new_env = assign_args(args, init_env())
    val deffun_instrs =
      T.Labelize(fun_label) :: trans_tailexpr(new_env, e)

    deffun_instrs
  }

  // --- DEF_CONT ---

  def extract_from_array(idx: Int, pos: Int): List[T.Instruction] = {
    val store: List[T.Instruction] =
      if (idx == 0) { T.Unbox :: store_v0 }
      else { List(T.AStore(idx)) }
    load_v1 ++ List(
      T.Push(pos),
      T.AALoad
    ) ++ store
  }

  def extract_args(env: S.FormalEnv, r: S.Ident): (Env, List[T.Instruction]) = {
    var cont_env = init_env()
    cont_env += (r -> 2)
    var instrs: List[T.Instruction] = List()
    var idx = 0
    for (id <- env) {
      cont_env = cont_env + (id -> (idx + 3))
      instrs =
        instrs ++
          extract_from_array(idx + 3, idx + 2)
      idx += 1
    }
    (cont_env, instrs)
  }

  def trans_defcont(
      f: S.ContName,
      env: S.FormalEnv,
      r: S.Ident,
      e: S.TailExpr
  ): List[T.Instruction] = {
    val cont_label = env_fun(f)
    val (cont_env, cont_instrs) = extract_args(env, r)
    val defcont_instrs =
      List(T.Labelize(cont_label)) ++
        extract_from_array(0, 0) ++
        cont_instrs ++
        extract_from_array(1, 1) ++
        trans_tailexpr(cont_env, e)

    defcont_instrs
  }

  // --- SWITCH ---

  def trans_tableswitch(): List[T.Instruction] = {
    val labels = get_labels_from_switch()

    val ret_instr = List(
      T.Labelize(lab_ret),
      T.Return
    )

    val error_instr = List(
      T.Labelize(label_error),
      T.Push(1),
      T.IPrint,
      T.Return
    )

    val table_instr = List(
      T.Labelize(label_dispatch),
      T.Tableswitch(1000, labels, label_error)
    )

    val table_instrs =
      table_instr ++
        ret_instr ++
        error_instr

    table_instrs
  }

  // --- DEF ---

  def trans_defs(defs: List[S.Definition]): List[T.Instruction] = {
    defs match {
      case Nil => List()
      case S.DefFun(f, args, e) :: defs =>
        trans_deffun(f, args, e) ++
          trans_defs(defs)
      case S.DefCont(f, env, args, e) :: defs =>
        trans_defcont(f, env, args, e) ++
          trans_defs(defs)
    }
  }



  /** ******** TailExpr *********/

  // --- RET ---

  def trans_ret(env: Env, be: S.BasicExpr): List[T.Instruction] = {
    val instr_be = trans_basicexpr(env, be)
    val instr_store_be = List(T.AStore(2))
    val ret_instrs = load_v0 ++ instr_be ++ instr_store_be ++ goto_dispatch

    ret_instrs
  }

  // --- LET ---

  def trans_let(
      env: Env,
      id: S.Ident,
      be1: S.BasicExpr,
      e2: S.TailExpr
  ): List[T.Instruction] = {
    val i1 = trans_basicexpr(env, be1)
    val (i, new_env) = assign_in_env(env, id)
    val i2 = trans_tailexpr(new_env, e2)
    val let_instrs = i1 ++ i ++ i2

    let_instrs
  }

  // --- IF ---

  def trans_if(
      env: Env,
      c: S.Comparison,
      e1: S.TailExpr,
      e2: S.TailExpr
  ): List[T.Instruction] = {
    val (op, bc1, bc2) = c
    val be1 = trans_basicexpr(env, bc1) ++ List(T.Unbox)
    val be2 = trans_basicexpr(env, bc2) ++ List(T.Unbox)
    val te1 = trans_tailexpr(env, e1)
    val te2 = trans_tailexpr(env, e2)

    val lab_else = "else_" + fresh_label()

    val if_instrs = be1 ++ be2 ++ List(
      T.Ificmp(CompOp.neg(op), lab_else)
    ) ++ te1 ++ List(
      T.Labelize(lab_else)
    ) ++ te2

    if_instrs
  }

  // --- CALL ---

  def compute_args(env: Env, args: List[S.BasicExpr]): List[T.Instruction] = {
    args match {
      case Nil => List()
      case arg :: args =>
        trans_basicexpr(env, arg) ++ compute_args(env, args)
    }
  }

  def store_args(idx: Int): List[T.Instruction] = {
    if (idx > 1) {
      T.AStore(idx) :: store_args(idx - 1)
    } else {
      List()
    }
  }

  def trans_funcall(
      env: Env,
      f: S.FunName,
      args: List[S.BasicExpr]
  ): List[T.Instruction] = {
    val call_label = env_fun(f)
    val args_computed = compute_args(env, args)
    val store_computed = store_args(args.size + 1) // -1 size + 2 v0, v1

    val call_instrs =
      args_computed ++
        store_computed ++
        List(T.Goto(call_label))

    call_instrs
  }

  def trans_indirect_call(
      env: Env,
      e: S.BasicExpr,
      args: List[S.BasicExpr]
  ): List[T.Instruction] = {
    val call_computed = trans_basicexpr(env, e)
    val args_computed = compute_args(env, args)
    val store_computed = store_args(args.size + 1) // -1 size + 2 v0, v1
    val call_instrs = call_computed ++
      List(T.Unbox) ++
      args_computed ++
      store_computed ++
      List(T.Goto(label_dispatch))

    call_instrs
  }

  // --- PUSHCONT ---

  def store_in_array(env: Env, v: S.Ident, pos: Int): List[T.Instruction] = {
    val get_v =
      if (v == "v0") { load_v0 ++ List(T.Box) }
      else { List(T.ALoad(env(v))) }
    List(
      T.Dup,
      T.Push(pos)
    ) ++ get_v ++
      List(
        T.AAStore
      )
  }

  def store_all_vars(env: Env, saves: List[S.Ident]): List[T.Instruction] = {
    var instrs: List[T.Instruction] =
      store_in_array(env, "v0", 0) ++
        store_in_array(env, "v1", 1)
    var idx = 2
    for (id <- saves) {
      instrs =
        instrs ++ store_in_array(env, id, idx)
      idx += 1
    }
    instrs
  }

  def trans_pushcont(
      env: Env,
      c: S.ContName,
      saves: List[S.Ident],
      e: S.TailExpr
  ): List[T.Instruction] = {
    val label_cont = get_label_index_from_id(c)
    val push_instrs =
      List(
        T.Push(saves.length + 3), // v2 servira à stocker la valeur de retour
        T.ANewarray
      ) ++
        store_all_vars(env, saves) ++
        store_v1 ++
        List(T.Push(label_cont)) ++
        store_v0 ++
        trans_tailexpr(env, e)

    push_instrs
  }

  // --- TAILEXPR ---

  def trans_tailexpr(env: Env, e: S.TailExpr): List[T.Instruction] = {
    e match {
      case S.Let(id, be1, e2)       => trans_let(env, id, be1, e2)
      case S.If(c, e1, e2)          => trans_if(env, c, e1, e2)
      case S.Call(S.Fun(fid), args) => trans_funcall(env, fid, args)
      case S.Call(be, args)         => trans_indirect_call(env, be, args)
      case S.Ret(be)                => trans_ret(env, be)
      case S.PushCont(c, saves, e)  => trans_pushcont(env, c, saves, e)
    }
  }




  /******** Basic Expr *********/

  // --- BLET ---

  def trans_blet(
      env: Env,
      id: S.Ident,
      be1: S.BasicExpr,
      be2: S.BasicExpr
  ): List[T.Instruction] = {
    val i1 = trans_basicexpr(env, be1)
    val (i, new_env) = assign_in_env(env, id)
    val i2 = trans_basicexpr(new_env, be2)
    val blet_instrs = i1 ++ i ++ i2

    blet_instrs
  }

  // --- BIF ---

  def trans_bif(
      env: Env,
      c: S.Comparison,
      be1: S.BasicExpr,
      be2: S.BasicExpr
  ): List[T.Instruction] = {
    val (op, bc1, bc2) = c
    val b1 = trans_basicexpr(env, bc1) ++ List(T.Unbox)
    val b2 = trans_basicexpr(env, bc2) ++ List(T.Unbox)
    val body1 = trans_basicexpr(env, be1)
    val body2 = trans_basicexpr(env, be2)

    val lab_else = "else_b" + fresh_label()
    var lab_end = "endbif_" + fresh_label()

    val body = List(
      T.Ificmp(CompOp.neg(op), lab_else)
    ) ++ body1 ++ List(
      T.Goto(lab_end),
      T.Labelize(lab_else)
    ) ++ body2 ++ List(
      T.Labelize(lab_end)
    )

    val bif_instrs = b1 ++ b2 ++ body

    bif_instrs
  }

  // --- FUN ---

  def trans_fun(fid: S.FunName): List[T.Instruction] = {
    val index = get_label_index_from_id(fid)
    val fun_instrs =
      List(
        T.Push(index),
        T.Box
      )

    fun_instrs
  }

  // --- OP ---

  def trans_op(
      env: Env,
      o: IntOp.T,
      be1: S.BasicExpr,
      be2: S.BasicExpr
  ): List[T.Instruction] = {
    val op_instrs =
      trans_basicexpr(env, be1) ++
        List(T.Unbox) ++
        trans_basicexpr(env, be2) ++
        List(T.Unbox) ++
        List(T.IOp(o), T.Box)

    op_instrs
  }

  // --- PRIM ---

  def trans_prim_new(env: Env, args: List[S.BasicExpr]): List[T.Instruction] = {
    if (args.length != 1) {
      throw new Invalid("Invalid array setter exception!")
    } else {
      val instr = trans_basicexpr(env, args(0))
      val build_instr = List(
        T.Unbox,
        T.ANewarray
      )
      val new_instrs =
        instr ++ build_instr

      new_instrs
    }
  }

  def trans_prim_get(env: Env, args: List[S.BasicExpr]): List[T.Instruction] = {
    if (args.length != 2) {
      throw new Invalid("Invalid array getter exception!")
    } else {
      val addr = trans_basicexpr(env, args(0))
      val index = trans_basicexpr(env, args(1))
      val get_instrs =
        addr ++ List(T.Checkarray) ++ index ++ List(T.Unbox, T.AALoad)

      get_instrs
    }
  }

  def trans_prim_set(env: Env, args: List[S.BasicExpr]): List[T.Instruction] = {
    if (args.length != 3) {
      throw new Invalid("Invalid array setter exception!")
    } else {
      val addr = trans_basicexpr(env, args(0))
      val index = trans_basicexpr(env, args(1))
      val value = trans_basicexpr(env, args(2))
      val set_instrs =
        addr ++ List(T.Checkarray) ++
          index ++ List(T.Unbox) ++
          value ++ List(T.AAStore, T.Push(0), T.Box)

      set_instrs
    }
  }

  def build_set_for_tuple(
      env: Env,
      exprs: List[S.BasicExpr]
  ): List[T.Instruction] = {
    var pos = 0
    exprs.foldLeft(List(): List[T.Instruction]) { (acc, expr) =>
      {
        val instr = trans_basicexpr(env, expr)
        val set_instr = List(
          T.Dup,
          T.Checkarray,
          T.Push(pos)
        ) ++ instr ++ List(
          T.AAStore
        )
        pos += 1
        acc ++ set_instr
      }
    }
  }

  def trans_prim_tuple(
      env: Env,
      args: List[S.BasicExpr]
  ): List[T.Instruction] = {
    val size = args.length
    val init = List(
      T.Push(size),
      T.ANewarray
    )
    val tuple_instrs = init ++ build_set_for_tuple(env, args)

    tuple_instrs
  }

  def trans_prim(
      env: Env,
      p: PrimOp.T,
      args: List[S.BasicExpr]
  ): List[T.Instruction] = {
    p match {
      case New   => trans_prim_new(env, args)
      case Get   => trans_prim_get(env, args)
      case Set   => trans_prim_set(env, args)
      case Tuple => trans_prim_tuple(env, args)
      case Printint =>
        trans_basicexpr(env, args.head) ++ List(
          T.Unbox,
          T.IPrint,
          T.Push(0),
          T.Box
        )
      case Printstr =>
        trans_basicexpr(env, args.head) ++ List(
          T.Checkstring,
          T.SPrint,
          T.Push(0),
          T.Box
        )
      case Cat =>
        val e1 :: e2 :: tl = args
        val cat_instrs =
          trans_basicexpr(env, e1) ++
            List(T.Checkstring) ++
            trans_basicexpr(env, e2) ++
            List(T.Checkstring, T.SCat)

        cat_instrs
    }
  }

  // --- BASICEXPR ---

  def trans_basicexpr(env: Env, e: S.BasicExpr): List[T.Instruction] = {
    e match {
      case S.Num(n)             => List(T.Push(n), T.Box)
      case S.Str(s)             => List(T.Ldc(s))
      case S.Var(id)            => List(T.ALoad(env(id)))
      case S.Fun(fid)           => trans_fun(fid)
      case S.BLet(id, be1, be2) => trans_blet(env, id, be1, be2)
      case S.BIf(c, be1, be2)   => trans_bif(env, c, be1, be2)
      case S.Op(o, be1, be2)    => trans_op(env, o, be1, be2)
      case S.Prim(p, args)      => trans_prim(env, p, args)
    }
  }

  /******** Max var *********/

  def local_var_size(instrs: List[T.Instruction], max: Int = 2): Int = {
    instrs match {
      case T.AStore(v) :: instrs => {
        val local_max =
          if (v > max) {
            v
          } else {
            max
          }
        local_var_size(instrs, local_max)
      }
      case T.IStore(v) :: instrs => {
        val local_max =
          if (v > max) {
            v
          } else {
            max
          }
        local_var_size(instrs, local_max)
      }
      case _ :: instrs => local_var_size(instrs, max)
      case Nil         => max
    }
  }



  /******** Remove Box unused *********/

  def reduce_instructions(instrs : List[T.Instruction]) : List[T.Instruction] = {
    instrs match {
      case Nil => List()
      case T.Box :: T.Unbox :: instrs => reduce_instructions(instrs)
      case T.Unbox :: T.Box :: instrs => reduce_instructions(instrs)
      case T.ALoad(v1) :: T.AStore(v2) :: instrs =>
        if (v1 == v2) {
          reduce_instructions(instrs)
        } else {
          T.ALoad(v1) :: T.AStore(v2) :: reduce_instructions(instrs)
        }
      case instr::instrs => instr :: reduce_instructions(instrs)
    }
  }




  /********** Stack size ************/
  val end_pattern = "endif_*".r

  def compute_nc(
    instr : T.Instruction,
    current : Int,
    d : Boolean = false
  ) : Int = {
    val T.StackInfo(_, max, delta) = T.stackUse(instr)
    if (d) {
      current + delta
    } else {
      current + max
    }
  }

  def is_endif(lab :T.Label) : Boolean = {
    end_pattern.findFirstIn(lab).isDefined
  }

  def compute_stack_size(
    c : Int = 0,
    m : Int = 0,
    instrs : List[T.Instruction]
  ) : Int = {
    instrs match {
      case Nil => m
      case i :: is => {
      i match {
        case T.Comment(_) | T.Return | T.Labelize(_) => compute_stack_size(c, m, is)
               case T.Box |
             T.Unbox  |
             T.Checkarray |
             T.Checkstring |
             T.ANewarray =>
          val nc = compute_nc(i, c)
          compute_stack_size(nc, nc.max(m), is)
        case T.Push(_) |
             T.Ldc(_) |
             T.Pop |
             T.Swap |
             T.Dup |
             T.SCat |
             T.IOp(_) |
             T.IStore(_) |
             T.AStore(_) |
             T.ALoad(_) |
             T.ILoad(_) |
             T.AAStore |
             T.AALoad |
             T.Tableswitch(_,_,_) =>
          val nc = compute_nc(i, c, true)
          compute_stack_size(nc, nc.max(m), is)
        case T.IPrint | T.SPrint =>
          val nc = compute_nc(i, c, true)
          val nm = (c+1).max(m)
          compute_stack_size(nc, nm, is)
        case T.Ificmp(_,_) | T.If(_,_)=>
          val nc = compute_nc(i, c, true)
          compute_stack_size(nc, nc.max(m), is)
        case T.Goto(l) =>
          if (is_endif(l)) {
            val nc = c - 1
            compute_stack_size(nc, nc.max(m), is)
          } else {
            compute_stack_size(0, m, is)
          }
      }}
    }
  }
}

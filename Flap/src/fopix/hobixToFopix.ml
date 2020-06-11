(** This module implements a compiler from Hobix to Fopix. *)

(** As in any module that implements {!Compilers.Compiler}, the source
    language and the target language must be specified. *)

module Source = Hobix
module S = Source.AST
module Target = Fopix
module T = Target.AST

(**

   The translation from Hobix to Fopix turns anonymous
   lambda-abstractions into toplevel functions and applications into
   function calls. In other words, it translates a high-level language
   (like OCaml) into a first order language (like C).

   To do so, we follow the closure conversion technique.

   The idea is to make explicit the construction of closures, which
   represent functions as first-class objects. A closure is a block
   that contains a code pointer to a toplevel function [f] followed by all
   the values needed to execute the body of [f]. For instance, consider
   the following OCaml code:

   let f =
     let x = 6 * 7 in
     let z = x + 1 in
     fun y -> x + y * z

   The values needed to execute the function "fun y -> x + y * z" are
   its free variables "x" and "z". The same program with explicit usage
   of closure can be written like this:

   let g y env = env[1] + y * env[2]
   let f =
      let x = 6 * 7 in
      let z = x + 1 in
      [| g; x; z |]

   (in an imaginary OCaml in which arrays are untyped.)

   Once closures are explicited, there are no more anonymous functions!

   But, wait, how to we call such a function? Let us see that on an
   example:

   let f = ... (* As in the previous example *)
   let u = f 0

   The application "f 0" must be turned into an expression in which
   "f" is a closure and the call to "f" is replaced to a call to "g"
   with the proper arguments. The argument "y" of "g" is known from
   the application: it is "0". Now, where is "env"? Easy! It is the
   closure itself! We get:

   let g y env = env[1] + y * env[2]
   let f =
      let x = 6 * 7 in
      let z = x + 1 in
      [| g; x; z |]
   let u = f[0] 0 f

   (Remark: Did you notice that this form of "auto-application" is
   very similar to the way "this" is defined in object-oriented
   programming languages?)

*)

(**
   Helpers functions.
*)

let error pos msg =
  Error.error "compilation" pos msg

let make_fresh_variable =
  let r = ref 0 in
  fun () -> incr r; T.Id (Printf.sprintf "_%d" !r)

let make_fresh_function_identifier =
  let r = ref 0 in
  fun () -> incr r; T.FunId (Printf.sprintf "_%d" !r)

let define e f =
  let x = make_fresh_variable () in
  T.Define (x, e, f x)

let rec defines ds e =
  match ds with
    | [] ->
      e
    | (x, d) :: ds ->
      T.Define (x, d, defines ds e)

let seq a b =
  define a (fun _ -> b)

let rec seqs = function
  | [] -> assert false
  | [x] -> x
  | x :: xs -> seq x (seqs xs)

let allocate_block e =
  T.(FunCall (FunId "allocate_block", [e]))

let write_block e i v =
  T.(FunCall (FunId "write_block", [e; i; v]))

let read_block e i =
  T.(FunCall (FunId "read_block", [e; i]))

let lint i =
  T.(Literal (LInt (Int64.of_int i)))

let predefined =
  [ "print_string"
  ; "equal_string"
  ; "equal_char"
  ; "observe_int"
  ; "print_int" ]

let is_predefined (S.Id op) =
  FopixInterpreter.is_binary_primitive op ||
    List.mem op predefined

(** [id] must be predifined *)
let arity_of_predifined (S.Id id) =
  match id with
  | "print_string" | "print_int" | "observe_int" -> 1
  | "equal_char" | "equal_string" -> 2
  | s when FopixInterpreter.is_binary_primitive s -> 2
  | _ -> assert false


(** [free_variables e] returns the list of free variables that
     occur in [e].*)
let free_variables =
  let module M =
    Set.Make (struct type t = S.identifier let compare = compare end)
  in
  let rec unions f = function
    | [] -> M.empty
    | [s] -> f s
    | s :: xs -> M.union (f s) (unions f xs)
  in
  let rec fvs = function
    | S.Literal l ->
       M.empty
    | S.Variable x ->
        if is_predefined x then M.empty
        else M.singleton x
    | S.While (cond, e) ->
       M.union (fvs cond) (fvs e)
    | S.Define (vd, a) ->
       let xs =
         match vd with
         | SimpleValue (i,e) -> [(i,e)]
         | RecFunctions xs -> xs in
       let ids,exprs = List.split xs in
       M.diff (unions fvs (a::exprs)) (M.of_list ids)
    | S.ReadBlock (a, b) ->
       unions fvs [a; b]
    | S.Apply (a, b) ->
       unions fvs (a :: b)
    | S.WriteBlock (a, b, c) | S.IfThenElse (a, b, c) ->
       unions fvs [a; b; c]
    | S.AllocateBlock a ->
       fvs a
    | S.Fun (xs, e) ->
       M.diff (fvs e) (M.of_list xs)
    | S.Switch (a, b, c) ->
       let c = match c with None -> [] | Some c -> [c] in
       unions fvs (a :: ExtStd.Array.present_to_list b @ c)
  in
  fun e -> M.elements (fvs e)

(**

    A closure compilation environment relates an identifier to the way
    it is accessed in the compiled version of the function's
    body.

    Indeed, consider the following example. Imagine that the following
    function is to be compiled:

    fun x -> x + y

    In that case, the closure compilation environment will contain:

    x -> x
    y -> "the code that extract the value of y from the closure environment"

    Indeed, "x" is a local variable that can be accessed directly in
    the compiled version of this function's body whereas "y" is a free
    variable whose value must be retrieved from the closure's
    environment.

*)
type environment = {
    vars : (HobixAST.identifier, FopixAST.expression) Dict.t;
    externals : (HobixAST.identifier, int) Dict.t;
}

let initial_environment () =
  { vars = Dict.empty; externals = Dict.empty }

let bind_external id n env =
  { env with externals = Dict.insert id n env.externals }

let bind_vars id e env =
  { env with vars = Dict.insert id e env.vars }

let is_external id env =
  Dict.lookup id env.externals <> None

let reset_vars env =
   { env with vars = Dict.empty }

(** Precondition: [is_external id env = true]. *)
let arity_of_external id env =
  match Dict.lookup id env.externals with
    | Some n -> n
    | None -> assert false (* By is_external. *)

let multiple_defs xs e =
  let aux x (xs,acc) =
    match x with
    | T.DefineValue (id,e1) ->
       xs,T.Define (id,e1,acc)
    | x ->
       x::xs,acc in
  List.fold_right aux xs ([],e)

let init_list size =
  let rec aux k =
    if k >= size then []
    else S.Id ("_" ^ (string_of_int k))::(aux (k+1))
  in aux 0

let replace_closure id arity =
    let args = init_list arity in
    let call_args = List.map (fun x -> S.Variable x) args in
    S.Fun (args, S.Apply (S.Variable id , call_args))

let filter_predifined_args e =
  match e with
  | S.Variable id when is_predefined id ->
    let arity = arity_of_predifined id in
    replace_closure id arity
  | _ -> e

let filter_predifined_apply a bs =
  match a with
  | S.Variable id when is_predefined id && List.length bs < arity_of_predifined id ->
     replace_closure id (arity_of_predifined id)
  | _ -> a

let eta_expanse env p=
  let rec program env defs =
    let _, defs =
      List.fold_left (
        fun (env, defs) def ->
          let env, def = definition env def in
          env, def::defs
      ) (env, []) defs in
    defs |> List.rev

  and definition env = function
    | S.DeclareExtern (id, n) as d ->
        bind_external id n env, d
    | S.DefineValue vd ->
       env, S.DefineValue (value_definition env vd)

  and value_definition env = function
    | S.SimpleValue (x, e) ->
       let e = expression env e in
       S.SimpleValue (x, e)
    | S.RecFunctions fdefs ->
        let fdefs = List.map (
          fun (id,e) -> (id, expression env e)
       ) fdefs
        in
      S.RecFunctions fdefs

  and expression env =
    let exp x = expression env x in
    function
    | S.Literal l as lit -> lit
    | S.While (cond, e) ->
       S.While(exp cond,exp e)
    | S.Variable (S.Id id as x) as v ->
        if is_external x env then
          let arity = arity_of_external x env in
          replace_closure x arity
        else v
    | S.Define (vdef, a) ->
        let vdef = value_definition env vdef in
        S.Define (vdef,exp a)
    | S.Apply (a, bs) ->
        let a = filter_predifined_apply (exp a) bs in
        let bs = List.map filter_predifined_args bs in
        let bs = List.map exp bs in
        S.Apply (a, bs)
    | S.IfThenElse (a, b, c) ->
       S.IfThenElse (exp a,exp b,exp c)
    | S.Fun (args, body) ->
       S.Fun (args, exp body)
    | S.AllocateBlock a ->
       S.AllocateBlock (exp a)
    | S.WriteBlock (a, b, c) ->
       S.WriteBlock (exp a, exp b, exp c)
    | S.ReadBlock (a, b) ->
       S.ReadBlock (exp a, exp b)
    | S.Switch (a, bs, default) ->
       let bs = Array.map (ExtStd.Option.map exp) bs in
       let default = ExtStd.Option.map exp default in
       S.Switch (exp a, bs, default)
  in program env p

(** [translate p env] turns an Hobix program [p] into a Fopix program
    using [env] to retrieve contextual information. *)
let translate (p : S.t) env =
  let rec program env defs =
    let defs = eta_expanse env defs in
    let env, defs = ExtStd.List.foldmap definition env defs in
    (List.flatten defs, env)

  and definition env = function
    | S.DeclareExtern (id, n) ->
       let env = bind_external id n env in
       (env, [T.ExternalFunction (function_identifier id, n)])
    | S.DefineValue vd ->
       (env, value_definition true env vd)

  and value_definition reset env = function
    | S.SimpleValue (x, e) ->
       let fs, e = expression (if reset then reset_vars env else env) e in
       fs @ [T.DefineValue (identifier x, e)]
    | S.RecFunctions fdefs ->
       let fs, defs = define_recursive_functions env fdefs in
       fs @ List.map (fun (x, e) -> T.DefineValue (x, e)) defs

  and define_recursive_functions env rdefs =
    let names = List.map fst rdefs in
    let strip_out_fun = function
      | S.Fun (args,body) ->
         free_variables (S.Fun ((args@names), body)),args,body
      | _ -> assert false in
    let rdefs =
      List.map
        (fun (name,body) -> name,strip_out_fun body) rdefs in
    match rdefs with
    | [name,(fv,args,body)] ->
       let defs,expr = anonymous_fun ~name env fv args body in
       defs,[identifier name,expr]
    | _ ->
       let mk_closure_name (S.Id x) =  T.Id ("_"^x) in
       let additional = List.length rdefs - 1 in
       let fs,rdefs =
         List.split @@
           List.map
             (fun (name,(fv,a,b)) ->
               let cname = mk_closure_name name in
               T.DefineValue (cname, create_closure ~additional fv), ((name,cname),(fv,a,b)))
             rdefs in
       let names = List.map fst rdefs in
       let translated =
         List.map
           (fun ((name,nname),(fv,args,body)) ->
             let names = List.filter (fun (x,_) -> x != name) names in
             let defs, expr = anonymous_fun ~name ~block:(nname,names) env fv args body in
             defs, (identifier name,expr))
           rdefs in
       let fs',exprs = List.split translated in
       fs @ List.concat fs', exprs

  and expression env = function
    | S.Literal l ->
      [], T.Literal (literal l)
    | S.While (cond, e) ->
       let cfs, cond = expression env cond in
       let efs, e = expression env e in
       cfs @ efs, T.While (cond, e)
    | S.Variable x ->
      let xc =
        match Dict.lookup x env.vars with
          | None -> T.Variable (identifier x)
          | Some e -> e
      in
      ([], xc)
    | S.Define (vdef, a) ->
       local_definition env vdef a
    | S.Apply (a, bs) ->
       apply env a bs
    | S.IfThenElse (a, b, c) ->
      let afs, a = expression env a in
      let bfs, b = expression env b in
      let cfs, c = expression env c in
      afs @ bfs @ cfs, T.IfThenElse (a, b, c)
    | S.Fun (args, body) as f ->
        anonymous_fun env (free_variables f) args body
    | S.AllocateBlock a ->
      let afs, a = expression env a in
      (afs, allocate_block a)
    | S.WriteBlock (a, b, c) ->
      let afs, a = expression env a in
      let bfs, b = expression env b in
      let cfs, c = expression env c in
      afs @ bfs @ cfs,
      T.FunCall (T.FunId "write_block", [a; b; c])
    | S.ReadBlock (a, b) ->
      let afs, a = expression env a in
      let bfs, b = expression env b in
      afs @ bfs,
      T.FunCall (T.FunId "read_block", [a; b])
    | S.Switch (a, bs, default) ->
      let afs, a = expression env a in
      let bsfs, bs =
        ExtStd.List.foldmap (fun bs t ->
                    match ExtStd.Option.map (expression env) t with
                    | None -> (bs, None)
                    | Some (bs', t') -> (bs @ bs', Some t')
                  ) [] (Array.to_list bs)
      in
      let dfs, default = match default with
        | None -> [], None
        | Some e -> let bs, e = expression env e in bs, Some e
      in
      afs @ bsfs @ dfs,
      T.Switch (a, Array.of_list bs, default)

  and literal = function
    | S.LInt x -> T.LInt x
    | S.LString s -> T.LString s
    | S.LChar c -> T.LChar c

  and identifier (S.Id x) = T.Id x

  and function_identifier (S.Id x) = T.FunId x

  and apply env func args =
    let args = List.map (expression env) args in
    let defs',args = List.split args in
    let defs,call =
      match func with
      | S.Variable x when (is_external x env || is_predefined x) ->
         [],T.FunCall (function_identifier x, args)
      | _ ->
         let defs, func = expression env func in
         let func_name,with_func =
           match func with
           | T.Variable x ->
              x, (fun x -> x)
           | _ ->
              let func_name = T.Id "__func" in
              func_name, (fun x -> T.Define (func_name,func,x)) in
         let get_pointer = read_block (T.Variable func_name) (lint 0) in
         defs,with_func (T.UnknownFunCall (get_pointer, (T.Variable func_name)::args))
    in
    let defs = defs @ List.concat defs' in
    defs,call

  and oldenv_id = T.Id "__oldenv"
  and oldenv_var = T.Variable (oldenv_id)

  and env_id = T.Id "__env"
  and env_var = T.Variable (env_id)

  and write_closure fid env env_var fv recfuns =
    let fun_block = write_block env_var (lint 0) (T.Literal (T.LFun fid)) in
    let vars, env, k =
      List.fold_left (
          fun (instrs, (env : environment), k) id ->
          let newenv = bind_vars id (read_block oldenv_var (lint k)) env in
          let tid = T.Variable (identifier id) in
          let loc =
            match Dict.lookup id env.vars with
            | None -> tid
            | Some v -> v in
          let instr = write_block env_var (lint k) loc in
          (instr::instrs, newenv, k+1)
        ) ([], env, 1) fv in
    let vars', env, k =
      List.fold_left (
          fun (instrs, (env : environment), k) (id,tid) ->
          let newenv = bind_vars id (read_block oldenv_var (lint k)) env in
          let instr = write_block env_var (lint k) (T.Variable tid) in
          (instr::instrs, newenv, k+1)
      ) ([], env, k) recfuns in
    List.rev (env_var::fun_block::vars@vars') |> seqs, env

  and create_closure ?(additional=0) fv =
    let size = additional + List.length fv + 1 in
    allocate_block (lint size)

  and define_closure name block fid fv env =
    let env =
      match name with
      | None -> env
      | Some name ->
         bind_vars name oldenv_var env in
    match block with
    | None ->
       let init_block = create_closure fv in
       let blocks, env = write_closure fid env env_var fv [] in
       T.Define (env_id, init_block, blocks), env
    | Some (block,recfuns) ->
       write_closure fid env (T.Variable block) fv recfuns

  and define_func env fid args body =
    let defs, body = expression env body in
    let defs,body = multiple_defs defs body in
    defs@[T.DefineFunction (fid, oldenv_id::args, body)]

  and anonymous_fun ?name ?block env fv args body =
    let fid = make_fresh_function_identifier () in
    let args = List.map identifier args in
    let closure, env = define_closure name block fid fv env in
    let defs = define_func env fid args body in
    defs, closure

  and local_definition env vdef e =
    let ys, e = expression env e in
    match vdef with
    | S.SimpleValue (x, a) ->
      let xs, a = expression env a in
      xs@ys ,T.Define (identifier x, a, e)
    | S.RecFunctions fdefs ->
      let fs, defs = define_recursive_functions env fdefs in
      ys @ fs, defines defs e
  in
  program env p

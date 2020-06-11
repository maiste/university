(** A scope-checker for Retrolix programs. *)

open RetrolixAST

(** {2 Errors} *)

let type_error ?(loc = Position.dummy) message =
  Error.error "typechecking" loc message

let unknown_variable (Id xn) =
  type_error ("Unbound variable " ^ xn)

let unknown_function (FId fn) =
  type_error ("Unbound function " ^ fn)

let unknown_label (Label ln) =
  type_error ("Unbound label " ^ ln)

let duplicate_global (Id xn) =
  type_error ("Global variable " ^ xn ^ " has been declared twice")

let duplicate_function (FId fn) =
  type_error ("Function " ^ fn ^ " has been declared twice")

let duplicate_label (Label ln) =
  type_error ("Label " ^ ln ^ " has been declared twice")

(** {2 Runtime functions} *)

let runtime_funs =
  List.map
    (fun fn -> FId fn)
    [
      "allocate_block";
      "read_block";
      "write_block";
      "equal_string";
      "equal_char";
      "observe_int"; "print_int";
      "print_string";
      "add_eight_int";
    ]

(** {2 Environments} *)

type typing_environment =
  {
    variables : IdSet.t;
    functions : FIdSet.t;
    labels : LabelSet.t
  }

let print_typing_environment _ =
  ""

let initial_typing_environment () =
  {
    variables = IdSet.empty;
    functions = FIdSet.of_list runtime_funs;
    labels = LabelSet.empty;
  }

let var_is_declared env x =
  IdSet.mem x env.variables

let fun_is_declared env f =
  FIdSet.mem f env.functions

let label_is_declared env l =
  LabelSet.mem l env.labels

let declare_var env x =
  { env with variables = IdSet.add x env.variables; }

let declare_vars ?on_shadowing env xs =
  let f =
    match on_shadowing with
    | None -> fun _ _ -> ()
    | Some f -> fun env x -> if var_is_declared env x then f x
  in
  let declare_enrich_var env x = f env x; declare_var env x in
  List.fold_left declare_enrich_var env xs

let define_label env l =
  if label_is_declared env l then duplicate_label l;
  { env with labels = LabelSet.add l env.labels; }

let define_labels env body =
  List.fold_left (fun env (l, _) -> define_label env l) env body

let declare_fun env f =
  { env with functions = FIdSet.add f env.functions; }

let with_labels ~base ~labelled =
  { base with labels = labelled.labels; }

(** {2 Type-checking} *)

let typecheck_literal env lit =
  match lit with
  | LInt _ | LChar _ | LString _ ->
     ()
  | LFun f ->
     if not (fun_is_declared env f) then unknown_function f

let typecheck_lvalue env (lv : lvalue) =
  match lv with
  | `Variable x ->
     if not (var_is_declared env x) then unknown_variable x
  | `Register _ ->
     ()

let typecheck_rvalue env (rv : rvalue) =
  match rv with
  | `Immediate lit ->
     typecheck_literal env lit
  | #lvalue as lv ->
     typecheck_lvalue env lv

let typecheck_rvalues env =
  List.iter (typecheck_rvalue env)

let typecheck_label env l =
  if not (label_is_declared env l) then unknown_label l

let typecheck_labels env =
  List.iter (typecheck_label env)

let typecheck_instruction env ins =
  match ins with
  | Call (rv1, rvs, _) ->
     typecheck_rvalues env (rv1 :: rvs)
  | Ret | Comment _ | Exit ->
     ()
  | Assign (lv, _, rvs) ->
     typecheck_lvalue env lv;
     typecheck_rvalues env rvs
  | Jump l ->
     typecheck_label env l
  | ConditionalJump (_, rvs, l1, l2) ->
     typecheck_rvalues env rvs;
     typecheck_label env l1;
     typecheck_label env l2
  | Switch (rv, ls, lo) ->
     typecheck_rvalue env rv;
     Array.iter (typecheck_label env) ls;
     ExtStd.Option.iter (typecheck_label env) lo

let typecheck_labelled_instruction env (_, ins) =
  typecheck_instruction env ins

let typecheck_block env (locals, body) =
  let env = declare_vars env locals in
  let env = define_labels env body in
  List.iter (typecheck_labelled_instruction env) body;
  env

let enrich_env_with_def env def =
  match def with
  | DValues (globals, _) ->
     declare_vars ~on_shadowing:duplicate_global env globals
  | DFunction (f, _, _) | DExternalFunction f ->
     if fun_is_declared env f then duplicate_function f;
     declare_fun env f

let typecheck_def env def =
  let env' =
    match def with
    | DValues (_, block) ->
       typecheck_block env block
    | DFunction (_, params, block) ->
       let env = declare_vars env params in
       typecheck_block env block
    | DExternalFunction _ ->
       env
  in
  with_labels ~base:env ~labelled:env'

let typecheck env ast =
  let env = List.fold_left enrich_env_with_def env ast in
  List.fold_left typecheck_def env ast

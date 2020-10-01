open Ast
open Types
open Position


let check_expected_type pos ty ty' =
  if ty<>ty' then
    Error.type_error ty' pos (
      "Expected: " ^ ty ^ ", given: " ^ ty'
    )

let check_args_type pos ty ty' =
  if ty<>ty' then
    Error.type_error ty pos (
      ty' ^ " does not match argument type: " ^ ty
    )

let check_bounds index arity name v =
  if index < 0 || index > arity then
    Error.type_error v.value v.pos (
      v.value ^ " is out of bounds of " ^ name.value ^ " array, it should be inside [0.." ^ (string_of_int arity) ^"]"
    )

let check_comparaison_type pos ty ty' =
  if ty<>ty' then
    Error.type_error ty' pos (
      "You can't compare " ^ ty ^ " with " ^ ty'
    )

let check_atomicity (pos: Lexing.position) id b1 b2 =
  if b1 && b2 then
  Printf.printf "Warning: the %s is not atomic, on line: %d\n"  (id) (pos.pos_lnum)

let check_atomicity_no_pos id b1 b2 =
  if b1 && b2 then
  Printf.printf "Warning: one %s is not atomic\n"  id

let rec already_exist env = function
  | v::vs ->
    begin
      try
      (* if lookup doesnt raise exception, that means the value exist *)
        let _ = lookup_ty_of_value v env in Some v
      with
      | _ -> already_exist env vs
    end
  | [] -> None

let is_valid_name name env =
  try
    is_name_value name env;
    let x = List.find (fun (x,_) -> x=name.value) env.global_vars in
    Error.type_error name.value name.pos (name.value ^ " will mask `"^fst x^"` (global variable)")
  with
  | Not_found -> (* we lookup in the global arrays *)
    try
      let x = List.find (fun (x, _) -> x=name.value) env.arrays in
      Error.type_error name.value name.pos (name.value ^ " will mask `"^fst x^"` (global variable)")
    with
    | Not_found -> () (* The variable name is clean *)

let type_check ast =
  let env = empty_environment in

  let rec program env = function
  | Program (head, procs, m, _, _) ->
    let env = header env head in
    let env = List.fold_left (fun env x -> proc env x) env procs in
    main env m

  and header env = function
  | Header (datas, Some globals) ->
    let env = List.fold_left (fun env x -> data env x) env datas in
    let env = List.fold_left (fun env x -> global env x) env globals in
    env

  | Header (datas, None) ->
    let env = List.fold_left (fun env x -> data env x) env datas in
    env

  and data env d = match d.value with
  | DefineType (ty, values) ->
    match already_exist env values with
    | Some v -> Error.type_error v.value v.pos (ty ^ " already contains declared values")
    | None -> bind_types ty (List.map (fun x -> x.value) values) env

  and global env g = match g.value with
  | EmptyArray (name, ty, value) ->
    bind_array name (ty_of_ty ty env) (int_of_value value) env

  | Array (name, ty, values) ->
    let values' = lookup_values_of_ty ty env in
    List.iter (fun x -> cmp_value ty values' x) values;
    bind_array name ty.value (List.length values) env

  | GlobalVar (name, ty, value) ->
    let values' = lookup_values_of_ty ty env in
    cmp_value ty values' value;
    bind_global_var name ty.value env

  and proc env = function
  | Process (proc_name, args, decls, instrs) ->
    let (env, args') = arguments proc_name env [] (List.map (fun x -> x.value) args) in
    let env' = declarations proc_name env args' (List.map (fun x -> x.value) decls) in
    List.iter (fun x -> instruction proc_name env' x) instrs;
    env

  and arguments proc_name env args = function
  | [] -> (bind_args proc_name args env, args)
  | Arg(name, ty)::s ->
    let _ = lookup_values_of_ty ty env in
    is_valid_name name env;
    let args = (name.value, ty.value)::args in
    arguments proc_name env args s

  and declarations proc_name env vars = function
  | [] -> bind_local_vars proc_name vars env
  | DeclareVar (name, ty)::s ->
    is_valid_name name env;
    if List.mem name.value (List.map fst vars) then
      Error.type_error name.value name.pos (name.value ^ " is already declared in " ^ proc_name);

    let vars = (name.value, ty.value)::vars in
    declarations proc_name env vars s

  and instruction proc_name env i = match i.value with
  | Assign (name, l) ->
    let (atomic', ty') = literal proc_name env l in
    let (atomic, ty) =
      try lookup_ty_of_name proc_name name env
      with _ -> Error.type_error name.value name.pos (name.value ^ " is not a variable")
    in
    check_atomicity i.pos "instruction" atomic atomic';
    check_expected_type i.pos ty ty'
  | AssignArray (name, v, l) ->
    let ty, array_length = lookup_array_of_name name env in
    let index = int_of_value v in
    let (atomic, ty') = literal proc_name env l in
    check_atomicity i.pos "instruction" true atomic;
    check_bounds index array_length name v;
    check_expected_type i.pos ty ty'
  | Condition (cmp, if_instrs, Some else_instrs) ->
    compare proc_name env cmp;
    List.iter (fun x -> instruction proc_name env x) if_instrs;
    List.iter (fun x -> instruction proc_name env x) else_instrs
  | Condition (cmp, if_instrs, None) ->
    compare proc_name env cmp;
    List.iter (fun x -> instruction proc_name env x) if_instrs;
  | Switch (l, cs) ->
    let (_,ty) = literal proc_name env l in
    cases proc_name ty  env (List.map (fun x -> x.value) cs)
  | While (c, instrs) ->
    compare proc_name env c;
    List.iter (fun x -> instruction proc_name env x) instrs
  | Decide _ -> ()

  and cases proc_name ty env = function
  | Case (Wildcard, instrs)::s ->
    List.iter (fun x -> instruction proc_name env x) instrs;
    cases proc_name ty env s
  | Case (CaseArg l, instrs)::s ->
    let (_,ty') = literal proc_name env l in
    check_expected_type l.pos ty ty';
    List.iter (fun x -> instruction proc_name env x) instrs;
    cases proc_name ty env s
  | [] -> ()

  and compare proc_name env = function
  | And (c, c') | Or (c, c') ->
    let is_global = is_global_compare proc_name env c in
    let is_global' = is_global_compare proc_name env c in
    check_atomicity_no_pos "comparaison" is_global is_global';
    compare proc_name env c;
    compare proc_name env c'
  | Equal (l, l') | NonEqual (l, l') ->
    let (is_global, t) = literal proc_name env l in
    let (is_global', t') = literal proc_name env l' in
    check_atomicity l'.pos "comparaison" is_global is_global';
    check_comparaison_type l'.pos t t'
  | Boolean _ -> ()

  and is_global_compare proc_name env = function
    | And (c, c') | Or (c, c') ->
      let l = is_global_compare proc_name env c in
      let r = is_global_compare proc_name env c' in
      l && r
    | Equal (l, l') | NonEqual(l, l') ->
      let is_global, _ = literal proc_name env l in
      let is_global', _ = literal proc_name env l' in
      is_global || is_global'
    | Boolean _ -> false

  and literal proc_name env l = match l.value with
  | ArrayValue (name, v) ->
    let array_ty, array_length = lookup_array_of_name name env in
    let index = int_of_value v in
    check_bounds index array_length name v;
    (true, array_ty)
  | Value v -> value proc_name env v

  and value proc_name env v =
    try
      (false, lookup_ty_of_value v env)
    with _ ->
      try lookup_ty_of_name proc_name v env
      with _ -> Error.type_error v.value v.pos (v.value ^ " is neither a value or a variable")

  and main env = function
  | Main c -> callable env [] c.value

  and callable env args = function
  | ForAll (names, ty, c) ->
    let ty' = ty_of_ty ty env in
    let forall_args = List.map (fun x -> (x, ty')) names in
    callable env (forall_args@args) c.value

  | Parallel (c, c') | SeqI (c, c') ->
    callable env args c.value;
    callable env args c'.value

  | CallProcess (name, ls) ->
    let ls = match ls with | Some ls -> ls | None -> [] in
    let l = lookup_args_of_proc name env in
    if List.length l <> List.length ls then
      Error.type_error name.value name.pos (
        "Process excepts " ^ string_of_int (List.length ls) ^ " arguments"
      )
    else(
      List.iter2
        (fun x y ->
           let ty =
             try
               match Position.value x with
               | Value x ->
                 List.assoc (Position.value x) args
               | ArrayValue _ ->
                 raise Not_found
             with
             |Not_found ->
               let (_, ty) = (literal name.value env x) in
               ty
           in check_args_type x.pos (snd y) ty
        ) ls l
    )

  in program env ast

let properties_check ast =
  Properties.Checker.create ast
  |> Properties.Checker.init_env
  |> Properties.Checker.check

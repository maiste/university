open Position
open Ast

type typing_environement = {
  types  : (ty * value list) list;
  arrays : (name * (ty * int)) list;
  global_vars : (name * ty) list;
  local_vars  : (name * ((name * ty) list)) list; (* proc_name ( (name_var * ty) list ) list *)
  args   : (name * ( (name * ty) list )) list (* proc_name ( (position_arg * ty) list ) list*)
}

let empty_environment = {
  types = [];
  arrays = [];
  global_vars = [];
  local_vars = [];
  args = [];
}

(* Bind name to values in env.types *)
let bind_types name vals env = {
  env with types = (name, vals) :: env.types
}

(* Bind name to ty with the size in env.arrays *)
let bind_array name ty int env = {
  env with arrays = (name, (ty, int)) :: env.arrays
}


(* Bind var_name to ty in env.global_vars *)
let bind_global_var x ty env = {
  env with global_vars = (x, ty) :: env.global_vars
}

(* Bind (name * ty) list to proc_name in env.local_vars *)
let bind_local_vars name vars env = {
  env with local_vars = (name, vars) :: env.local_vars
}

(* Bind (int * ty) list to name in env.args *)
let bind_args name args env = {
  env with args = (name, args) :: env.args
}

(* Return the values of ty in env *)
let lookup_values_of_ty ty env =
  try
    List.assoc ty.value env.types
  with Not_found ->
    Error.type_error ty.value ty.pos (
      ty.value ^ " is not a known type"
    )

(* Return Some ty in a list of variables *)
let lookup_ty_in_list var_name vars =
  try
    List.assoc var_name.value vars
  with
  | Not_found -> Error.type_error var_name.value var_name.pos (var_name.value ^ " is not a known variable")

(* Return the type of var_name in env *)
let lookup_ty_of_name proc_name var_name env =
  try
    (* we first look into the globals *)
    (true, List.assoc var_name.value env.global_vars)
  with Not_found ->
    let l = List.assoc proc_name env.local_vars in
    (false, lookup_ty_in_list var_name l)

(* Return the type of value in env *)
let lookup_ty_of_value v env =
  let aux values = match List.find_opt (fun x -> x=v.value) values with
  | Some _ -> true
  | _ -> false
  in
  match List.find_opt (fun (_, values) -> aux values) env.types with
  | Some (t, _) -> t
  | _ -> Error.type_error v.value v.pos (
    v.value ^ " is not a known value in the declared types"
  )

(* Return the array of name in env *)
let lookup_array_of_name name env =
  try
    List.assoc name.value env.arrays
  with Not_found ->
    Error.type_error name.value name.pos (
      name.value ^ " is not a known array"
    )

(* Return the ty idendity if it exists *)
let ty_of_ty ty env =
  let _ = lookup_values_of_ty ty env in
  ty.value

(* Check if the value is a correct formed int *)
let int_of_value v =
  let rec aux i n =
    if i < n then
      let c = String.get v.value i in
      let c = Char.code c in
      if c >= 48 && c <= 57 then aux (i+1) n
      else Error.type_error v.value v.pos (
        v.value ^ " is supposed to be an integer"
      )
  in
  aux 0 (String.length v.value);
  int_of_string v.value

(* Compare search value in a list of values, raise exception if not found *)
let cmp_value ty (values: value list) (x: value position) =
  try
    let _ = List.find (fun y -> y=x.value) values in ()
  with Not_found ->
    Error.type_error x.value x.pos (
      x.value ^ " does not belong to " ^ ty.value
    )

(* Check if name is already in the values *)
let is_name_value name env =
  let rec aux = function
  | (ty, values)::s ->
    begin
      try
        let _ = List.find (fun x -> name.value = x) values in
        Error.type_error name.value name.pos (
          name.value ^ " is already a value in " ^ ty ^ " type"
        )
      with Not_found -> aux s
    end
  | [] -> ()
  in aux env.types

(* Return the list of argument's process *)
let lookup_args_of_proc proc_name env =
  try
    List.assoc proc_name.value env.args
  with Not_found ->
    Error.type_error proc_name.value proc_name.pos (
      proc_name.value ^ " is not a known process"
    )

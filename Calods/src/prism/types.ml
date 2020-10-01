(*
 * Chaboche - Marais
 * CALODS - 2019
 *)

(* Source Ast *)
module S = Compiler.Ast

(* Target Ast *)
module T = PrismAst
  
type ty = string

type value = string

type name = string

type type_association = {
  (* Every type becomes an integer *)
  types : (ty * ((value * int) array)) list;
  (* Next int is the next int we'll attribute to calods types *)
  next_int : int;
}

type env = {  
  vars : (name * ty) list;
  (* We store every global variable name *)
  globals: name list;
  ty_assoc: type_association;
  (* We store every args_name of each process *)
  args : (name * name list) list;
}

(* Record of process configuration, contains:
   process name,
   process label,
   current id_state,
   next id in case of non incremental state id
   environment
*)
type config = {
  name: string;
  label: string;
  id: int;
  next_id: int option;
  env: env;
  double: bool;
}

let config ~name ~label ?(id=0) ?(next_id=None) ~env ~d () =
  {
    name = name;
    label = label;
    id = id;
    next_id = next_id;
    env = env;
    double = d;
  }

(* Convert Calods.value to integer from environment, raise Exception if it's not a value *)
exception NotValue
  
let is_arg env name v =
  try
    List.mem v (List.assoc name env.args)
  with
  | Not_found -> false
  
let value_to_int env v =
  let rec each_types = function
    | (_, arr)::s ->
      begin
        match List.find_opt (fun (v', _) -> v=v') (Array.to_list arr) with
        | Some (_, i) -> i
        | None -> each_types s
      end
    | [] -> raise NotValue              
  in
  each_types env.ty_assoc.types

(* Returns values of a type *)
let values_of_ty env ty =
  List.assoc ty env.ty_assoc.types

(* Return the type's interval of integers *)
let bounds_values_from_type env ty =
  let values = values_of_ty env ty in
  let b0 = snd (Array.get values 0) in
  let b1 = snd (Array.get values (Array.length values-1)) in
  (b0, b1)

let rec how_many_types = function
  | [] -> 0
  | (_, x)::xs ->
    Array.length x + how_many_types xs

let is_global env name =
  List.mem name env.globals

(* Return wether the v is a declared value in types *)
let is_value env v =
  try
    let _ = value_to_int env v in
    true
  with
  | _ -> false

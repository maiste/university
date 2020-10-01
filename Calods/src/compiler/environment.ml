(*
 * Chaboche - Marais
 * CALODS - 2019
 *)

open Ast

(** Type to store in local variable *)
type variable =
  | EArray of value array
  | EVariable of value

(** Associate a string to an 'a *)
module StrMap = Map.Make (
  struct
    type t = string
    let compare = compare
  end
)

(** Associate a string*string to an 'a *)
module PairMap = Map.Make(
  struct
    type t = string*string
    let compare = compare
  end
)

(** Environment to evaluate trees *)
type env = {
    global_vars : variable StrMap.t       ;
    local_vars  : variable PairMap.t      ;
    proc_lines  : int StrMap.t            ;
    decide_vars : value StrMap.t          ;
}


(** Create an empty env *)
let empty_env = {
  global_vars = StrMap.empty  ;
  proc_lines = StrMap.empty   ;
  local_vars = PairMap.empty  ;
  decide_vars = StrMap.empty  ;
}

let display env =
  let display_var = function
    | EVariable v -> Format.printf "%s\n" v
    | EArray vs ->
       begin
         Format.printf "[";
         Array.iter (fun x -> Format.printf "%s, " x) vs;
         Format.printf "]\n" ;
       end
  in
  begin
    Format.printf "=====Env====\n";
    Format.printf "Globals:\n";
    StrMap.iter (fun n v -> Format.printf "* %s->" n ; display_var v) env.global_vars ;
    Format.printf "Process lines:\n";
    StrMap.iter (fun n i -> Format.printf "* %s at %d\n" n i ) env.proc_lines ;
    Format.printf "Locals:\n";
    PairMap.iter (fun (p, n) v -> Format.printf "* %s, %s->" p n ; display_var v) env.local_vars ;
    Format.printf "Decides:\n";
    StrMap.iter (fun n v ->  Format.printf "* %s decides %s\n" n v ) env.decide_vars ;
    Format.printf "=============\n\n"
  end


(** Get string value of variable type *)
let tuple_of_variable = function
  | EArray l ->
        let str = Array.fold_left (
          fun str v -> str ^v ^ " "
        ) "[ " l in str ^ "]"
  | EVariable v -> v

(** Generate an empty array **)
let empty_array size =
  EArray (Array.make size "")

(** Generate an array **)
let array_of v =
  EArray (Array.of_list v)

(** Generate a variable from a value **)
let var_of v =
  EVariable v

(** Add a value to a global variable **)
let add_global_var env n v =
  { env with global_vars = StrMap.add n v env.global_vars }

(** Add a value to local variables *)
let add_local_var env p n v =
  { env with local_vars = PairMap.add (p,n) v env.local_vars }

(** Update global variables with [id] <- [v] *)
let update_global_var env (id, v) =
  { env with global_vars = StrMap.add id v env.global_vars }

(** Update local variables with [p], [id] <- [value] *)
let update_local_var env p (id, v) =
   { env with local_vars = PairMap.add (p,id) v env.local_vars }

(** Update the line of [p] with [l] *)
let update_proc_line env p l =
  { env with proc_lines = StrMap.add p l env.proc_lines }

(** Add one line to proc **)
let move_to_next env p =
  let l = (StrMap.find p env.proc_lines) + 1 in
  update_proc_line env p l

(** Set proc line to -1 if decided *)
let decide_proc env p v =
  if StrMap.mem p env.decide_vars then env
  else { env with decide_vars = StrMap.add p v env.decide_vars }

(** Tell if a process has_decide **)
let has_decide env p = StrMap.mem p env.decide_vars

(** Return the line of p **)
let get_line env p =
  StrMap.find p env.proc_lines

(** Get [p], [n] in [env] *)
let get_value_of env p id =
  try
    Some (StrMap.find id env.global_vars)
  with _ ->
    try
      Some (PairMap.find (p,id) env.local_vars)
    with _ -> None

(** Get the value of [id] in [env] (at [pos] if it's an array **)
let get_variable_of env p id pos =
  match get_value_of env p id, pos with
  | Some EVariable v, None -> Some (EVariable v)
  | Some EArray c, Some pos ->
      let v = c.(int_of_string pos) in
      Some (EVariable v)
  | None, None -> Some (EVariable id)
  | _ -> None


(** Update [p], [id] <- [value] in one of the variables *)
let update_variable_of env  p id value =
  if StrMap.mem id env.global_vars then
    update_global_var env (id,value)
  else if PairMap.mem (p, id) env.local_vars then
    update_local_var env p (id, value)
  else failwith "[failwith] Error access to an undifined variable"


(** Update n[pos] with v **)
let update_array_of env p n pos v =
  match get_value_of env p n with
  | Some (EArray c) ->
     let c = Array.copy c in
     c.(int_of_string pos) <- v;
     update_variable_of env p n (EArray c)
  | _ -> failwith "[failwith] Error access to an undifined array"

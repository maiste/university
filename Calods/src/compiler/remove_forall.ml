(*
 * Chaboche - Marais
 * CALODS 2019
 *)

open Ast
open Position

(**
   This module will remove every forall introduction in the main section
   For example:

   type int = { 0, 1 }

   proc p(int x, int y){
   }

   forall x,y in int {
     p(0, 0) || p(x, y)
   }

   =>

   ( p(0, 0) || p(0, 0) ) +
   ( p(0, 0) || p(0, 1) ) +
   ( p(0, 0) || p(1, 0) ) +
   ( p(0, 0) || p(1, 1) ) +

*)

(**
   We need to store every information about types.
   We put them into the environement, and get them from type
*)
type env = {
  values : (ty * (value list)) list
}

let empty_env = {
  values = []
}

let is_value v var_v =
  List.mem v (List.map fst var_v)

(* Values must contains ty *)
let values_from_type ty env =
  List.assoc ty env.values

let add_ty_values env datas =
  List.fold_right
    (fun (DefineType (ty, vs)) env ->
       { values = (ty, values vs) :: env.values }
    ) datas env  


(**
   All_posibilites will return the combinaison of every values,
   associated to variables.
   For example:

   all_posibilites [0; 1] ["x"; "y"] ->

   [ 
     [("x", 0); ("y", 0)]
     [("x", 0); ("y", 1)]
     [("x", 1); ("y", 0)]
     [("x", 1); ("y", 1)]
   ]
*)

let all_posibilites vs vars =
  (*
     Associate every variable with a value
  *)
  let add_values_to_var var =
    List.map (fun v -> (var, v)) vs
  in

  (*
     Add_new_var_to_acc returns the acc with the new values added.
     For example:
     add_new_var_to_acc [[]] [("x", 0); ("x", 1)] ->
     [
       [("x", 0)];
       [("x", 1)];
     ]
  *)
  let add_new_var_to_acc acc vs =
    List.fold_left
      (fun acc a ->
         List.map (fun v -> v :: a) vs
         @
         acc
      ) [] acc
  in

  List.fold_right
    (fun v acc ->
       let x = add_values_to_var v in
       add_new_var_to_acc acc x
    ) vars [[]]


let rec expand_callable env v_of_vars (c : callable position) : callable position = match value c with
    | ForAll (names, ty, c) ->
      let ty = value ty in

      let values = values_from_type ty env in
      let posibilites = all_posibilites values names in

      seqs env c posibilites

    | Parallel (c1, c2) ->
      Parallel (
        expand_callable env v_of_vars c1,
        expand_callable env v_of_vars c2
      )
      |> w_pos


    | SeqI (c1, c2) ->
      SeqI (
        expand_callable env v_of_vars c1,
        expand_callable env v_of_vars c2
      )
      |> w_pos

    | CallProcess (_,  None) ->
      c

    | CallProcess (n, Some lits) ->
      let lits = List.map
          (function
            | ArrayValue _ as l -> w_pos l
            | Value x ->
              let x = value x in
              let v' =
                if is_value x v_of_vars then
                  List.assoc x v_of_vars
                else
                  x
              in
              w_pos (Value (w_pos v'))
          ) (values lits) in
      CallProcess (n, Some lits) |> w_pos

(**
   seqs return SeqI of callable where each callable is created
   with a variable valuation.
   For example:
   seqs env p(x) [ [("x", 0)]; [("x", 1)]; ["x", 2] ]
   ->
   SeqI (
     p(0),
     SeqI (
       p(1),
       p(2)
     )
   )
*)
and seqs env c = function
  | [] -> assert false
  | v1::v2::[] ->
    SeqI (
      expand_callable env v1 c,
      expand_callable env v2 c)
    |> w_pos
  | v::vs ->
    SeqI (
      expand_callable env v c,
      seqs env c vs
    )
    |> w_pos

(**
   Remove every Forall occurences in the main,
   replaced by SeqI.
*)
let rec remove_forall = function
  | Program (Header (datas, _) as h, p, main, s, props) ->
    let env = empty_env in
    let env = add_ty_values env (values datas) in
    let main = expand_main env main in
    Program (h, p, main, s, props)

and expand_main env = function
  | Main c ->
    Main (expand_callable env [] c)

(** Subset maker by E. MARAIS **)

(** Nil type for subset Module *)
module type Nil =
sig
  type t
  val nil : t
end

(* Module maker for subset *)
module Make (N : Nil) =
struct
  (* Concatenate a sub_list with all elt in list *)
  let concat sub_list list =
    let rec aux acc l =
      match l with
      | [] -> acc
      | h :: q  -> aux (acc @ [sub_list::h]) q
    in aux [] list

  let rec subset s =
    if s = [] then [[]]
    else
      let h,t = match s with
        | [] -> N.nil, []
        | h::q -> h, q
      in let exc_h = subset t
      in let inc_h = concat h exc_h
      in exc_h @ inc_h

  (* Create k comb *)
  let set_of size s =
    subset s
    |> List.filter (fun l -> List.length l = size)
  
end 

(* Module pour la création de sous module *)
module Boolset = Make (
  struct
    type t = (bool * int)
    let nil = (false, -1)
  end )

module Sol = Sat_solver.Make (
  struct
    type t = int
    let compare x y =
      if x > y  then 1
      else if x = y then 0
      else -1
  end )

(* Division entière à l'arrondi supérieur *)
let div a b =
  if (a mod 2) = 0 then (a/b)
  else (a+1) / b

(* Génération de tous les sous-ensembles *)
let get_all_subset_aux purple list =
  let size = List.length list
  in let nb =
       if purple then (div (size+1) 2)
       else (div size 2)
  in Boolset.set_of nb list

(* Solver lancement *)
let check list =
  let res =  Sol.solve list 
  in match res with
  | None -> false
  | Some _ -> true

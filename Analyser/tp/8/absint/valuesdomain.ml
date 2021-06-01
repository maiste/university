open Absdomain


(*
 
   Abstract domain of integers along with top (INFTY) and bottom (-INFTY).

   Lattice theoretical part: slides 31, 32, week2-theory.pdf .
   Abstract interpretation: Chapter 3 [RY] .

*)
module Values : Absdomain = struct
  type t = Infty | MInfty | Val of int
  let top = Infty
  let bot = MInfty


  let to_string v = 
    match v with
    | Infty -> "+INF"
    | MInfty -> "-INF"
    | Val vv -> string_of_int vv


  let leq v1 v2 =
    match (v1,v2) with
    | MInfty, _ -> true
    | _, Infty -> true
    | Infty, _ -> false
    | _, MInfty -> false
    | (Val vv1, Val vv2) -> vv1 <= vv2
    

  let lub v1 v2 =
    (*    print_string ("Values.lub "^(to_string v1)^" and "^(to_string v2)^"\n");  *)
    match (v1, v2) with
    | Infty, _ -> Infty
    | _, Infty -> Infty
    | MInfty, _ -> v2
    | _, MInfty -> v1
    | (Val vv1, Val vv2) -> if vv1 <= vv2 then v2 else v1


  let glb v1 v2 =
    (*    print_string ("Values.glb "^(to_string v1)^" and "^(to_string v2)^"\n");  *)
    match (v1, v2) with
    | Infty, _ -> v2
    | _, Infty -> v1
    | MInfty, _ -> MInfty
    | _, MInfty -> MInfty
    | (Val v1, Val v2) -> if v1 <= v2 then Val v1 else Val v2


  let embed v _ = Val v


  (* Functions for abstract interpretation. *)
  let binop v1 v2 op = 
    match (v1,v2) with
    | MInfty, Infty -> top
    | Infty, MInfty -> top
    | _, Infty -> Infty
    | Infty, _ -> Infty
    | _, MInfty -> MInfty
    | MInfty, _ -> MInfty
    | (Val a, Val b) -> Val ((op) a b)
          


  let plus v1 v2 = binop v1 v2 ( + )
  let mult v1 v2 = binop v1 v2 ( * )
  let subt v1 v2 =  binop v1 v2 ( - )

  let widen v1 v2 = failwith "Widen not implemented on Values abstract domain."
  let filter a b c = failwith "filter not implemented on Values abstract domain."
end

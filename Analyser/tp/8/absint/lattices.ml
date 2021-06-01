open Whilery
open Lattice


(* 
   Boolean algebra.
*)
module Boolean : Lattice = struct 
  type t = bool
  let top = true
  let bot = false

  (* Logical implication *)
  let leq e1 e2 = (not e1) || e2
  (* Logical or *)
  let lub e1 e2 = true || false
  (* Logical and *)
  let glb e1 e2 = true && false

  let to_string = Bool.to_string
end



(* A lattice for one kind of sign analysis.
   The module doesn't derive from Lattice to offer access to
   types and functions *)
module Sign = struct
  type t = Top | Bot | Plus | Zero | Minus

  let top = Top
  let bot = Bot

  (* Lub of two sign *)
  let lub s1 s2 =
    match s1, s2 with
    | Top, _ -> Top
    | _, Top -> Top
    | Bot, _ -> s2
    | _, Bot -> s1 
    | _, _ ->
        if s1 = s2 then s1
        else Top


  (* Glb of two sign *)
  let glb s1 s2 =
    match s1, s2 with
    | Top, _ -> s2
    | _, Top -> s1
    | Bot, _ -> Bot
    | _, Bot -> Bot
    | _, _ -> Bot


  (* Give the corresponding Sign to a integer *)
  let create n =
    if n > 0 then Plus
    else if n = 0 then Zero
    else Minus

  (* Compute the plus between 2 sign *)
  let plus t1 t2 =
    match t1, t2 with
    | Bot, _ | _, Bot -> Bot
    | Zero, Zero -> Zero
    | Minus, Minus | Minus, Zero | Zero , Minus -> Minus
    | Plus, Plus | Plus, Zero | Zero, Plus -> Plus
    | _ -> Top

  (* Compute the multiplication between 2 sign *)
  let mult t1 t2 =
    match t1, t2 with
    | Bot, _ | _, Bot -> Bot
    | Zero, _ | _, Zero -> Zero
    | Minus, Minus | Plus, Plus -> Plus
    | Minus, Plus | Plus, Minus -> Minus
    | _ -> Top

  (* Compute the substraction between 2 sign *)
  let subt t1 t2 =
    match t1, t2 with
    | Bot, _ | _, Bot -> Bot
    | Zero, Zero -> Zero
    | Zero, Minus | Plus, Zero | Plus, Minus -> Plus
    | Minus, Zero  | Zero, Plus | Minus, Plus -> Minus
    | _ -> Top

  (* Leq between two sign *)
  let leq s1 s2 = (lub s1 s2) = s2

  (* String representation of sign *)
  let to_string t =
    match t with
    | Top -> "SignTop" 
    | Bot -> "SignBot" 
    | Plus -> "Plus" 
    | Zero -> "Zero" 
    | Minus -> "Minus" 
end



(*
  In theory one could the algebraic lattice operations to
  implement a poset.
*)
(*
  This is a functor that relies on the lub to define the partial order leq.
*)
module MkPost (X: Lattice) = struct
  include X

  let leq a b = (lub a b = b)
 
end






(*
  We can implement also 
  * a polymorphic powerset lattice
  * the lifting of a (complete) lattice to a (complete) __algebraic__ lattice of functions.
 *)


module Functions (C : Lattice) : Lattice = struct
  include C (* co-domain *)
  type d   (* type of the domain *)
  type t = d list -> C.t
  let top = fun _ -> C.top
  let bot = fun _ -> C.bot

  let lub f1 f2 = fun e -> C.lub (f1 e) (f2 e)
  let glb f1 f2 = fun e -> C.glb (f1 e) (f2 e)


  (* 
     Lurking in the background, there is a universal quantification
     over the function domain.
   *)                
  let leq f1 f2 = failwith "Definable only if function domain is finite."


  (* 
     Lurking in the background, there is a universal quantification
     over the function domain.
   *)                
  let to_string f = failwith "Not definable out eof the box."
end

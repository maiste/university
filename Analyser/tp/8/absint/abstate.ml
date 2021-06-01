open Whilery
open Lattices
open Absdomain
open Valuesdomain
open Intvdomain


(* 

   An abstract state is a function from variables to abstract values,
   that is elements of an abstract domain AD.

   To represent this function in memory we use an ugly imperative ugly array ugly.
   This deserves but shame.

*)


module Abstate (AD : Absdomain) = struct
  include AD


  (*  type abs_state = Intervals.t array *)
  type t = AD.t array


  let to_string abss =
    let strings = Array.mapi (fun i v -> "("^(string_of_int i)^", "^(AD.to_string v)^"), ") abss
    in 
    Array.fold_right (fun s1 s2 -> s1^s2) strings ""
            

  (* 
     Starting from an existing abstract state,
     create an abstract state that represent the bottom state,
     i.e. the constant function bottom.
   *)
  let bot abss = Array.map (fun _ -> AD.bot) abss


  (* Same for top. *)
  let top abss = Array.map (fun _ -> AD.top) abss

  (* 
   Create a __new__ abstract state that is defined
   asabss, except for the variable x, that takes
   the abst ract value n.
   *)
    
  let abs_init x n abss =
    let nabss = Array.copy abss in
    nabss.(x) <- n;
    nabss


  let lub abss0 abss1 =
    print_string ("[ABS_LUB ]: "^(to_string abss0)^"\n");
    print_string ("[AND     ]: "^(to_string abss1)^"\n");
    let res = Array.mapi 
                (fun x a0 -> let res = AD.lub a0 (abss1.(x)) in                           
                             (* print_string ("ABS_LUB: "^(AD.to_string a0)^" AND "^(AD.to_string (abss1.(x)))^" = "^(AD.to_string res)^"\n"); *)
                             res)
                abss0
    in
    print_string ("[RES     ]: "^(to_string res)^"\n");
    res


  (* Overapproximation of a lub, useful to approximate in finite time fixed points. *)
  let widen abss0 abss1 =
    print_string ("[ABS_WIDENING ]: "^(to_string abss0)^"\n");
    print_string ("[AND          ]: "^(to_string abss1)^"\n");
    let res = Array.mapi 
                (fun x a0 -> let res = AD.widen a0 (abss1.(x)) in                           
                             (* print_string ("WIDEN: "^(AD.to_string a0)^" AND "^(AD.to_string (abss1.(x)))^" = "^(AD.to_string res)^"\n"); *)
                             res)
                abss0
    in
    print_string ("[RES     ]: "^(to_string res)^"\n");
    res
    

  let leq abss0 abss1 =
    let r = ref true in
    Array.iteri (fun x a0 -> r := !r && AD.leq a0 abss1.(x)) abss0;
    !r

  

(*
  Filtering of the abstract state via a boolean condition.
  See Section 3.3.2 of [RY].

  It has to return a "refined" abstract state, computed
  starting from "abss", so that in this new state the
  checked contition is true.

  If the condtion cannot be true then it returns the
  bottom abstract state.
 *)
  let filter (r,x,n) abss =
  print_string ("[FILTER ABS (input):] "^(to_string abss)^"\n");
  let new_av_x = AD.filter r n abss.(x) in
  if new_av_x = AD.bot
  then bot abss
  else abs_init x new_av_x abss 


  

(* 
   Computes the abstract state that is the best abstraction
   of the concrete state mem.
 *)
let abs_const mem =
  let abss = Array.make (Array.length mem) AD.top in
  Array.iteri (fun i v -> Array.set abss i (AD.embed v v)) mem;
  abss




let rec ai_expr e abss =
  match e with
  | Ecst n -> AD.embed n n
  | Evar x -> abss.(x)
  | Ebop (Badd, e0, e1) -> let v0 = (ai_expr e0 abss) in 
                           let v1 = (ai_expr e1 abss) in
                           let res = (AD.plus v0 v1) in
                           print_string ((AD.to_string v0)^" + "^(AD.to_string v1)^"   =   "^(AD.to_string res)^"\n");
                           res
  | Ebop (Bsub, e0, e1) -> let v0 = (ai_expr e0 abss) in 
                           let v1 = (ai_expr e1 abss) in
                           let res = AD.subt v0 v1 in
                           print_string ((AD.to_string v0)^" - "^(AD.to_string v1)^"   =   "^(AD.to_string res)^"\n");
                           res
  | Ebop (Bmul, e0, e1) -> let v0 = (ai_expr e0 abss) in 
                           let v1 = (ai_expr e1 abss) in
                           let res = AD.mult v0 v1 in
                           print_string ((AD.to_string v0)^" * "^(AD.to_string v1)^"   =   "^(AD.to_string res)^"\n");
                           res



end

open Whilery
open Lattices
open Absdomain
open Valuesdomain
open Intvdomain
open Signdomain
open Abstate

module IntervalAbstate = Abstate (Intervals)
module SignAbstate = Abstate (SignAnalysis)

(* Abstract interpreter *)


module Absem = struct
 include SignAbstate


 let rec postlfp f abss =
   print_string ("[POSTLFP ABS (input):] "^(SignAbstate.to_string abss)^"\n");
   let anext = f abss in
   if SignAbstate.leq anext abss
   then abss
   else postlfp f (SignAbstate.widen abss anext)


 let cneg (r,v,c) =
   let nr = if r = Cinfeq then Csup else Cinfeq in (nr,v,c)


 (*  Abstract interpreter for the minimal WHILE language à la [RY]. *)

 let rec interpreter (l, c) abss =
   match c with
   | Cskip ->
     print_string  ("[Cskip, "^(string_of_int l)^"] result abss: ");
     abss
   | Cassign (x, e) ->
     print_string ("[Cassign, "^(string_of_int l)^"] result abss: ");
     let new_abs = SignAbstate.abs_init x (SignAbstate.ai_expr e abss) abss in
     print_string ((SignAbstate.to_string new_abs)^"\n");
     new_abs
   | Cseq (c0, c1) -> interpreter c1 (interpreter c0 abss)
   | Cinput x -> SignAbstate.abs_init x SignAnalysis.top abss
   | Cif (bcond, c0, c1) -> SignAbstate.lub (interpreter c0 (filter bcond abss)) (interpreter c1 (filter (cneg bcond) abss))
   | Cwhile (bcond, c) ->
     print_string ("[Cwhile, "^(string_of_int l)^"]\n");
     let f_loop = fun a -> interpreter c (SignAbstate.filter bcond a) in
     let abs_fix = (postlfp f_loop abss)  in
     print_string ("ABOUT TO FILTER cneg USING"^(SignAbstate.to_string abs_fix)^"\n");
     let new_abs = filter (cneg bcond) abs_fix
     (* CLAIM:
                   One of the following is true,
                   (a) A while loop that does not terminate will result in the bottom abstract state.
                   (b) A program whose output abstract state if the bottom one does not terminate.
     *)
     in
     print_string ("[Cwhile, "^(string_of_int l)^"] result abss: ");
     print_string ((SignAbstate.to_string new_abs)^"\n");
     new_abs
end

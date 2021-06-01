open Whilery
open Lattices
open Absdomain
open Valuesdomain



(* 
   Lattice of intervals over the value lattice,
   plus functions for abstract interpretation.

   Lattice theoretical part: slides 31, 32, week2-theory.pdf .
   Abstract interpretation: Chapter 3 [RY] .

*)

module Intervals : Absdomain = struct
  include Values

  type t = Bot | Intv of Values.t * Values.t
  let top = Intv (Values.bot, Values.top)
  let bot = Bot

  let to_string i =
    match i with
    | Bot -> "[BOT_INTV]"
    | Intv (v1, v2) -> "["^(Values.to_string v1)^", "^(Values.to_string v2)^"]"


  let lub i1 i2 =
    (* print_string ("Itervals.lub "^(to_string i1)^" and "^(to_string i2)^"\n");  *)
    match i1 with
    | Bot -> i2
    | Intv (a1, b1) -> 
       (match i2 with
        | Bot -> i1
        | Intv (a2, b2) -> Intv (Values.glb a1 a2, Values.lub b1 b2)
       )


  (* See def. on page 260 of [RY]. *)
  let sensible_intv rval lval =
    if (leq rval lval)  
    then if (leq rval lval)
         then Intv (lval, rval)
         else Bot                       
    else Intv (lval, rval)
  let glb i1 i2 =
    (* print_string ("Itervals.glb "^(to_string i1)^" and "^(to_string i2)^"\n");  *)
    match i1 with 
    | Bot -> Bot 
    | Intv (a1, b1) -> 
       (match i2 with
        | Bot -> Bot
        | Intv (a2, b2) -> 
           let rval = Values.glb b1 b2 in
           let lval = Values.lub a1 a2 in
           sensible_intv rval lval)


  let leq i1 i2 = (lub i1 i2 = i2)



  (* Abstract Domain *)

  let embed v1 v2 = Intv (Values.embed v1 0, Values.embed v2 0)



  let binop i1 i2 op = 
    match (i1, i2) with
    | _, Bot -> Bot
    | Bot, _ -> Bot
    | Intv (a1, b1), Intv (a2, b2) -> 
       let a = (op a1 a2) in
       let b = (op b1 b2) in
       if a = Values.top 
       then failwith ((Values.to_string a)^" on the left makes no sense")
       else 
         if b = Values.bot 
         then failwith ((Values.to_string b)^" on the right makes no sense")
         else Intv (a,b) 


  let plus i1 i2 = binop i1 i2 (Values.plus)
  let subt i1 i2 = binop i1 i2 (Values.subt)
  let mult i1 i2 = binop i1 i2 (Values.mult)


  (* 
     A very crude / simple widening operation that
     (1) over-aproximates the lub operation
     (2) allows quick termination, in spite of precision of the analysis
   *)
  let widen i1 i2 = 
    print_string ("Intervals.widening: "^(to_string i1)^" and "^(to_string i2)^"\n");
    match (i1,i2) with
    | _, Bot -> Bot
    | Bot, _ -> Bot
    | Intv (a1, b1), Intv (a2, b2) -> 
       let lb = if (Values.leq a2 a1 && (not (a1 = a2))) then Values.bot else a1 in
       let rb = if (Values.leq b1 b2 && (not (b1 = b2))) then Values.top else b1 in
       Intv (lb, rb)



  (* 
     Split a given interval to make a boolean condition true,
     or return bottom.
   *)
  let filter r n intv =
    print_string ("[FILTER INTERVAL:] "^(to_string intv)^"\n");
    match intv with
    | Bot -> Bot
    | Intv (a, b) ->
       let valn = Values.embed n 0 in
       match r with
       | Cinfeq -> 
          print_string (("Condition to ensure  ")^(to_string intv)^" <= "^(string_of_int n)^"\n");
          (* x <= n *)
          let res = if (Values.leq valn a && (not (a = valn)))
                    then bot 
                    else if (Values.leq b valn)
                    then intv
                    else Intv (a, valn) 
          in 
          print_string ("RESULT OF FILTER:"^(to_string res)^"\n");
          res
       | Csup -> 
          print_string (("Condition to ensure  ")^(to_string intv)^" > "^(string_of_int n)^"\n");
          let res = if (Values.leq b valn && (not (b = valn)) )
                    then bot 
                    else if (Values.leq valn a && (not (a = valn))  )
                    then intv
                    else Intv (Values.plus valn (Values.embed 1 0), b)
          in
          print_string ("RESULT OF FILTER:"^(to_string res)^"\n");
          res
end

open Whilery
open Lattices
open Absdomain
open Valuesdomain
open Intvdomain
open Signdomain
open Abstate
open Absint
open Programs

(* ********************************************************************************* *)
(* Functions to run and check abstract interpretation. *)


let yodawg exref prog abss = 
  print_string ("\n=======================\nExample "^exref^"\n");
  print_string ((program_to_str prog) ^ "\n");
  print_string "Abstract state input: ";
  print_string (SignAbstate.to_string abss);
  print_string "\n";
  let absout = Absem.interpreter prog abss in
  print_string "Abstract state output: ";
  print_string (SignAbstate.to_string absout);
  print_string "\n"

(* Create array from list of values *)
let create_vars l =
  let a = Array.make (List.length l) (SignAnalysis.bot) in
  let _ = List.iteri (fun i x -> a.(i) <- SignAnalysis.embed x x) l in
  a

let examples_bin () =
  print_string "Binary operations: + - *";
  yodawg "Operation +" plus (create_vars [-5; 0; 8; 0]);
  yodawg "Operation -" minus (create_vars [-5; 0; 8; 0]);
  yodawg "Operation *" mult (create_vars [-5; 5; 0; 0])

let example_conditional () =
  print_string "Conditional evaluation";
  yodawg "3.13 of [RY], page 80" conditional (Array.make 2 SignAnalysis.top)

let example_divergence () =
  print_string "Divergence evaluation";
  let memin = write 1 0 (Array.init 3 (fun _ -> 3)) in
  yodawg  "3.9(a) of [RY]" divergence (SignAbstate.abs_const memin);
  print_string "Ugly divergence evaluation";
  yodawg "Divergence ugly" divergence_ugly (create_vars [0; 0])
  
let absint () = 
  print_string "ABSTRACT INTERPRETER\n+++++++++\n";
  examples_bin ();
  print_string "\n";
  example_conditional ();
  print_string "\n";
  example_divergence ()


(* 
   Here is the "main" function. 
   - use "execute" to execute the programs (using the denotational semantics)
   - use "absint" to run the abstract interpretation
 *)
let () = absint ()

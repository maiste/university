(*
 WHILE language as per 

 Introduction to Static Analysis,
 Xavier RIVAL and Kwangkeun YI,
 Chapter 7.
 
*)

type label = int
type const = int
type var = int


type bop = Badd | Bsub | Bmul
type rel = Cinfeq | Csup
type expr = 
  | Ecst of const
  | Evar of var
  | Ebop of bop * expr * expr
type cond =
  rel * var * const
type command =
  | Cskip
  | Cseq of com * com
  | Cassign of var * expr
  | Cinput of var
  | Cif of cond * com * com
  | Cwhile of cond * com
and com =
  label * command

type program = com


let rec program_to_str (l, c) =
  command_to_str c

and command_to_str = function
  | Cskip -> "Skip"
  | Cseq ((_, c1), (_, c2)) -> (command_to_str c1) ^ " ; " ^ (command_to_str c2)
  | Cassign (v, e) -> (string_of_int v) ^ " := " ^ (expr_to_str e)
  | Cinput v ->  "Input(" ^ (string_of_int v) ^ ")"
  | Cif (cond, (_,c1), (_,c2)) ->
    let c1 = command_to_str c1 in
    let c2 = command_to_str c2 in
    "If(" ^ cond_to_str cond ^ ") then\n\t" ^ c1 ^ "\nelse\n\t" ^ c2
  | Cwhile (cond, (_, com)) ->
    "While(" ^ cond_to_str cond ^ ")\n\t" ^ command_to_str com ^ "\n"

and expr_to_str = function
  | Ecst c -> string_of_int c
  | Evar v -> "var(" ^(string_of_int v) ^ ")"
  | Ebop (bop, e1, e2) -> "(" ^ expr_to_str e1 ^ " " ^ bop_to_str bop ^ " " ^expr_to_str e2 ^ ")"

and bop_to_str = function
  | Badd -> "+"
  | Bsub -> "-"
  | Bmul -> "*"

and cond_to_str (r, v, c) =
  let r = match r with
    | Cinfeq -> " <= "
    | Csup -> " > "
  in
  string_of_int v ^ r ^ string_of_int c

let pp_program (l, c) =
  Printf.printf "%s\n" (program_to_str c)
(*
  In the programming language there are no function defintions / calls,
  so variables are global, and life is easy.
*)



(* 
   A concrete state is meant to represent the memory of a machine.
   The simplest definition, which is enough to define a denotational
   sematics, is to let a concrete state be a function from the set 
   of variables to the set of concrete values.
 *)
type mem = const array



(* Reads the value of variable x from the memory m *)
let read x m = m.(x)


(* 
   Writes the value n in the variable x leaving the rest
   of the memory m unchanged. 
*)

let write x n m =
  let nm = Array.copy m in
  nm.(x) <- n;
  nm






(* Concrete semantics of binary operations *)
let binop o n1 n2 =
  match o with
  | Badd -> n1 + n2
  | Bsub -> n1 - n2
  | Bmul -> n1 * n2



(* Concrete semantics of scalar expressions *)
let rec sem_expr e m =
  match e with
  | Ecst n -> n
  | Evar x -> read x m
  | Ebop (o, e1, e2) -> binop o 
                           (sem_expr e1 m) 
                           (sem_expr e2 m) 

(* 
   En passant, note the absence of threading, __the same__ m is passed
   to the two recursive calls. This is OK because the first
   call cannot modify the memory.     
*)



(* 
   Concrete semantics of boolean expressions 
*)
let relop c v0 v1 =
  match c with
  | Cinfeq -> v0 <= v1
  | Csup -> v0 > v1
let sem_cond (c, x, n) m =
  relop c (read x m) n


(*
  Concrete semantics of the input command
*)
let input () = Random.int max_int;;



(* 
   Semantics of commands.

   According to this semantics a command denotes 
   a function that transforms an input state into 
   another (output) state.
*)
let rec sem_com (l, c) m =
  match c with
  | Cskip -> m
  | Cseq (c0, c1) -> sem_com c1 (sem_com c0 m)
  | Cassign (x, e) -> write x (sem_expr e m) m
  | Cinput x -> write x (input()) m
  | Cif (b, c0, c1) ->
     if sem_cond b m
     then sem_com c0 m
     else sem_com c1 m
  | Cwhile (b, c) ->
     if sem_cond b m
     then sem_com (l, Cwhile (b, c)) (sem_com c m)
     else m

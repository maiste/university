(*
 * Chaboche - Marais
 * CALODS 2019
 *)

open IlodsAst
open Format

let print_vlist fmt v =
  let rec aux = function
    | [] -> fprintf fmt " }"
    | [x] -> fprintf fmt "%s }" x
    | x::xs ->
       fprintf fmt "%s, " x ; aux xs
  in
  if v <> [] then (
    fprintf fmt "{ ";
    aux v
  )
  else
    fprintf fmt "empty"

let print_literal fmt = function
  | ArrayValue (n, v) -> fprintf fmt "%s[%s]" n v
  | Value v -> fprintf fmt "%s" v

let print_llist fmt l =
  let rec aux = function
    | [] -> fprintf fmt "}"
    | [x] -> fprintf fmt "%a }" print_literal x
    | x::xs ->
       fprintf fmt "%a, " print_literal x ; aux xs
  in
  if l <> [] then (
    fprintf fmt "{ ";
    aux l
  )
  else
    fprintf fmt "empty"


(** AST **)

let print_call fmt (n, l) =
  fprintf fmt "Call %s -> @[<v 0>%a @]" n print_llist l

let rec print_calls fmt = function
  | Call (n, l) -> fprintf fmt "@[%a@;@]" print_call (n,l)
  | Parallel cs ->
     fprintf fmt "Parallel:@;";
     List.iter (
         fun c -> fprintf fmt "%a@;" print_calls c
       ) cs
  | SeqI cs ->
     fprintf fmt "Choice:@;";
     List.iter (
         fun c -> fprintf fmt "@[<v 0>%a@;@]" print_calls c
       ) cs
  | _ -> failwith "TODO"

let rec print_compare fmt = function
  | And (c1, c2) ->
     fprintf fmt "%a and %a" print_compare c1 print_compare c2
  | Or (c1, c2) ->
     fprintf fmt "%a or %a" print_compare c1 print_compare c2
  | Equal (l1, l2) ->
     fprintf fmt "%a == %a" print_literal l1 print_literal l2
  | NonEqual (l1, l2) ->
     fprintf fmt "%a != %a" print_literal l1 print_literal l2
  | Bool b ->
     fprintf fmt "%b" b

let print_goto fmt = function
  | Goto i -> fprintf fmt "goto %d" i
  | Next -> fprintf fmt "next"
  | Finish -> fprintf fmt "finish"
  | Unknown -> fprintf fmt "unknown"
  | EOF ->  fprintf fmt "eof"


let print_action fmt = function
  | Empty -> fprintf fmt "Empty"
  | Declare n ->
     fprintf fmt "declare %s" n
  | Assign (n,l) ->
     fprintf fmt "%s = %a" n print_literal l
  | AssignArray (n,v,l) ->
     fprintf fmt "%s[%s] = %a" n v print_literal l
  | Decide l ->
     fprintf fmt "decide %a" print_literal l
  | Jump (c,g) ->
     fprintf fmt "jump if %a to %a"
       print_compare c
       print_goto g
  | Move ->
     fprintf fmt "move"

let print_inst fmt (a,g) =
  fprintf fmt "%a, %a" print_action a print_goto g

let print_instrs fmt is =
  let c = ref 0 in
  Array.iter (
      fun e ->
      fprintf fmt "%i: {%a}@;" !c print_inst e;
      c := !c +1
    ) is

let print_proc fmt (n,a,is) =
  fprintf fmt "= %s %a =@;%a "
    n
    print_vlist a
    print_instrs is

let print_processes fmt ps =
  List.iter (
      fun p -> fprintf fmt "%a@;" print_proc p
    ) ps

let print_data fmt (ty, v) =
  fprintf fmt "type %s = %a" ty print_vlist v

let print_global fmt = function
  | EmptyArray (n, v) ->
     fprintf fmt "Array %s[%s]" n v
  | Array (n,v) ->
     fprintf fmt "Array %s[] = %a" n print_vlist v
  | GlobalVar (n,v) ->
     fprintf fmt "Var %s = %s" n v

let print_header fmt (d,g) =
  let print_globals fmt gs =
    List.iter (
        fun g -> fprintf fmt "%a@;" print_global g
      ) gs
  in
  let print_datas fmt ds =
    List.iter (
        fun d -> fprintf fmt "%a@;" print_data d
      ) ds
  in
  fprintf fmt "@[<v 0>%a%a@]" print_datas d print_globals g


let print_program fmt ((h,ps,c): IlodsAst.program ) =
  fprintf fmt "@[<v 0>%a@;%a@;%a@;@]"
    print_header h
    print_processes ps
    print_calls c

let print_ast ast =
  print_program std_formatter ast

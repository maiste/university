(*
 * Chaboche - Marais
 * CALODS 2019
 *)

open Ast
open Position
open Format

(** Utils **)

(* Print a boolean *)
let print_boolean fmt b =
  if b then fprintf fmt "True"
  else fprintf fmt "False"

(* Print identity *)
let print_ident fmt s =
  fprintf fmt "%s" s

(* Print identity position *)
let print_ident_pos fmt s =
  fprintf fmt "%s" s.value

(* Print option type *)
let print_option fmt (f, opt) =
  match opt with
  | Some o -> fprintf fmt "%a" f o
  | None -> fprintf fmt ""

(* Print keyword *)
let print_keyword fmt (k,pp,e) =
  fprintf fmt "%s (%a)" k pp e

(* Remove position for printing *)
let list_position_opt_to_n = function
  | None -> None
  | Some lst ->
       Some(
         List.fold_left (
           fun acc elt -> (elt.value)::acc
          ) [] lst |> List.rev
       )

(* Print an inline list *)
let print_list fmt (p,l,s) =
  let line fmt () =
    let rec aux = function
      | [] -> ()
      | [h] -> fprintf fmt "%a" p h
      | h::q ->
          fprintf fmt "%a%s " p h s; aux q
    in aux l
  in fprintf fmt "@[%a@]" line ()

(* Print a block *)
let print_block_list fmt (p1,k,p2,l) =
  let block fmt () =
    let rec aux = function
      | [] -> ()
      | [h] -> fprintf fmt "%a" p2 h.value
      | h::q -> fprintf fmt "%a@;" p2 h.value ; aux q
    in aux l
  in
  fprintf fmt "@[<v 0>%a {@;<0 2>@[<v 0>%a@]@]@;} " p1 k block ()


(** Ast **)

let rec print_program fmt = function
  | Program (h, p, m, sch, props) ->
      let print_processes fmt  p =
        List.fold_left (
          fun _ elt ->
            fprintf fmt "%a@;" print_process elt
        ) () p
      in
      fprintf fmt "@[<v 0>%a@;@;@[<v 0>%a@]%a@;@;%a@;@]"
        print_header h
        print_processes p
        print_main m
        print_option (print_properties, props);
      match sch with
      | Some sch -> Automata.Nba.Scheduler.print sch
      | None -> ()

and print_header fmt = function
  | Header (d,g) ->
      let print_global fmt g =
        print_block_list fmt (print_ident, "global", print_global, g)
      in
      fprintf fmt "@[<v 0>%a@;%a@]"
        print_block_list (print_ident, "datatype(s)", print_data, d)
        print_option (print_global, g)

and print_data fmt = function
  | DefineType (ty, n) ->
      fprintf fmt "type %s = { %a }" ty print_list (print_ident_pos, n, ",")

and print_global fmt = function
  | EmptyArray (n,t,s) ->
      fprintf fmt "%s %s[%s]" t.value n s.value
  | Array (n,t,l) ->
      let l = List.map (fun x -> x.value) l in
      fprintf fmt "%s %s[] = %a" t.value n print_list (print_ident, l, ",")
  | GlobalVar (n,t, v) ->
      fprintf fmt "%s %s = %a" t.value n print_ident v.value

and print_process fmt = function
  | Process (n, a, d, i) ->
      let print_args fmt args =
        print_list fmt (print_arg, args, ",")
      in
      fprintf fmt "@[<v 0>proc %s(%a){@;<0 2> @[<v 0>%a@;%a@]@;}@;@]"
        n print_args a
        print_block_list (print_ident, "declaration.s",print_declaration, d)
        print_block_list (print_ident, "instruction.s", print_instruction,i)

and print_arg fmt arg =
  match arg.value with
  | Arg (n, t) -> fprintf fmt "%s %s" t.value n.value

and print_declaration fmt = function
  | DeclareVar (n,t) ->
      fprintf fmt "%s %s" t.value n.value

and print_instruction fmt = function
  | Assign (n,l) -> fprintf fmt "%s = %a" n.value print_literal l.value
  | AssignArray (n,i,l) ->
      fprintf fmt "%s[%s] = %a" n.value i.value print_literal l.value
  | Condition (c,i1,i2) ->
      print_if_else fmt (c, i1, i2)
  | Switch (l,c) ->
      print_switch fmt (l,c)
  | While (c,i) ->
      print_while fmt (c,i)
  | Decide l ->
      fprintf fmt "decide(%a)" print_literal l

and print_if_else fmt (c,i1, i2) =
  let print_else fmt i2 =
    print_block_list fmt
      (print_keyword, ("else", print_ident,""), print_instruction, i2)
  in
  fprintf fmt "@[<v 0>%a%a@]"
    print_block_list
      (print_keyword, ("if", print_compare, c), print_instruction, i1)
    print_option (print_else, i2)

and print_while fmt (c,i) =
   fprintf fmt "%a"
    print_block_list (
      print_keyword, ("while", print_compare, c), print_instruction,i
      )

and print_switch fmt (l,c) =
  fprintf fmt "%a"
    print_block_list (
      print_keyword, ("switch", print_literal,l.value), print_case, c
      )

and print_case fmt = function
  | Case (c,i) ->
      print_block_list fmt (print_case_args,c,print_instruction,i)

and print_case_args fmt = function
  | Wildcard -> fprintf fmt "Default"
  | CaseArg l ->
      fprintf fmt "(%a)" print_literal l.value

and print_compare fmt = function
  | And (c1, c2) ->
      fprintf fmt "@[(%a) and (%a)@]"
      print_compare c1 print_compare c2
  | Or (c1, c2) ->
      fprintf fmt "@[(%a) or (%a)@]"
      print_compare c1 print_compare c2
  | Equal (l1, l2) ->
      fprintf fmt "@[%a == %a@]"
      print_literal l1.value print_literal l2.value
  | NonEqual (l1, l2) ->
      fprintf fmt "@[%a != %a@]"
      print_literal l1.value print_literal l2.value
  | Boolean b -> fprintf fmt "%a" print_boolean b


and print_literal fmt = function
  | ArrayValue (n, i) -> fprintf fmt "%s[%s]" n.value i.value
  | Value v -> fprintf fmt "%s" v.value

and print_main fmt = function
  | Main c ->
      fprintf fmt "@[<v 0>main:@;%a@]" print_callable c

and print_callable fmt call =
  match call.value with
  | ForAll (n, t, c) ->
      fprintf fmt "@[for all %a in %s {@;%a@;}@]"
        print_list (print_ident, n, ",") t.value print_callable c
  | Parallel (c1, c2) ->
      fprintf fmt "(%a || %a)"
        print_callable c1 print_callable c2
  | SeqI (c1, c2) ->
      fprintf fmt "%a + %a"
        print_callable c1 print_callable c2
  | CallProcess (n,l) ->
      let args = list_position_opt_to_n l in
      let print_args fmt s =
        print_list fmt (print_literal, s, ",")
      in
      fprintf fmt "%s(%a)" n.value print_option (print_args, args)


(** Properties **)
and print_properties fmt properties =
  let rec print_return fmt = function
    | [] -> ()
    | x::xs ->
        fprintf fmt "%a@;" print_property x;
        print_return fmt xs
  in
  fprintf fmt "properties:@;@[<v 0>%a@]" print_return properties

and print_property fmt (e,o) =
  fprintf fmt "@[<v 0>* %a =>@;@[<v 0>%a@]" print_entry e print_output o

and print_entry fmt e =
  fprintf fmt "%a" print_list (print_proc_args, e, ",")

and print_output fmt o =
  let rec print_return fmt = function
    | [] -> ()
    | x::xs ->
        fprintf fmt "%a@;" print_procs_decide x;
        print_return fmt xs
  in
  fprintf fmt "%a" print_return o

and print_proc_args fmt (n,v) =
  fprintf fmt "%s(%a)" n print_list (print_ident, v, ",")

and print_procs_decide fmt lst =
  fprintf fmt "%a" print_list (print_proc_decide, lst, " &")

and print_proc_decide fmt = function
  | Tauto n -> fprintf fmt "%s:-" n
  | Exist (n,v) -> fprintf fmt "%s:%s" n v

(*
 * Chaboche - Marais
 * CALODS 2019
 *)

open PrismAst
open Types
open Helper

let prefix = "mdp\n\n"

(* Convert Prism Ast to str *)
let rec tostr_pm env = function
  | Program (gs, ps, i, sch, pps) ->
    let dict = dict_value_int env in
    let init_str = init env i in
    let sch_str = scheduler env sch in
    let gs_str = List.fold_left (fun acc x -> global x ^ acc) "\n" gs in
    let ps_str = List.fold_left (fun acc x -> process env x ^ "\n" ^ acc) "" (List.rev ps) in
    let (labels, formula) = properties pps in
    dict ^ prefix ^ gs_str ^ init_str ^ sch_str ^ ps_str ^ labels, formula

and global = function
  | GlobalVar (name, (b0, b1), i)->
     let b0' = b0 |> string_of_int in
     let b1' =
       if b1 = 0 then "1"
       else b1 |> string_of_int
     in
     "global " ^ name ^ " : [" ^ b0' ^ ".." ^ b1' ^ "] init " ^ (string_of_int i) ^ ";\n"

and dict_value_int env =
  let rec aux array i =
    if i >= Array.length array then ""
    else
      "//" ^ fst (Array.get array i) ^ " -> " ^ string_of_int (snd (Array.get array i)) ^ "\n"
      ^ aux array (i+1)
  in List.fold_left (fun acc x -> (aux (snd x) 0) ^ "\n" ^ acc) "" env.ty_assoc.types

and process env = function
  | Process m ->
    mods env m

and declaration = function
  | LocalVar (name, (b0, b1), init) ->
    " " ^ name ^ " : [" ^ (string_of_int b0) ^ ".." ^ (string_of_int b1) ^ "] init " ^ (string_of_int init) ^ ";\n"

and init env = function
  | Init m ->
    mods env m

and scheduler env = function
  | Some (Scheduler m) ->
    mods env m
  | None -> ""

and mods env = function
  | (name, Some id, decls, instrs) ->
    instructions env name id decls instrs ^ "\n"
  | (name, None, decls, instrs) ->
    instructions env name name decls instrs ^ "\n"
    
and instructions env name id decls instrs =
  let decls_str = List.fold_left (fun acc x -> acc ^ declaration x) "" decls in
  let instrs_str = List.fold_left (fun acc x -> acc ^ instruction env x ^ "\n") "" instrs in
  let labels = " "^ id ^" : [0.." ^ ((distinct_state instrs) |> string_of_int) ^ "]" ^ " init 0;\n"in
  "module " ^ name ^ "\n" ^ labels ^ decls_str ^instrs_str ^ "endmodule\n"
  
and instruction env = function
  | Instruction (lab, st_l, instr, st_r) ->
    let lab_str = " [" ^ (match lab with | Some x -> x | None -> "") ^ "] " in
    let stl_str = state true st_l in
    let str_str =
      match st_r with
      | None -> ""
      | Some st_r -> state false st_r ^ " & "
    in
    let instr_str = instruction env instr in
    if instr_str = "" then lab_str ^ stl_str ^ " -> " ^ (String.sub str_str 0 (String.length str_str - 3)) ^ ";"
    else lab_str  ^ stl_str ^ " -> " ^ str_str ^ instr_str ^ ";"
  | Affectation (n, i) ->
    "(" ^ n ^"'=" ^ i ^")"
  | IEmpty -> ""
  | Seq (i, i') ->
    let i_str = instruction env i in
    let i'_str = instruction env i' in
    i_str ^ " & " ^ i'_str

and from_dst from n i =
  if from then
    n ^ "=" ^ i
  else "(" ^ n ^ "'" ^ "=" ^ i ^ ")"

and state_args = function
  | Eq (v,v') -> " " ^ v ^ "=" ^ v'
  | NEq (v, v') -> " " ^ v ^ "!=" ^ v'
  | Bool true -> " true"
  | Bool false -> " false"
  | And (s, s') -> state_args s ^ " &" ^ state_args s'
  | Or (s, s') -> state_args s ^ " |" ^ state_args s'
  | SEmpty -> ""
   
and state from = function
  | State (n, i, Some c) ->
    from_dst from n (string_of_int i) ^ " &" ^ state_args c
  | State (n, i, None) ->
    from_dst from n (string_of_int i)

and test = function
  | EqualT (n, s) ->
    n ^ "=" ^ s
  | OrT (t1, t2) ->
    test t1 ^ " | " ^ test t2
  | AndT (t1, t2) ->
    test t1 ^ " & " ^ test t2
  | BoolT b ->
    string_of_bool b

and labels = function
  | [] -> "\n"
  | Label (name, t)::xs ->
    "label \"" ^ name ^ "\" = " ^ test t ^ ";\n" ^ labels xs


and formula = function
  | A f -> "A [\n" ^ formula f ^ "\n]\n"
  | G f -> "\tG (\n" ^ formula' f ^ "\n\t)"
  | F f -> "F " ^ formula' f
  | ArrowF (f1, f2) -> "(" ^ formula f1 ^ ") => (\n" ^ formula f2 ^ "\n\t\t)"
  | LabelF (name) -> "\"" ^ name ^ "\""
  | NotLabelF name -> "!\"" ^ name ^ "\""
  | BoolF b -> string_of_bool b
  | AndF (f, f') -> formula f ^ " & "^ formula f'
  | OrF (f1, f2) -> "(" ^ formula f1 ^ ") | " ^ formula_or f2

and formula_or = function
  | OrF (f1, f2) ->  "(" ^ formula_or f1 ^ ") | " ^ formula_or f2
  | f -> "(" ^ formula f ^ ")"

and formula' = function
  | OrF (f1, f2) -> "\t\t" ^ formula f1 ^ " |\n" ^ formula' f2
  | ArrowF (f1, f2) -> "\t\t(" ^ formula f1 ^ ") => (\n" ^ formula f2 ^ "\n\t\t)"
  | G f -> "G (" ^ formula f ^ ")"
  | f -> formula f

and properties = function
  | None -> "", ""
  | Some (l, f) ->
    labels l, formula f

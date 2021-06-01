(*

WHILE language à la [NNH].

*)


let get_fresh_id =
  let r = ref 0 in
  fun () -> r := !r +1 ; !r

let print_env env =
  Printf.printf "Env: ";
  List.iter (fun (c, i) -> Printf.printf "(%c, %i)" c i) env;
  Printf.printf "\n"

let print_store store =
  Printf.printf "Store: ";
  List.iter (fun (c, i) -> Printf.printf "(%i, %i)" c i) store;
  Printf.printf "\n"

(* Locations are a countable set; int is a finite approximation. *)
type location = int


(* An environment maps variable names into locations. *)
type env = (char * location) list


(* A store maps a finite set of locations into values. *)
type store = (location * int) list


(* Arithmetic expressions of WHILE *)
type aexpr =
  Var of char
| Cnt of int
| Plus of aexpr * aexpr
| Minus of aexpr * aexpr
| Mult of aexpr * aexpr

let rec string_of_aexpr = function
  | Var c -> String.make 1 c
  | Cnt i -> string_of_int i
  | Plus (e1, e2) -> "(" ^ string_of_aexpr e1 ^ "+" ^ string_of_aexpr e2 ^ ")"
  | Minus (e1, e2) -> "(" ^ string_of_aexpr e1 ^ "-" ^ string_of_aexpr e2 ^ ")"
  | Mult (e1, e2) -> "(" ^ string_of_aexpr e1 ^ "*" ^ string_of_aexpr e2 ^ ")"

(* Boolean expressions of WHILE *)
type bexpr =
  TT
| FF
| Not of bexpr
| And of bexpr * bexpr
| Eq of aexpr * aexpr
| Gt of aexpr * aexpr

let rec string_of_bexpr = function
  | TT -> "true"
  | FF -> "false"
  | Not e -> "(not " ^ string_of_bexpr e ^ ")"
  | And (e1, e2) -> "(" ^ string_of_bexpr e1 ^ " and " ^ string_of_bexpr e2 ^ ")"
  | Eq (e1, e2) -> "(" ^ string_of_aexpr e1 ^ " == " ^ string_of_aexpr e2 ^ ")"
  | Gt (e1, e2) -> "(" ^ string_of_aexpr e1 ^ " > " ^ string_of_aexpr e2 ^ ")"

(* Label *)
type label = int



(* Set of labels *)
module Label = struct
  type t = label
  let compare = compare
end
module LabelSet = Set.Make(Label)



(* Procedures have names that are strings *)
type procname = string


(* Statements of WHILE plus the runtime syntax, i.e. bind *)
type stm =
  Skip of label
| Ass of char * aexpr * label
| If of bexpr * label * stm * stm
| While of bexpr * label * stm
| Seq of stm * stm
| Call of procname * (aexpr list) * char * label * label
| Bind of env * stm * char * char

(* Returns a string representation of a statement *)
let rec string_of_stm ?(code = false) stm =
  let labelize str lab =
    "[" ^ str ^ "]" ^ (string_of_int lab)
  in
  match stm with
  | Skip l -> labelize "Skip" l
  | Ass (c, aexpr, l) ->
    let str = (String.make 1 c) ^ " := " ^ (string_of_aexpr aexpr) in
    labelize str l
  | If (bexpr, l, stm1, stm2) ->
    let bexpr_str = string_of_bexpr bexpr in
    if code then
      "If " ^ (labelize bexpr_str l)
    else
      "If " ^ (labelize bexpr_str l) ^ " then\n" ^ (string_of_stm stm1)
      ^ "\nelse\n" ^ (string_of_stm stm2)
  | While (bexpr, l, stm) ->
    let bexpr_str = string_of_bexpr bexpr in
    if code then
      "While " ^ (labelize bexpr_str l)
    else
      "While " ^ (labelize bexpr_str l) ^ " do\n" ^
      "(" ^ string_of_stm stm ^ ")"
  | Seq (stm1, stm2) ->
    (string_of_stm stm1) ^ ";\n" ^ (string_of_stm stm2)

  (* We do not suppor bind and call *)
  | Bind _ | Call _ -> "[Bind and Call are not supported]"

let print_stm stm =
  Printf.printf "%s\n" (string_of_stm stm)


(* Procedure declarations *)
type proc =
  Proc of procname * (char list) * char * stm * label * label



(* A program in WHILE is
   - a list of procedure declarations followed by
   - a statement that plays the role of the "main" procedure *)
type program = Program of (proc list) * stm



(* 
   Program blocks are either statements, function calls,
   entry/exit points of procedures, and boolean conditions in
   if ... then ... else ... 
 *)
type block =
  BBool of bexpr * label
| BSkip of label
| BAss of char * aexpr * label
| BCall of procname * (aexpr list) * char * label * label
| BIs of label
| BEnd of label

(* Set of blocks *)
module Block = struct
  type t = block
  let compare = compare
end
module BlockSet = Set.Make(Block)

(* Create a b set fills with b *)
let bset (b : block) =
  BlockSet.empty
  |> BlockSet.add b

(* Compute the blocks contained in a given program *)
let rec blocks_stm (s : stm) =
  match s with
  | Skip l -> bset (BSkip l)
  | Ass (c, a ,l) -> bset (BAss (c,a,l))
  | If (b,l, s1, s2) ->
      let b = bset (BBool (b,l)) in
      let b1 = blocks_stm s1 in
      let b2 = blocks_stm s2 in
      BlockSet.union b b1 |> BlockSet.union b2
  | While (b, l, s) ->
      let b = bset (BBool (b, l)) in
      let b1 = blocks_stm s in
      BlockSet.union b b1
  | Seq (s1, s2) ->
      let b1 = blocks_stm s1 in
      let b2 = blocks_stm s2 in
      BlockSet.union b1 b2
  | Call (p, al, c, lc, lr) -> bset (BCall (p, al, c, lc, lr))
  | _ -> assert false

let blocks (p : program) = match p with
  | Program ([], stm) -> blocks_stm stm
  | _ -> assert false


(* Compute the labels contained in a given statement *)
let labels_stm (s : stm) =
  let get_label block acc =
    match block with
    | BBool (_, l) -> LabelSet.add l acc
    | BSkip l -> LabelSet.add l acc
    | BAss (_,_, l) -> LabelSet.add l acc
    | BCall (_,_,_,lc, lr) ->
        LabelSet.add lc acc |> LabelSet.add lr
    | BIs l -> LabelSet.add l acc
    | BEnd l -> LabelSet.add l acc
  in
  let blocks = blocks_stm s in
  BlockSet.fold get_label blocks LabelSet.empty

let labels (p : program) = match p with
  | Program ([], stm) -> labels_stm stm
  | _ -> assert false



(* Evaluates to true iff every label in stm appears only in one block. *)
let label_consistent_stm stm =
  let check labels_list =
    (LabelSet.cardinal (labels_stm stm)) = (List.length labels_list)
  in
  let rec labels_list stm = match stm with
    | Skip l -> [l]
    | Ass (_, _, l) -> [l]
    | While (b, l, s) -> l :: (labels_list s)
    | If (_, l, _, _) -> [l]
    | Seq (s1, s2) -> (labels_list s1) @ (labels_list s2)
    | Bind _ | Call _-> assert false
  in
  check (labels_list stm)

let label_consistent (p : program) = match p with
  | Program ([], stm) -> label_consistent_stm stm
  | _ -> assert false


(* Evaluates to true iff stm
   - is label consistent
   - contains no *bind* statement *)
let well_formed_stm (s : stm) =
  match s with
  | Bind (_,_,_,_) -> false
  | s -> label_consistent_stm s

let well_formed (p : program) = match p with
  | Program ([], stm) -> well_formed_stm stm
  | _ -> assert false



(*****   OPERATIONAL SEMANTICS OF WHILE PROGRAMS   ****)


(* The result of __one step__ of computation *)
type result =
  RUpdated of stm * store
| RStore of store



(**** Auxiliary functions: evaluations of expressions. ****)

(* Evaluate an arithmetic expression in a given environment and store *)
let rec aaeval exp env store = match exp with
  | Var c -> List.assoc (List.assoc c env) store
  | Plus (e1, e2) -> aaeval e1 env store + aaeval e2 env store
  | Minus (e1, e2) -> aaeval e1 env store - aaeval e2 env store
  | Mult (e1, e2) -> aaeval e1 env store * aaeval e2 env store
  | Cnt n -> n


(* Evaluate a boolean expression in a given environment and store *)
let rec bbeval exp env store = match exp with
| TT -> true
| FF-> false
| Not b -> not (bbeval b env store)
| And (b1, b2)  -> (bbeval b1 env store) && (bbeval b2 env store)
| Eq (a1, a2) -> (aaeval a1 env store) = (aaeval a2 env store)
| Gt (a1, a2) -> (aaeval a1 env store) < (aaeval a2 env store)


(* Make a statement stm perform one step of computation, according to the SOS rules. *)
let rec one_step stm (procs : proc list) env store =
  match stm with
  | Skip _l -> RStore store
  | Ass (c, e, _l) ->
    let vc = List.assoc c env in
    let ve = aaeval e env store in
    let store = (vc, ve) :: store in
    RStore store
  | If (bexpr, _l, stm1, stm2) ->
    let b = bbeval bexpr env store in
    if b then RUpdated (stm1, store)
    else RUpdated (stm2, store)
  | While (bexpr, _l, stm) as loop ->
    let b = bbeval bexpr env store in
    if b then 
      let seq = Seq (stm, loop) in
      RUpdated (seq, store)
    else RUpdated (stm, store)
  | Seq (stm1, stm2) ->
    let st = one_step stm1 procs env store in
    begin
      match st with
      | RStore store ->
        RUpdated (stm2, store)
      | RUpdated (stm, store) ->
        RUpdated (Seq (stm, stm2), store)
    end
  | Call (procname, exprs, z, _l1, _l2) ->
    let proc = List.find (fun (Proc(procname', _, _, _, _, _)) -> procname=procname') procs in
    let (args, y, stm) = match proc with Proc (_, args, y, stm, _, _) -> (args, y, stm) in
    let ids = List.map (fun _ -> get_fresh_id ()) args in
    let new_env= List.fold_left2 (fun acc c id -> (c, id) :: acc) env args ids in
    let store = List.fold_left2
        (fun acc e id ->
           let v = aaeval e env store in
           (id, v) :: acc
        ) store exprs ids
    in
    let id_y = get_fresh_id () in
    let new_env = (y, id_y) :: new_env in
    let store = (id_y, 0) :: store in
    RUpdated (Bind (new_env, stm, z, y), store)
  | Bind (env, stm, c1, c2) ->
    let st = one_step stm procs env store in
    begin
      match st with
      | RStore store ->
        let vc1 = List.assoc c1 env in
        let vc2 = List.assoc (List.assoc c2 env) store in
        let store = (vc1, vc2) :: store in
        RStore store
      | RUpdated (stm, store) ->
        RUpdated (Bind (env, stm, c1, c2), store)
    end

(*
  Capture and add global variables.
  *)
let add_if_not_exists c env store =
  match List.assoc_opt c env with
  | Some _ -> env, store
  | None ->
      let id = get_fresh_id () in
      let env = (c, id) :: env in
      let store = (id, 0) :: store in
      env, store

let rec capture_globals env store = function
  | Seq (stm1, stm2) ->
      let env, store = capture_globals env store stm1 in
      capture_globals env store stm2
  | Ass (c, e, _) ->
      let env, store = add_if_not_exists c env store in
      capture_globals_a env store e
  | If (bexpr, _, stm1, stm2) ->
    let env, store = capture_globals_b env store bexpr in
    let env, store = capture_globals env store stm1 in
    let env, store = capture_globals env store stm2 in
    capture_globals env store stm2
  | While (bexpr, _, stm) ->
    let env, store = capture_globals_b env store bexpr in
    capture_globals env store stm
  | _ -> env, store

and capture_globals_a env store = function
  | Var c -> add_if_not_exists c env store
  | Plus (e1, e2) |Minus (e1, e2) | Mult (e1, e2) ->
    let env, store = capture_globals_a env store e1 in
    let env, store = capture_globals_a env store e2 in
    env, store
  | _ -> env, store

and capture_globals_b env store = function
| Not b -> capture_globals_b env store b
| And (b1, b2) ->
    let env, store = capture_globals_b env store b1 in
    let env, store = capture_globals_b env store b2 in
    env, store
| Eq (a1, a2) | Gt (a1, a2) ->
    let env, store = capture_globals_a env store a1 in
    let env, store = capture_globals_a env store a2 in
    env, store
| _ -> env, store




(* 
   Run a program p in an environment env and a store str.
 *)
let rec run (p : program) env str =
  let Program (procs, stm) = p in
  let step = one_step stm procs env str in
  match step with
  | RStore _ as store -> store
  | RUpdated (stm, str) ->
      let program = Program (procs, stm) in
      run program env str



(* Here are two WHILE programs that you can use to test your functions *)

let prog1 = Seq (Ass ('x', Cnt 5, 1),
               Seq (Ass ('y', Cnt 1, 2),
                    While (Gt (Var 'x', Cnt 1), 3,
                                Seq (Ass ('y', Mult (Var 'x', Var 'y'), 4),
                                     Ass ('x', Minus (Var 'x', Cnt 1), 5)))))


let prog2 = Seq (Ass ('x', Plus (Var 'z', Var 'b'), 1),
               Seq (Ass ('y', Mult (Var 'z', Var 'b'), 2),
                    While (Gt (Var 'y', Plus (Var 'z', Var 'b')), 3,
                           Seq (Ass ('z', Plus (Var 'z', Cnt 1), 4),
                                Ass ('x', Plus (Var 'z', Var 'b'), 5)))))

let prog3 =
    Seq (
    Ass ('x', Cnt 0, 1),
    Seq (
    Ass ('y', Cnt 1, 2),
    Seq (
    Ass('z', Cnt (-1), 3),
    Seq (
    Ass ('a', Mult (Var 'x', Var 'y'), 4),
    Seq (
    Ass ('b', Mult (Var 'y', Var 'z'), 5),
    Seq (
    Ass ('c', Mult (Var 'x', Var 'z') , 6),
    Seq (
    Ass ('d', Minus (Var 'x', Var 'y'), 7),
    Seq (
    Ass ('e', Minus (Var 'y', Var 'z'), 8),
    Seq (
    Ass ('f', Minus (Var 'x', Var 'z'), 9),
    Seq (
    Ass ('g', Plus (Var 'x', Var 'y'), 10),
    Seq (
    Ass ('h', Plus (Var 'y', Var 'z'), 11),
    Ass ('i', Plus (Var 'x', Var 'z'), 12)
    )))))))))))


let _ =
  let env, store = capture_globals [] [] prog3 in
  let p = Program ([], prog3) in
  match run p env store with
  | RStore store ->
      print_env env;
      print_store store
  | _ -> Printf.printf "Nope\n"

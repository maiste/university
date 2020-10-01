(*
 * Chaboche - Marais
 * CALODS - 2019
 *)

open PrismAst
open Types
open Helper

(* This file will implements translation of instructions *)

module S = Compiler.Ast

module T = PrismAst

 

(* Add a synchronizing state before any read/write on global variables *)
let synchronize_label conf b =
  if b && conf.double then
    let set_active = Affectation ("_active_"^conf.label, "1") in
    let curr = state ~l:conf.label ~id:conf.id () in
    let next = state ~l:conf.label ~id:(conf.id+1) () in
    let instr = Instruction (Some conf.label, curr, set_active, Some next) in
    ([instr], {conf with id=conf.id+1; })
  else
    ([], conf)

let set_active_to_false conf bef is =
  let afct = T.Affectation ("_active_"^conf.label, "0") in
  if List.length bef > 0 then
    match is with
    | T.IEmpty -> afct
    | _ -> Seq (is, afct)
  else
    is

(* Reverse a comparison *)
let rec reverse_comp = function
  | S.Boolean true -> S.Boolean false
  | S.Boolean _ -> S.Boolean true
  | S.Equal (l, l') -> S.NonEqual (l, l')
  | S.NonEqual (l, l') -> S.Equal (l, l')
  | S.And (c, c') -> S.Or (reverse_comp c, reverse_comp c')
  | S.Or (c, c') -> S.And (reverse_comp c, reverse_comp c')
                      
(* Create state with comparison *)
let state_of_comp env conf cmp =
  let rec args = function
    | S.Boolean b -> T.Bool b
    | S.Equal (l, l') ->
      T.Eq (literal env conf.name (explode l), literal env conf.name (explode l'))
    | S.NonEqual (l, l') ->
      T.NEq (literal env conf.name (explode l), literal env conf.name (explode l'))
    | S.And (c, c') ->
      T.And (args c, args c')
    | S.Or (c, c') ->
      T.Or (args c, args c')
  in
  let arg = args cmp in
  state ~args:(Some arg) ~l:conf.label ~id:(conf.id) ()


let rec translate_instruction env conf = function
  | S.Assign ({value=n; _}, {value = l; _}) ->
    translate_assign env conf n (T.Affectation (n, literal env conf.name l))
  | S.AssignArray ({value = n; _}, {value =v; _}, {value=l; _}) ->
    let n' = "_" ^ n ^ "_case" ^ v in
    translate_assign env conf n' (T.Affectation (n', literal env conf.name l))
  | S.Switch ({value= v; _}, cases) ->
    translate_switch env conf (literal env conf.name v) cases
  | S.Decide v -> translate_decide env conf (literal env conf.name v)
  | S.While (cmp, ins) ->
    translate_while env conf cmp (explodes ins)
  | S.Condition (cmp, if_ins, else_ins) ->
    translate_ifelse env conf cmp if_ins else_ins

and translate_instructions_with_next_id env conf next =
  let rec match_on_instrs env conf acc = function
    | [] -> acc
    | [i] ->
      let conf = { conf with next_id = Some next } in
      let (_, l) = translate_instruction env conf i in
      acc@l
    | x::xs ->
      let (conf, l) = translate_instruction env conf x in
      match_on_instrs env conf (acc@l) xs
  in
  (fun e -> match_on_instrs env conf [] e)
  
  
and translate_assign env conf name afct =
  let (bef, conf) = synchronize_label conf (is_global env name) in
  let curr = state ~l:conf.label ~id:conf.id () in
  let next = state_next ~l:conf.label ~id:(conf.id+1) ~next_id:conf.next_id () in
  ({ conf with id = conf.id+1 },
   bef @
   [Instruction (None, curr, set_active_to_false conf bef afct, Some next)]
  )

and translate_switch env conf v cases =
   let length_case = function
    | S.Case (_, ins) ->
      let ins = translate_instructions_with_next_id env conf 0 (explodes ins) in
      distinct_state ins
   in

   let next_id = match conf.next_id with
     | Some i -> i
     | None ->
       let offset = List.fold_left
           (fun acc x -> acc + length_case x) 0 (explodes cases) in
       offset + conf.id
   in
   
   let (bef, conf) = synchronize_label conf (is_global env v) in
  
    
   let (switches, branchs, _) = List.fold_left
       (fun (switches, branchs, branch_id) x ->
          let (switches', branchs') = translate_case env conf next_id bef branch_id v x in
          (switches@switches', branchs@branchs', branch_id+(distinct_state branchs'))
       ) ([], [], conf.id+1) (explodes cases)
   in
   ({conf with id=conf.id+(distinct_state branchs)}, bef@switches@branchs)

and translate_case env conf next_id bef branch_id value c =
  let rebuild_head = function
    | T.Instruction (n, s, i, s')::xs ->
      T.Instruction (n, s, set_active_to_false conf bef i, s') :: xs
    | _ -> assert false
  in
  let case = function
    | S.Case (Wildcard, ins) ->
      (state ~l:conf.label ~id:conf.id (), ins)
    | S.Case (S.CaseArg ({value=arg; _}), ins) ->
      let arg = literal env conf.name arg in
      let cmp = Some (T.Eq (value, arg)) in
      (state ~l:conf.label ~id:conf.id ~args:cmp (), ins)
  in
  let (curr, ins) = case c in
  let conf = { conf with id=branch_id } in
  let branchs = translate_instructions_with_next_id env conf next_id (explodes ins) in
  let next = state_next ~l:conf.label ~id:branch_id ~next_id:conf.next_id () in
  ( [T.Instruction (None, curr, T.IEmpty, Some next)],
    (rebuild_head branchs)
  )
    
and translate_decide env conf v =
  let (bef, conf) = synchronize_label conf (is_global env v) in
  let curr = state ~l:conf.label ~id:conf.id () in
  let afct = T.Affectation ("_decide_"^conf.label, v) in
  ({conf with id=conf.id+1}, [T.Instruction (None, curr, set_active_to_false conf bef afct, None)])

and translate_while env conf cmp ins =
  let (bef, conf) = synchronize_label conf (rw_on_cmp env cmp) in
  let offset = translate_instructions_with_next_id env conf 0 ins |> List.length in

  (* Create a prism test for the comparison *)
  let test_true = state_of_comp env conf cmp in
  let test_false = state_of_comp env conf (reverse_comp cmp) in
      
  (* Create tests with test *)
  let state_true_next =
    state_next ~l:conf.label
    ~id:(
      if offset>0 then conf.id+1
      else if (List.length bef = 0) then conf.id
      (* The test is synchronized, we need to go back to active the proc again *)
      else conf.id - 1
    )
    ~next_id:conf.next_id ()
  in
  let state_false_next =  state_next ~l:conf.label ~id:(conf.id+offset+1) ~next_id:conf.next_id () in

  let is = set_active_to_false conf bef T.IEmpty in

  (* We translate the instructions in the while *)
  let next_id = match conf.next_id with
    | Some i -> i
    | None -> conf.id+offset+1
  in

  let instrs = translate_instructions_with_next_id env {conf with id=conf.id+1} next_id ins in

  ({conf with id=next_id},
   bef @
   [
   T.Instruction (None, test_true, is, Some state_true_next);
   T.Instruction (None, test_false, is, Some state_false_next);
 ] @
   instrs
  )

and translate_ifelse env conf cmp if_ins else_ins =
  let (bef, conf) = synchronize_label conf (rw_on_cmp env cmp) in
  
  let if_ins = explodes if_ins in
  let else_ins = match else_ins with
    | None -> []
    | Some l -> (explodes l)
  in

  let offset_if = translate_instructions_with_next_id env conf 0 if_ins |> distinct_state in
  let offset_else = translate_instructions_with_next_id env conf 0 else_ins |> distinct_state in

  
  (* Create real next_id *)
  let next_id = match conf.next_id with
    | None -> conf.id + offset_if + offset_else + 1
    | Some i -> i
  in

  (* Translate branch when if is true *)
  let conf_if = {conf with id=conf.id+1} in
  let if_ins = translate_instructions_with_next_id env conf_if next_id if_ins in
  let test_true = state_of_comp env conf cmp in
  let state_true_next = state_next ~l:conf.label ~id:(if List.length if_ins = 0 then conf.id+offset_else+1 else conf.id+1) ~next_id:conf.next_id () in
  
  (* Translate branch when if is false *)
  let conf_else = {conf with id=conf.id+offset_if+1} in
  let else_ins = translate_instructions_with_next_id env conf_else next_id else_ins in
  let test_false = state_of_comp env conf (reverse_comp cmp) in
  let state_false_next = state_next ~l:conf.label ~id:(conf.id+offset_if+1) ~next_id:conf.next_id () in

  let total_offset = conf.id + offset_if + offset_else in


  let is =  set_active_to_false conf bef T.IEmpty in

  ({conf with id=total_offset+1},
   bef @
   [ T.Instruction (None, test_true, is, Some state_true_next);
     T.Instruction (None, test_false, is, Some state_false_next) ] @
   if_ins @
   else_ins
  )

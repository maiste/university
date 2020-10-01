(*
 * Chaboche - Marais
 * CALODS - 2019
 *)



(** Modules **)

module Position =  Compiler.Position
module S = Compiler.Ast
module T = IlodsAst



(** Builder **)

(* Build an action instruction *)
let build_action action =
  action, T.Next

(* Build a jump *)
let build_jump comp jump =
  T.Jump (comp, T.Next), T.Goto jump

(* Build an unknown jump *)
let build_unknown_move () =
  T.Move, T.Unknown

let replace_unknown jump =
  T.Move, T.Goto jump

let replace_all_unknown lst jump =
  List.map (
    fun (action, goto) ->
      if (action, goto) = (T.Move, T.Unknown) then
        replace_unknown jump
      else action, goto
  ) lst

(* Build return to line *)
let build_move line =
  T.Move, T.Goto line

(* Build a decide block *)
let build_decide l =
  T.Decide l, T.Finish



(** Tanslation **)

(* Translate arguments into a name list *)
let translate_args args =
  let get_name a =
    match Position.value a with
    S.Arg (n,_) -> Position.value n
  in
  let translate_arg acc elt =
    (get_name elt) :: acc
  in
  List.fold_left translate_arg [] args
  |> List.rev

(* Translate declarations *)
let translate_decls d =
  let get_name d =
    match Position.value d with
    S.DeclareVar (n,_) -> Position.value n
  in
  let translate_decl acc elt =
    let decl = build_action (T.Declare (get_name elt))
    in decl::acc
  in
  List.fold_left translate_decl [] d
  |> List.rev

(* Transform a Calods litteral into an Ilods one *)
let translate_lit l =
  match l with
  | S.ArrayValue (n,v) ->
      T.ArrayValue (Position.value n, Position.value v)
  | S.Value v -> T.Value (Position.value v)

(* Translate compare *)
let rec translate_comp c =
  match c with
  | S.And (c1, c2) ->
      let c1 = translate_comp c1 in
      let c2 = translate_comp c2 in
      T.And (c1, c2)
  | S.Or (c1, c2) ->
      let c1 = translate_comp c1 in
      let c2 = translate_comp c2 in
      T.Or (c1, c2)
  | S.Equal (l1, l2) ->
      let l1 = translate_lit (Position.value l1) in
      let l2 = translate_lit (Position.value l2) in
      T.Equal (l1, l2)
  | S.NonEqual (l1, l2) ->
      let l1 = translate_lit (Position.value l1) in
      let l2 = translate_lit (Position.value l2) in
      T.NonEqual (l1, l2)
  | S.Boolean b -> T.Bool b

(* Translate instructions *)
let rec translate_instr line inst =
  match inst with
  | S.Assign (n,l) ->
      let n = Position.value n in
      let l = Position.value l in
      assign n l
  | S.AssignArray (n,v,l) ->
      let n = Position.value n in
      let v = Position.value v in
      let l = Position.value l in
      assign_array n v l
  | S.Condition (cond, i1, i2) ->
      if_then_else line cond i1 i2
  | S.Switch (lit, cases) ->
      let lit = Position.value lit in
      switch line lit cases
  | S.While (cond, instrs) ->
      loop line cond instrs
  | S.Decide l ->
      decide l

and translate_instrs line l =
  let _, lst =
    List.fold_left (fun (line, acc) elt ->
      let inst = Position.value elt in
      let inst = translate_instr line inst in
      (line + List.length inst, acc @ inst)
    ) (line,[]) l
  in lst

and assign n l =
  [T.Assign (n, translate_lit l) |> build_action]

and assign_array n v l =
  [T.AssignArray (n,v, translate_lit l) |> build_action]

and if_then_else l c i1 i2 =
  let i1 = translate_instrs (l+1) i1 in
  let jump_if = List.length i1 + l + 1 in
  let else_jump = else_branch jump_if i2 in
  let jump_if = jump_if + if else_jump = [] then 0 else 1 in
  let test_if = build_jump (translate_comp c) jump_if in
  (test_if :: i1) @ (else_branch jump_if i2)

and else_branch l i2 =
  match i2 with
  | None -> []
  | Some i2 ->
      let i2 = translate_instrs l i2 in
      let jump_else = List.length i2 + l in
      let move_else = build_move jump_else in
      move_else :: i2

and switch line l cs =
  let l = translate_lit l in
  let treat_case (line, acc) elt =
    let elt = Position.value elt in
    let c = case line l elt in
    (line + List.length c, acc @ c)
  in
  let (_, lst) = List.fold_left treat_case (line, []) cs in
  replace_all_unknown lst (List.length lst)

and case line lit (S.Case (c, is)) =
  let is = translate_instrs line is in
  let c = case_arg lit c in
  let jump = List.length is + line + 2 in
  let test_jump = build_jump c jump in
  let escaped_jmp = build_unknown_move () in
  test_jump :: is @ [escaped_jmp]

and case_arg l1 c =
  match c with
  | S.Wildcard -> T.Bool true
  | S.CaseArg l2 ->
      let l2 = Position.value l2 in
      T.Equal (l1, translate_lit l2)

and loop l c i =
  let c = translate_comp c in
  let i = translate_instrs (l+1) i in
  let jump = List.length i + l + 2 in
  let test_loop = build_jump c jump in
  let ret_loop = build_move l in
  test_loop :: i @ [ret_loop]

and decide l =
  [translate_lit l |> build_decide]

let translate_proc (S.Process (n,args, dcls, insts)) : T.process =
  let args = translate_args args in
  let dcls = translate_decls dcls in
  let insts = translate_instrs (List.length dcls) insts in
  let last_inst = [T.Empty, T.EOF] in
  let block = (dcls @ insts @ last_inst) |> Array.of_list
  in n,args,block


let translate_data d =
  match Position.value d with
  | S.DefineType (ty, vs) ->
      let vs =
        List.fold_left (
          fun acc v -> Position.value v :: acc
        ) [] vs |> List.rev
        in ty, vs

let translate_global g =
  match Position.value g with
  | S.EmptyArray (n,_,v) ->
      let v = Position.value v in
      T.EmptyArray (n, v)
  | S.Array (n, _, vs) ->
      let vs =
        List.fold_left (
          fun acc v -> Position.value v :: acc
        ) [] vs |> List.rev
      in T.Array (n, vs)
  | S.GlobalVar (n, _, v) ->
      let v = Position.value v in
      T.GlobalVar (n, v)

let translate_header (S.Header (ds,gs)) =
  let get_list gs =
    Option.to_list gs |> List.flatten
  in
  let gs =
    List.fold_left (
      fun acc g -> translate_global g :: acc
    ) [] (get_list gs) |> List.rev
  in
  let ds =
    List.fold_left (
      fun acc d -> translate_data d :: acc
    ) [] ds |> List.rev
  in ds, gs

let rec translate_callable c =
  match Position.value c with
  | S.ForAll (n,t,c) ->
      let t = Position.value t in
      let c = translate_callable c in
      T.ForAll (n, t, c)
  | S.Parallel (c1, c2) ->
      let c1 = translate_callable c1 in
      let c2 = translate_callable c2 in
      begin
        match c1, c2 with
        | T.Parallel c1, T.Parallel c2 -> T.Parallel (c1 @ c2)
        | T.Parallel cp, c -> T.Parallel (cp @ [c])
        | c, T.Parallel cp -> T.Parallel (c::cp)
        | _ -> T.Parallel (c1::c2::[])
      end
  | S.SeqI (s1, s2) ->
      let s1 = translate_callable s1 in
      let s2 = translate_callable s2 in
      begin
        match s1, s2 with
        | T.SeqI s1, T.SeqI s2 -> T.SeqI (s1 @ s2)
        | T.SeqI s1, s -> T.SeqI (s1 @ [s])
        | s, T.SeqI s1 -> T.SeqI (s :: s1)
        | _ -> T.SeqI (s1::s2::[])
      end
  | S.CallProcess (n, ls) ->
      let n = Position.value n in
      let ls = Option.to_list ls |> List.flatten in
      let ls =
        List.fold_left (
          fun acc l ->
            (Position.value l
            |> translate_lit) :: acc
        ) [] ls |> List.rev
      in T.Call (n, ls)

let translate_main (S.Main c) =
  translate_callable c

let translate_prog (S.Program (h,ps,m,_,_)) : T.program =
  let h = translate_header h in
  let ps =
    List.fold_left (
      fun acc p -> translate_proc p :: acc
    ) [] ps |> List.rev
  in
  let m = translate_main m in
  h, ps, m

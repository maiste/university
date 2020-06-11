(** This module provides helper functions for Retrolix program
   analysis and manipulation. *)

open RetrolixAST

module LValueOrd = struct
  type t = lvalue
  let compare = compare
  let print = RetrolixPrettyPrinter.lvalue
end

module LValueMap = ExtStd.Map (LValueOrd)
module LValueSet = ExtStd.Set (LValueOrd)

module LabelOrd = struct
  type t = label
  let compare (Label l1) (Label l2) = String.compare l1 l2
end

type location = lvalue

module LSet = Set.Make (struct
    type t = location
    let compare = compare
end)

let find_default d k m =
  try LabelMap.find k m with Not_found -> d

let join rs =
  List.fold_left (fun s x -> LSet.add x s) LSet.empty rs

let string_of_register (RId r) = r

let string_of_lvalue = function
  | `Register (RId r) -> r
  | `Variable (Id r) -> r

let string_of_label (Label s) = s

let string_of_lset s =
  String.concat " " (List.map string_of_lvalue (LSet.elements s))

let string_of_lmap m =
  String.concat "\n" (
      List.map (fun (l, s) ->
          Printf.sprintf "  %s : %s\n" (string_of_label l) (string_of_lset s)
      ) (LabelMap.bindings m)
  )

let register r =
  `Register (RId (X86_64_Architecture.string_of_register r))

let global_variables p =
  let translate p =
    let rec program ds =
      List.(concat (map definition ds))
    and definition = function
      | DValues (xs, b) ->
         xs
      | _ ->
         []
    in
    program p
  in
  translate p

let map_on_value lvalue rvalue = function
  | Call (r, rs, b) ->
     Call (rvalue r, List.map rvalue rs, b)
  | Ret ->
     Ret
  | Assign (l, o, rs) ->
     Assign (lvalue l, o, List.map rvalue rs)
  | Jump l ->
     Jump l
  | ConditionalJump (c, rs, l1, l2) ->
     ConditionalJump (c, List.map rvalue rs, l1, l2)
  | Switch (r, ls, l) ->
     Switch (rvalue r, ls, l)
  | Comment c ->
     Comment c
  | Exit ->
     Exit

(** [predecessors b] returns a function [pred] such that [pred l]
   returns the predecessors of [l] in the control flow graph of
   the block [b]. *)
let predecessors b =
  let block m (_, instructions) =
    let new_predecessor prev m curr =
      try
        let s = LabelMap.find curr m in
        let s = LabelSet.add prev s in
        LabelMap.add curr s m
      with Not_found ->
        LabelMap.add curr (LabelSet.singleton prev) m
    in
    let rec traverse m = function
      | (label, Jump goto_label) :: instructions ->
         let m = new_predecessor label m goto_label in
         traverse m instructions
      | (label, ConditionalJump (_, _, l1, l2)) :: instructions ->
         let m = List.fold_left (new_predecessor label) m [l1; l2] in
         traverse m instructions
      | (label, Switch (_, labels, default)) :: instructions ->
         let labels =
           (match default with None -> [] | Some x -> [x])
           @ (Array.to_list labels)
         in
         let m = List.fold_left (new_predecessor label) m labels in
         traverse m instructions
      | (ilabel, _) :: (((nlabel, _) :: _) as instructions) ->
         let m = new_predecessor ilabel m nlabel in
         traverse m instructions
      | [ _ ] | [] ->
         m
    in
    traverse m instructions
  in
  let m = block LabelMap.empty b in
  fun l -> try LabelMap.find l m with Not_found -> LabelSet.empty

let nondefault_targets insn =
  match insn with
  | Call _ | Ret | Assign _ | Comment _ | Exit ->
     []
  | Jump l ->
     [l]
  | ConditionalJump (_, _, l1, l2) ->
     [l1; l2]
  | Switch (_, a, o) ->
     ExtStd.Option.fold (fun l acc -> acc @ [l]) o (Array.to_list a)

let rec instruction_targets (insns : labelled_instruction list) =
  let targets lab insn next_lab =
    match insn with
    | Call (_, _, false) | Assign _ | Comment _  ->
       next_lab

    | Call (_, _, true) | Ret | Exit ->
       []

    | Jump lab ->
       [lab]

    | ConditionalJump (_, _, lab1, lab2) ->
       [lab1; lab2]

    | Switch (_, laba, labo) ->
       ExtStd.Option.fold (fun lab acc -> acc @ [lab]) labo (Array.to_list laba)
  in
  let rec loop insns =
    match insns with
    | [] ->
       []
    | (lab, insn) :: insns ->
       (lab,
        insn,
        targets lab insn (match insns with [] -> [] | (lab, _) :: _ -> [lab]))
       :: loop insns
  in
  loop insns

let map_blocks f definitions =
  let definition def =
    match def with
    | DValues (xs, block) ->
       DValues (xs, f block)
    | DFunction (fn, xs, block) ->
       DFunction (fn, xs, f block)
    | DExternalFunction _ ->
       def
  in
  List.map definition definitions

let map_instructions f ((locals, insns) : block) =
  (locals, List.map (fun ((l, i) as li) -> l, f li) insns)

let transform_block analyse rewrite block =
  let open PPrint in
  let info = analyse block in
  if Options.get_debug_mode () then
    ExtPPrint.to_stdout
      (!^ "Input block:"
       ^//^ RetrolixPrettyPrinter.block
                RetrolixPrettyPrinter.nodecorations
                block);
  let block = map_instructions (rewrite info) block in
  if Options.get_debug_mode () then
    ExtPPrint.to_stdout
      (!^ "Output block:"
       ^//^ RetrolixPrettyPrinter.block
                RetrolixPrettyPrinter.nodecorations
                block);
  block

let transform_blocks analyse rewrite definitions =
  map_blocks (transform_block analyse rewrite) definitions

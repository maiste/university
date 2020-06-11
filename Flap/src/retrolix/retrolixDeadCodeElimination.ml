open RetrolixAST
open RetrolixUtils

let activated = ref false

module Source = Retrolix

let shortname = "dce"

let longname = "dead-code elimination"

(** {2 The Analysis Itself} *)

module LivenessDomain =
  struct
    type t = LValueSet.t

    let print = LValueSet.print

    let equal = LValueSet.equal

    let compare = LValueSet.compare

    let bot = LValueSet.empty

    let le = LValueSet.subset

    let lub = LValueSet.union

    let from_value x =
      match x with
      | #lvalue as lv -> LValueSet.singleton lv
      | _ -> LValueSet.empty

    let global_variables =
      ref bot

    let from_value_list xs =
      List.fold_left (fun acc x -> LValueSet.union (from_value x) acc) LValueSet.empty xs

    let retrolix_register_of xs =
      X86_64_Architecture.(List.map (fun x -> `Register (RId (string_of_register x))) xs)
      |> LValueSet.of_list

    let argument_passing_registers =
      retrolix_register_of X86_64_Architecture.argument_passing_registers

    let caller_saved_registers =
      retrolix_register_of X86_64_Architecture.caller_saved_registers

    let unions xs = List.fold_left LValueSet.union LValueSet.empty xs

    let rax = LValueSet.singleton (`Register (RId "rax"))

    (* Use *)
    let gen insn =
      match insn with
      | Comment _ | Jump _ ->
         LValueSet.empty
      | Exit ->
          argument_passing_registers
      | Ret ->
         LValueSet.union (!global_variables) rax
      | Assign (_,_,args) ->
         from_value_list args
      | ConditionalJump (_,xs,_,_) ->
         from_value_list xs
      | Switch (x,_,_) ->
         from_value x
      | Call (x,xs,_)  ->
         unions [from_value x; from_value_list xs; argument_passing_registers]

    (* Def *)
    let kill insn =
      match insn with
      | Exit | Comment _ | Jump _ | ConditionalJump _ | Switch _ | Ret ->
         LValueSet.empty
      | Assign (x,_,_) ->
         from_value x
      | Call (_,xs,_) ->
         unions [from_value_list xs; argument_passing_registers; caller_saved_registers; rax]

    let transfer (_,insn) liveout =
        LValueSet.union (gen insn) (LValueSet.diff liveout (kill insn))
  end

module LivenessAnalysis = RetrolixDataflowEngines.Default(LivenessDomain)

(** {2 Putting Everything Together} *)

let string_of_insn insn =
  let buf = Buffer.create 42 in
  PPrintEngine.ToBuffer.compact buf (RetrolixPrettyPrinter.instruction insn);
  Buffer.contents buf

let analyze blocks =
  let direction = `Backward in
  LivenessAnalysis.analyze ~direction blocks

let rewrite sol (lab, insn) =
  let liveout = fst (sol lab) in
  let def = LivenessDomain.kill insn in
  if LValueSet.(not (is_empty def) && is_empty (inter def liveout))
  then Comment "dead"
  else insn

let translate p =
  LivenessDomain.global_variables :=
    LValueSet.of_list
    @@ List.map (fun v -> `Variable v)
    @@ RetrolixUtils.global_variables p;
  RetrolixUtils.transform_blocks analyze rewrite p

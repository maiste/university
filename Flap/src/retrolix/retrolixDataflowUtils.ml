open RetrolixDataflowSigs

module AST = RetrolixAST
module Utils = RetrolixUtils
module PP = RetrolixPrettyPrinter

module PerLValueProperty (P : PROPERTY)
       : PROPERTY with type t = P.t Utils.LValueMap.t = struct
  type t = P.t Utils.LValueMap.t

  let print m =
    Utils.LValueMap.print P.print m

  let equal =
    Utils.LValueMap.equal P.equal

  let compare =
    Utils.LValueMap.compare P.compare

  let le m1 m2 =
    Utils.LValueMap.for_all
      (fun k x ->
        let y = try Utils.LValueMap.find k m2 with Not_found -> P.bot in
        P.le x y)
      m1

  let bot =
    Utils.LValueMap.empty

  let lub m1 m2 =
    let p_of_opt xo = match xo with None -> P.bot | Some x -> x in
    let merge id xo yo = Some (P.lub (p_of_opt xo) (p_of_opt yo)) in
    Utils.LValueMap.merge merge m1 m2

  let bot_lvalues lvs =
    List.fold_left
      (fun m lv -> Utils.LValueMap.add lv P.bot m)
      Utils.LValueMap.empty
      (lvs @ List.map RetrolixUtils.register X86_64_Architecture.all_registers)
end

module FlowGraph (Edge : Digraph.EDGE) = struct
  open RetrolixAST

  module Vertex = struct
    type t =
      {
        label : label;
        insn : instruction;
      }

    let print { label; insn; } =
      RetrolixPrettyPrinter.labelled_instruction
        RetrolixPrettyPrinter.nodecorations
        0
        (label, insn)

    module Label = struct
      type t = label
      let compare = Stdlib.compare
      let print = RetrolixPrettyPrinter.label 0
    end

    let label { label; _ } = label
  end

  module G = Digraph.Make(Edge)(Vertex)

  type t =
    {
      initial : label;
      locals : IdSet.t;
      graph : G.t;
    }

  let flow_graph_of_block make_default_edge ((locals, insns) : block) =
    let initial =
      match insns with
      | [] ->
         failwith "flow_graph_of_block: empty block"
      | (initial, _) :: _ ->
         initial
    in
    let locals =
      List.fold_left (fun locals id -> IdSet.add id locals) IdSet.empty locals
    in
    let graph =
      List.fold_left
        (fun graph (label, insn) -> G.add_vertex graph { label; insn; })
        G.empty
        insns
    in
    let graph =
      let targets = RetrolixUtils.instruction_targets insns in
      List.fold_left
        (fun graph (src, _, ldsts) ->
          List.fold_left
            (fun graph dst -> G.add_edge graph ~src ~dst (make_default_edge ()))
            graph
            ldsts)
        graph
        targets
    in
    { initial; locals; graph; }
end

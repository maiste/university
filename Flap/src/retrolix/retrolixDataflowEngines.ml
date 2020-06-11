open RetrolixDataflowSigs

open AST

(** This module implements a slow and inefficient dataflow engine using a very
    naive iteration strategy, with no acceleration at all. *)
module Naive : ENGINE =
  functor (D : DOMAIN) ->
  struct
    module D = D

    module Edge = struct
      type t = unit
      let compare = Pervasives.compare
      let print () = PPrint.empty
    end

    module FG = RetrolixDataflowUtils.FlowGraph(Edge)
    open FG

    type result = label -> (D.t * D.t)

    let same_solution s1 s2 =
      LabelMap.equal
        (fun (x1, x2) (y1, y2) -> D.equal x1 y1 && D.equal x2 y2)
        s1
        s2

    let input_of, output_of =
      let get f sol v = f (LabelMap.find v.FG.Vertex.label sol) in
      get fst, get snd

    let join_input x v sol =
      let x', y = LabelMap.find v.FG.Vertex.label sol in
      LabelMap.add v.FG.Vertex.label (D.lub x x', y) sol

    let join_output y v sol =
      let (x, _) = LabelMap.find v.FG.Vertex.label sol in
      LabelMap.add v.FG.Vertex.label (x, y) sol

    let transfer dir gr sol =
      let is_fwd = dir = `Forward in
      let fold_outputs =
        if is_fwd then G.fold_successors else G.fold_predecessors
      in
      let transfer_vertex v sol =
        let y = D.transfer (v.Vertex.label, v.Vertex.insn) (input_of sol v) in
        let sol = join_output y v sol in
        fold_outputs
          v.FG.Vertex.label
          (fun out () -> join_input y out)
          sol
          gr.graph
      in
      G.fold_vertices transfer_vertex gr.graph sol

    let analyze ?(init = `Input D.bot) ~direction block =
      let gr = FG.flow_graph_of_block (fun () -> ()) block in
      if Options.get_debug_mode () then
        (
          let fn = Filename.temp_file "cfg" ".dot" in
          let oc = open_out fn in
          FG.G.dump_graphviz gr.graph oc;
          close_out oc;
          Printf.printf "[dataflow] CFG stored in %s.\n" fn
        );
      let start =
        let input_vertices =
          LabelSet.of_list
          @@ List.map FG.Vertex.label
          @@ match direction with
             | `Forward -> FG.G.initial_vertices gr.graph
             | `Backward -> FG.G.terminal_vertices gr.graph
        in
        FG.G.fold_vertices
          (fun v init_sol ->
            let vl = FG.Vertex.label v in
            let x =
              match init with
              | `Input x ->
                 if LabelSet.mem vl input_vertices then x else D.bot
              | `All f ->
                 f vl
            in
            LabelMap.add vl (x, D.bot) init_sol)
          gr.FG.graph
          LabelMap.empty
      in
      let rec fix i current =
        if Options.get_debug_mode () then
          (
            Printf.printf "[dataflow] Solution at iteration %d:\n" i;
            ExtPPrint.to_stdout
              (LabelMap.print
                 (fun (i, o) -> PPrint.OCaml.tuple [D.print i; D.print o])
                 current)
        );
        let next = transfer direction gr current in
        if same_solution current next then current else fix (i + 1) next
      in
      let res = fix 0 start in
      fun lab -> LabelMap.find lab res
  end

(** This module implements a more efficient strategy involving a worklist and
    iterative computation of the solution. *)
module Worklist : ENGINE =
  functor (D : DOMAIN) ->
  struct
    module D = D

    module Edge = struct
      type t = unit
      let compare () () = 0
      let print () = PPrint.empty
    end

    module FG = RetrolixDataflowUtils.FlowGraph(Edge)
    open FG

    type result = label -> (D.t * D.t)

    let analyze ?(init = `Input D.bot) ~direction block =
      let work = Queue.create () in
      let dirty = LabelTab.create 100 in

      failwith "Students! This is your job!"
  end

module Default : ENGINE = Naive

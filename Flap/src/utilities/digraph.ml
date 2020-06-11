module type EDGE = sig
  include ExtStd.PrintableType
end

module type VERTEX = sig
  include ExtStd.PrintableType
  module Label : ExtStd.OrderedPrintableType
  val label : t -> Label.t
end

module Make (Edge : EDGE) (Vertex : VERTEX) = struct
  module VM = ExtStd.Map(Vertex.Label)
  module VPM = ExtStd.Map(ExtStd.OrderedPrintablePairs(Vertex.Label))

  type vinfo =
    {
      contents : Vertex.t;
      incoming : (Vertex.Label.t * Edge.t) list;
      outgoing : (Vertex.Label.t * Edge.t) list;
    }

  (** The type of directed graphs. *)
  type t =
    {
      vertices : vinfo VM.t;
      edges : Edge.t VPM.t;
    }

  let print { vertices; edges; } =
    let open PPrint in
    let edge_list =
      separate_map
        comma
        (fun (v, e) -> OCaml.tuple [Vertex.Label.print v; Edge.print e])
    in
    let vinfo { contents; incoming; outgoing; } =
      ExtPPrint.record
        [
          "contents", Vertex.print contents;
          "incoming", edge_list incoming;
          "outgoing", edge_list outgoing;
        ]
    in
    ExtPPrint.record
      [
        "vertices", VM.print vinfo vertices;
        "edges", VPM.print Edge.print edges;
      ]

  let dump_graphviz gr oc =
    let open PPrint in
    let dquotes s = "\"" ^ String.escaped s ^ "\"" in
    let label l = dquotes @@ ExtPPrint.to_string Vertex.Label.print l in
    let dump_vertex (vl, v) =
      output_string oc (label vl);
      output_string oc "[label = ";
      output_string oc
        (dquotes @@ ExtPPrint.to_string ~width:40 Vertex.print v.contents);
      output_string oc "];\n"
    in
    let dump_edge ((srcl, dstl), _) =
      output_string oc (label srcl);
      output_string oc " -> ";
      output_string oc (label dstl);
      output_string oc ";\n"
    in
    output_string oc "digraph {\n";
    List.iter dump_vertex @@ List.of_seq @@ VM.to_seq gr.vertices;
    List.iter dump_edge @@ List.of_seq @@ VPM.to_seq gr.edges;
    output_string oc "}\n"
  ;;

  let empty =
    {
      vertices = VM.empty;
      edges = VPM.empty;
    }

  exception Vertex_already_present of Vertex.Label.t
  exception Vertex_not_found of Vertex.Label.t
  exception Edge_already_present of Vertex.Label.t * Vertex.Label.t

  let add_vertex gr v =
    let vl = Vertex.label v in
    if VM.mem vl gr.vertices
    then raise (Vertex_already_present vl);
    let vi = { contents = v; incoming = []; outgoing = []; } in
    { gr with vertices = VM.add vl vi gr.vertices; }

  let find_vertex_info gr v =
    try VM.find v gr.vertices with Not_found -> raise (Vertex_not_found v)

  let add_edge gr ~src ~dst e =
    if VPM.mem (src, dst) gr.edges then raise (Edge_already_present (src, dst));
    let srci = find_vertex_info gr src in
    let dsti = find_vertex_info gr dst in
    let srci = { srci with outgoing = (dst, e) :: srci.outgoing; } in
    let dsti = { dsti with incoming = (src, e) :: dsti.incoming; } in
    { vertices = VM.(add src srci @@ add dst dsti @@ gr.vertices);
      edges = VPM.add (src, dst) e gr.edges; }

  let find_vertex vl gr =
    (find_vertex_info gr vl).contents

  let fold_vertices f gr acc =
    VM.fold (fun _ vi acc -> f vi.contents acc) gr.vertices acc

  let fold_edges f gr acc =
    VPM.fold (fun (srcl, dstl) e acc ->
        let src = find_vertex srcl gr in
        let dst = find_vertex dstl gr in
        f ~src ~dst e acc)
      gr.edges
      acc

  type 'a edge_folder = Vertex.t -> Edge.t -> 'a -> 'a

  let fold_successors srcl (f : 'a edge_folder) acc gr =
    let srci = find_vertex_info gr srcl in
    List.fold_left
      (fun acc (dstl, e) -> f (find_vertex dstl gr) e acc)
      acc
      srci.outgoing

  let fold_predecessors dstl f acc gr =
    let dsti = find_vertex_info gr dstl in
    List.fold_left
      (fun acc (srcl, e) -> f (find_vertex srcl gr) e acc)
      acc
      dsti.incoming

  let iter_vertices f gr =
    VM.iter (fun _ v -> f v.contents) gr.vertices

  let iter_edges f gr =
    VPM.iter
      (fun (srcl, dstl) e ->
        let src = find_vertex srcl gr in
        let dst = find_vertex dstl gr in
        f ~src ~dst e)
      gr.edges

  type edge_iter = Vertex.t -> Edge.t -> unit

  let iter_successors f gr srcl =
    let srci = find_vertex_info gr srcl in
    List.iter (fun (dstl, e) -> f (find_vertex dstl gr) e) srci.outgoing

  let iter_predecessors f gr dstl =
    let dsti = find_vertex_info gr dstl in
    List.iter (fun (srcl, e) -> f (find_vertex srcl gr) e) dsti.incoming

  let initial_vertices, terminal_vertices =
    let gather proj gr =
      VM.fold
        (fun _ v l -> if proj v = [] then v.contents :: l else l)
        gr.vertices
        []
    in
    gather (fun v -> v.incoming), gather (fun v -> v.outgoing)
end

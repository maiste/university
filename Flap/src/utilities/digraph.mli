(** A module for directed graphs.

    This module, like the one in {!m Graph}, provides a functional
    representation of directed graphs.
 *)

module type EDGE = sig
  include ExtStd.PrintableType
end

module type VERTEX = sig
  include ExtStd.PrintableType
  module Label : ExtStd.OrderedPrintableType
  val label : t -> Label.t
end

module Make (Edge : EDGE) (Vertex : VERTEX) : sig
  (** The type of directed graphs. *)
  type t

  (** Pretty-print the internal representation of the graph for debugging. *)
  val print : t -> PPrint.document

  (** Dump the graph in the Graphviz "dot", so that it can later be displayed by
      dotty and friends. *)
  val dump_graphviz : t -> out_channel -> unit

  (** {2 Exceptions} *)

  exception Vertex_not_found of Vertex.Label.t

  exception Edge_already_present of Vertex.Label.t * Vertex.Label.t

  (** {2 Graph construction operations} *)

  (** The empty digraph. *)
  val empty : t

  exception Vertex_already_present of Vertex.Label.t

  (** [add_vertex gr v] add the vertex [v] to the graph [gr]. This function
     raises {!e Vertex_already_present} if a node with label [V.label v] has
     already been added to [gr]. *)
  val add_vertex : t -> Vertex.t -> t

  (** [add_edge gr ~src ~dst e] adds an edge [e] between two vertices [src] and
     [dst] of [gr], identified by their labels. This function raises {!e
     Vertex_not_found} when either [src] or [dst] is not already present in
     [gr], and raises {!e Edge_already_present} when an edge between [src] and
     [dst] has already been added. *)
  val add_edge : t -> src:Vertex.Label.t -> dst:Vertex.Label.t -> Edge.t -> t

  (** {2 Graph traversal operations} *)

  (** [find_vertex vl g] finds the vertex with label [vl] in [g], or raises {!
     Vertex_not_found} if [g] contains no such vertex. *)
  val find_vertex : Vertex.Label.t -> t -> Vertex.t

  (** [fold_vertices f acc gr] applies [f v1 (f v2 (... acc))] to all the
     vertices [vi] of [gr]. They are enumerated in no particular order. *)
  val fold_vertices : (Vertex.t -> 'a -> 'a) -> t -> 'a -> 'a

  type 'a edge_folder = Vertex.t -> Edge.t -> 'a -> 'a

  (** [fold_edges f acc gr] is similar to [fold_vertices] but applies to the
     edges of [gr]. *)
  val fold_edges :
    (src:Vertex.t -> dst:Vertex.t -> Edge.t -> 'a -> 'a) -> t -> 'a -> 'a

  val fold_successors : Vertex.Label.t -> 'a edge_folder -> 'a -> t -> 'a

  val fold_predecessors : Vertex.Label.t -> 'a edge_folder -> 'a -> t -> 'a

  (** [iter_vertices f gr] applies function [f] to every vertex of [gr], in no
     particular order. *)
  val iter_vertices : (Vertex.t -> unit) -> t -> unit

  type edge_iter = Vertex.t -> Edge.t -> unit

  (** [iter_edges f gr] applies function [f] to every edge of [gr], in no
     particular order. *)
  val iter_edges :
    (src:Vertex.t -> dst:Vertex.t -> Edge.t -> unit) -> t -> unit

  (** [iter_successors f gr v] applies function [f] to every outgoing edge of
     [v] in [gr], in LIFO order. This function raises {!e Vertex_not_found} when
     [v] has not been added to [gr]. *)
  val iter_successors : edge_iter -> t -> Vertex.Label.t -> unit

  (** [iter_predecessors f gr v] applies function [f] to every incoming edge of
     [v] in [gr], in LIFO order. This function raises {!e Vertex_not_found} when
     [v] has not been added to [gr]. *)
  val iter_predecessors : edge_iter -> t -> Vertex.Label.t -> unit

  (** [initial_vertices gr] returns the list of all nodes of [gr] that have no
     predecessors. *)
  val initial_vertices : t -> Vertex.t list

  (** [terminal_vertices gr] returns the list of all nodes of [gr] that have no
     successors. *)
  val terminal_vertices : t -> Vertex.t list
end

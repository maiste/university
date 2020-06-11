module AST = RetrolixAST

(** A property is the result of a dataflow analysis. The set of properties
   should form a semilattice, that is, be ordered, have a least element, and a
   least upper bound operator. *)
module type PROPERTY =
  sig
    (** The type of properties. *)
    type t

    val print : t -> PPrint.document
    val equal : t -> t -> bool
    val compare : t -> t -> int

    (** The ordering relation of the semilattice. *)
    val le : t -> t -> bool
    (** The smallest element of the semilattice. *)
    val bot : t
    (** The least upper bound (or "join") of the semilattice. *)
    val lub : t -> t -> t
  end

(** A domain provides a space of properties together with a transfer function
   that specifies the semantics of an instruction w.r.t. to properties. *)
module type DOMAIN =
  sig
    include PROPERTY
    val transfer : AST.labelled_instruction -> t -> t
  end

(** An analysis provides a way to compute the result of a dataflow analysis for
   the specified domain D. *)
module type ANALYSIS =
  sig
    module D : DOMAIN

    type result = AST.label -> (D.t * D.t)

    val analyze :
      ?init:[ `Input of D.t | `All of AST.label -> D.t ] ->
      direction:[`Forward | `Backward] ->
      AST.block ->
      result
  end

(** An engine implements an algorithm turning a domain into an analysis for this
   domain. *)
module type ENGINE = functor (D : DOMAIN) -> ANALYSIS with module D = D

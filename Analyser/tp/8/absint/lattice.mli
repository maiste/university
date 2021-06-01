(*
  A lattice has a top and a bottom element,
  the lub and the glb are defined for any pair 
  of its elements (of type t), and it is
  related to a partial order relation leq.  
  See slides of week2-thoery.pdf for details
  on the relation between lub, glb, and leq.
*)
module type Lattice = sig
  type t
  val top: t
  val bot: t
  val leq: t -> t -> bool
  val lub: t -> t -> t
  val glb: t -> t -> t

  (* Handy for user-friendly implementations. *)
  val to_string : t -> string
end

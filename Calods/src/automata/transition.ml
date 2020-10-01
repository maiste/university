open State

module type TRANSITION = sig
  type t
  type transition

  val compare: t -> t -> int
  val print : t -> unit
  val create : transition -> t

  val get : t -> transition
end

module T : TRANSITION with type transition = (S.t * S.t * string) = struct
  type t =
    S.t * S.t * string

  type transition =
    S.t * S.t * string

  let compare =
    compare

  let print (s1, s2, t) =
    S.print s1;
    Printf.printf " -> %s -> " t;
    S.print s2;
    Printf.printf "\n"

  let create (s1, s2, t) =
    (s1, s2, t)

  let get t =
    t
end

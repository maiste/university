module type STATE = sig
  type t
  type state

  val compare : t -> t -> int
  val print : t -> unit
  val create : state -> t
  val get : t -> state
end

module S : STATE with type state = int = struct
  type t =
    int

  type state =
    int

  let compare =
    compare

  let print t =
    Printf.printf "%d" t

  let create s =
    s

  let get st =
    st
end

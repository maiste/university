open State
open Transition

module type NBA = sig

  type states
  type alphabet
  type transitions
  type initials
  type accepts

  type t

  val create : states -> transitions -> initials -> accepts -> t

  val get_transitions : t -> transitions

  val get_initials : t -> initials
  
  val print : t -> unit
end


module Scheduler : NBA with
  type states = S.t list
  and type transitions = T.t list
  and type initials = S.t list
  and type accepts = S.t list =
struct

  type states =
    S.t list

  type alphabet =
    string

  type transitions =
    T.t list

  type initials =
    S.t list

  type accepts =
    S.t list

  type t =
    { states : states;
      transitions : transitions;
      initials : initials;
      accepts : accepts;
    }

  let create st ts inits acpts =
    {
      states = st;
      transitions = ts;
      initials = inits;
      accepts = acpts
    }

  let print t =
    let print_states st =
      List.iter (fun s -> S.print s; Printf.printf " ; ") st;
    in
    match t with
    | {states = st; transitions = ts; initials = inits; accepts = acpts} ->
      Printf.printf "\nStates: ";
      print_states st;
      Printf.printf "\nTransitions:\n";
      List.iter (fun t -> T.print t; Printf.printf " ; ") ts;
      Printf.printf "\nInitials: ";
      print_states inits;
      Printf.printf "\nAccepts: ";
      print_states acpts;
      Printf.printf "\n\n\n"

  let get_transitions {transitions=ts; _} =
    ts

  let get_initials {initials=ins; _} =
    ins

end

let example () =
  let s0 = S.create 0 in
  let s1 = S.create 1 in

  let ts0 = T.create (s0, s1, "p0") in
  let ts1 = T.create (s1, s0, "p1") in

  let basic = Scheduler.create [s0; s1] [ts0; ts1] [s0] [s1] in

  Scheduler.print basic


open Whilery
open Lattice

(*
  An abstract domains is a (complete) lattice, enriched
  with additional operations that to define the 
  "abstract" semanics of a programming language.
 *)


module type Absdomain = sig
  include Lattice

  (* 
     Operations to perform arithmetic on abstract values,
     i.e. abstract operations.
   *)
  val plus : t -> t -> t
  val mult : t -> t -> t
  val subt : t -> t -> t
    
  (* 
     Overapproximation of the lub operation, that guarantees
     termination in case of a infinite height lattice.
   *)
  val widen : t -> t -> t

  (* 
     Function to refine an abstract value so as
     to make sure that a given boolean condition
     is true.
   *)
  val filter : rel -> int -> t -> t


  (* 
     Pick two constants and create
     a value of the abstract domain.
   *)
  val embed : int -> int -> t
end

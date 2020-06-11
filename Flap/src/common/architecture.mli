(** This module defines a common interface to specify target architectures. *)

module type S = sig

  (** The type of hardware registers. *)
  type register

  (** Hardware registers that can be used by register allocation. *)
  val allocable_registers : register list

  (** Registers used as effective arguments for functions. *)
  val argument_passing_registers : register list

  (** Registers that must be preserved through function calls. *)
  val callee_saved_registers : register list

  (** Registers that are *not* preserved by function calls. *)
  val caller_saved_registers : register list

  (** The register that holds the value returned by a function. *)
  val return_register : register

  (** A human representation of register identifier. *)
  val string_of_register : register -> string

end

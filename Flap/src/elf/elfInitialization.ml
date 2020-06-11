open Optimizers

(** Register some compilers that have ELF as a target or source language. *)
let initialize () =
  Languages.register (module Elf);
  Compilers.register (optimizing_compiler (module X86_64toElf));
  ()

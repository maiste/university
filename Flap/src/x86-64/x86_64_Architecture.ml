type register =
  | RAX
  | RBX
  | RCX
  | RDX
  | RSP
  | RBP
  | RSI
  | RDI
  | R8
  | R9
  | R10
  | R11
  | R12
  | R13
  | R14
  | R15
  | RIP

(** All registers. *)
let all_registers : register list =
  [
    RAX;
    RBX;
    RCX;
    RDX;
    RSP;
    RBP;
    RSI;
    RDI;
    R8;
    R9;
    R10;
    R11;
    R12;
    R13;
    R14;
    R15;
    RIP;
  ]

(** Hardware registers that can be used by register allocation. *)
let allocable_registers : register list =
  [
    RAX;
    RBX;
    RCX;
    RDX;
    RSI;
    RDI;
    R8;
    R9;
    R10;
    R11;
    R12;
    R13;
    R14;
  ]

(** Registers used as effective arguments for functions. *)
let argument_passing_registers : register list =
  [
    RDI;
    RSI;
    RDX;
    RCX;
    R8;
    R9;
  ]

(** Registers that must be preserved through function calls. *)
let callee_saved_registers : register list =
  [
    RBX;
    RSP;
    RBP;
    R12;
    R13;
    R14;
  ]

(** Registers that are *not* preserved by function calls. *)
let caller_saved_registers : register list =
  [
    RAX;
    RDI;
    RSI;
    RDX;
    RCX;
    R8;
    R9;
    R10;
    R11;
  ]

(** Registers that are preserved by function calls and allocable.*)
let allocable_callee_saved_registers =
  List.(filter (fun r -> mem r allocable_registers) callee_saved_registers)

(** The register that holds the value returned by a function. *)
let return_register : register =
  RAX

(** A register that is never used by the register allocator. *)
let scratch_register : register =
  R15

(** A human representation of register identifier. *)
let string_of_register r =
  match r with
  | RAX ->
     "rax"
  | RBX ->
     "rbx"
  | RCX ->
     "rcx"
  | RDX ->
     "rdx"
  | RSP ->
     "rsp"
  | RBP ->
     "rbp"
  | RSI ->
     "rsi"
  | RDI ->
     "rdi"
  | R8 ->
     "r8"
  | R9 ->
     "r9"
  | R10 ->
     "r10"
  | R11 ->
     "r11"
  | R12 ->
     "r12"
  | R13 ->
     "r13"
  | R14 ->
     "r14"
  | R15 ->
     "r15"
  | RIP ->
     "rip"

let register_of_string s =
  match s with
  | "rax" ->
     RAX
  | "rbx" ->
     RBX
  | "rcx" ->
     RCX
  | "rdx" ->
     RDX
  | "rsp" ->
     RSP
  | "rbp" ->
     RBP
  | "rsi" ->
     RSI
  | "rdi" ->
     RDI
  | "r8" ->
     R8
  | "r9" ->
     R9
  | "r10" ->
     R10
  | "r11" ->
     R11
  | "r12" ->
     R12
  | "r13" ->
     R13
  | "r14" ->
     R14
  | "r15" ->
     R15
  | "rip" ->
     RIP
  | _ ->
     invalid_arg ("register_of_string: " ^ s)

(** Registers are numbered, RAX being 0 and R15 being 15. *)
let register_number r =
  (* TODO: is this reasonable? *)
  assert Obj.(is_int @@ repr r);
  (Obj.magic r : int)

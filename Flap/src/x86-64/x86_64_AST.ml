(** The abstract syntax tree for X86-64 programs. *)

type reg = X86_64_Architecture.register

type label = string

type lit = Mint.t

type scale =
  [ `One | `Two | `Four | `Eight ]

type imm =
  | Lit of lit
  | Lab of label

type address =
  {
    offset : imm option;
    base : reg option;
    idx : reg option;
    scale : scale;
  }

type dst =
  [ `Addr of address | `Reg of reg ]

type src =
  [ dst | `Imm of imm ]

type suffix =
  [ `b | `w | `l | `q ]

type condcode =
  | E                           (* equal *)
  | NE                          (* not equal *)
  | S                           (* negative *)
  | NS                          (* not negative *)
  | G                           (* greater *)
  | GE                          (* greater or equal *)
  | L                           (* lower *)
  | LE                          (* lower or equal *)
  | A                           (* above *)
  | AE                          (* above or equal *)
  | B                           (* below *)
  | BE                          (* below or equal *)

type instruction =
  | Add of { s : suffix; src : src; dst : dst; }
  | Sub of { s : suffix; src : src; dst : dst; }
  | Imul of { s : suffix; src : src; dst : dst; }
  | Idiv of { s : suffix; src : src; }
  | And of { s : suffix; src : src; dst : dst; }
  | Or  of { s : suffix; src : src; dst : dst; }
  | Xor of { s : suffix; src : src; dst : dst; }
  | Not of { s : suffix;            dst : dst; }
  | Lea of { s : suffix; src : address; dst : dst }
  | Cmp of { s : suffix; src1 : src; src2 : src; }

  | Inc of { s : suffix; dst : dst; }
  | Dec of { s : suffix; dst : dst; }

  | Push of { s : suffix; src : src; }
  | Pop of { s : suffix; dst : dst; }
  | Mov of { s : suffix; src : src; dst : dst; }

  | CallD of { tgt : imm; }
  | CallI of { tgt : dst; }
  | Ret
  | JmpD of { tgt : imm; }
  | JmpI of { tgt : dst; }
  | Jcc of { cc : condcode; tgt : imm; }

  | Cmov of { cc : condcode; s : suffix; src : reg; dst : dst; }
  | Ct of { s : [ `w | `l | `q ]; }

  | Comment of string

type directive =
  | Section of string
  | Extern of string
  | Global of string
  | String of string
  | Quad of imm list
  | PadToAlign of { pow : int; fill : int; }

type line =
  | Directive of directive
  | Label of label
  | Instruction of instruction

type t =
  line list

let int_of_scale s =
  match s with
  | `Zero -> 0
  | `One -> 1
  | `Two -> 2
  | `Four -> 4
  | `Eight -> 8

(* Helper functions *)

let lit i =
  `Imm (Lit i)

let liti i =
  lit (Mint.of_int i)

let addr ?offset ?idx ?(scale = `One) ?base () =
  {
    offset;
    base;
    idx;
    scale;
  }

let addq ~src ~dst =
  Add { s = `q; src; dst; }

let subq ~src ~dst =
  Sub { s = `q; src; dst; }

let imulq ~src ~dst =
  Imul { s = `q; src; dst; }

let idivq ~src =
  Idiv { s = `q; src; }

let andq ~src ~dst =
  And { s = `q; src; dst; }

let orq ~src ~dst =
  Or { s = `q; src; dst; }

let xorq ~src ~dst =
  Xor { s = `q; src; dst; }

let notq ~dst =
  Not { s = `q; dst; }

let leaq ~src ~dst =
  Lea { s = `q; dst; src; }

let incq ~dst =
  Inc { s = `q; dst; }

let decq ~dst =
  Dec { s = `q; dst; }

let cmpq ~src1 ~src2 =
  Cmp { s = `q; src1; src2; }

let pushq ~src =
  Push { s = `q; src; }

let popq ~dst =
  Pop { s = `q; dst; }

let pushr ~reg =
  Push { s = `q; src = `Reg reg; }

let popr ~reg =
  Pop { s = `q; dst = `Reg reg; }

let movq ~src ~dst =
  Mov { s = `q; src; dst; }

let calld ~tgt =
  CallD { tgt; }

let calli ~tgt =
  CallI { tgt; }

let calldi ~tgt =
  match tgt with
  | `Imm tgt ->
     calld tgt
  | #dst as tgt ->
     calli tgt

let calll ~tgt =
  calld (Lab tgt)

let jmpd ~tgt =
  JmpD { tgt; }

let jmpi ~tgt =
  JmpI { tgt; }

let jmpdi ~tgt =
  match tgt with
  | `Imm tgt ->
     jmpd tgt
  | #dst as tgt ->
     jmpi tgt

let jmpl ~tgt =
  jmpd (Lab tgt)

let jcc ~cc ~tgt =
  Jcc { cc; tgt; }

let jccl ~cc ~tgt =
  jcc ~cc ~tgt:(Lab tgt)

let cmovq ~cc ~src ~dst =
  Cmov { s = `q; cc; src; dst; }

let cqto =
  Ct { s = `q; }

let insns =
  List.map (fun i -> Instruction i)

let string s =
  Directive (String s)

let text_section =
  Directive (Section "text")

let data_section =
  Directive (Section "data")

(* Compute labels referenced from an assembly file. *)

module Lab = struct type t = label let compare = Pervasives.compare end
module LabMap = Map.Make(Lab)
module LabSet = Set.Make(Lab)

let referenced_labels_imm imm refs =
  match imm with
  | Lit _ -> refs
  | Lab l -> LabSet.add l refs

let referenced_labels_addr addr refs =
  ExtStd.Option.fold referenced_labels_imm addr.offset refs

let referenced_labels_dst (dst : dst) refs =
  match dst with
  | `Addr addr -> referenced_labels_addr addr refs
  | `Reg _ -> refs

let referenced_labels_src (src : src) =
  match src with
  | #dst as dst -> referenced_labels_dst dst
  | `Imm imm -> referenced_labels_imm imm

let referenced_labels_ins ins refs =
  match ins with
  | Add { src; dst; } | Sub { src; dst; } | Imul { src; dst; }
  | And { src; dst; } | Or { src; dst; } | Xor { src; dst; }
  | Mov { src; dst; }
    ->
     referenced_labels_src src @@ referenced_labels_dst dst refs
  | Not { dst; } | Inc { dst; } | Dec { dst; } | Pop { dst; } | Cmov { dst; } ->
     referenced_labels_dst dst refs
  | Idiv { src; } | Push { src; } ->
     referenced_labels_src src refs
  | Cmp { src1; src2; } ->
     referenced_labels_src src1 @@ referenced_labels_src src2 refs
  | Lea { src; dst; } ->
     referenced_labels_addr src @@ referenced_labels_dst dst refs
  | CallD { tgt; } | JmpD { tgt; } | Jcc { tgt; } ->
     referenced_labels_imm tgt refs
  | CallI { tgt; } | JmpI { tgt; } ->
     referenced_labels_dst tgt refs
  | Ret | Comment _ | Ct _ ->
     refs

let referenced_labels_directive d refs =
  match d with
  | Quad imms ->
     List.fold_left (fun refs imm -> referenced_labels_imm imm refs) refs imms
  | Global l ->
     LabSet.add l refs
  | Section _ | Extern _ | String _ | PadToAlign _ ->
     refs

let referenced_labels_line refs line =
  match line with
  | Directive d ->
     referenced_labels_directive d refs
  | Label _ ->
     refs
  | Instruction ins ->
     referenced_labels_ins ins refs

let referenced_labels_prog p =
  List.fold_left referenced_labels_line LabSet.empty p

let remove_unused_labels p =
  let refs = referenced_labels_prog p in
  let useful line =
    match line with
    | Label l -> LabSet.mem l refs
    | Directive _ | Instruction _ -> true
  in
  List.filter useful p

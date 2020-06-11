(** This module offers a pretty-printer for X86-64 programs. *)

open PPrint
open PPrintCombinators
open PPrintEngine

open X86_64_AST

let string = PPrint.string

let tab =
  string "\t"

let quote s =
  string "\"" ^^ s ^^ string "\""

let commas docs =
  separate (string ", ") docs

let reg r =
  string ("%" ^ X86_64_Architecture.string_of_register r)

let label l =
  string l

let mint n =
  string (Mint.to_string n)

let int i =
  string @@ string_of_int i

let int32 i =
  string "%" ^^ string @@ Int32.to_string i

let imm ?(dollar = true) ie =
  let d =
    match ie with
    | Lit n ->
       string (Mint.to_string n)
    | Lab l ->
       label l
  in
  (if dollar then string "$" else empty) ^^ d

let address { offset; base; idx; scale; } =
  let map f o =
    match o with
    | None -> empty
    | Some x -> f x
  in
  let pp idx =
    string "," ^^ reg idx ^^ string "," ^^ int (int_of_scale scale)
  in
  map (imm ~dollar:false) offset
  ^^ parens (map reg base ^^ map pp idx)

let suffix s =
  string (match s with `b -> "b" | `w -> "w" | `l -> "l" | `q -> "q")

let condcode cc =
  match cc with
  | E -> string "e"
  | NE -> string "ne"
  | S -> string "s"
  | NS -> string "ns"
  | G -> string "g"
  | GE -> string "ge"
  | L -> string "l"
  | LE -> string "le"
  | A -> string "a"
  | AE -> string "ae"
  | B -> string "b"
  | BE -> string "be"

let operand ?(dollar = true) o =
  match o with
  | `Imm ie ->
     imm ~dollar ie
  | `Addr a ->
     address a
  | `Reg r ->
     reg r

let instruction i =
  let ins ?cc ?s ins ops =
    let cc =
      match cc with
      | None -> empty
      | Some cc -> condcode cc
    in
    let s =
      match s with
      | None -> empty
      | Some s -> suffix s
    in
    string ins ^^ cc ^^ s
    ^^ (if ops = [] then empty else string " " ^^ commas ops)
  in
  match i with
  | Add { s; src; dst; } ->
     ins "add" ~s [operand src; operand dst]
  | Sub { s; src; dst; } ->
     ins "sub" ~s [operand src; operand dst]
  | Imul { s; src; dst; } ->
     ins "imul" ~s [operand src; operand dst]
  | Idiv { s; src; } ->
     ins "idiv" ~s [operand src]

  | And { s; src; dst; } ->
     ins "and" ~s [operand src; operand dst]
  | Or { s; src; dst; } ->
     ins "or" ~s [operand src; operand dst]
  | Xor { s; src; dst; } ->
     ins "xor" ~s [operand src; operand dst]
  | Not { s; dst; } ->
     ins "not" ~s [operand dst]

  | Lea { src; dst; } ->
     ins "lea" [address src; operand dst]

  | Cmp { s; src1; src2; } ->
     ins "cmp" ~s [operand src1; operand src2]

  | Inc { s; dst; } ->
     ins "inc" ~s [operand dst]
  | Dec { s; dst; } ->
     ins "dec" ~s [operand dst]

  | Push { s; src; } ->
     ins "push" ~s [operand src]
  | Pop { s; dst; } ->
     ins "pop" ~s [operand dst]
  | Mov { s; src; dst; } ->
     ins "mov" ~s [operand src; operand dst]

  | CallD { tgt; } ->
     ins "call" [imm ~dollar:false tgt]
  | CallI { tgt; } ->
     string "call *" ^^ operand tgt
  | JmpD { tgt; } ->
     ins "jmp" [imm ~dollar:false tgt]
  | JmpI { tgt; } ->
     string "jmp *" ^^ operand tgt
  | Ret ->
     ins "ret" []

  | Jcc { cc; tgt; } ->
     ins "j" ~cc [imm ~dollar:false tgt]
  | Cmov { cc; s; src; dst; } ->
     ins "cmov" ~cc ~s [reg src; operand dst]

  | Ct { s; } ->
     let f, t =
       match s with
       | `w -> "w", "d"
       | `l -> "l", "q"
       | `q -> "q", "o"
     in
     string ("c" ^ f ^ "t" ^ t)

  | Comment s ->
     string "/* " ^^ string s ^^ string " */"

let directive d =
  let needs_tab, k, s =
    match d with
    | Section s ->
       false, s, empty
    | Extern s ->
       true, "extern ", string s
    | Global s ->
       true, "globl ", string s
    | String s ->
       true, "string ", quote (string (String.escaped s))
    | Quad ns ->
       true,
       "quad ",
       separate (string ", ") (List.map (imm ~dollar:false) ns)
    | PadToAlign { pow; fill; } ->
       false,
       "p2align ",
       separate (comma ^^ space) [int pow; int fill]
  in
  (if needs_tab then tab else empty) ^^ string ("." ^ k) ^^ s

let line li =
  match li with
  | Directive d ->
     directive d
  | Label l ->
     label l ^^ string ":"
  | Instruction i ->
     tab ^^ instruction i

let program ast =
  separate_map hardline line ast ^^ hardline

let to_string f x =
  let b = Buffer.create 13 in
  ToBuffer.pretty 0.5 80 b (f x);
  Buffer.contents b

(*
 * Chaboche - Marais
 * CALODS 2019
 *)

(* Module to define a compiler *)
module type Compiler = sig

  (* == Parsing section == *)

  (* Ast passed as an args*)
  type ast

  (* Parse 3 files:
     - calods
     - scheduler (optional)
     - properties (optional)
     to get an ast *)
  val parse_filenames : string -> string -> string -> Ast.t

  (* Expand Forall into SeqI *)
  val expand_forall : Ast.t -> Ast.t

  (* Debug function : print the ast *)
  val print_ast : Ast.t -> unit

  (* Check if the ast is well typed *)
  val check : Ast.t -> unit

end


(* Implement the compiler *)
module Calods  : Compiler with type ast = Ast.program = struct

  type ast = Ast.t

  let parse_sch sch_file =
    if sch_file <> "" then
      let input_sch = open_in sch_file in
      let lexbuf_sch = Lexing.from_channel input_sch in
      let sch =
        try
          Parser.scheduler Lexer.scheduler lexbuf_sch
        with e -> Error.readable e lexbuf_sch
      in
      Some sch
    else None

  let parse_properties pps_file =
    if pps_file <> "" then
      let input_pps = open_in pps_file in
      let lexbuf_pps = Lexing.from_channel input_pps in
      let pps =
        try
          Parser.properties Lexer.properties lexbuf_pps
        with e -> Error.readable e lexbuf_pps
      in
      Some pps
    else None

  let parse_filenames cds_file sch_file pps_file =
    let input = open_in cds_file in
    let lexbuf = Lexing.from_channel input in
    let (header, processes) =
      try
        Parser.program Lexer.token lexbuf
      with e -> Error.readable e lexbuf
    in
    let main =
      try
        Parser.main Lexer.main lexbuf
      with e -> Error.readable e lexbuf
    in
    let sch = parse_sch sch_file in
    let properties = parse_properties pps_file in
    Ast.Program (header, processes, main, sch, properties)

  let expand_forall ast =
    Remove_forall.remove_forall ast

  let print_ast ast =
    CalodsPrettyPrinter.print_program
      Format.std_formatter ast

  let check ast =
    Checker.type_check ast;
    Checker.properties_check ast
end

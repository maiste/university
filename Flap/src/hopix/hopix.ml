(** The Hopix programming language. *)

open Sexplib.Std

let name = "hopix"

module AST = HopixAST

type ast = HopixAST.t

let parse lexer_init input =
  SyntacticAnalysis.process
    ~lexer_init
    ~lexer_fun:HopixLexer.token
    ~parser_fun:HopixParser.program
    ~input

let parse_filename filename =
  if Options.get_use_sexp_in () then
    ExtStd.Pervasives.file_content filename
    |> Sexplib.Sexp.of_string
    |> HopixAST.program_of_sexp
  else
    parse Lexing.from_channel (open_in filename)

let extension =
  ".hopix"

let executable_format =
  false

let parse_string =
  parse Lexing.from_string

let print_ast ast =
  if Options.get_use_sexp_out () then
    HopixAST.sexp_of_program ast |> Sexplib.Sexp.to_string
  else
    HopixPrettyPrinter.(to_string program ast)

let print_expression e =
  HopixPrettyPrinter.(to_string expression e)

include HopixInterpreter

include HopixTypechecker

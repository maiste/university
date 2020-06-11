(** The ELF binary format. *)

module AST = ElfAST

let name = "elf"

type ast = AST.t

let parse lexer_init input =
  SyntacticAnalysis.process
    ~lexer_init
    ~lexer_fun:RetrolixLexer.token
    ~parser_fun:RetrolixParser.program
    ~input

let no_parser () =
  Error.global_error
    "during source analysis"
    "There is no parser for ELF in flap."

let parse_filename filename =
  no_parser ()

let extension =
  ".elf"

let executable_format =
  true

let parse_string _ =
  no_parser ()

let print_ast (buf : ast) =
  Buffer.contents buf

include ElfInterpreter
include ElfTypechecker

{ (* -*- tuareg -*- *)
  open Lexing
  open Error
  open Position
  open HopixParser

  let next_line_and f lexbuf  =
    Lexing.new_line lexbuf;
    f lexbuf

  let error lexbuf =
    error "during lexing" (lex_join lexbuf.lex_start_p lexbuf.lex_curr_p)

  (* Buffer to manage string *)
  let str_buff = Buffer.create 1024

  (* Stack to manage block comments *)
  let comment_stack = ref 0
  let down_stack () = comment_stack := !comment_stack + 1
  let up_stack () =
    comment_stack := !comment_stack - 1;
    if !comment_stack < 0 then
      raise Stack.Empty
    else
      !comment_stack = 0

  (* Transform char into its ascii code *)
  let get_char lex s =
    let first_char = String.get s 0 in
    let length = String.length s in
    if length = 1 then first_char     (* 'char' *)
    else
      if first_char = '\\' then
        match String.get s 1 with     (* 'escaped' *)
        | 'n' -> Char.chr 10
        | 'r' -> Char.chr 13
        | 'b' -> Char.chr 8
        | 't' -> Char.chr 9
        | '\'' -> Char.chr 39
        | '\\' -> Char.chr 92
        | '0' | '1' | '2' ->
            (String.sub s 1 (length-1)) |> int_of_string |> Char.chr
        | _  -> error lex "unexpected escaped character."
      else error lex "unexpected character."
}

(** Regexpr **)
let newline = ('\010' | '\013' | "\013\010")
let blank   = [' ' '\009' '\012']

let digit = ['0'-'9']
let lowercase = ['a'-'z']
let uppercase = ['A'-'Z']
let letters = (digit | lowercase | uppercase)

(* ASCII *)
let ascii_num = '\\' ['0'-'2']['0'-'9']['0'-'9']
let ascii_print =  ['\x20'-'\x7f'] # ['"' '\'']
let ascii_hex = "\\0x" letters letters
let ascii_esc = "\\\\" | "\\n" | "\\b" | "\\'" | "\\t" | "\\r"
let atom_pur = ascii_esc | ascii_print | ascii_hex | ascii_num
let atom = atom_pur | '"'
let str_token = atom_pur | '\'' | "\\\""

(* Number *)
let binary = "0b" ['0'-'1']+
let octal = "0o" ['0'-'7']+
let hexa = "0x" (digit | lowercase | uppercase)+
let integer = digit+ | hexa | octal | binary
let ident = (lowercase | uppercase | digit | '_')*

rule token = parse
  (** Layout *)
  | newline  { next_line_and token lexbuf }
  | blank+   { token lexbuf               }
  | eof      { EOF       }

  | "extern" { EXTERN }
  | "type"   { TYPE }

  | "fun"    { FUN }
  | "and"    { AND }

  | "=?"     { PEQ }
  | "<=?"    { PLEQ }
  | ">=?"    { PGEQ }
  | "<?"     { PLE }
  | ">?"     { PGR }

  | "->"     { ARROW }
  | '<'      { LCHEVRON }
  | '>'      { RCHEVRON }
  | '('      { LPAREN }
  | ')'      { RPAREN }
  | '{'      { LBRACK }
  | '}'      { RBRACK }
  | '['      { LCROCH }
  | ']'      { RCROCH }
  | ','      { COMA }
  | ';'      { SEMICOLON }

  | '+'      { PLUS }
  | '-'      { MINUS }
  | '*'      { STAR }
  | '/'      { DIV }
  | '.'      { DOT }

  | "&&"     { LAND }
  | "||"     { LOR }

  | "="      { EQ }
  | ":="     { ASSIGN  }
  | ":"      { TWODOT }
  | '|'      { PIPE }
  | '&'      { CAND }
  | '_'      { UNDERSCORE }
  | '\\'     { BSLASH }
  | '!'      { READ }

  | "ref"    { REF }
  | "let"    { LET }
  | "if"     { IF }
  | "else"   { ELSE }
  | "while"  { WHILE }
  | "do"     { DO }
  | "for"    { FOR }
  | "in"     { IN }
  | "to"     { TO }
  | "switch" { SWITCH }

  | integer as i          { Int (int_of_string i) }
  | "'" (atom as a) "'"   { Char (get_char lexbuf a) }
  | '"'                   { Buffer.clear str_buff ; str lexbuf }

  | lowercase ident* as id    { LowerId(id) }
  | uppercase ident* as id    { UpperId(id) }
  | "`" ident* as id          { TypeId(id) }

  | "//"_*'\n'          { token lexbuf }
  | "/*"                { down_stack () ; comment lexbuf }

  (** Lexing error. *)
  | _                   { error lexbuf "unexpected character." }


and str = parse
  | "\\\""         { Buffer.add_string str_buff "\"" ; str lexbuf }
  | '"'            { String (Buffer.contents str_buff) }
  | "/*"           { down_stack () ; comment lexbuf }
  | str_token as s { Buffer.add_char str_buff (get_char lexbuf s) ; str lexbuf }
  | _                   { error lexbuf "unexpected character." }


and comment = parse
  | "/*"    { down_stack () ; comment lexbuf }
  | "*/"    { if  up_stack () then token lexbuf else comment lexbuf }
  | _       { comment lexbuf }

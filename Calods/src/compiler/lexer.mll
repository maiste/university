(*
 * CALODS - 2019
 * Chaboche - Marais
 *)

{
  open Parser
  open Error
  open Lexing

  (* Update lines in lexer *)
  let next_line lexbuf =
    let pos = lexbuf.lex_curr_p in
      lexbuf.lex_curr_p <-
      { pos with pos_bol = lexbuf.lex_curr_pos;
               pos_lnum = pos.pos_lnum + 1
      }
}

let blank = [' ' '\009']

let id = ['a'-'z' 'A'-'Z']+['a'-'z' 'A'-'Z' '0'-'9' '_']*
let value = ['a'-'z' 'A'-'Z' '0'-'9' '_']['a'-'z' 'A'-'Z' '0'-'9' '_']*


let alpha = ['a'-'z' 'A'-'Z' '0'-'9' '_']+

rule token = parse
(* Comments *)
| "/*"            { comment_lines lexbuf; token lexbuf }
| "//"            { comment lexbuf; token lexbuf }
(* Layout *)
| blank+          { token lexbuf }

(* Key words *)
| "type"          { TYPE }
| "proc"          { PROC }
| "while"         { WHILE }
| "if"            { IF }
| "else"          { ELSE }
| "switch"        { SWITCH }
| "decide"        { DECIDE }
| "_"             { WILDCARD }
| "run"           { MAIN }
| "var"           { VAR }

(* Punctuations *)
| "\n"            { new_line lexbuf ; LF }
| "("             { LPAREN }
| ")"             { RPAREN }
| "["             { LCROCH }
| "]"             { RCROCH }
| "{"             { LBRACK }
| "}"             { RBRACK }
| ","             { COMMA }
| ":"             { COLON }
| ";"             { SEMICOLON }

(* Expressions *)
| "="             { ASSIGN }

(* Binop *)
| "and"           { AND }
| "or"            { OR }
| "=="            { EQUAL }
| "!="            { DIFF }

(* Booleans *)
| "true"          { TRUE }
| "false"         { FALSE }

(* Regex *)
| id as s         { ID s  }
| value as s      { VAL s }

| _ as s          { syntax_error s }



and main = parse
(* Layout *)
| blank+          { main lexbuf }
| '\n'            { new_line lexbuf; main lexbuf }

(* Procs *)
| "||"            { P_PIPE }
| "+"             { P_PLUS }

(* Key words *)
| "forall"        { FORALL }
| "in"            { IN }

(* Punctuations *)
| "("             { LPAREN }
| ")"             { RPAREN }
| "{"             { LBRACK }
| "}"             { RBRACK }
| ","             { COMMA }

(* Regex *)
| id as s         { ID s  }
| value as s      { VAL s }

| eof             { EOF }
| _   as  s       { syntax_error s }

and scheduler = parse
| blank+          { scheduler lexbuf }
| '\n'            { new_line lexbuf; scheduler lexbuf }

| ("Ɛ" | "ε" | "epsilon" | "eps")     { EPSILON }
| ("@" | "omega" | "OMEGA")           { OMEGA }
| "*"             { STAR }
| "."             { DOT }
| "("             { LPAREN }
| ")"             { RPAREN }
| "+"             { PLUS }
| alpha as s      { ALPHA(s) }

| eof             { EOF }



and properties = parse
| blank+          { properties lexbuf }
| '\n'            { new_line lexbuf; properties lexbuf }

| ","             { COMMA }
| ";"             { SEMICOLON }
| "=>"            { ARROW }
| "("             { LPAREN }
| ")"             { RPAREN }
| "&"             { AND }
| ":"             { COLON }
| "-"             { WILDCARD }

| id as s         { ID s  }
| value as s      { VAL s }

| eof             { EOF }
| _ as s          { syntax_error s }


and comment_lines = parse
| "/*"            { comment_lines lexbuf; comment_lines lexbuf }
| "*/"            { () }
| "\n"            { new_line lexbuf; comment_lines lexbuf }
| eof             { raise (SyntaxError "EOF") }
| _               { comment_lines lexbuf }

and comment = parse
| "\n"            { new_line lexbuf }
| eof             { () }
| _               { comment lexbuf }

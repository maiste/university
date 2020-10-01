(*
 * Chaboche - Marais
 * CALODS 2019
 *)

open Lexing

exception SyntaxError of string
exception CalodsError of string
exception TypesError of string
exception Undifined of string
exception MultiplesDecids

(* Generate a syntax error *)
let syntax_error c =
  raise (SyntaxError (Format.sprintf "%c" c))

(* Syntax Error message *)
let syntax_message s p =
  let s = String.escaped s in
  Format.sprintf "Syntax Error :\n* unexpected '%s'  (%s)\n" s p

(* Parser Error message *)
let parser_message t p =
  let t = String.escaped t in
  Format.sprintf "Parsing Error :\n* unexpected \"%s\" (%s)\n" t p


(* Get position of the error *)
let get_pos_msg t p =
  let offset_word = (p.pos_cnum - p.pos_bol + 1) in
  let offset = offset_word - (String.length t) in
  Format.sprintf "line %d, character %d-%d"
    p.pos_lnum offset offset_word

(* Make exception readable *)
let readable e l =
  let p = l.lex_curr_p in
  let token = Lexing.lexeme l in
  let pos_msg = get_pos_msg token p in
  match e with
  | SyntaxError s -> raise (CalodsError (syntax_message s pos_msg))
  | Parser.Error -> raise (CalodsError (parser_message token pos_msg))
  | _ -> raise e

(* Type Error message *)
let type_error_message t p message =
  Format.sprintf "Typing Error: \n* %s (%s)" message (get_pos_msg t p)

(* Undifined Proc message *)
let undifined_error_message e n =
  Format.sprintf "Undifined %s Error :\n* undifinied %s" e n

(* Type error message with no pos *)
let type_error_message_np t expected =
  Format.sprintf "Typing Error :\n* wrong value %s expected type %s" t expected

let undifined_error e n =
  raise (CalodsError (undifined_error_message e n))

let decide_error () =
  raise (CalodsError "Muliple decides Error :\n* you are trying to decide to many things")

let type_error t p message =
  raise (CalodsError (type_error_message t p message))

let type_error_np t expected =
  raise (CalodsError (type_error_message_np t expected))

(* Make readable exception for cmdline *)
let exception_to_cmd = function
  | CalodsError s -> Error (`Msg s)
  | e -> raise e

(*
 * Chaboche - Marais
 * CALODS - 2019
 *)

open Position
open Automata

(* Type *)
type ty = string

(* Variable name *)
type name = string

(* Values *)
type value = string



(** Main program **)
type program =
  Program of header * process list * main * scheduler option * properties option

(* Type definition and optional values *)
and header = Header of data position list  * global position list option

(* Type definition *)
and data = DefineType of ty * value position list

(* Global variable definition(s) *)
and global =
  | EmptyArray of name * ty position * value position
  | Array of name * ty position * value position list
  | GlobalVar of name * ty position * value position

(* Process definition *)
and process =
  Process of
    name *
    arg position list *
    declaration position list *
    instruction position list

(* Args are define like tuple of (var, type) *)
and arg = Arg of name position * ty position

(* Declaration of local variables *)
and declaration = DeclareVar of  name position * ty position

(* Instruction *)
and instruction =
  | Assign of name position * literal position
  | AssignArray of name position * value position * literal position
  | Condition of
        compare *
        instruction position list *
        instruction position list option
  | Switch of literal position * case position list
  | While of compare * instruction position list
  | Decide of literal

(* Case for the switch-case *)
and case =
  Case of case_argument * instruction position list

(* Case argument : default or a literal *)
and case_argument =
  | Wildcard
  | CaseArg of literal position

(* Comparaison allowed *)
and compare =
  | And of compare * compare
  | Or of compare * compare
  | Equal of literal position * literal position
  | NonEqual of literal position * literal position
  | Boolean of bool

(* Literal values *)
and literal =
  | ArrayValue of name position * value position
  | Value of value position



(** Running execution **)
and main =
  Main of callable position

(* Action allowed *)
and callable =
  | ForAll of name list * ty position * callable position
  | Parallel of callable position * callable position
  | SeqI of callable position * callable position
  | CallProcess of name position * literal position list option



(** Schedulers **)
and scheduler =
  Nba.Scheduler.t




(** Properties **)
and properties =
  (entry * output) list

and entry =
  proc_args list

and output =
  (proc_decide list) list

and proc_decide =
  | Tauto of name
  | Exist of name * value

and proc_args =
  name * value list



and t = program

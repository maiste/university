(*
 * Chaboche - Marais
 * CALOD - 2019
 *)


(* Types bounds *)
type bounds =
  int * int

(* Variable name *)
type name = string

(* Values *)
type value = string

(* Array length *)
type length = int

(* Line label *)
type label = string


(* Prism labels *)
type labels =
  Label of name * test

and test =
  | EqualT of name * string
  | OrT of test * test
  | AndT of test * test
  | BoolT of bool

(* LTL Formula *)
type formula =
  | A of formula
  | G of formula
  | F of formula
  | ArrowF of formula * formula
  | OrF of formula * formula
  | AndF of formula * formula
  | LabelF of name
  | NotLabelF of name
  | BoolF of bool

(* Properties *)
type properties =
  labels list * formula

(* Globals * Processes *)
type program =
  Program of global list * process list * init * sch option * properties option

(* Initialization block *)
and init =
    Init of mods

(* Scheduler block *)
and sch =
    Scheduler of mods

(* Processes *)
and process =
    Process of mods

(* Module *)
and mods =
  name *
  string option * (* If this is None, the name is used as an id *)
  declaration list *
  instruction list

(* Global declarations *)
and global =
  | GlobalVar of name * bounds * int


(* Modules declarations *)
and declaration =
  LocalVar of name * bounds * int

(* State *)
and state =
  | State of name * int * state_args option

and state_args =
  | Eq of value * value
  | NEq of value * value
  | And of state_args * state_args
  | Or of state_args * state_args
  | Bool of bool
  | SEmpty

(* Instructions *)
and instruction =
  | Instruction of label option * state * instruction * state option
  | Affectation of name * string
  | IEmpty
  | Seq of instruction * instruction

and t = program

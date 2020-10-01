(*
 * Chaboche - Marais
 * CALODS - 2019
 *)


(** ILODS is an intermadiate language to express process as
    an array of instruction *)



(*** Type of variables ***)

type name = string

type value = string

type ty = string

type literal =
  | ArrayValue of name * value
  | Value of value

type compare =
  | And of compare * compare
  | Or of compare * compare
  | Equal of literal * literal
  | NonEqual of literal * literal
  | Bool of bool

type action =
  | Empty                     (* No action *)
  | Declare of name           (* Declare a value *)
  | Assign of name * literal (* Assign a value *)
  | AssignArray of name * value * literal (* Assign a value to an array *)
  | Decide of literal  (* Decide a litteral *)
  | Jump of compare * goto    (* Execute a jump if the test is right *)
  | Move               (* Return to a line *)

and goto =
  | Goto of int (* Specify a line *)
  | Next        (* Goto next line *)
  | Finish      (* After a decide *)
  | Unknown     (* Unknown jump *)
  | EOF         (* Last line in a file *)



(*** Process type ***)

type args = name list

type instruction = action * goto

type process = name * args * instruction array


(*** Header type ***)

type data = ty * value list

type global =
  | EmptyArray of name * value
  | Array of name * value list
  | GlobalVar of name * value

type header = data list * global list


(*** Main type ***)

type callable =
  | ForAll of name list * ty * callable
  | Parallel of callable list
  | SeqI of callable list
  | Call of name * literal list

(*** Program type ***)

type program = header * process list * callable

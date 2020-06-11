(** The abstract syntax tree for Fopix programs. *)

(**

   Fopix is a first order language.

   Like the C language, Fopix only allows toplevel functions. These
   functions can be called directly by using their names in the source
   code or indirectly by means of (dynamically computed) function
   pointers. Toplevel functions are mutually recursive.

   As in C, the control-flow can be structured by loops,
   conditionals and switchs.

   Contrary to C, Fopix does not make a distinction between statements
   and expressions. Besides, the notion of variable is similar to the
   one of functional language: variables are immutable.

*)

type program = definition list

and definition =
  (** [val x = e]  *)
  | DefineValue      of identifier * expression
  (** [def f (x1, ..., xN) = e] *)
  | DefineFunction   of function_identifier * formals * expression
  (** [external f : arity] *)
  | ExternalFunction of function_identifier * int

and expression =
  (** [0, 1, 2, ], ['a', 'b', ...], ["Dalek", "Master", ...] *)
  | Literal of literal
  (** [x, y, z, ginette] *)
  | Variable of identifier
  (** [val x = e1; e2] *)
  | Define of identifier * expression * expression
  (** [f (e1, .., eN)] *)
  | FunCall of function_identifier * expression list
  (** [call e with (e1, .., eN)] *)
  | UnknownFunCall of expression * expression list
  (** [while e do e' end] *)
  | While of expression * expression
  (** [if e then e1 else e2 end] *)
  | IfThenElse of expression * expression * expression
  (** [switch e in c1 | c2 | .. | cN orelse e_default end]
      where [ci := ! | e]. *)
  | Switch of expression * expression option array * expression option

and literal =
  | LInt    of Mint.t
  | LString of string
  | LChar   of char
  | LFun    of function_identifier

and identifier =
  | Id of string

and formals =
    identifier list

and function_identifier =
  | FunId of string

and t = program

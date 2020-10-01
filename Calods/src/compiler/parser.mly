(*
 * CALODS - 2019
 * Chaboche - Marais
 *)

%{
  open Automata
  open Omega
  open Ast
  open Position
%}

%token EOF

%token<string> ID
%token<string> VAL

(* Key words *)
%token TYPE
%token PROC
%token WHILE
%token IF
%token ELSE
%token SWITCH
%token DECIDE
%token MAIN
%token FORALL
%token IN
%token WILDCARD
%token VAR

(* Punctuations *)
%token LF
%token LPAREN
%token RPAREN
%token LCROCH
%token RCROCH
%token LBRACK
%token RBRACK
%token COMMA
%token COLON
%token SEMICOLON
%token ARROW

(* Expressions *)
%token ASSIGN

(* Binop *)
%token AND
%token OR
%token EQUAL
%token DIFF

(* Booleans *)
%token TRUE
%token FALSE

(* Procs *)
%token P_PLUS
%token P_PIPE

(* Prec rules *)
%left OR
%left AND

(* Omega tokens *)
%token<string> ALPHA
%token DOT
%token OMEGA
%token STAR
%token PLUS
%token EPSILON


%left PLUS
%left concat
%left DOT
%left STAR
%left OMEGA

%left P_PIPE

%right LF

%start<Ast.header * Ast.process list> program
%start<Ast.main> main
%start<Ast.scheduler> scheduler
%start<'a list> properties
%%

(* PROGRAM *)
program:
| h=header p=nonempty_list(process) list(LF) MAIN
{ (h, p) }


(* HEADER *)
header:
| datas=nonempty_list(position(data)) globs=list(position(global)) list(LF)
{
  let globs = match globs with
  | [] -> None
  | _ -> Some globs
  in
  Header (datas, globs)
}

data: TYPE t=ID ASSIGN LBRACK values=separated_nonempty_list(COMMA, position(VAL)) RBRACK nonempty_list(LF)
| TYPE t=ID ASSIGN LBRACK values=separated_nonempty_list(COMMA, position(ID)) RBRACK nonempty_list(LF)
{ DefineType (t, values) }

global:
| VAR t=position(ID) var=ID LCROCH v=position(VAL) RCROCH nonempty_list(LF)
| VAR t=position(ID) var=ID LCROCH v=position(ID) RCROCH nonempty_list(LF)
{ EmptyArray (var, t, v) }
| VAR t=position(ID) var=ID LCROCH RCROCH ASSIGN LBRACK values=separated_nonempty_list(COMMA, position(VAL)) RBRACK nonempty_list(LF)
| VAR t=position(ID) var=ID LCROCH RCROCH ASSIGN LBRACK values=separated_nonempty_list(COMMA, position(ID)) RBRACK nonempty_list(LF)
{ Array (var, t, values) }
| VAR t=position(ID) var=ID ASSIGN i=position(VAL) nonempty_list(LF)
| VAR t=position(ID) var=ID ASSIGN i=position(ID) nonempty_list(LF)
{ GlobalVar (var, t, i) }


(* PROCESS *)
process:
| PROC pname=ID LPAREN arg_l=separated_list(COMMA, position(arg)) RPAREN LBRACK LF decl_l=list(position(declaration)) instr_l=instructions RBRACK
nonempty_list(LF)
{ Process (pname, arg_l, decl_l, instr_l) }

arg:
| ty=position(ID) var=position(ID)
{ Arg (var, ty) }

declaration:
| VAR ty=position(ID) var=position(ID) nonempty_list(LF)
{ DeclareVar (var, ty) }

instructions:
| i=position(instruction) nonempty_list(LF) suite=instructions
{ i::suite }
| { [] }

instruction:
| v=position(ID) ASSIGN l=position(literal)
{ Assign (v, l) }
| v=position(ID) LCROCH i=position(VAL) RCROCH ASSIGN l=position(literal)
{ AssignArray (v, i, l) }
| IF c=compare LBRACK LF instrs=instructions RBRACK
{ Condition (c, instrs, None) }
| IF c=compare LBRACK LF instrs_1=instructions RBRACK ELSE LBRACK LF instrs_2=instructions RBRACK
{ Condition (c, instrs_1, Some instrs_2) }
| SWITCH l=position(literal) LBRACK LF cs=nonempty_list(position(case)) RBRACK
{ Switch (l, cs) }
| WHILE c=compare LBRACK LF instrs=instructions RBRACK
{ While (c, instrs) }
| DECIDE l=literal
{ Decide l }

case: LPAREN c=case_argument RPAREN COLON LF instrs=instructions
{ if instrs = [] then assert false else Case (c, instrs) }

case_argument:
| WILDCARD
{ Wildcard }
| l=position(literal)
{ CaseArg l }

compare:
| c1=compare AND c2=compare
{ And (c1, c2) }
| c1=compare OR c2=compare
{ Or (c1, c2) }
| l1=position(literal) EQUAL l2=position(literal)
{ Equal (l1, l2) }
| l1=position(literal) DIFF l2=position(literal)
{ NonEqual (l1, l2) }
| b=boolean
{ b }
| LPAREN c=compare RPAREN
{ c }

literal:
| s=position(ID) LCROCH i=position(VAL) RCROCH
{ ArrayValue (s, i) }
| v=position(VAL)
| v=position(ID)
{ Value v }
| LPAREN l=literal RPAREN
{ l }

boolean:
| TRUE
{ Boolean true }
| FALSE
{ Boolean false }


(* MAIN *)
main:
| p=position(forall) EOF
{ Main p }

forall:
| FORALL vars=separated_nonempty_list(COMMA, ID) IN ty=position(ID) LBRACK c=position(forall) RBRACK
{ ForAll (vars, ty, c) }
| s=sequence { s }


sequence:
| c1=position(callable) P_PLUS c2=position(sequence)
{ SeqI (c1, c2) }
| c=callable { c }

callable:
| c1=position(callable) P_PIPE c2=position(callable)
{ Parallel (c1, c2) }
| proc_name=position(ID) LPAREN RPAREN
{ CallProcess (proc_name, None) }
| proc_name=position(ID) LPAREN ls=separated_nonempty_list(COMMA, position(literal)) RPAREN
{ CallProcess (proc_name, Some (ls))}

(* SCHEDULER *)
scheduler:
| o=omega EOF
{ Omega.omega_to_nba o}

omega:
| r=reg OMEGA
{ Omega r }
| r=reg DOT o=omega %prec concat
{ OmegaConcat (r, o) }
| o1=omega PLUS o2=omega
{ OmegaChoice (o1, o2) }
| LPAREN o=omega RPAREN
{ o }

reg:
| EPSILON
{ Epsilon }
| a=ALPHA
{ Alpha a }
| r1=reg PLUS r2=reg
{ Choice (r1, r2) }
| r1=reg DOT r2=reg %prec concat
{ Concat (r1, r2) }
| r=reg STAR
{ Multiple r }
| LPAREN r=reg RPAREN
{ r }

(* PROPERTIES *)
value:
| x=ID | x=VAL { x }

properties:
| props=list(property) EOF
{ props }

property:
| e=entry ARROW o=separated_nonempty_list(SEMICOLON, output)
{ (e,o) }

proc_args:
| id=ID LPAREN args=separated_list(COMMA, value) RPAREN
{ (id, args) }

entry:
| e=separated_nonempty_list(COMMA, proc_args)
{ e }

proc_decide:
| id=ID COLON value=value
{ Exist (id, value) }
| id=ID COLON WILDCARD
{ Tauto id }

output:
| o=separated_nonempty_list(AND, proc_decide)
{ o }

%inline position(X): x=X {
  make $endpos x
}

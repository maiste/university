%{ (* Emacs, use -*- tuareg -*- to open this file! *)

  open RetrolixAST

  let fresh_label =
    let r = ref 0 in
    fun () -> incr r; Label ("_L" ^ string_of_int !r)

  let bool_of_unit_option o =
    match o with None -> false | Some () -> true
%}

%token SEMICOLON COLON COMMA EOF DEF EXTERNAL GLOBALS END LPAREN RPAREN
%token LOCAL CALL TAIL RET LARROW RARROW EXIT UPPERSAND
%token JUMP JUMPIF SWITCH ORELSE
%token GT LT GTE LTE EQ
%token ADD MUL DIV SUB COPY AND OR
%token<Mint.t> INT
%token<string> ID RID COMMENT LSTRING
%token<char> LCHAR
%type<lvalue> lvalue
%type<rvalue> rvalue
%start<RetrolixAST.t> program

%%

program: ds=definition* EOF
{
  ds
}
| error {
  let pos = Position.lex_join $startpos $endpos in
  Error.error "parsing" pos "Syntax error."
}

definition: GLOBALS LPAREN xs=separated_list(COMMA, identifier)
                               RPAREN b=block END {
  DValues (xs, b)
}
| DEF f=function_identifier
  LPAREN xs=separated_list(COMMA, identifier) RPAREN
  b=block
  END
{
  DFunction (f, xs, b)
}
| EXTERNAL f=function_identifier
{
  DExternalFunction f
}

locals: LOCAL xs=separated_nonempty_list(COMMA, identifier) COLON
{
  xs
}
| /* empty word */
{
  []
}

block: xs=locals ls=labelled_instruction*
{
  (xs, ls)
}

identifier: x=ID {
  Id x
}

labelled_instruction: l=label COLON i=instruction SEMICOLON {
  (l, i)
}
| i=instruction SEMICOLON {
  (fresh_label (), i)
}

label: l=ID {
  Label l
}

orelse_label: ORELSE l=label {
  l
}

instruction:
  CALL f=rvalue
  LPAREN xs=separated_list(COMMA, rvalue) RPAREN t=option(TAIL)
{
  Call (f, xs, bool_of_unit_option t)
}
| f=function_identifier
  LPAREN xs=separated_list(COMMA, rvalue) RPAREN t=option(TAIL)
{
  Call (`Immediate (LFun f), xs, bool_of_unit_option t)
}
| RET
{
  Ret
}
| l=lvalue LARROW o=op xs=separated_list(COMMA, rvalue)
{
  Assign (l, o, xs)
}
| JUMP l=label
{
  Jump l
}
| JUMPIF c=condition xs=separated_list(COMMA, rvalue)
  RARROW l1=label COMMA l2=label
{
  ConditionalJump (c, xs, l1, l2)
}
| SWITCH rv=rvalue
  RARROW ls=separated_list(COMMA, label)
  dl=option(orelse_label)
{
  Switch (rv, Array.of_list ls, dl)
}
| c=COMMENT
{
  Comment c
}
| EXIT
{
  Exit
}

condition:
  GT  { GT }
| LT  { LT }
| GTE { GTE }
| LTE { LTE }
| EQ  { EQ }

op:
  ADD { Add }
| MUL { Mul }
| DIV { Div }
| SUB { Sub }
| COPY { Copy }
| AND { And }
| OR  { Or }

lvalue:
 v=identifier
{
  `Variable v
}
| r=register
{
  `Register r
}

register: r=RID
{
  RId r
}

rvalue:
  l=lvalue
{
  (l :> rvalue)
}
| l=literal
{
  `Immediate l
}

literal: x=INT
{
  LInt x
}
| UPPERSAND x=function_identifier
{
  LFun x
}
| c=LCHAR
{
  LChar c
}
| s=LSTRING
{
  LString s
}


%inline located(X): x=X {
  Position.with_poss $startpos $endpos x
}

function_identifier: x=ID
{
  FId x
}

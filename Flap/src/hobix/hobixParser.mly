%{

  open HobixAST

%}

%token VAL
%token PLUS MINUS STAR SLASH
%token FUN WHILE
%token LTE LT GT GTE EQUAL EQ LAND LOR
%token IF THEN ELSE FI NEWBLOCK
%token AND OR EXTERN NOTHING IN SWITCH PIPE
%token LBRACKET RBRACKET COMMA BACKSLASH DRARROW
%token LBRACE RBRACE COLON
%token<string> LSTRING
%token<char> LCHAR
%token LPAREN RPAREN
%token SEMICOLON DEQUAL EOF
%token<Int64.t> INT
%token<string> ID INFIXID
%type <HobixAST.expression> expression

%right SEMICOLON
%nonassoc FUN AND ELSE
%nonassoc DEQUAL
%nonassoc DRARROW
%left LOR
%left LAND
%nonassoc LTE LT GT GTE EQ
%left INFIXID
%left PLUS MINUS
%left STAR SLASH

%start<HobixAST.t> program

%%

program: ds=definition* EOF
{
  ds
}

definition:
VAL d=value_def
{
  let (x, e) = d in
  DefineValue (SimpleValue (x, e))
}
| FUN d=function_definition ds=mutfun
{
  DefineValue (RecFunctions (d :: ds))
}
| EXTERN x=identifier COLON n=INT
{
  DeclareExtern (x, Int64.to_int n)
}
| error {
  let pos = Position.lex_join $startpos $endpos in
  Error.error "parsing" pos "Syntax error."
}

%inline value_def:
x=identifier EQUAL e=expression
{
  (x, e)
}

%inline function_definition:
x=identifier
LPAREN xs=separated_list(COMMA, identifier) RPAREN
EQUAL e=expression
{
  (x, Fun (xs, e))
}

mutfun:
/* empty */ %prec AND { [] }
| AND d=function_definition ds=mutfun
{ d::ds }

expression:
s=simple_expression
{
  s
}
| e1=expression SEMICOLON e2=expression
{
  Define (SimpleValue (Id "__nothing__", e1), e2)
}
| VAL vdef=value_def SEMICOLON e2=expression
{
  let (id,e1) = vdef in Define (SimpleValue (id, e1),e2)
}
| FUN d=function_definition ds=mutfun SEMICOLON e=expression %prec FUN
{
  Define (RecFunctions (d::ds), e)
}
| WHILE e=expression LBRACE b=expression RBRACE
{
  While (e, b)
}
| NEWBLOCK LPAREN e=expression RPAREN
{
  AllocateBlock e
}
| b=simple_expression LBRACKET i=expression RBRACKET DEQUAL rhs=expression
{
  WriteBlock (b, i, rhs)
}
| lhs=expression b=binop rhs=expression
{
  Apply (Variable (Id b), [lhs; rhs])
}
| IF c=expression THEN t=expression ELSE e=expression FI
{
  IfThenElse (c, t, e)
}
| BACKSLASH
    LPAREN xs=separated_list(COMMA, identifier) RPAREN
    DRARROW e=expression
{
  Fun (xs, e)
}
| SWITCH e=expression IN bs=list(branch) OR ELSE d=default
{
  let i = List.fold_left (fun i (j, _) -> max i j) 0 bs in
  let abs = Array.make i None in
  List.iter (fun (i, e) -> abs.(i) <- Some e) bs;
  Switch (e, abs, d)
}

%inline default: NOTHING { None }
| e=expression { Some e }

branch: PIPE x=INT DRARROW e=expression
{
  (Int64.to_int x, e)
}

simple_expression:
| a=simple_expression
  LPAREN bs=separated_list(COMMA, expression) RPAREN
{
  Apply (a, bs)
}
| b=simple_expression LBRACKET i=expression RBRACKET
{
  ReadBlock (b, i)
}

| e=very_simple_expression
{
  e
}

very_simple_expression:
  l=literal
{
  Literal l
}
| x=identifier
{
  HobixAST.Variable x
}
| LPAREN e=expression RPAREN
{
  e
}

%inline binop:
  x=INFIXID { String.(sub x 0 (length x - 1)) }
| PLUS  { "`+`"  }
| MINUS { "`-`"  }
| STAR  { "`*`"  }
| SLASH { "`/`"  }
| GT    { "`>?`"  }
| GTE   { "`>=?`" }
| LT    { "`<?`"  }
| LTE   { "`<=?`" }
| EQ    { "`=?`"  }
| LAND  { "`&&`" }
| LOR   { "`||`" }

%inline literal:
  x=INT
{
  LInt x
}
| MINUS x=INT
{
  LInt (Int64.neg x)
}
| s=LSTRING
{
  LString s
}
| c=LCHAR
{
  LChar c
}

%inline identifier: x=ID {
  Id x
}

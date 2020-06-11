%{ (* -*- tuareg -*- *)

  open HopixAST
  open Position

  let patterns_of_ident xs =
    List.map (
        fun x -> Position.with_pos (Position.position x) (PVariable x)
      ) xs

  (* Define binop as an Apply AST.t *)
  let apply_op e1 e2 (op, (startpos, endpos)) =
    let symbol = match op with
      | STAR -> "*"
      | DIV -> "/"
      | MINUS -> "-"
      | PLUS -> "+"
      | LAND -> "&&"
      | LOR -> "||"
      | PEQ -> "=?"
      | PLEQ -> "<=?"
      | PGEQ -> ">=?"
      | PLE -> "<?"
      | PGR -> ">?"
      | _ -> raise Parsing.Parse_error
    in
    let symbol = "`" ^ symbol ^ "`" in
    let pos x = Position.with_poss (startpos) (endpos) x in
    Apply (
      pos (Apply (pos (Variable (pos (Id symbol), None)), e1)), e2
    )

  let list_of_option xs =
    match xs with
    | None -> []
    | Some xs -> xs

  let define_type id var ty =
    let var = list_of_option var in
    let ty =
      match ty with
      | None -> Abstract
      | Some ty -> ty in
    DefineType (id,var,ty)
%}

(*** TOKENS ***)

(* Symbol tokens *)
%token EOF ARROW  READ
%token EQ ASSIGN
%token TWODOT PIPE UNDERSCORE BSLASH CAND
%token DOT

(* Surrounding tokens *)
%token LPAREN "(" RPAREN ")" LBRACK "{" RBRACK "}" LCROCH "[" RCROCH "]"
%token SEMICOLON ";" COMA
%token LCHEVRON "<" RCHEVRON ">"

(* Binop tokens *)
%token PLUS MINUS STAR DIV
%token PEQ PGEQ PLEQ PLE PGR LAND LOR

(* Expression tokens *)
%token LET IF ELSE WHILE DO FOR IN TO REF SWITCH
%token FUN AND
%token EXTERN
%token TYPE

(* Typed tokens *)
%token<string> LowerId UpperId TypeId String
%token<char> Char
%token<int> Int


(*** TYPES ***)

%start<HopixAST.t> program
%type<HopixAST.ty> ty ty_factor
%type<HopixAST.expression> expr


(*** PRIORITY ***)
%nonassoc typer
%right ARROW
%right SEMICOLON
%left ASSIGN

%left LOR
%left LAND
%left PEQ PLEQ PGEQ PLE PGR
%left PLUS MINUS
%left DIV STAR
%left apply
%left constr
%left LPAREN
%left fun_def
%left let_def fun_in
%%

(*** RULES ***)

(* Starting rules *)
program: lst=list(located(definition)) EOF {lst}


(* Definitions *)
definition:
| TYPE id=located(type_con) var=option(cargs(type_variable)) ty=option(tdefinition)
  { define_type id var ty }
| EXTERN id=located(identifier) ty=located(type_scheme)
  { DeclareExtern(id,ty) }
| vdef=vdefinition { DefineValue(vdef) }

tdefinition:
| EQ option(PIPE) x=constructor_with_ty { DefineSumType [x] }
| EQ option(PIPE) x=constructor_with_ty PIPE xs=separated_nonempty_list(PIPE,constructor_with_ty)
  { DefineSumType (x::xs) }
| EQ "{" xs=separated_nonempty_list(COMA, typed_label) "}"
  { DefineRecordType xs }


vdefinition :
| LET id=located(identifier) ty=option(located(type_scheme)) EQ e=located(expr)
  { SimpleValue (id,ty,e) }
| FUN ty=option(located(type_scheme)) name=located(identifier) p=located(pattern)
 EQ e=located(expr)
  { RecFunctions [(name, ty, FunctionDefinition (p,e))]  }
| FUN x=fundef AND xs=separated_list(AND, fundef)
  { RecFunctions (x::xs) }

vdefinition_in :
| LET id=located(identifier) ty=option(located(type_scheme)) EQ e=located(expr)
  { SimpleValue (id,ty,e) } %prec let_def
| FUN ty=option(located(type_scheme)) name=located(identifier) p=located(pattern)
 EQ e=located(expr) %prec fun_in
  { RecFunctions [(name, ty, FunctionDefinition (p,e))]  }
| FUN x=fundef AND xs=separated_list(AND, fundef)
  { RecFunctions (x::xs) }

fundef:
| ty=option(located(type_scheme)) name=located(identifier) p=located(pattern)
  EQ e=located(expr) %prec fun_def
  { (name, ty, FunctionDefinition (p, e)) }

constructor_with_ty:
| x=located(constructor) y=option(args(ty)) { (x,list_of_option y) }

constructor:
| x=UpperId { KId x }


(* Types *)
ty :
| t=ty_factor
  { t }
| left=located(ty) ARROW right=located(ty)
  { TyArrow (left,right) }
| x=located(ty_factor) STAR xs=separated_nonempty_list(STAR,located(ty_factor))
   { TyTuple (x::xs) }

ty_factor :
| id=type_con
  { TyCon (id,[]) }
| id=LowerId lst=cargs(ty)
  { TyCon (TCon id,lst) }
| v=type_variable
  { TyVar v }
| REF "<" x=located(ty_factor) ">"
  { TyCon(TCon "ref", [x]) }
| LPAREN x=ty RPAREN
  { x }

type_con:
| id=LowerId { TCon id }

type_variable:
| id=TypeId { TId id }

type_scheme :
| TWODOT ty=located(ty)
  { ForallTy ([],ty) }
| TWODOT "[" xs=nonempty_list(located(type_variable)) "]" ty=located(ty)
  { ForallTy (xs,ty) }

typed_label:
| id=located(label) TWODOT ty=located(ty) { (id,ty) }


(* Expressions *)
expr :
| e=expr_op
  { e }
| e1=located(expr) ";" e2=located(expr)
  { Sequence [e1;e2] }
| v=vdefinition_in ";" e=located(expr)
  { Define (v,e) }
| BSLASH p=located(pattern) ARROW e=located(expr)
  { Fun (FunctionDefinition (p, e)) }
| SWITCH "(" c =located(expr) ")" "{" b=branches "}"
  { Case (c, b)  }
| IF "(" c=located(expr) ")" "{" e1=located(expr) "}"
  ELSE "{" e2=located(expr) "}"
  { IfThenElse (c, e1, e2) }
| REF e=located(expr_factor)
  { Ref e }
| e1=located(expr) ASSIGN e2=located(expr)
  { Assign (e1, e2) }
| WHILE "(" e1=located(expr) ")" "{" e2=located(expr) "}"
  { While (e1, e2) }
| DO "{" e=located(expr) "}" w=location(WHILE) "(" c=located(expr) ")"
  { Sequence [e ; Position.with_poss (fst w) (snd w) (While (c, e)) ] }
| FOR id=located(identifier) IN "(" start=located(expr)
  TO stop=located(expr) ")" "{" e=located(expr) "}"
  { For (id, start, stop, e) }

expr_op:
| e=expr_const
  { e }
| e1=located(expr) op=binop e2=located(expr)
  { apply_op e1 e2 op }

%inline binop:
| op=location(PLUS)  { (PLUS, op)  }
| op=location(MINUS) { (MINUS, op) }
| op=location(STAR)  { (STAR, op)  }
| op=location(DIV)   { (DIV, op)   }
| op=location(LAND)  { (LAND, op)  }
| op=location(LOR)   { (LOR, op)   }
| op=location(PEQ)   { (PEQ, op)   }
| op=location(PLEQ)  { (PLEQ, op)  }
| op=location(PGEQ)  { (PGEQ, op)  }
| op=location(PLE)   { (PLE, op)   }
| op=location(PGR)   { (PGR, op)   }

expr_const:
| e=expr_middle %prec apply
  { e }
| c=located(constructor) t=option(cargs(ty)) e=args(expr)
  { Tagged (c, t, e) }

expr_middle:
| e=expr_factor
  { e }
| e1=located(expr_middle) e2=located(expr_factor)
  { Apply (e1,e2) }

expr_factor :
| e=expr_atom
  { e }
| e=located(expr_factor) DOT id=located(label)
  { Field(e,id) }

expr_atom:
| x=located(litteral)
  { Literal (x) }
| x=located(identifier) t=option(cargs(ty))
  { Variable (x, t) }
| c=located(constructor) t=option(cargs(ty)) %prec constr
  { Tagged (c, t, [])}
| "(" h=located(expr) COMA hs=separated_nonempty_list(COMA, located(expr)) ")"
  { Tuple (h::hs) }
| "{" r=separated_nonempty_list(COMA, record_expr) "}" t=option(cargs(ty))
  { Record (r, t) }
| READ e=located(expr_atom)
  { Read e }
| "(" e=expr ")"
  { e }
| "(" e=located(expr) TWODOT t=located(ty) ")"
  { TypeAnnotation (e,t) }

record_expr:
| l=located(label) EQ p=located(expr)
  { (l, p) }


(* Branches *)
branches:
| option(PIPE) x=located(branch)
  { [x] }
| option(PIPE) x=located(branch) PIPE xs=separated_list(PIPE, located(branch))
  { x::xs }

branch:
| p=located(pattern) ARROW e=located(expr) { Branch (p, e) }

pattern:
| p=pattern_sum { p }
| p=located(pattern_sum) PIPE ps=separated_nonempty_list(PIPE, located(pattern_sum))
  { POr (p::ps) }

pattern_sum:
| p=pattern_factor
  { p }
| p=located(pattern_factor) CAND
  ps=separated_nonempty_list(CAND, located(pattern_factor))
  { PAnd (p::ps) }

pattern_factor:
| x=located(identifier)
  { PVariable x }
| UNDERSCORE
  { PWildcard }
| "(" p=located(pattern) COMA ps=separated_nonempty_list(COMA,located(pattern)) ")"
  { PTuple (p::ps) }
| p=located(pattern_factor) TWODOT t=located(ty) %prec typer
  { PTypeAnnotation (p, t) }
| x=located(litteral)
  { PLiteral x }
| "(" p=pattern ")"
  { p }
| "{" r=separated_nonempty_list(COMA, record_observer) "}" t=option(cargs(ty))
  { PRecord (r, t) }
| c=located(constructor) t=option(cargs(ty)) p=option(args(pattern))
  { PTaggedValue (c, t, list_of_option p) }

record_observer:
| l=located(label) EQ p=located(pattern) { (l, p) }


(* Identificateur *)
litteral:
| x=Int
  { LInt (Mint.of_int x) }
| MINUS x=Int
  { LInt (Mint.of_int (-x)) }
| x=Char
  { LChar x }
| x=String
  { LString x }

identifier :
| id=LowerId { Id id }

label:
| id=LowerId { LId id }


(*** POSITION ***)

 %inline located(X): x=X {
  Position.with_poss $startpos $endpos x
}

%inline location(X): X {
  ($startpos, $endpos)
}

(** LIST **)

%inline args(X):
| "(" xs=separated_nonempty_list(COMA, located(X)) ")" { xs }

%inline cargs(X):
|  "<" xs=separated_list(COMA, located(X)) ">" { xs }

%{
  #include <stdio.h>
  #include <stdlib.h>
  #include <string.h>

  #include "arith.h"
  #include "nodes.h"
  #include "cmd.h"

  int yylex(void);
  int yyparse(void);
  extern int yy_scan_string(int line);

  int yyerror(char const *str){
    fprintf(stderr,"%s\n",str);
    return 1;
  }

%}

%token SPACE

%token PLUS
%token MINUS
%token TIMES
%token LEFT
%token RIGHT
%token LB
%token RB

 // Avec peut être des espaces après
%token LESSER
%token GREATER
%token TWOGREATER

 // Tous les opérateurs suivant sont considérés entourés d'espaces
%token SEMICOLON
%token PIPE
%token LAND
%token BG
%token LOR

%union
{
  char * string;
  node * nd;
  pre_cmd * cm;
  logics * log;
  arith * ar;
  int nat;
}

%token<string> STRING
%token<string> WHILE
%token<string> FOR
%token<string> FROM
%token<string> TO
%token<nat> NAT

%type<nd> multiples //Des node de (logics *)
%type<nd> whileloop;
%type<log> conjonc_pipeds
%type<nd> piped_cmds //Des nodes de (pre_cmd *)
%type<cm> cmd
%type<nd> args
%type<nd> arg
%type<string> word
%type<ar> arith_expr
%type<ar> artih_natv

/* liste des terminaux */
%%
toplevel :
multiples {parseres = $1;}
;

// Une ligne de commande est soit une un arbre d'évaluation logique suivi d'une ligne de commande, soit une boucle while.
multiples:
conjonc_pipeds SEMICOLON multiples {$$ = add_front_node(mknode_logics($1),$3);}
| conjonc_pipeds {$$ = mknode_logics($1);}
| whileloop {$$ = $1;}
;

// Une boucle while est soit une vrai boucle while, soit une boucle for
whileloop:
WHILE SPACE word SEMICOLON multiples {$$ = mknode_whilel(mk_var($3),$5); }
| WHILE SPACE LB arith_expr RB SEMICOLON multiples {$$ = mknode_whilel($4,$7); }
| FOR SPACE word SPACE FROM SPACE LB arith_expr RB SPACE TO SPACE LB arith_expr RB SEMICOLON multiples
{ $$ = mkforl($3,$8,$14,$17);}
;

// Un arbre d'évaluation logique est soit une feuille (consitué d'une liste de commandes séparées par des tubes), soit un noeud.
conjonc_pipeds:
piped_cmds BG {$$ = mkleaflogics($1,1);}
| piped_cmds {$$ = mkleaflogics($1,0);}
| piped_cmds LAND conjonc_pipeds {$$ = mkreallogics(AND, mkleaflogics($1,0) ,$3);}
| piped_cmds LOR conjonc_pipeds  {$$ = mkreallogics(OR, mkleaflogics($1,0),$3);}
;

// Une liste de commande séparées par des tubes.
piped_cmds:
cmd PIPE piped_cmds { $$ = add_front_node(mknode_pre_cmd($1),$3);}
| cmd { $$ = mknode_pre_cmd($1);}
;

// Une commande est un mot seul peut être suivi d'une liste arguments
cmd:
word SPACE args { $$ = mkpre_cmd($1,$3);}
| word SPACE { $$ = mkpre_cmd($1, NULL);}
| word { $$ = mkpre_cmd($1, NULL);}
;

// Une liste d'arguments
args:
arg SPACE args { $$ = add_front_node($1,$3);}
| arg SPACE { $$ = $1 ;}
| arg { $$ = $1; }
;

// Un argument est soit un mot soit une redirection
arg:
word { $$ =  mknode_char($1);}
| LB arith_expr RB { $$ = mknode_arith($2);}
| LESSER word { $$ = mknode_redir(IN,$2);}
| GREATER word { $$ = mknode_redir(OUT,$2);}
| TWOGREATER word { $$ = mknode_redir(ERR,$2);}
;

// Un mot est soit un mot normal, soit un mot-clé hors de son contexte
word:
STRING { $$ = $1;}
| FOR { $$ = strdup("for");}
| WHILE { $$ = strdup("while");}
| FROM { $$ = strdup("from");}
| TO { $$ = strdup("to");}
;

// Les expressions arithmétiques entières, représentées en arbre
arith_expr:
artih_natv {$$ = $1;}
| artih_natv PLUS arith_expr {$$ = mk_op(op_PLUS,$1,$3);}
| artih_natv TIMES arith_expr {$$ = mk_op(op_MULT,$1,$3);}
| artih_natv MINUS arith_expr {$$ = mk_op(op_MINUS,$1,$3);}
| LEFT arith_expr RIGHT {$$ = $2;}
;

// Une feuille d'un arbre arithmétique est soit un naturel, soit une variable
artih_natv:
NAT {$$ = mk_nat($1);}
| STRING {$$ = mk_var($1);}
%%

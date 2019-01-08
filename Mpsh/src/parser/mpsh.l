%{
#include <stdlib.h>

#include "arith.h"
#include "cmd.h"
#include "mpsh.tab.h"

char string_buf[MAX_STR_LEN];
int size;
%}

%option noinput
%option nounput

spacet [ \t\n]*
space  [ \t\n]+
word   [^ \>\<\t\n\;\&\|\"\{]{1,2}[^ \t\n\;\&\|]*
natural [0-9]+

%x str arith

%%

<INITIAL>{
{spacet}\|{spacet}   {return PIPE; }
{spacet}\&\&{spacet} {return LAND; }
{spacet}\&{spacet}   {return BG; }
{spacet}\|\|{spacet} {return LOR; }
{spacet}\;{spacet}   {return SEMICOLON; }
\<{spacet}  {return LESSER; }
\>{spacet}  {return GREATER;}
2\>{spacet} {return TWOGREATER; }
{space}     {return SPACE; }
while       {return WHILE; }
for         {return FOR; }
from        {return FROM; }
to          {return TO; }
\" { /* C'est le début d'un string */
   size=0;
   BEGIN(str);
}
\{ {BEGIN(arith); return LB;}
{word}      {yylval.string=strdup(yytext);return STRING; }
}

<str>{

\" { /* C'est la fin d'un string */
   BEGIN(INITIAL);
   string_buf[size] = '\0';
   yylval.string=strdup(string_buf);
   return STRING;
   }

[^\\\"]+ { /* Tout ce qui n'est ni échappé ni la fin du string */
        char *yptr = yytext;
        while (*yptr && (size < MAX_STR_LEN))
          string_buf[size++] = *yptr++;
        }

\\.  {string_buf[size++] = yytext[1];} /* Gère l'échappement */
}

<arith>{
\} {BEGIN(INITIAL); return RB;}
{natural} { yylval.nat=atoi(yytext);
 return NAT;
}

"+" return PLUS;
"-" return MINUS;
"*" return TIMES;
"(" return LEFT;
")" return RIGHT;

\$[a-zA-Z]+ {yylval.string=strdup(yytext+1);return STRING; }

. {}
}
%%

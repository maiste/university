#include <stdio.h>
#include <stdlib.h>
#include <limits.h>

#include <readline/readline.h>
#include <readline/history.h>

#include "cmd.h"

#define YYTOKENTYPE
#include "mpsh.tab.h"
#include "lex.yy.h"

#include "handle.h"

void history(char ** args, int nb_args){
  HIST_ENTRY ** hist = history_list();
  if(nb_args < 2){ // On affiche l'historique numéroté
    if(hist == NULL)
      printf("There is no history\n");
    int i = 0;
    while(hist[i] != NULL){
      printf("%d. %s\n", i, (hist[i])->line);
      i++;
    }
  }
  else{
    long l = strtol(args[1],NULL, 10);
    if(l==LONG_MAX || l==LONG_MIN || l==0)
      printf("There was an error with your argument: %s",args[1]);
    else{
      if(l>0){ // on lance la l-ième commande
	if(hist == NULL){
	  printf("There is no history\n");
	  return;
	}
	long tmp = 0;
	while (hist[tmp] != NULL && tmp < l )
	  tmp++;
	if(hist[tmp] == NULL){
	  printf("There is no command at rank %ld, the last one is at rank %ld\n",l,tmp+1);
	  return;
	}
	yy_scan_string(hist[tmp]->line);
	if(!yyparse())
	  handle_multiples(parseres,1);
      }
      else{// On met le nombre max d'entrée à l
	stifle_history(-l);
      }
    }
  }
}

#include <string.h>
#include <stdlib.h>
#include <stdio.h>

#include "parsing_struct.h"
#include "struct_utils.h"
#include "cmd.h"
#include "environment.h"

/**** PRINT ****/
// Affiche un tableau de tableau de string
void print_arr_arr(char **arr_arr, int size){
  for(int i=0; i<size; i++){
    printf("%s, ",arr_arr[i]);
  }
  printf("\n");
}

// Affiche une cmd
void print_cmd(cmd * cm){
  if(cm != NULL){
    printf("NAME:    %s,\n", cm->name);
    printf("NB_ARGS: %d,\n",cm->nb_args);
    printf("STDIN:   %s,\n",cm->stdin);
    printf("STDOUT:  %s,\n",cm->stdout);
    printf("STDERR:  %s,\n",cm->stderr);
    printf("ARGS:    ");
    print_arr_arr(cm->args, cm->nb_args);
  }
}

void print_piped(piped_cmd * piped){
  if(piped != NULL){
    printf("PIPED:\n");
    printf("NB_ELEM: %d\n\n",piped->nb_elem);
    printf("---\n");
    for(int i = 0; i<piped->nb_elem;i++ ){
      print_cmd(piped->cmds[i]);
      printf("---\n");
    }
  }
}

/**** FREE ****/
// Libère la mémoire allouée pour un cmd
void free_cmd(cmd * cm){
  if(cm != NULL){
    for(int i=0; i<cm->nb_args; i++)
      free(cm->args[i]);
    free(cm->args);
    //free(name) -> args[0] = name, donc l'adresse a déjà été libérée

    if(cm->stdin)
      free(cm->stdin);
    if(cm->stdout)
      free(cm->stdout);
    if(cm->stderr)
      free(cm->stderr);

    free(cm);
  }
}

void free_piped(piped_cmd * piped){
  if(piped != NULL){
    for(int i=0; i<piped->nb_elem; i++)
      free_cmd(piped->cmds[i]);
    free(piped->cmds);
    free(piped);
  }
}

void free_redir(redir * r){
  free(r->to);
  free(r);
}

void free_logics(logics * l){
  if(l->log == AND || l->log == OR){
    free_logics(l->left);
    free_logics(l->right);
  }
  else{
    free_node(l->piped);
  }
  free(l);
}

void free_whilel(whilel * w){
  free_arith(w->cond);
  free_node(w->todo);
  free(w);
}

void free_pre_cmd(pre_cmd * c){
  free(c->name);
  free_node(c->args);
  free(c);
}

void free_node(node * n){
  if(n!=NULL){
    switch(n->content){
    case CHAR:
      free(n->val.charet);
      break;
    case CMD:
      free_pre_cmd(n->val.cmdet);
      break;
    case REDIR:
      free_redir(n->val.rediret);
      break;
    case LOGICS:
      free_logics(n->val.logicset);
      break;
    case WHILEL:
      free_whilel(n->val.whilelet);
      break;
    case ARITH:
      free_arith(n->val.arithet);
    }
    free_node(n->next);
    free(n);
  }
}

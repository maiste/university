#include <stdio.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>

#include "arith.h"
#include "parsing_struct.h"
#include "struct_utils.h"
#include "cmd.h"

#define YYTOKENTYPE
#include "mpsh.tab.h"

#include "redirection.h"
#include "environment.h"
#include "handle.h"

// Execute deux commandes séparées par un &&
int execute_and(logics * left, logics * right, short isfree) {
  if(handle(left,isfree) == 0) {
    return handle(right,isfree);
  }
  return 1;
}

// Execute deux commandes séparées par un ||
int execute_or(logics * left, logics * right, short isfree) {
  // Inversion 0 / 1 du shell par rapport à C
  return handle(left, isfree) && handle(right, isfree);
}

// Traite une liste de commande, peut être terminée par uen boucle while
int handle_multiples(node * nd, short isfree){
  if(nd == NULL)
    return -1;
  short res = 1;
  if(nd->content == WHILEL){
    handle_while(nd->val.whilelet, isfree);
  }
  else{
    if(nd->next == NULL)
      res = handle(nd->val.logicset,isfree);
    else{
      handle(nd->val.logicset, isfree);
      res = handle_multiples(nd->next,isfree);
    }
  }
  if(isfree)
    free(nd);
  return res;
}

// Exécute une boucle while
void handle_while(whilel * w, short isfree){
  while(eval(w->cond))
    handle_multiples(w->todo, 0);

  if(isfree)
    free_whilel(w);
}

// Traite une commande *s
int handle(logics * l, short isfree){
  int res = -1;

  if(l == NULL)
    return res;

  if(l->log == AND)
    res = execute_and(l->left,l->right,isfree);

  else if(l->log == OR)
    res = execute_or(l->left, l->right,isfree);

  else{
    piped_cmd * p = compute_words_in_piped(l->piped);
    res = execute_with_pipe(p,l->is_bg);

    if(isfree)
      free_node(l->piped);
    free_piped(p);
  }

  if(isfree)
    free(l);

  return res;
}

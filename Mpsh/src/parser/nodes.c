#include <string.h>
#include <stdlib.h>
#include <stdio.h>

#include "parsing_struct.h"
#include "struct_utils.h"
#include "cmd.h"

// Ajoute un noeud en tête d'un autre noeud
node * add_front_node(node * res, node* n){
  if(res == NULL)
    return n;
  res->next=n;
  return res;
}

node * add_last_node(node * res, node *n){
  if(res == NULL)
    return n;
  node * tmp = res;
  while(tmp->next)
    tmp = tmp->next;
  tmp->next=n;
  return res;
}

node * mkNode(enum nodet_content t){
  node * res = malloc(sizeof(node));
  if (res == NULL)
    return NULL;
  res->content = t;
  res->next = NULL;
  return res;
}

// Créer un noeud char
node * mknode_char(char * h){
  node * res = mkNode(CHAR);
  if (res == NULL)
    return NULL;
  res->val.charet = h;
  return res;
}

// Créer un noeud cmd
node * mknode_pre_cmd(pre_cmd * h){
  node * res = mkNode(CMD);
  if (res == NULL)
    return NULL;
  res->val.cmdet = h;
  return res;
}

// Creér un node pour une redirection
node * mknode_redir(enum rediroff t,char* to){
  redir * res = malloc(sizeof(redir));
  if(res == NULL)
    return NULL;
  res->of=t;
  res->to=to;

  node * nd = mkNode(REDIR);
  if(nd==NULL){
    free(res);
    return NULL;
  }
  nd->val.rediret=res;

  return nd;
}

node * mknode_logics(logics* c){
  node * res = mkNode(LOGICS);
  if (res == NULL)
    return NULL;
  res->val.logicset = c;

  return res;
}

node * mknode_whilel(arith * cond, node * nd){
  whilel * res = malloc(sizeof(whilel));
  if(res == NULL)
    return NULL;
  res->cond=cond;
  res->todo=nd;

  node * n = mkNode(WHILEL);
  if (n == NULL)
    return NULL;
  n->val.whilelet = res;

  return n;
}

node * mknode_arith(arith * a){
  node * res = mkNode(ARITH);
  if (res == NULL)
    return NULL;
  res->val.arithet = a;

  return res;
}

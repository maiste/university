#include <stdlib.h>
#include <ctype.h>
#include <errno.h>
#include <limits.h>
#include <math.h>

#include "arith.h"

#include "environment.h"

/* FREE */
void free_op(op * o){
  free_arith(o->left);
  free_arith(o->right);
  free(o);
}

void free_arith(arith * a){
  if(a==NULL)
    return;
  switch(a->what){
  case arith_NAT:
    break;

  case arith_VAR:
    free(a->val.var);
    break;

  case arith_OP:
    free_op(a->val.opa);
    break;
  }

  free(a);
}

// Créer un noeud correspondant à un naturel
arith * mk_nat(int n){
  arith * res = malloc(sizeof(arith));
  if (res == NULL)
    return NULL;

  res->what = arith_NAT;
  res->val.nat=n;

  return res;
}

// Créer un noeud avec une variable
arith * mk_var(char * v){
  arith * res = malloc(sizeof(arith));
  if (res == NULL)
    return NULL;

  res->what = arith_VAR;
  res->val.var=v;

  return res;
}

// Créer un noeud avec un opérateur
arith * mk_op(enum op_e e,arith * l, arith * r){
  arith * res = malloc(sizeof(arith));
  if (res == NULL)
    return NULL;

  res->what = arith_OP;

  op * o = malloc(sizeof(op));

  if(o == NULL){
    free(res);
    return NULL;
  }

  o->what=e;
  o->left=l;
  o->right=r;

  res->val.opa=o;

  return res;
}

// Évalue une expression arithmétique
int eval(arith * ar){
  char * res = NULL;
  switch(ar->what){
  case arith_NAT:
    return ar->val.nat;
    break;

  case arith_VAR:
    res = get_var_value(ar->val.var);
    if(res == NULL)
      res = getenv(ar->val.var);
    if(res == NULL){
      printf("There is no variable %s, replacing by 0\n",ar->val.var);
      return 0;
    }
    char * endptr = NULL;
    long int val = strtol(res,&endptr,10);
    if ((errno == ERANGE && (val == LONG_MAX || val == LONG_MIN)) || endptr == ar->val.var){
      printf("Error in arithmetic expression: %s contains no integer value, replacing by 0.\n",ar->val.var);
      return 0;
    }
    else{
      return val;
    }
    break;

  case arith_OP:
    switch(ar->val.opa->what){
    case op_PLUS:
      return eval(ar->val.opa->left) + eval(ar->val.opa->right);
    case op_MINUS:
      return eval(ar->val.opa->left) - eval(ar->val.opa->right);
    case op_MULT:
      return eval(ar->val.opa->left) * eval(ar->val.opa->right);
    }
    break;
  }
  return 0;
}

// Évalue une expression arithmétique et stocke le résultat dans une chaine de caractères
char * eval_to_char(arith * a){
  int c = eval(a);
  int exp = log10(10+abs(c));
  char * str = malloc((exp+2)*sizeof(char));
  sprintf(str,"%d",c);
  return str;
}

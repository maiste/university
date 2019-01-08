#include <string.h>
#include <stdlib.h>
#include <stdio.h>

#include "parsing_struct.h"
#include "struct_utils.h"
#include "nodes.h"
#include "cmd.h"
#include "environment.h"

// Essaye de remplacer le string par un alias existant
void try_change_alias(char ** str){
  char * res = get_alias_value(*str);
  if(res != NULL){
    char * tmp =res;
    short isneq = 1;
    while(isneq && tmp != NULL){
      res=tmp;
      tmp=get_alias_value(tmp);
      isneq=(tmp==NULL)?1:strcmp(*str,tmp);
    }
    if(isneq){
      free(*str);
      *str=strdup(res);
    }
  }
}

// Si la commande est de la forme "a=b" la réécrit en "set a b"
void try_change_set(char ** str, node ** nd){
  char * tmp = *str;
  short haseq=0;
  while(*tmp){
    if(*tmp == '=')
      haseq=1;
    tmp+=1;
  }
  if(haseq){
    node * fst = mknode_char(strdup(strtok(*str,"=")));
    char * s = strtok(NULL,"=");
    if(s == NULL)
      s="";
    node * snd = mknode_char(strdup(s));
    *nd = add_front_node(fst, add_front_node(snd,*nd));
    free(*str);
    *str=strdup("set");
  }
}

/* Si str contient une chaine de la forme "$chaine"
   cette fonction remplace $chaine par sa valeur locale si elle existe,
   sinon globale si elle existe, sinon par la chaine vide.
 */
void try_change_local(char ** str){
  char * tmp  = *str;
  int i = 0;
  while(*tmp){
    if (*tmp == '$'){
      char * start = tmp;
      tmp += 1;
      int j = 0;
      int maxj = 0;
      char * res = NULL;
      char * max = NULL;
      char old = 0;
      while(tmp[j-1]){
	old = tmp[j];
	tmp[j] = '\0';
	res = get_var_value(tmp);
	if(res == NULL)
	  res = getenv(tmp);
	if(res != NULL){
	  max = res;
	  maxj=j;
	}
	tmp[j] = old;
	j++;
      }
      if(max){
	char * new = malloc(strlen(max)+strlen(*str) - maxj);
	strncpy(new,*str,i);
	new[i]='\0';
	strcat(new,max);
	strcat(new,(*str)+i+maxj+1);
	free(*str);
	*str=new;
	tmp = new + i;
	continue;
      }
      else{
	*start='\0';
      }
    }
  tmp+=1;
  i++;
  }
}

// Créer une pre_cmd
pre_cmd * mkpre_cmd(char * name, node * nd){
  pre_cmd * res = malloc(sizeof(pre_cmd));
  if(res == NULL)
    return NULL;
  try_change_set(&name,&nd);
  res->name = name;
  res->args = nd;
  return res;
}

// Créer une boucle for: c'est un while déguisé
node * mkforl(char * var, arith * from, arith * to, node * nd){
  // Ajoute une commande "set var from"
  pre_cmd * fst = mkpre_cmd(strdup("set"),add_front_node(mknode_char(strdup(var)),mknode_char(eval_to_char(from))));
  node * fstp = mknode_logics(mkleaflogics(mknode_pre_cmd(fst),0));

  // Ajoute une commande "set var {$var+1}"
  arith * plusone = mk_op(op_PLUS,mk_var(strdup(var)),mk_nat(1));
  pre_cmd * lst = mkpre_cmd(strdup("set"),add_front_node(mknode_char(strdup(var)),mknode_arith(plusone)));
  node * lstp = mknode_logics(mkleaflogics(mknode_pre_cmd(lst),0));

  // Ajoute la boucle while
  arith * cond_while = mk_op(op_MINUS,mk_op(op_PLUS,to,mk_nat(1)),mk_var(strdup(var)));
  node * whiled = mknode_whilel(cond_while, add_last_node(nd,lstp));

  free(var);
  return (add_front_node(fstp,whiled));
}

// Créer une feuille "logique"
logics * mkleaflogics(node * list,short bg){
  logics * res = malloc(sizeof(logics));
  if(res == NULL)
    return NULL;
  res->log = LEAF;
  res->left = NULL;
  res->right = NULL;
  res->piped = list;
  res->is_bg = bg;
  return res;
}

// Créer un noeud "logique"
logics * mkreallogics(enum logical l,logics * left, logics * right){
  logics * res = malloc(sizeof(logics));
  if(res == NULL)
    return NULL;
  res->log = l;
  res->piped = NULL;
  res->left = left;
  res->right = right;
  return res;
}

/* Convertit une pre_cmd en cmd:
   Évalue les expressions arithmétiques,
   remplace les variables par leur valeurs,
   définie les redirections.
 */
cmd * compute_words(pre_cmd * pre){
  cmd * res = malloc(sizeof(cmd));
  if(res == NULL)
    return NULL;

  res->stdin = NULL;
  res->stdout = NULL;
  res->stderr = NULL;

  res->name = strdup(pre->name);
  try_change_alias(&(res->name));

  node * nd = pre->args;
  res->nb_args = 2;
  while(nd != NULL){
    if(nd->content == CHAR || nd->content == ARITH){
      res->nb_args++;
    }
    nd = nd->next;
  }

  char ** args = malloc(res->nb_args * sizeof(char *));
  if(args == NULL){
    free_cmd(res);
    return NULL;
  }

  args[0]=res->name;

  int i=1;

  nd = pre->args;
  while(nd != NULL){
    if(nd->content == CHAR){
      args[i]=strdup(nd->val.charet);
      try_change_local(&(args[i]));
    }
    if(nd->content == ARITH){
      args[i] = eval_to_char(nd->val.arithet);
    }
    if(nd->content == REDIR){
      if((nd->val).rediret->of == IN)
	res->stdin=strdup((nd->val).rediret->to);
      if((nd->val).rediret->of == OUT)
	res->stdout=strdup((nd->val).rediret->to);
      if((nd->val).rediret->of == ERR)
	res->stderr=strdup((nd->val).rediret->to);
      i--;
    }
    nd = nd->next;
    i++;
  }

  res->nb_args--;
  args[res->nb_args] = NULL;

  res->args = args;
  return res;
}

/* Cette fonction va évaluer les expressions arithmétiques et remplacer les variables par leurs valeurs.
La fonction préserve la liste de noeuds qui constitue les arguments.
 */
piped_cmd * compute_words_in_piped(node * p){
  piped_cmd * res = malloc(sizeof(piped_cmd));
  if(res==NULL)
    return NULL;
  res->nb_elem = 0;

  node * tmp = p;
  while(tmp){
    res->nb_elem++;
    tmp=tmp->next;
  }

  res->cmds = malloc(sizeof(cmd *));
  if(res->cmds == NULL){
    free_piped(res);
    return NULL;
  }

  for(int i = 0; i<res->nb_elem;i++ ){
    res->cmds[i] = compute_words(p->val.cmdet);
    p=p->next;
  }
  return res;
}

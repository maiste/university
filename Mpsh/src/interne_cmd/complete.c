#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include "parsing_struct.h"
#include "nodes.h"
#include "struct_utils.h"
#include "cmd.h"

#include "complete.h"

void init_complete_db(){
  complete_db = NULL;
}

// Ajoute une completion possible pour une commande
short add_completion(char ** args, int nb_args){
  if(nb_args < 3)
    return 0;
  char * name = args[1];
  char * ext = args[2];

  if(complete_db == NULL){
    complete_db = mknode_pre_cmd(mkpre_cmd(strdup(name),mknode_char(strdup(ext))));
    return 1;
  }
  node * tmp = complete_db;
  while(tmp->next!=NULL){
    if(!strcmp(tmp->val.cmdet->name,name)){
      tmp->val.cmdet->args = add_front_node(tmp->val.cmdet->args, mknode_char(strdup(ext)));
      return 1;
    }
    tmp = tmp->next;
  }
  tmp->next = mknode_pre_cmd(mkpre_cmd(strdup(name),mknode_char(strdup(ext))));
  return 1;
}

void free_complete_db(){
  if(complete_db != NULL)
    free_node(complete_db);
}

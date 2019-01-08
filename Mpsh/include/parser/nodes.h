#ifndef NODES_H
#define NODES_H

#include "arith.h"
#include "parsing_struct.h"

node * add_front_node(node* c, node * n);
node * add_last_node(node* c, node * n);

node * mknode_char(char* c);
node * mknode_pre_cmd(pre_cmd* c);
node * mknode_redir(enum rediroff t,char* to);
node * mknode_logics(logics* c);
node * mknode_whilel(arith * cond, node * nd);
node * mknode_arith(arith * a);
#endif

#ifndef LIST_H
#define LIST_H

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct node node;

struct node {
	char *value;
	node *next;
};

node * init_node(char *value);
node * insert_head(node *list, char *value);
void free_all(node *list);

#endif // LIST_H
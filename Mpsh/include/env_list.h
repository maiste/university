
#ifndef ENV_LIST_H
#define ENV_LIST_H

#include <stdlib.h>
#include <stdio.h>
#include <assert.h>
#include <string.h>

// structs : 

struct node {
    char * name; 
    char * value;
    struct node * next;
};

typedef struct node node;

struct list {
    node * first;
};

typedef struct list list;

enum {ALIAS, VARS};

// prototypes :

short create_env_list();
short add(char *, char *, int);
short remove_key(char *, int);
char * get_list_elem(char *, int);
void print_list();


#endif

#ifndef PARSING_STRUCT_H
#define PARSING_STRUCT_H

#include "arith.h"

struct node;

typedef struct{
  char * name;
  struct node * args;
} pre_cmd;

// Structure pour les commandes
typedef struct {
  char *name;
  char ** args; // NULL terminated
  int nb_args; // length(args) - 1

  char * stdin;  // Sera NULL si l'utilisateur n'a pas redirigé l'entrée standard
  char * stdout; // Sera NULL si l'utilisateur n'a pas redirigé la sortie standard
  char * stderr; // Sera NULL si l'utilisateur n'a pas redirigé la sortie d'erreur
} cmd;

// Structure représentant des commandes séparées par des pipes
typedef struct {
  cmd ** cmds;
  int nb_elem;
} piped_cmd;

// Structure représentant un embranchement logique, ou une feuille
enum logical {AND, OR, LEAF};
typedef struct logics{
  enum logical log;
  struct logics * left;
  struct logics * right;

  struct node * piped; // If log is set to LEAF, contains only (pre_cmd *)
  short is_bg;
} logics;

// Structure représentant des redirections
enum rediroff {IN,OUT,ERR};
typedef struct redir{
  enum rediroff of;
  char * to;
} redir;

typedef struct whilel {
  arith * cond;
  struct node * todo; // List of node to execute, must be LOGICS
} whilel;

// Structure de type union pour le contenu des noeuds
enum nodet_content {CHAR,CMD,REDIR,LOGICS,WHILEL, ARITH};
typedef union nodet {
  char * charet;
  pre_cmd *  cmdet;
  redir * rediret;
  logics * logicset;
  whilel * whilelet;
  arith * arithet;
} nodet;

// Structure representant une liste chainée
struct node {
  enum nodet_content content;
  nodet val;
  struct node * next;
};

typedef struct node node;

#endif

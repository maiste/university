#include "cmd.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include "cmd.h"
#include "cd.h"
#include "history.h"
#include "alias.h"
#include "complete.h"
#include "unalias.h"
#include "type.h"
#include "umask.h"
#include "set.h"
#include "unset.h"
#include "export.h"
#include "builtin.h"

int nb_builtins = 12;
int fork_builtins = 4;

char *builtins [12] =
  {
   "history",
   "umask",
   "pwd",
   "type",
   "cd",
   "exit",
   "alias",
   "unalias",
   "set",
   "unset",
   "export",
   "complete"
  };

/* Dit si il s'agit d'une builtin
   et si son contenu est modifiable */
int isbuiltin(char *exe) {
  for (int i = 0 ; i < nb_builtins; i++) {
    if(strcmp(builtins[i], exe) == 0)
        return 1;
  }
  return 0;
}

/* Lance les commandes builtins */
short launch_builtin(cmd *exe) {
  char ** args = exe->args;
  if(strcmp(exe->name, "cd") == 0) {
    if(exe->nb_args > 2)
      return 1;
    return cd(args[1]);
  } else if(strcmp(exe -> name, "complete") == 0) {
    return (add_completion(args, exe->nb_args));
  } else if(strcmp(exe -> name, "history") == 0) {
    return (history(args, exe->nb_args) != NULL);
  } else if(strcmp(exe -> name, "alias") == 0) {
    return alias(args[1]);
  } else if(strcmp(exe -> name, "set") == 0) {
    if(exe->nb_args != 3)
      return 1;
    return set(args[1], args[2]);
  } else if(strcmp(exe -> name, "unset") == 0) {
    if(exe->nb_args == 2)
      return unset(args[1]);
    return 1;
  } else if(strcmp(exe -> name, "export") == 0) {
    return export(args[1]);
  } else if(strcmp(exe -> name, "unalias") == 0) {
    return unalias(args[1]);
  } else if(strcmp(exe->name, "umask") == 0) {
    return umask_builtin(exe);
  } else if(strcmp(exe->name, "type") == 0) {
    if(exe->nb_args < 2)
      return 1;
    return type(exe->nb_args,args);
  } else if(strcmp(exe->name, "exit") == 0) {
    if(exe->nb_args <= 1)
      return 256;
    return 257 + atoi(args[1]);
  }  else if (strcmp(exe->name, "pwd") == 0) {
    char buf[100];
    printf("%s\n", getcwd(buf, 100));
    return 1;
  }
  return 1;
}

/* Remet les I/O du shell sur les I/O standards */
void reset_builtin(int in, int out, int err) {
  dup2(in, 0);
  dup2(out,1);
  dup2(err, 2);
}

/* Execute une commande builtin en fonction du contexte */
int exec_builtin(cmd* exe, int last) {
  int in = dup(0), out = dup(1), err = dup(2), ret = 0;
  if(last) {
    ret = launch_builtin(exe);
    reset_builtin(in,out,err);
    return ret;
  } else {
    exit(launch_builtin(exe));
  }
}

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "builtin.h"
#include "cmd.h"
#include "redirection.h"
#include "type.h"

short type(int nb_args, char *commands []) {
  for (int i = 1 ; i < nb_args ; i++) {
    if (commands[i] != NULL) {
      if (isbuiltin(commands[i])) {
        printf("%s is a builtin of mpsh\n", commands[i]);
      } else {
        char * old = strdup(commands[i]);
        update_cmd_with_path(&commands[i]);
        printf("%s is the command %s\n",old,commands[i]);
        free(old);
      }
    } else {
      return 1;
    }
  }  
  return 0;
}

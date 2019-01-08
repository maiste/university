#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <errno.h>
#include "cmd.h"
#include "umask.h"

/* Ajoute l'environment */
extern char ** environ;

/* Lance la builtin de Umask */
short umask_builtin(cmd *exe) {
  if(exe->nb_args == 2){
    int mask = strtol(exe->args[1],NULL,8);
    if(mask == -1)
      return 1;
    umask(mask);
    return 0;
  } else if(exe->nb_args == 1) {
    mode_t old = umask(0002);
    printf("%.3o\n", old);
    umask(old);
    return 0;
  }
  return 1;
}

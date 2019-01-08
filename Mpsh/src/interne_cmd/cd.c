#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <errno.h>
#include "environment.h"
#include "env_list.h"
#include "cd.h"

/* Met à jour la variable PWD de l'environement */
void update_pwd() {
  char *buf = malloc(sizeof(char)*BUF_SIZE);
  if (getcwd(buf, BUF_SIZE) != NULL) {
      setenv("PWD",buf,1);  
      return;
  }
  fprintf(stderr, "cd : can't update PWD\n");
}


/* Modifie la variable pwd par la variable
 * path si cela est possible */
int cd (char *path) {
  if(path == NULL) {
    char *home = getenv("HOME");
    if (home == NULL) {
      fprintf(stderr,"cd : can't find $HOME in env\n");
      return 1;
    }
    path = home;
  } 
  if(chdir(path) == 0) {
    update_pwd();
    return 0;
  } else {
    perror("cd");
    return 1;
  }
}


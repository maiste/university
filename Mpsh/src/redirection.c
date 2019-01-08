#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <unistd.h>
#include <string.h>
#include <dirent.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <errno.h>
#include <fcntl.h>

#include "parsing_struct.h"
#include "struct_utils.h"
#include "nodes.h"
#include "cmd.h"
#include "builtin.h"
#include "history.h"
#include "completion.h"
#include "redirection.h"
#include "pid.h"
#include "set.h"
#include "environment.h"
#include "redirection.h"

extern char **environ; // Environnement natif
int current_pid = -1; // Dernier PID lancer (pour les signaux)

/* Place le shell en attente de la fin de commande
 si elle n'est pas en background  */
int waitPid(short background) {
  int status;
  if(!background) {
    waitpid(current_pid,&status,0);
    if(WIFEXITED(status)){
      short res = WEXITSTATUS(status);
      char str[4];
      sprintf(str,"%d",res);
      set("?",str);
      current_pid = -1;
      return res;
    }
  }
  current_pid = -1;
  return 0;
}

/* Lance les commandes avec un pipe si necessaire */
int execute_with_pipe(piped_cmd *chain, short background) {
  int fd[2], save_in = dup(0);
  for (int i = 0 ; i < chain->nb_elem ; i++) {
    if(i >= chain->nb_elem-1 && isbuiltin(chain->cmds[i]->name))
      return execute_cmd(chain->cmds[i],1);
    pipe(fd);
    current_pid = fork();
      if(current_pid < 0) {
        fprintf(stderr, "Can't fork");
        return -1;
      } else if (current_pid == 0) {
        if(i < chain->nb_elem-1) {
          dup2(fd[1],1);
        }
        dup2(save_in, 0);
        close(fd[0]);
        close(fd[1]);
        close(save_in);
        execute_cmd(chain->cmds[i], 0);
        exit(1);
      } else {
        close(fd[1]);
        dup2(fd[0], save_in);
        close(fd[0]);
      }
  }
  close(save_in);
  return waitPid(background);
}

/* Met à jour l'entrée */
short update_stdin(char *exe_stdin) {
  int fd = open(exe_stdin, O_RDONLY);
    if (fd == -1) {
      fprintf(stderr, "%s can't be opened (stdin)\n", exe_stdin);
      exit(1);
    }
  dup2(fd, 0);
  close(fd);
  return 0;
}

/* Met à jour la sortie */
short update_stdout(char *exe_stdout) {
  int fd = open(exe_stdout, O_WRONLY | O_CREAT, S_IRUSR | S_IWUSR);
  if (fd == -1) {
    fprintf(stderr, "%s can't be opened (stdout)\n", exe_stdout);
    exit(1);
  }
  dup2(fd,1);
  close(fd);
  return 0;
}

/* Met à jour la sortie d'erreur */
short update_stderr(char *exe_stderr) {
  int fd = open(exe_stderr, O_WRONLY | O_CREAT, S_IRUSR | S_IWUSR);
  if (fd == -1) {
    fprintf(stderr, "%s can't be opened (stderr)\n", exe_stderr);
    exit(1);
  }
  dup2(fd, 2);
  close(fd);
  return 0;
}

/* met à jour les entrées/sorties du shell */
short update_cmd(cmd *exe) {
  short res = 0;
  if (exe->stdin != NULL) {
    res = update_stdin(exe->stdin);
  }
  if (exe->stdout != NULL) {
    res = update_stdout(exe->stdout);
  }
  if (exe->stderr != NULL) {
    res = update_stderr(exe->stderr);
  }
  return res;
}

// Éffectue une recherche récursive dans un dossier
// dir est un répertoire
short rec_search(char ** exe, char * str, uid_t u, gid_t g){
  struct stat buf;
  int t = 0;
  short ok = 0;

  node * dirs= NULL;

  DIR * dir = opendir(str);
  if(dir != NULL){
    struct dirent * di;
    while(((di = readdir(dir)) != NULL) && strcmp(di->d_name,".") && strcmp(di->d_name,"..")){
      char * fp = malloc(strlen(str)+1+strlen(di->d_name)+1);
      if(fp == NULL)
	exit(errno);
      strcpy(fp,str);
      strcat(fp,"/");
      strcat(fp,di->d_name);
      t=stat(fp,&buf);
      if (!t){
	if (!strcmp(*exe, di->d_name) && is_exec(buf.st_mode,buf.st_uid,u,buf.st_gid,g)) {
	  *exe=fp;
	  closedir(dir);
	  return 1;
	}
	else if(is_readable(buf.st_mode,buf.st_uid,u,buf.st_gid,g)){
	  dirs = add_front_node(dirs,mknode_char(fp));
	}
	else
	  free(fp);
      }
    }
    closedir(dir);

    node * tmp;
    while(dirs != NULL){
      if(!ok)
	ok = rec_search(exe,dirs->val.charet,u,g);
      free(dirs->val.charet);
      tmp = dirs;
      dirs = dirs->next;
      free(tmp);
    }
  }
  return ok;
}

/* Met à jour le cmd en fonction cherchant le nom
   de l'exécutable dans le CHEMIN.
 */
void update_cmd_with_path(char ** exe){
  char * chemin = strdup(getenv("CHEMIN"));
  char * str = strtok(chemin,":");

  char *tmp = NULL;

  while(str != NULL && *str){
    int n = strlen(str);
    if(n>=2 && str[n-1] == '/' && str[n-2] == '/'){ // On doit faire une recherche récursive
      str[n-2]='\0';
      struct stat buf;
      int t = 0;
      uid_t u = getuid();
      gid_t g = getgid();
      t=stat(str,&buf);
      if(!t && is_readable(buf.st_mode,buf.st_uid,u,buf.st_gid,g)){
	rec_search(exe,str,u,g);
      }
      return;
    }
    else{
      tmp = malloc((strlen(*exe)+n+2) * sizeof(char));
      if(tmp == NULL)
	exit(errno);
      strcpy(tmp,str);
      strcat(tmp,"/");
      strcat(tmp,*exe);
      if( access(tmp , F_OK ) != -1 ){
	*exe=tmp;
	break;
      }
    }
    str = strtok(NULL,":");
    free(tmp);
  }
  free(chemin);
}

/* exécute une commande forkée */
short execute_cmd(cmd *exe, short last) {
  if (update_cmd(exe) == 0) {
    if(!isbuiltin(exe->name)) {
      if(access(exe->name, F_OK ) == -1 ){ // Si le fichier n'est pas directement accessible
        update_cmd_with_path(&(exe->name));
      }
      execve(exe->name, exe->args, environ);
      perror(exe->name);
      exit(errno);
    } else
      return exec_builtin(exe,last);
  } else
    exit(1);
}

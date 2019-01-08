#include <stddef.h>
#include <string.h>
#include <stdio.h>
#include <dirent.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>

#include <readline/readline.h>

#include "builtin.h"
#include "complete.h"

int min(int a, int b){
  return (a<b)?a:b;
}

short is_exec(mode_t mod, uid_t prop, uid_t origu, gid_t grp, gid_t origg){
  short b = S_ISREG(mod);
  if(origu == prop)
    b=mod & S_IXUSR;
  if(origg == grp)
    b = b | (mod & S_IXGRP);
  return b | (mod & S_IXOTH);
}

short is_readable(mode_t mod, uid_t prop, uid_t origu, gid_t grp, gid_t origg){
  short b = S_ISDIR(mod);
  if(origu == prop)
    b=mod & S_IRUSR;
  if(origg == grp)
    b = b | (mod & S_IRGRP);
  return b | (mod & S_IROTH);
}

// Va générer la complétion d'un nom de commande, en cherchant dans le PATH
char * completion_generator(const char *text, int state){
  static int list_index, dir_index,dir_in_dir, len; // To remember where we were
  char *name;

  if (!state) {
    list_index = 0;
    dir_index=0;
    dir_in_dir=0;
    len = strlen(text);
  }

  while (list_index < nb_builtins) {
    name = builtins[list_index++];
    if (strncmp(name, text, len) == 0) {
      return strdup(name);
    }
  }

  char * chemin = strdup(getenv("CHEMIN"));
  if(chemin == NULL)
    return NULL;

  char * str = strtok(chemin,":");

  struct stat buf;
  int t = 0;

  uid_t u = getuid();
  gid_t g = getgid();

  int count = 0;
  while(str != NULL){
    if(count == dir_index){
      t=stat(str,&buf);
      if(!t && is_readable(buf.st_mode,buf.st_uid,u,buf.st_gid,g)){
	DIR * dir = opendir(str);
	if(dir != NULL){
	  struct dirent * di;
	  int count2=0;
	  while((di = readdir(dir)) != NULL){
	    if(count2==dir_in_dir){
	      dir_in_dir++;
	      if (!strncmp(text, di->d_name, min(len,256))) {
		char * fp = malloc(strlen(str)+1+strlen(di->d_name)+1);
		strcpy(fp,str);
		strcat(fp,"/");
		strcat(fp,di->d_name);
		t=stat(fp,&buf);
		free(fp);
		if (is_exec(buf.st_mode,buf.st_uid,u,buf.st_gid,g)){
		  char * res = strdup(di->d_name);
		  closedir(dir);
		  free(chemin);
		  return res;
		}
	      }
	    }
	    count2++;
	  }
	  closedir(dir);
	}
	dir_index++;
	dir_in_dir=0;
      }
    }
    count++;
    str = strtok(NULL,":");
  }

  free(chemin);

  return NULL;
}

// Noeud contenant les complétions obligatoires pour la commande en cours
node * curr_compl_poss;

// Va générer des complétions pour les arguments d'une commande
char * completion_generator_local(const char *text, int state){
  static int len,pos;
  static char * pwd;

  if (!state) {
    pos = 0;
    len = strlen(text);
    pwd = getenv("PWD");
  }

  DIR * dir = opendir(pwd);
  int count = 0;
  if(dir != NULL){
    struct dirent * di;
    while((di = readdir(dir)) != NULL){
      if(pos == count){
	pos++;
	if(curr_compl_poss == NULL && !strncmp(text, di->d_name, min(len,256))){
	  char * res = strdup(di->d_name);
	  closedir(dir);
	  return res;
	}
	else{
	  int lentmp = 0;
	  int dlen = strlen(di->d_name);
	  node * tmp = curr_compl_poss;
	  while(tmp){
	    lentmp = strlen(tmp->val.charet);
	    if(!strncmp(text, di->d_name, min(len,256)) && dlen >= lentmp && !strcmp((di->d_name + dlen-lentmp),tmp->val.charet)){
	      char * res = strdup(di->d_name);
	      closedir(dir);
	      return res;
	    }
	    tmp = tmp->next;
	  }
	}
      }
      count++;
    }
    closedir(dir);
  }

  return NULL;
}

// Retourne un noeud contenant une liste de suffixes possibles pour une commande donnée, ou NULL
node * get_if_here(char * cmd_name){
  node * tmp = complete_db;
  while(tmp){
    if(!strcmp(tmp->val.cmdet->name, cmd_name))
      return tmp->val.cmdet->args;
    tmp=tmp->next;
  }
  return NULL;
}

char ** completion(const char *text, int start, int end){
  if (start == 0 && end != 0) // Si c'est une commande
    return rl_completion_matches(text, completion_generator);
  if(start != 0){ // Si c'est un argument
    int fst_space_ind = 0;
    char * tmp = rl_line_buffer;
    while(tmp){
      if(isspace(*tmp))
	break;
      fst_space_ind++;
      tmp+=1;
    }
    char * cmd_name = malloc(fst_space_ind+1);
    strncpy(cmd_name,rl_line_buffer,fst_space_ind);
    cmd_name[fst_space_ind] = '\0';
    curr_compl_poss = get_if_here(cmd_name);
    return rl_completion_matches(text, completion_generator_local);
  }
  return NULL;
}

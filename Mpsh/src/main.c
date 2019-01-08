#include <stdio.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>

#include <readline/readline.h>
#include <readline/history.h>

#include "arith.h"
#include "cmd.h"

#define YYTOKENTYPE
#include "mpsh.tab.h"
#include "lex.yy.h"

#include "redirection.h"
#include "environment.h"
#include "complete.h"
#include "completion.h"
#include "handle.h"
#include "signal_handler.h"

// Initialize readline : lance l'historique et la complétion
void initialize_readline(){
  using_history(); // Nous allons utiliser l'historique

  rl_attempted_completion_function = completion;
}

// Lis et exécute ~/.mpshrc
void assign_mpshrc_values () {
  char * rc = "/.mpshrc";
  char * mpshrc_path = malloc(strlen(getenv("HOME"))+strlen(rc)+1);
  int h = 0;
  if (mpshrc_path == NULL)
    return ;
  strcpy(mpshrc_path,getenv("HOME"));
  strcat(mpshrc_path,rc);
  FILE * fic = fopen(mpshrc_path, "r");
  if (fic == NULL) // le fichier n'existe pas, on le crée
    fclose(fopen(mpshrc_path, "w"));
  else {
    char line[SIZE] = "";
    while(fgets(line, SIZE, fic) != NULL) {
      yy_scan_string(line);
      h = yyparse();
      if(!h)
	handle_multiples(parseres,1);
    }
    fclose(fic);
  }
  free(mpshrc_path);
}

// Mets les bonnes variables dans l'environnement
void setup_env() {
  setenv("CHEMIN", getenv("PATH"),1);
  setenv("SHELL", "mpsh", 1);
  setenv("INVITE", "\e[1;33mmpsh\e[0m@\e[1;34mlocal\e[0m$ ",1);
}

int main() {
  // Init
  init_complete_db();
  initialize_readline();
  init_signal_handler();
  init_env();
  setup_env();
  assign_mpshrc_values();

  // History
  char * hist = "/.mpsh_history";
  char * history_file = malloc(strlen(getenv("HOME"))+strlen(hist)+1);
  if (history_file == NULL)
    return -1;
  strcpy(history_file,getenv("HOME"));
  strcat(history_file,hist);
  short pre_status = 0;
  short status = 0;

  int h = read_history(history_file);

  if (h)
    fprintf(stderr, "Could not open %s: %s\n",history_file, strerror(h));

  char *s = NULL;

  // Boucle Read-Eval-Print
  while(1){
    s = readline(getenv("INVITE"));
    if(s==NULL){ // Si l'utilisateur a fait un Ctrl-D
      printf("\n");
      pre_status = 256;
      status=0;
      break;
    }
    if(*s != '\0'){ // Si ce n'est pas une ligne vide
      add_history(s);
      yy_scan_string(s);
      h = yyparse();
      if(!h)
	pre_status = handle_multiples(parseres,1);
      if(pre_status > 255){
	free(s);
	break;
      }
      status = pre_status;
    }

    yylex_destroy();
    free(s);
  }

  free_complete_db();

  h = write_history(history_file);

  if (h) {
    fprintf(stderr, "Could not save history file to %s: %s\n",history_file, strerror(h));
  }

  free(history_file);

  return (pre_status == 256)?status:(pre_status - 257);
}

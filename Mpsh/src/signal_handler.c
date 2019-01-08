#include <stdio.h>
#include <stdlib.h>
#include <signal.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>
#include "pid.h"
#include "signal_handler.h"

static struct sigaction sig;

/* Fonction qui récupère le signal */
void signal_catcher(int sigval) {
  if(current_pid == -1) {
    fflush(stdout);
    printf("\b\b  \b\b");
  }
  else {
    printf("\n");
    kill(current_pid, sigval);
    while(wait(NULL)>= 0);
  }
  current_pid = -1;
}

/* Fonction qui initialise les signaux */
void init_signal_handler(){
  sig.sa_handler=signal_catcher;
  sigemptyset(&sig.sa_mask);
  sigaction(SIGINT, &sig, NULL);
  sigaction(SIGSTOP, &sig, NULL);
  sigaction(SIGTSTP, &sig, NULL);
}


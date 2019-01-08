#ifndef BUILTIN_H
#define BUILTIN_H

#include "cmd.h"

extern int nb_builtins;
extern char *builtins [12];

int isbuiltin (char * name);

int exec_builtin(cmd *exe, int last);

#endif

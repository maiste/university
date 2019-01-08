#ifndef ENVIRONMENT_H
#define ENVIRONMENT_H

// open :
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h> // write 
#include <string.h> // strcat()
#include <stdlib.h> // getenv()
#include <stdio.h> // fopen()

#define SIZE 100

void init_env();
// gets 
char * get_alias_value (char *); 
char * get_var_value (char *); 
// adds
short add_to_aliases (char *, char *);
short add_to_vars (char *, char *);
// removes 
short remove_to_aliases (char *);
short remove_to_vars (char *);
// prints
void print_aliases();
void print_vars();

char * string_without_quotes(char *);

#endif

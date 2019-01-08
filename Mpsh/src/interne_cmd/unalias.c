#include "unalias.h"

/*
  enlève la clé arg de la liste des alias
*/
short unalias(char * arg) {
  if (arg == NULL)
    return 1;
  return !remove_to_aliases(arg);
}

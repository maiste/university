#include "unset.h"

/*
  enlève le couple de la liste des variables
*/
short unset(char * var){
  short res = remove_to_vars (var);
  return !res;
}

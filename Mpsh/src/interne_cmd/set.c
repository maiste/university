#include "set.h"
#include "unset.h"

/*
  ajoute la paire var/val à la liste de variables, si la clé
  var existait déjà, la supprime avant l'ajout
*/
short set(char * var, char * val){
  unset(var);
  return !add_to_vars(strdup(var), string_without_quotes(val)); // string_without_quotes fait un malloc
}

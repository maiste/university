#include "alias.h"

/*
  - si arg = NULL affiche tout les alias
  - sinon, découpe arg en deux string key/value avec pour délimiteur
  le symbol "=" et ajoute key/value à la liste d'alias
*/
short alias (char * arg) {
  if (arg == NULL) {
    print_aliases();
  }
  else {
    char * key, * value;
    key = strtok(strdup(arg), "=");
    value = strtok(NULL, "=");
    add_to_aliases(key, string_without_quotes(value));
  }
  return 0;
}

#include "export.h"

extern char ** environ;

/*
    - si arg = NULL affiche toute les variables d'environement
    - sinon, parse arg en un couple clé/valeur avec délimiteur "="
    et ajoute le couple à l'environement, si il n'y à rien après
    le "=" alors on cherche à exporter une variable, on la récupère
    donc dans la liste de variables et l'ajoute à l'environement
*/
short export(char * arg) {
    if (arg == NULL) {
      char ** env = environ;
      while (*env) {
          printf("%s\n", *env);
          env++;
      }
      return 0;
    }
    else {
        char * key, * value;
        key = strtok(strdup(arg), "=");
        value = strtok(NULL, "=");
        if (value == NULL) { // on veut export une variable
            value = get_var_value(key);
            if (value == NULL) return 0; // la variable n'existe pas
            else {
                setenv(key,value, 1);
                return 0;
            }
        }
        else { // on veut export l'argument
            setenv(key,value, 1);
        }
        return 0;
    }
}

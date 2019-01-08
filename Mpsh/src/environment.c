#include "environment.h"
#include "env_list.h"

/*
    ********************************
    FONCTIONS "PUBLIQUES"
    ********************************
*/

/*
    initialise l'environnement
 */
void init_env () {
    create_env_list();
}

/*
    cherche l'alias passée en paramètre
    dans les alias
    @return
        - NULL si aucun resultat
        - la valeure à name associée sinon
 */
char * get_alias_value (char * name) {
    return get_list_elem(name, ALIAS);
}

/*
    cherche la variable passée en paramètre
    dans les variables
    @return
        - NULL si aucun resultat
        - la valeure à name associée sinon
 */
char * get_var_value (char * name) {
    return get_list_elem(name, VARS);
}

/*
    ajoute la valeure name = value aux alias
    si elle n'existe pas déjà, sinon la met à jour
    @return 1 si réussi, 0 sinon
 */
short add_to_aliases (char * name, char * value) {
    remove_to_aliases(name);
    return add(name, value, ALIAS);
}

/*
    ajoute la valeure name = value aux variables
    si elle n'existe pas déjà, sinon la met à jour
    @return 1 si réussi, 0 sinon
 */
short add_to_vars (char * name, char * value) {
    remove_to_vars(name);
    return add(name, value, VARS);
}

/*
    supprime la valeure name = value des alias
    si elle existe
    @return 1 si l'element existait, 0 sinon
 */
short remove_to_aliases (char * name) {
    return remove_key(name, ALIAS);
}

/*
    supprime la valeure name = value des variables
    si elle existe
    @return 1 si l'element existait, 0 sinon
 */
short remove_to_vars (char * name) {
    return remove_key(name, VARS);
}

/* affiche la liste d'alias */
void print_aliases () {
    print_list(ALIAS);
}

/* affiche la liste de variables */
void print_vars () {
    print_list(VARS);
}


/*
    supprime tout les " en début et fin de la chaine s
    retourne le resultat
*/
char * string_without_quotes(char * s) {
    int nb_quotes = 0;
    for (unsigned int i = 0; i < strlen(s); i++) {
        if (s[i] == '"') nb_quotes++;
    }
    char * res = malloc(sizeof(char) * (strlen(s) - nb_quotes+1));
    int j = 0;
    for (unsigned int i = 0; i < strlen(s); i++) {
        if (s[i] != '"') {
            res[j] = s[i];
            j++;
        }
    }
    res[j] = '\0';
    return res;
}

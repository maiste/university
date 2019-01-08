#include "env_list.h"

// prototypes fonctions "privées"
node * newNode(char *, char *);
int get_size(int);
list * get_associated_list(int);

static list * aliasesList = NULL;
static list * varsList = NULL;

/*
    ********************************
    FONCTIONS "PUBLIQUES"
    ********************************
*/

/*
    crée l'environement et les alias
    @return 1 si les listes sont crées, arrête le programme sinon
*/
short create_env_list() {
    aliasesList = malloc(sizeof(aliasesList));
    varsList = malloc(sizeof(varsList));
    if (aliasesList == NULL || varsList == NULL)
        exit(1);
    aliasesList -> first = NULL;
    varsList -> first = NULL;
    return 1;
}

/*
    ajouté le noeud [key, value] à la liste to
    si to = ENV ajoute a env, si to = ALIAS ajoute à aliases
    @return 1 si le noeud à pu être ajouté, 0 sinon
*/
short add(char * key, char * value, int to) {
    list * list = get_associated_list(to);
    node * oldFirst = list -> first;
    node * newFirst = newNode(key, value);
    list -> first = newFirst;
    newFirst -> next = oldFirst;
    return 1;
}

/*
    supprime le noeud de clé key de la liste to
    si to = ENV supprime de env, si to = ALIAS supprime de aliases
    @return
            - 1 si le noeud à pu être supprimé (si il existait)
            - 0 si le noeud n'existait pas
 */
short remove_key(char * key, int to) {
    if (key == NULL)
        return 0;
    list * list = get_associated_list(to);
    node * current = list -> first;
    node * pred = current;
    while(current != NULL) {
        if (strcmp(current -> name, key) == 0) {
            pred -> next = current -> next;
            if (current == list -> first)
                list -> first = current -> next;
            free(current -> name);
            free(current -> value);
            free(current);
            return 1;
        }
        pred = current;
        current = current -> next;
    }
    return 0;
}

/*
    return le noeud de nom name de la liste to
    si to = ENV supprime de env, si to = ALIAS supprime de aliases
    @return
        - la valeure du noeud si il est dans la liste
        - NULL sinon
 */
char * get_list_elem(char * name, int to) {
    if (name == NULL) 
        return NULL;
    list * list = get_associated_list(to);
    node * current = list -> first;
    while(current != NULL) {
        if (strcmp(current -> name, name) == 0)
            return current -> value;
        current = current -> next;
    }
    return NULL;
}

/*
    ********************************
    FONCTIONS "PRIVEES"
    ********************************
*/

/*
    return la liste correspondate à la constante to
    (to peut valoir ENV, ALIAS ou VARS)
*/
list * get_associated_list(int to) {
    return (to == ALIAS)?aliasesList:varsList;
}

/*
    crée un nouveau noeud de valeur [key, value]
    @return l'adresse du noeud
 */
node * newNode(char * name, char * value) {
    assert(name != NULL && value != NULL);
    node * res = malloc(sizeof(node));
    if(res == NULL)
      return NULL;
    res -> name = name;
    res -> value = value;
    res -> next = NULL;
    return res;
}

/*
    retourne la taille de la liste to
*/
int get_size(int to) {
    list * list = get_associated_list(to);
    node * current = list -> first;
    int res = 0;
    while(current != NULL) {
        res++;
        current = current -> next;
    }
    return res;
}

/*
    Affiche l'intégralitée de la liste to
*/
void print_list(int to) {
    list * list = get_associated_list(to);
    node * current = list -> first;
    while(current != NULL) {
        printf("%s=%s\n", current -> name, current -> value);
        current = current -> next;
    }
}

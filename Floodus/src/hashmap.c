/**
 * @file hashmap.c Fichier source d'une Hashmap en C.
 * @author Floodus
 * @brief Implémentation d'une hashmap
 * 
 */

#include "hashmap.h"

/**
 * @brief Fonction de Hachage
 * 
 * @param key clé à hacher
 * @return ssize_t résulat de la fonction de hachage. Il peut être négatif si la fonction de hachage n'a pas pu aboutir.
 */
static ssize_t hash(data_t *key)
{
    if (key == NULL)
        return -1;
    u_int16_t res = 0;
    size_t len = key->iov_len;
    if (len == 0)
        return 0;
    size_t nb_block = (len & 1) ? len / 2 + 1 : len / 2;
    u_int16_t *block = malloc(nb_block * sizeof(u_int16_t));
    if (block == NULL)
        return -1;
    memset(block, 0, nb_block * sizeof(u_int16_t));
    for (size_t i = 0; i < nb_block; i++)
    {
        size_t taille = (len > 1) ? 2 : len;
        len -= taille;
        block[i] = (1UL << 15) - 1;
        block[i] <<= 8; // ajout de 8 bits à 1 au debut du block
        memmove(&block[i], key->iov_base + (i * 2), taille);
    }
    res = block[0];
    for (size_t i = 0; i < nb_block; i++)
    {
        res ^= block[i];
        if (i < nb_block - 1)
            res += block[i + 1];
    }
    free(block);
    return res % HASHMAP_SIZE; // suppression des bits pour que res soit borné entre 0 et HASHMAP_SIZE
}

/**
 * @brief Création d'une hashmap.
 * 
 * @return hashmap_t* Hashmap créée.
 */
hashmap_t *init_map(void)
{
    hashmap_t *map = malloc(sizeof(hashmap_t));
    if (map == NULL)
    {
        debug(D_HASHMAP, 1, "init-map", "malloc renvoie un NULL");
        return NULL;
    }
    memset(map, 0, sizeof(hashmap_t));
    debug(D_HASHMAP, 0, "init-map", "création d'une hashmap");
    return map;
}

/**
 * @brief On récupère la valeur associée à 'key'
 * 
 * @param key clé de la valeur recherchée
 * @param value data qu'on remplie avec la valeur contenu dans la hashmap
 * @param map hashmap dans lequel on cherche.
 * @return bool_t Remplie 'value' avec la valeur trouvée et renvoie '1', '0' si la key n'existe pas.
 */
bool_t get_map(data_t *key, data_t *value, hashmap_t *map)
{
    if (key == NULL || map == NULL)
    {
        if (key == NULL)
            debug(D_HASHMAP, 1, "get_map", "key est NULL");
        if (map == NULL)
            debug(D_HASHMAP, 1, "get_map", "map est NULL");
        return false;
    }
    ssize_t ind = hash(key);
    if (ind < 0)
    {
        debug(D_HASHMAP, 1, "get_map", "problème de hash");
        return false;
    }
    node_t *p = map->content[ind];
    while (p != NULL && compare_iovec(key, p->key) != 0)
        p = p->next;
    if (p == NULL)
    {
        debug(D_HASHMAP, 1, "get_map", "node inexistante");
        return false;
    }
    if (p->value->iov_len != value->iov_len)
    {
        debug(D_HASHMAP, 1, "get_map", "taille non correspondante");
        return false;
    }
    memmove(value->iov_base, p->value->iov_base, value->iov_len);
    debug(D_HASHMAP, 0, "get_map", "renvoie de la valeur");
    return true;
}

/**
 * @brief change la valeur de key par value. Si key n'existe pas, on crée la valeur.
 * 
 * @param key clé
 * @param value valeur
 * @param map hashmap dans lequel on fait l'opération.
 * @return char Renvoie 1 si la modification/ajout s'est bien fait, 0 sinon.
 */
bool_t insert_map(data_t *key, data_t *value, hashmap_t *map)
{
    if (key == NULL || map == NULL)
    {
        if (key == NULL)
            debug(D_HASHMAP, 1, "insert_map", "key est NULL");
        if (map == NULL)
            debug(D_HASHMAP, 1, "insert_map", "map est NULL");
        return false;
    }
    // fin verification des arguments

    data_t *new_key = copy_iovec(key);
    if (new_key == NULL)
    {
        debug(D_HASHMAP, 1, "insert_map", "copie de la key impossible");
        return false;
    }
    data_t *new_value = copy_iovec(value);
    if (new_value == NULL)
    {
        freeiovec(new_key);
        debug(D_HASHMAP, 1, "insert_map", "copie de la value impossible");
        return false;
    }
    // fin initialisation

    ssize_t ind = hash(key);
    if (ind < 0)
    {
        debug(D_HASHMAP, 1, "insert_map", "problème de hash");
        return false;
    }
    node_t *p = map->content[ind];
    node_t *r = p;
    while (p != NULL && compare_iovec(key, p->key) != 0)
    {
        r = p;
        p = p->next;
    }

    // si la node existe deja
    if (p != NULL)
    {
        freeiovec(p->value);
        p->value = new_value;
        freeiovec(new_key);
        debug(D_HASHMAP, 0, "insert_map", "changement de la valeur de la node dans map");
        return true;
    }

    // si la node n'existe pas
    p = malloc(sizeof(node_t));
    if (p == NULL)
    {
        freeiovec(new_key);
        freeiovec(new_value);
        debug(D_HASHMAP, 1, "insert_map", "problème dans la création de la node");
        return false;
    }
    p->key = new_key;
    p->value = new_value;
    p->next = NULL;
    if (r == NULL)
        map->content[ind] = p;
    else
        r->next = p;
    map->size++;
    debug(D_HASHMAP, 0, "insert_map", "création d'une node key/value dans map");
    return true;
}

/**
 * @brief On fait une copie en profondeur de la node donnée en paramètre (copie du next récursivement)
 * 
 * @param node node à copier
 * @return node_t* copie de la node donnée en argument
 */
node_t *deep_copy_node(node_t *node)
{
    node_t *res = NULL;
    node_t *tmp = res;
    while (node != NULL)
    {
        node_t *copy = malloc(sizeof(node_t));
        if (copy == NULL)
        {
            debug(D_HASHMAP, 1, "deep_copy_node", "malloc de copy impossible");
            freedeepnode(res);
            return NULL;
        }
        data_t *key = copy_iovec(node->key);
        if (key == NULL)
        {
            debug(D_HASHMAP, 1, "deep_copy_node", "copy de key impossible");
            free(copy);
            freedeepnode(res);
            return NULL;
        }
        data_t *val = copy_iovec(node->value);
        if (val == NULL)
        {
            debug(D_HASHMAP, 1, "deep_copy_node", "copy de value impossible");
            free(copy);
            freeiovec(key);
            freedeepnode(res);
            return NULL;
        }
        copy->key = key;
        copy->value = val;
        copy->next = NULL;
        if (res == NULL)
            res = copy;
        else
            tmp->next = copy;
        tmp = copy;
        node = node->next;
    }
    debug(D_HASHMAP, 0, "deep_copy_node", "renvoie de la copie");
    return res;
}

/**
 * @brief On renvoie une liste sous forme de liste chainée de node de tous les éléments
 * de la hashmap. Se référer à la structure de node pour les différents champs.
 * 
 * @param map Hashmap dont on veut la liste des éléments.
 * @return node_t* Liste simplement chainée des éléments de la hashmap.
 */
node_t *map_to_list(hashmap_t *map)
{
    node_t *res = NULL;
    node_t *tmp = res;
    for (size_t i = 0; i < HASHMAP_SIZE; i++)
    {
        node_t *el = map->content[i];
        if (el != NULL)
        {
            node_t *copy = deep_copy_node(el);
            if (copy == NULL)
            {
                debug(D_HASHMAP, 1, "map_to_list", "echec de deep_copy_node");
                freedeepnode(res);
                return NULL;
            }
            else
            {
                if (res == NULL)
                {
                    res = copy;
                    tmp = res;
                }
                else
                {
                    while (tmp->next != NULL)
                        tmp = tmp->next;
                    tmp->next = copy;
                }
            }
        }
    }
    if (res == NULL)
    {
        debug(D_HASHMAP, 0, "map_to_list", "map est vide");
    }
    debug(D_HASHMAP, 0, "map_to_list", "renvoie de la liste de map");
    return res;
}

/**
 * @brief On cherche l'existance de la clé dans le hashmap.
 * 
 * @param key clé
 * @param map hashmap dans lequel on fait la recherche.
 * @return renvoie 1 si la key existe dans map, 0 sinon.
 */
bool_t contains_map(data_t *key, hashmap_t *map)
{
    if (key == NULL || map == NULL)
    {
        if (key == NULL)
            debug(D_HASHMAP, 1, "contains_map", "key est NULL");
        if (map == NULL)
            debug(D_HASHMAP, 1, "contains_map", "map est NULL");
        return false;
    }
    // fin verification des arguments

    ssize_t ind = hash(key);
    if (ind < 0)
    {
        debug(D_HASHMAP, 1, "contains_map", "problème de hash");
        return false;
    }
    node_t *p = map->content[ind];
    while (p != NULL && compare_iovec(key, p->key) != 0)
        p = p->next;
    debug_int(D_HASHMAP, 0, "contains_map -> résultat", p != NULL);
    return p != NULL;
}

/**
 * @brief On supprime 'key' de 'map'.
 * 
 * @param key clé à supprimer
 * @param map hashmap dans lequel on fait l'opération
 * @return  renvoie 1 si la key a bien été supprimer de map, 0 sinon.
 */
bool_t remove_map(data_t *key, hashmap_t *map)
{
    if (key == NULL || map == NULL)
    {
        if (key == NULL)
            debug(D_HASHMAP, 1, "remove_map", "key est NULL");
        if (map == NULL)
            debug(D_HASHMAP, 1, "remove_map", "map est NULL");
        return false;
    }
    // fin verification des arguments

    ssize_t ind = hash(key);
    if (ind < 0)
    {
        debug(D_HASHMAP, 1, "remove_map", "problème de hash");
        return false;
    }
    node_t *p = map->content[ind];
    node_t *r = p;
    while (p != NULL && compare_iovec(key, p->key) != 0)
    {
        r = p;
        p = p->next;
    }
    if (p == NULL)
    {
        debug(D_HASHMAP, 1, "remove_map", "map ne contient pas la key");
        return false;
    }
    if (p == r)
        map->content[ind] = p->next;
    else
        r->next = p->next;
    freeiovec(p->key);
    freeiovec(p->value);
    free(p);
    map->size--;
    debug(D_HASHMAP, 0, "remove_map", "node enlevée");
    return true;
}

/**
 * @brief On récupère le nombre de clé stockés dans 'map'.
 * 
 * @param map hashmap dont on veut savoir la taille.
 * @return size_t renvoie le nombre d'éléments contenus dans map.
 */
size_t get_size_map(hashmap_t *map)
{
    if (map == NULL)
    {
        debug(D_HASHMAP, 1, "get_size_map", "argument NULL");
        return 0;
    }
    debug_int(D_HASHMAP, 0, "get_size_map -> resultat", map->size);
    return map->size;
}

/**
 * @brief On vérifie si 'map' est vide (ne contient aucun élément).
 * 
 * @param map hashmap
 * @return renvoie 1 si map ne contient aucun élément, 0 sinon.
 */
bool_t empty_map(hashmap_t *map)
{
    uint8_t res = 0 == get_size_map(map);
    debug_int(D_HASHMAP, 0, "empty_map -> resultat", res);
    return res;
}

/**
 * @brief vide map de tous ses éléments.
 * 
 * @param map hashmap
 */
void clear_map(hashmap_t *map)
{
    if (map == NULL)
    {
        debug(D_HASHMAP, 1, "clear_map", "argument NULL");
        return;
    }
    node_t **content = map->content;
    for (size_t i = 0; i < HASHMAP_SIZE; i++)
    {
        freedeepnode(content[i]);
        content[i] = NULL;
    }
    debug(D_HASHMAP, 0, "clear_map", "mémoire libérée du contenu du map");
}

/**
 * @brief On libère la mémoire de map et de son contenu.
 * 
 * @param map hashmap
 */
void freehashmap(hashmap_t *map)
{
    clear_map(map);
    free(map);
    debug(D_HASHMAP, 0, "freehashmap", "free map");
}

/**
 * @brief On libère en profondeur la mémoire d'une node, 
 * c'est à dire qu'on free aussi les nodes que la suive (champs next)
 * 
 * @param node node à free
 */
void freedeepnode(node_t *node)
{
    node_t *p = node;
    while (p != NULL)
    {
        node = p;
        p = p->next;
        freeiovec(node->key);
        freeiovec(node->value);
        free(node);
    }
    debug(D_HASHMAP, 0, "freedeepnode", "free node");
}

/**
 * @brief Affichage du contenu de la hashmap donnée en argument
 * 
 * @param map hashmap
 */
void print_hashmap(hashmap_t *map)
{
    size_t node = 0;
    if (map == NULL)
    {
        printf("(null)\n");
        return;
    }
    printf("size : %ld\n", map->size);
    node_t **content = map->content;
    for (size_t i = 0; i < HASHMAP_SIZE; i++)
    {
        node_t *p = content[i];
        while (p != NULL)
        {
            node++;
            printf("Node numero %ld\n", node);
            printf("Key : \n");
            print_iovec(p->key);
            printf("Value : \n");
            print_iovec(p->value);
            p = p->next;
        }
    }
}

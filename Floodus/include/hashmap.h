#ifndef _HASHMAP_H
#define _HASHMAP_H

#include <sys/uio.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <sys/types.h>

#include "iovec.h"
#include "debug.h"

#define D_HASHMAP 0
#define HASHMAP_SIZE 4096

/**
 * @brief Structure correspondant à une node la hashmap.
 * Le champs 'key' correspond à l'identifiant de la node.
 * Le champs 'value' correspond à la valeur de la node.
 * Le champs 'next' correspond à la node qui suit (liste simplement chainée de node)
 */
typedef struct node_t
{
    data_t *key;
    data_t *value;
    struct node_t *next;
} node_t;

/**
 * @brief Structure correspondant à une hashmap.
 * Le champs 'size' correspond au nombre d'éléments contenus dans la hashmap.
 * Le champs 'content' correspond au contenu de la hashmap.
 */
typedef struct hashmap_t
{
    size_t size;
    node_t *content[HASHMAP_SIZE];
} hashmap_t;

hashmap_t *init_map(void);
bool_t get_map(data_t *key, data_t *value, hashmap_t *map);
bool_t insert_map(data_t *key, data_t *value, hashmap_t *map);
node_t *deep_copy_node(node_t *node);
node_t *map_to_list(hashmap_t *map);
bool_t contains_map(data_t *key, hashmap_t *map);
bool_t remove_map(data_t *key, hashmap_t *map);
size_t get_size_map(hashmap_t *map);
bool_t empty_map(hashmap_t *map);
void clear_map(hashmap_t *map);
void freehashmap(hashmap_t *map);
void freedeepnode(node_t *node);
void print_hashmap(hashmap_t *map);

#endif

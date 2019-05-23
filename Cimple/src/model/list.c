#include "list.h"


/**
 * Init new node with value
 *
 * @param value string to add
 * @return NULL if it failed else new node
 */
node *init_node(char *value){
	node *head = malloc(sizeof(node));
	if (head == NULL)
		return NULL;
	head->value = malloc(strlen(value) + 1);
	if (head->value == NULL) {
		free(head);
		return NULL;
	}
	memcpy(head->value, value, strlen(value) + 1);
	head->next = NULL;
	return head;
}

/**
 * Add new node to the head of the list
 *
 * @param list where head is inserted in
 * @param value new value to insert
 * @return NULL if it failed, new list else
 */
node *insert_head(node *list, char *value){
	if (value == NULL)
		return NULL;
	node *new_node = init_node(value);
	if (new_node == NULL) return NULL;
	new_node->next = list;
	return new_node;
}

/**
 * Free list
 *
 * @param list list to free
 */
void free_all(node *list){
	node *current = list;
	node *tmp;
	while (current != NULL) {
		tmp = current;
		current = current->next;
		if (tmp->value != NULL)
			free(tmp->value);
		free(tmp);
	}
}

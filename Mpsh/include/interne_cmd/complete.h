#include "parsing_struct.h"

// Ne contient que des pre_cmd où le nom est la commande concernée et les arguments, la liste des suffixes tolérés.
node * complete_db;

void init_complete_db();

short add_completion(char ** args, int nb_args);

void free_complete_db();

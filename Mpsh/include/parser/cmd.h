/* Structure to manage cmd after parsing */

#ifndef CMD_H
#define CMD_H

#define MAX_STR_LEN 256

#include "arith.h"
#include "parsing_struct.h"

// Variable utilisée pour le résultat du parsing
node * parseres;

pre_cmd * mkpre_cmd(char * name, node * nd);
logics * mkleaflogics(node * list,short is_bg);
logics * mkreallogics(enum logical l, logics * left, logics * right);
node * mkforl(char * var, arith * from, arith * to, node * nd);

piped_cmd * compute_words_in_piped(node * p); // Node as in logics
#endif

#ifndef STRUCT_UTILS_H
#define STRUCT_UTILS_H

void print_cmd(cmd * cm);

void free_node(node * n);
void free_cmd(cmd * cm);
void free_piped(piped_cmd * piped);
void free_redir(redir * r);
void free_logics(logics * l);
void free_whilel(whilel * w);
#endif

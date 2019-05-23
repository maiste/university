#ifndef PARSE_H
#define PARSE_H
#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <unistd.h>
#include <string.h>
#include <ctype.h>
#include <errno.h>
#include "parse_token.h"
#include "err_flags.h"

#define LEN_MAX         13
#define LEN_INFO        24
#define LEN_BNW         3
#define LEN_COPY        3
#define LEN_CONSTRAST   4
#define LEN_CUT         3
#define LEN_FILL        7
#define LEN_HELP        2
#define LEN_LOAD        5
#define LEN_LIST_BUFFER 2
#define LEN_MOVE_BUFFER 3
#define LEN_LIGHT       4
#define LEN_QUIT        4
#define LEN_PASTE       3
#define LEN_RESIZE      5
#define LEN_ROTATE      4
#define LEN_TRUNCATE    7
#define LEN_SAVE        4
#define LEN_SWITCH      3
#define LEN_GREYS       3
#define LEN_NEG         3
#define LEN_REPLACE     13
#define LEN_SYM         3
#define LEN_SCRIPT		3
#define LEN_BUNDLE		4

typedef struct cmd{
	char * name;
	char ** args;
	int size;
}cmd;


cmd* parse_line(char * cmd_line);
short check_arguments(cmd * command);
void free_cmd(cmd * cmd_struct);
#endif
#ifndef _VIEW_H
#define _VIEW_H

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <curses.h>
#include <sys/types.h>
#include <ctype.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <ifaddrs.h>
#include <limits.h>
#include <netdb.h>

#define D_VIEW 1

#define BUF_LEN USHRT_MAX
#define MAX_SURNAME 30

#define COL 0
#define RED_COL 1
#define GREEN_COL 2
#define BLUE_COL 3

WINDOW *get_panel(void);
void set_in_red(void);
void set_in_green(void);
void set_in_blue(void);
void restore(void);

void print_data(u_int8_t *content, int content_len);
int handle_input(void);

void init_graph(void);
void end_graph(void);

#endif

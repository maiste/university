#ifndef _MAKE_DEMAND_H
#define _MAKE_DEMAND_H

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <netdb.h>
#include <errno.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <arpa/inet.h>


#include "TLV.h"
#include "debug.h"
#include "voisin.h"
#include "writer.h"
#include "controller.h"

#define D_MAKE 1

int send_hello(char *dest, char *port);
int try_connect_pair(char *content);

#endif //_MAKE_DEMAND_H

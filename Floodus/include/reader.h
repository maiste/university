#ifndef _READER_H
#define _READER_H

#include <sys/types.h>
#include <sys/socket.h>

#include "writer.h"
#include "iovec.h"
#include "voisin.h"
#include "controller.h"
#include "hashmap.h"

#define D_READER 1

#define NB_TLV 8
#define READBUF 4096
#define RDHDRLEN 4

extern hashmap_t *g_ancillary;

bool_t init_ancillary(void);
void free_ancillary(void);
ssize_t read_msg(void);

#endif

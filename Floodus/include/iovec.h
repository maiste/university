#ifndef _IOVEC_H
#define _IOVEC_H

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/uio.h>

#include "debug.h"

#define D_IOVEC 0

typedef struct iovec data_t;

void freeiovec(data_t *data);
data_t  *create_iovec(void *content, size_t content_len);
data_t *copy_iovec(data_t *data);
int compare_iovec(data_t *data1, data_t *data2);
void print_iovec(data_t *data);

#endif

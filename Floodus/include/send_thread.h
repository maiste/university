#ifndef H_SEND_THREAD
#define H_SEND_THREAD

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <pthread.h>

#include "debug.h"
#include "TLV.h"
#include "writer.h"
#include "voisin.h"

#define D_SEND_THREAD 1
#define MIN 8
#define SLEEP_NEIGHBOURS 60
#define SLEEP_HELLO 30

bool_t init_sender(void);
void destroy_thread(void);

#endif // H_SEND_THREAD

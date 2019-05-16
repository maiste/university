#ifndef PTHREAD_VAR_H
#define PTHREAD_VAR_H

#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <string.h>

#include "debug.h"
#include "hashmap.h"

#define D_PTHREAD 1

typedef struct pthread_var_t {
  pthread_mutex_t locker; // Mutex pour locker les threads
  hashmap_t *content; // La variable contenue
} pthread_var_t;

bool_t lock(pthread_var_t *g_lock);
bool_t unlock(pthread_var_t *g_lock);

#endif // PTHREAD_VAR_H

#ifndef H_DEBUG
#define H_DEBUG

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <fcntl.h>
#include <unistd.h>

#include "view.h"

#ifndef D_FLAG
#define DEBUG 0
#else
#define DEBUG 1
#endif // D_FLAG

#define false 0
#define true 1
#define FILE_LOG "debug_floodus.log"

typedef u_int8_t bool_t;

bool_t log_load(int id);
void log_close(int id);
void debug(uint8_t flag, uint8_t error, char *name, const char *msg);
void debug_and_exit(uint8_t flag, uint8_t error, char *name, const char *msg, int exit_code);
void debug_hex(uint8_t flag, uint8_t error, char *name, void *data, int data_len);
void debug_hex_and_exit(uint8_t flag, uint8_t error, char *name, void *data, int data_len, int exit_code);
void debug_int(uint8_t flag, uint8_t error, char *name, int rc);
void debug_int_and_exit(uint8_t flag, uint8_t error, char *name, int rc, int exit_code);

#endif

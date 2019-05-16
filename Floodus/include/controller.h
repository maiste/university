#ifndef _CONTROLLER_H
#define _CONTROLLER_H

#include <sys/select.h>
#include <sys/time.h>
#include <sys/types.h>
#include <fcntl.h>
#include <unistd.h>

#include "inondation.h"
#include "reader.h"
#include "writer.h"
#include "view.h"

#define D_CONTROL 1

#define MAX_NETWRK_LOOP 5

extern u_int32_t g_socket;

int create_socket(uint16_t port);
void launch_program(void);
void stop_program(void);
void close_sock(void);

#endif

#ifndef TLV_H
#define TLV_H

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/uio.h>
#include <string.h>

#include "debug.h"
#include "iovec.h"

#define D_TLV 1
#define IPV6_LEN 16

bool_t pad1(data_t *);
bool_t pad_n(data_t *, uint8_t len);
bool_t hello_short(data_t *, uint64_t src_id);
bool_t hello_long(data_t *, uint64_t src_id, uint64_t dest_id);
bool_t neighbour(data_t *, uint8_t src_ip[IPV6_LEN], uint16_t port);
bool_t data(data_t *, uint64_t dest_id, uint32_t nonce, uint8_t type, uint8_t *msg, uint8_t msg_len);
bool_t ack(data_t *, uint64_t dest_id, uint32_t nonce);
bool_t go_away(data_t *, uint8_t code, uint8_t *msg, uint8_t msg_len);
bool_t warning(data_t *, uint8_t *msg, uint8_t msg_len);

#endif

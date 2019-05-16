#ifndef _WRITER_H
#define _WRITER_H

#include <limits.h>

#include "hashmap.h"
#include "voisin.h"
#include "controller.h"

#define D_WRITER 1

#define MAX_PER_TLV 2800

void free_writer(void);
bool_t send_tlv(ip_port_t dest, data_t *tlvs, size_t tlvs_len);
bool_t add_tlv(ip_port_t dest, data_t *tlv);
bool_t buffer_is_empty(void);
bool_t send_buffer_tlv(void);

#endif

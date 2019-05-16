#ifndef _INONDATION_H
#define _INONDATION_H

#include <time.h>
#include <math.h>

#include "debug.h"
#include "iovec.h"
#include "TLV.h"
#include "hashmap.h"
#include "voisin.h"
#include "data.h"

#define D_INOND 1
#define COUNT_INOND 5

typedef struct message_t
{
    struct timespec send_time; // temps absolu à partir duquel on peut envoyer le message
    u_int8_t count;            // compteur du nombre d'envoi effectué
    u_int8_t type;             // type du message
    data_t *content;           // contenu du message
    u_int64_t id;              // id de celui qui a envoyé le message
    u_int32_t nonce;           // nounce rendant le message unique
    hashmap_t *recipient;      // ensemble de ceux à qui on doit envoyer le message sous forme (ip_port_t, ip_port_t)
    struct message_t *next;    // message suivant dans la liste
} message_t;

void free_inondation(void);
int compare_time(struct timespec ta, struct timespec tb);
bool_t add_message(ip_port_t dest, u_int64_t id, uint32_t nonce, uint8_t type, data_t *content);
bool_t get_nexttime(struct timespec *tm);
bool_t launch_flood(void);
bool_t apply_tlv_data(ip_port_t dest, data_t *data, size_t *head_read);
bool_t apply_tlv_ack(ip_port_t dest, data_t *data, size_t *head_read);

#endif

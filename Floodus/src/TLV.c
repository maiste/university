/**
 * @file TLV.c
 * @author Floodus
 * @brief Module s'occupant de tout ce qui est constructon de TLV
 * 
 */

#include "TLV.h"

/* Fonction affichage erreur */
static void error(char *obj)
{
  debug(D_TLV, 1, obj, "Erreur TLV => can't allocate memory");
}

/**
 * @brief Créer une struct iovec pour PAD1
 * 
 * @param pad struct iovec à remplir
 * @return bool_t '1' si le remplissage s'est bien passé, '0' sinon.
 */
bool_t pad1(data_t *pad)
{
  if (pad == NULL)
  {
    error("pad1 -> NULL argument");
    return false;
  }
  uint8_t size = 1;
  uint8_t *content = malloc(sizeof(uint8_t) * size);
  if (content == NULL)
  {
    error("pad1 -> content");
    return false;
  }
  memset(content, 0, sizeof(uint8_t) * size);
  pad->iov_base = content;
  pad->iov_len = size;
  debug_hex(D_TLV, 0, "pad1 contruction TLV", pad->iov_base, pad->iov_len);
  return true;
}

/**
 * @brief Créer une struct iovec pour PADN
 *
 * @param pad struct iovec à remplir
 * @param len taille du champs MBZ dans le PADN
 * @return bool_t '1' si le remplissage s'est bien passé, '0' sinon.
 */
bool_t pad_n(data_t *pad, uint8_t len)
{
  if (pad == NULL)
  {
    error("pad_n -> NULL argument");
    return false;
  }
  uint8_t size = len + 2;
  uint8_t *content = malloc(sizeof(uint8_t) * size);
  if (content == NULL)
  {
    error("pad_n -> content");
    return false;
  }
  memset(content, 0, sizeof(uint8_t) * size);
  content[0] = 1;
  content[1] = len;
  pad->iov_base = content;
  pad->iov_len = size;
  debug_hex(D_TLV, 0, "pad_n contruction TLV", pad->iov_base, pad->iov_len);
  return true;
}

/**
 * @brief Construit un TLV Hello Court
 *
 * @param hello struct iovec à remplir
 * @param src_id l'id de 64 bits de l'émetteur 
 * @return bool_t '1' si le remplissage s'est bien passé, '0' sinon.
 */
bool_t hello_short(data_t *hello, uint64_t src_id)
{
  if (hello == NULL)
  {
    error("hello_short -> NULL argument");
    return false;
  }
  uint8_t size = 10;
  uint8_t *content = malloc(sizeof(uint8_t) * size);
  if (content == NULL)
  {
    error("hello_short -> content");
    return false;
  }
  memset(content, 0, sizeof(uint8_t) * size);
  content[0] = 2;
  content[1] = 8;
  memmove(content + 2, &src_id, sizeof(uint64_t));
  hello->iov_base = content;
  hello->iov_len = size;
  debug_hex(D_TLV, 0, "hello_short contruction TLV", hello->iov_base, hello->iov_len);
  return true;
}

/**
 * @brief Construit un TLV Hello Long
 *
 * @param hello struct iovec à remplir
 * @param src_id l'id de 64 bits de l'émetteur
 * @param dest_id l'id de 64 bits du destinataire
 * @return bool_t '1' si le remplissage s'est bien passé, '0' sinon.
 */
bool_t hello_long(data_t *hello, uint64_t src_id, uint64_t dest_id)
{
  if (hello == NULL)
  {
    error("hello_long -> NULL argument");
    return false;
  }
  uint8_t size = 18;
  uint8_t *content = malloc(sizeof(uint8_t) * size);
  if (content == NULL)
  {
    error("hello_long -> content");
    return false;
  }
  memset(content, 0, sizeof(uint8_t) * size);
  content[0] = 2;
  content[1] = 16;
  memmove(content + 2, &src_id, sizeof(uint64_t));
  memmove(content + 10, &dest_id, sizeof(uint64_t));
  hello->iov_base = content;
  hello->iov_len = size;
  debug_hex(D_TLV, 0, "hello_long contruction TLV", hello->iov_base, hello->iov_len);
  return true;
}

/**
 * @brief Construit un Neighbour
 *
 * @param neighbour_i struct iovec à remplir
 * @param src_ip l'ip à partager
 * @param port le port de l'ip
 * @return bool_t '1' si le remplissage s'est bien passé, '0' sinon.
 */
bool_t neighbour(data_t *neighbour_i, uint8_t src_ip[IPV6_LEN], uint16_t port)
{
  if (neighbour_i == NULL)
  {
    error("iovec neighbour");
    return false;
  }
  uint8_t size = 20;
  uint8_t *content = malloc(sizeof(uint8_t) * size);
  if (content == NULL)
  {
    error("content neighbour");
    return false;
  }
  memset(content, 0, 20 * sizeof(uint8_t));
  content[0] = 3;
  content[1] = 18;
  memmove(content + 2, src_ip, sizeof(uint8_t) * IPV6_LEN);
  memmove(content + 18, &port, sizeof(uint16_t));
  neighbour_i->iov_base = content;
  neighbour_i->iov_len = size;
  debug_hex(D_TLV, 0, "neighbour contruction TLV", neighbour_i->iov_base, neighbour_i->iov_len);
  return true;
}

/**
 * @brief Construit un acquitement pour une data
 *
 * @param data_i struct iovec à remplir
 * @param dest_id l'id de  l'émetteur
 * @param nonce l'apax pour l'identification
 * @param type le type de message (0)
 * @param msg le message à envoyer
 * @param msg_len la taille du message, prend le min avec 242
 * @return bool_t '1' si le remplissage s'est bien passé, '0' sinon.
 */
bool_t data(data_t *data_i, uint64_t dest_id, uint32_t nonce,
            uint8_t type, uint8_t *msg, uint8_t msg_len)
{
  if (data_i == NULL)
  {
    error("data -> NULL argument");
    return false;
  }
  msg_len = ((242 - msg_len > 0) ? msg_len : 242);
  uint32_t size = 13 + msg_len + 2 /* + 2 pour l'entete */;
  uint8_t *content = malloc(sizeof(uint8_t) * size);
  if (content == NULL)
  {
    error("data -> content");
    return false;
  }
  memset(content, 0, sizeof(uint8_t) * size);
  content[0] = 4;
  content[1] = size - 2;
  memmove(content + 2, &dest_id, sizeof(uint64_t));
  memmove(content + 10, &nonce, sizeof(uint32_t));
  content[14] = type;
  memmove(content + 15, msg, msg_len);
  data_i->iov_base = content;
  data_i->iov_len = size;
  debug_hex(D_TLV, 0, "data contruction TLV", data_i->iov_base, data_i->iov_len);
  return true;
}

/**
 * @brief Construit un acquitement pour une data
 *
 * @param ack_i struct iovec à remplir
 * @param dest_id l'id de l'envoyeur (copie)
 * @param nonce l'apax pour l'acquitement (copie)
 * @return bool_t '1' si le remplissage s'est bien passé, '0' sinon.
 */
bool_t ack(data_t *ack_i, uint64_t dest_id, uint32_t nonce)
{
  if (ack_i == NULL)
  {
    error("ack -> NULL argument");
    return false;
  }
  uint32_t size = 14;
  uint8_t *content = malloc(sizeof(uint8_t) * size);
  if (content == NULL)
  {
    error("ack -> content");
    return false;
  }
  memset(content, 0, sizeof(uint8_t) * size);
  content[0] = 5;
  content[1] = 12;
  memmove(content + 2, &dest_id, sizeof(uint64_t));
  memmove(content + 10, &nonce, sizeof(uint32_t));
  ack_i->iov_base = content;
  ack_i->iov_len = size;
  debug_hex(D_TLV, 0, "ack contruction TLV", ack_i->iov_base, ack_i->iov_len);
  return true;
}

/**
 * @brief Construit un TLV go away
 *
 * @param go_away_i struct iovec à remplir
 * @param code la raison du go away
 * @param msg le message à joindre
 * @param msg_len taille du message, prend le min entre msg_len et 254
 * @return bool_t '1' si le remplissage s'est bien passé, '0' sinon.
 */
bool_t go_away(data_t *go_away_i, uint8_t code, uint8_t *msg, uint8_t msg_len)
{
  if (go_away_i == NULL)
  {
    error("go_away -> NULL argument");
    return false;
  }
  uint8_t size = 3 + ((msg_len < 254) ? msg_len : 254);
  uint8_t *content = malloc(sizeof(uint8_t) * size);
  if (content == NULL)
  {
    error("go_away -> content");
    return false;
  }
  memset(content, 0, sizeof(uint8_t) * size);
  content[0] = 6;
  content[1] = size - 3 + 1 /* + 1 pour le code */;
  content[2] = code;
  memmove(content + 3, msg, size - 3);
  go_away_i->iov_base = content;
  go_away_i->iov_len = size;
  debug_hex(D_TLV, 0, "go_away contruction TLV", go_away_i->iov_base, go_away_i->iov_len);
  return true;
}

/**
 * @brief Construit un TLV de warning
 *
 * @param warning_i struct iovec à remplir
 * @param msg_len taille du message
 * @param msg message
 * @return bool_t '1' si le remplissage s'est bien passé, '0' sinon.
 */
bool_t warning(data_t *warning_i, uint8_t *msg, uint8_t msg_len)
{
  if (warning_i == NULL)
  {
    error("warning -> NULL argument");
    return false;
  }
  uint8_t size = 2 + msg_len;
  uint8_t *content = malloc(sizeof(uint8_t) * size);
  if (content == NULL)
  {
    error("warning -> content");
    return false;
  }
  memset(content, 0, sizeof(uint8_t) * size);
  content[0] = 7;
  content[1] = msg_len;
  memmove(content + 2, msg, msg_len);
  warning_i->iov_base = content;
  warning_i->iov_len = size;
  debug_hex(D_TLV, 0, "warning contruction TLV", warning_i->iov_base, warning_i->iov_len);
  return true;
}

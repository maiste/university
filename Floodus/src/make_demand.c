#include "make_demand.h"

/**
 * @brief Envoie de hello court à un destinataire contenu dans un addrinfo
 * 
 * @param p destinataire
 * @return int boolean disant si tout s'est bien passé
 */
static int make_demand(struct addrinfo *p)
{
  data_t hs = {0};
  if (!hello_short(&hs, g_myid))
  {
    debug(D_MAKE, 1, "make_demand -> new_neighbour", "hs erreur");
    return 0;
  }
  ip_port_t ipport = {0};
  ipport.port = ((struct sockaddr_in6 *)p->ai_addr)->sin6_port;
  memmove(ipport.ipv6, &((struct sockaddr_in6 *)p->ai_addr)->sin6_addr, sizeof(ipport.ipv6));
  int rc = add_tlv(ipport, &hs);
  free(hs.iov_base);
  if (!rc)
  {
    debug(D_MAKE, 1, "make_demand -> add_tlv", "can't send");
    return 0;
  }
  data_t new_neighbour = {0};
  if (!neighbour(&new_neighbour, ipport.ipv6, ipport.port))
  {
    debug(D_MAKE, 1, "make_demand -> new_neighbour", " new = NULL");
    return 0;
  }
  size_t head = 1;
  rc = apply_tlv_neighbour(&new_neighbour, &head);
  free(new_neighbour.iov_base);
  if (rc == false)
    debug(D_MAKE, 1, "make_demand -> apply neighbour", " rc = false");

  return rc;
}

/**
 * @brief On récupère toutes les infos via getaddrinfo sur la destination et le port passés en arguments.
 * 
 * @param dest nom dns de la destination
 * @param port port de la destination
 * @return int '0' si ca s'est bien passé, '-1' sinon.
 */
int send_hello(char *dest, char *port)
{
  struct addrinfo h = {0};
  struct addrinfo *r = {0};
  int rc = 0;
  h.ai_family = AF_INET6;
  h.ai_socktype = SOCK_DGRAM;
  h.ai_flags = AI_V4MAPPED | AI_ALL;
  rc = getaddrinfo(dest, port, &h, &r);
  if (rc < 0)
  {
    debug(D_MAKE, 1, "send_hello -> rc", gai_strerror(rc));
    return -1;
  }
  struct addrinfo *p = r;

  if (p == NULL)
  {
    debug(D_MAKE, 1, "send_hello", "aucune interface détectée pour cette adresse");
    return -1;
  }
  make_demand(p);
  // fin de la demande à la première interface

  freeaddrinfo(r);
  debug(D_MAKE, 0, "send_hello", "demande effectuée pour getaddrinfo");
  return 0;
}

/**
 * @brief 
 * Envoie un hello à un pair proche
 */
int try_connect_pair(char *content)
{
  int i = 0;

  char dest[41] = {0};
  char port[6];

  while (content[i] != '\0' && content[i] != ' ')
    i++;

  if (content[i] == '\0')
    return 0;

  if (i < 40)
  {
    memcpy(dest, content, i);
    content = content + i + 1;
    i = 0;

    while (content[i] != '\0' && content[i] != ' ')
      i++;

    if (i < 6)
    {
      memcpy(port, content, i);
      if (send_hello(dest, port) < 0)
      {
        errno = 0;
        return 0;
      }
    }
  }
  return 1;
}

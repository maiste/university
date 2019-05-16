/**
 * @file send_thread.c Fichier source de send_thread
 * @author Floodus
 * @brief PThread envoyant des Hello et neighbours
 */

#include "send_thread.h"

/**
 * Threads pour hello et voisins
 */
static pthread_t th1 = {0}, th2 = {0};

/**
 * @brief
 * Donne le temps restant nécessaire au sleep 
 * 
 * @param TIME le temps original
 * @param tm le temps auquel le thread se réveille
 * @return le temps à sleep
 */
static uint32_t get_remain_time(uint32_t TIME, struct timespec tm)
{
  struct timespec current_time = {0};
  if (clock_gettime(CLOCK_MONOTONIC, &current_time) < 0)
  {
    debug(D_SEND_THREAD, 1, "get_remain_time", "can't get clockgetime");
    return 0;
  }
  uint32_t res = TIME - (current_time.tv_sec - tm.tv_sec);
  return (res > TIME) ? 0 : res;
}

/**
 * @brief
 * Envoie les voisins à un pair
 * 
 * @param dest l'adresse de l'envoyeur
 * @param n_list la liste des voisins à envoyer
 * @return 1 si l'envoi a marché 
 */
static bool_t send_neighbour(ip_port_t *dest, node_t *n_list)
{
  node_t *node = n_list;
  ip_port_t ipport = {0};
  neighbour_t content = {0};
  int rc = 0;
  bool_t error = 0;
  while (node != NULL)
  {
    memmove(&content, node->value->iov_base, sizeof(neighbour_t));
    if (is_more_than_two(content.hello))
    {
      node = node->next;
      continue;
    }

    memmove(&ipport, node->key->iov_base, sizeof(ip_port_t));
    if (memcmp(dest->ipv6, ipport.ipv6, 16) != 0 ||
        dest->port != ipport.port)
    {
      data_t tlv_neighbour;
      if (!neighbour(&tlv_neighbour, ipport.ipv6, ipport.port))
      {
        debug(D_SEND_THREAD, 1, "send_neighbour", "creation de tlv_neighbour impossible");
        node = node->next;
        error = true;
        continue;
      }
      rc = add_tlv(*dest, &tlv_neighbour);
      free(tlv_neighbour.iov_base);
      if (rc == false)
      {
        debug_int(D_SEND_THREAD, 1, "send_neighbour -> rc", rc);
        node = node->next;
        error = true;
        continue;
      }
    }
    node = node->next;
  }
  return (error) ? false : true;
}

/**
 * @brief
 * Envoie les voisins
 * 
 * @param n_list liste des voisins courants
 * @return 1 si tous les voisins ont été envoyés
 */
static bool_t send_neighbours(node_t *n_list)
{
  int rc = 1;
  bool_t error = false;
  node_t *node = n_list;
  while (node != NULL)
  {
    ip_port_t ipport = {0};
    neighbour_t content = {0};
    memmove(&ipport, node->key->iov_base, sizeof(ip_port_t));
    memmove(&content, node->value->iov_base, sizeof(neighbour_t));
    if (is_more_than_two(content.hello) && !update_neighbours(node, 2, "time out hello"))
    {
      debug(D_SEND_THREAD, 1, "send_neighbour", "error with go_away");
      error = true;
    }
    else
    {
      rc = send_neighbour(&ipport, n_list);
      error = (!rc) ? true : error;
    }
    node = node->next;
  }
  return (error) ? false : true;
}

/**
 * @brief
 * Boucle d'itération du thread, envoie un voisin 
 * suivant un temps aléatoire
 */
static void *neighbour_sender(void *unused)
{
  (void)unused; // Enleve le warning unused
  struct timespec tm = {0};
  if (clock_gettime(CLOCK_MONOTONIC, &tm) < 0)
  {
    debug(D_SEND_THREAD, 1, "neighbour_sender", "can't get clockgetime");
    pthread_exit(NULL);
  }
  while (1)
  {
    uint32_t remains = get_remain_time(SLEEP_NEIGHBOURS, tm);
    sleep(remains);
    if (clock_gettime(CLOCK_MONOTONIC, &tm))
    {
      debug(D_SEND_THREAD, 1, "neighbour_sender", "can't get clockgetime");
      pthread_exit(NULL);
    }
    debug_int(D_SEND_THREAD, 0, "neighbour_sender -> remains", remains);

    lock(&g_neighbours);
    node_t *n_list = map_to_list(g_neighbours.content);
    unlock(&g_neighbours);

    send_neighbours(n_list);
    freedeepnode(n_list);
    debug(D_SEND_THREAD, 0, "neighbour_sender", "Sending neighbour done");
  }
  pthread_exit(NULL);
}

/**
 * @brief
 * Envoie à tous les voisins un TLV Hello Court
 * 
 * @param e_list liste des voisins potentiels
 * @param nb le nombre à qui envoyer
 * @return nombre d'hellos envoyés
 */
static int send_hello_short(node_t *e_list, int nb)
{
  int rc = 0, count = 0;
  node_t *node = e_list;
  while (node != NULL && count < nb)
  {
    ip_port_t ipport = {0};
    memmove(&ipport, node->value->iov_base, sizeof(ip_port_t));

    data_t tlv_hello = {0};
    if (!hello_short(&tlv_hello, g_myid))
    {
      debug(D_SEND_THREAD, 1, "send_hello_short", "tlv_hello = NULL");
      node = node->next;
      continue;
    }

    rc = add_tlv(ipport, &tlv_hello);
    free(tlv_hello.iov_base);
    if (!rc)
    {
      debug_int(D_SEND_THREAD, 1, "send_hello_short -> rc", rc);
      node = node->next;
      continue;
    }
    count++;
    node = node->next;
  }
  debug_int(D_SEND_THREAD, 0, "send_hello_short -> count", count);
  return count;
}

/**
 * @brief
 * Envoie à tous les voisins un TLV Hello Long
 * @param n_list liste des voisins courants
 * @return nombre d'hellos envoyés
 */
static int send_hello_long(node_t *n_list)
{
  int rc = 0, count = 0, no_send = 0, away = 0;
  node_t *node = n_list;
  while (node != NULL)
  {
    ip_port_t ipport = {0};
    neighbour_t content = {0};
    memmove(&content, node->value->iov_base, sizeof(neighbour_t));
    memmove(&ipport, node->key->iov_base, sizeof(ip_port_t));
    if (is_more_than_two(content.hello) && !update_neighbours(node, 2, "time out hello"))
    {
      debug(D_SEND_THREAD, 1, "send_hello_long", "voisin non symétrique");
      away++;
      node = node->next;
      continue;
    }
    data_t tlv_hello = {0};
    if (!hello_long(&tlv_hello, g_myid, content.id))
    {
      debug(D_SEND_THREAD, 1, "send_hello_long", "tlv_hello = NULL");
      no_send++;
      node = node->next;
      continue;
    }

    rc = add_tlv(ipport, &tlv_hello);
    free(tlv_hello.iov_base);
    if (rc == false)
    {
      debug_int(D_SEND_THREAD, 1, "send_hello_long -> rc", rc);
      no_send++;
      node = node->next;
      continue;
    }
    count++;
    node = node->next;
  }
  if (away > 0)
    debug_int(D_SEND_THREAD, 1, "send_hello_long -> go_away envoyé", away);
  if (no_send > 0)
    debug_int(D_SEND_THREAD, 1, "send_hello_long -> non envoyé", no_send);
  debug_int(D_SEND_THREAD, 0, "send_hello_long -> envoyé", count);
  return count;
}

/**
 * @brief
 * Boucle d'itération du thread, envoie un Hello toutes les
 * 30 secondes
 */
static void *hello_sender(void *unused)
{
  (void)unused; // Enleve le warning unused

  int count = 0;
  struct timespec tm = {0};
  if (clock_gettime(CLOCK_MONOTONIC, &tm) < 0)
  {
    debug(D_VOISIN, 1, "hello_sender", "can't get clockgetime");
    pthread_exit(NULL);
  }
  while (1)
  {
    u_int32_t remains = get_remain_time(SLEEP_HELLO, tm);
    sleep(remains);
    if (clock_gettime(CLOCK_MONOTONIC, &tm) < 0)
    {
      debug(D_VOISIN, 1, "hello_sender", "can't get clockgetime");
      pthread_exit(NULL);
    }
    debug_int(D_SEND_THREAD, 0, "hello_sender -> remains", remains);

    lock(&g_neighbours);
    node_t *n_list = map_to_list(g_neighbours.content);
    unlock(&g_neighbours);

    count = send_hello_long(n_list);
    freedeepnode(n_list);
    debug_int(D_SEND_THREAD, 0, "hello_sender -> count neighbour", count);

    if (count < MIN)
    {
      lock(&g_environs);
      node_t *e_list = map_to_list(g_environs.content);
      unlock(&g_environs);
      count = send_hello_short(e_list, MIN - count);
      freedeepnode(e_list);
      debug_int(D_SEND_THREAD, 0, "hello_sender -> count environs", count);
    }

    debug(D_SEND_THREAD, 0, "hello_sender", "Sending Done");
  }
  pthread_exit(NULL);
}

/**
 * @brief 
 * Detruit les threads d'envoi
 */
void destroy_thread()
{
  debug(D_SEND_THREAD, 0, "destroy_thread", "destruction des threads");
  pthread_cancel(th1);
  pthread_cancel(th2);
}

/**
 * @brief
 * Declenche un nouveau thread d'envoi de Hello
 * et un nouveau d'envoi de neighbours
 *
 * @return si les threads ont été lancés
 */
bool_t init_sender()
{
  if (pthread_create(&th1, NULL, hello_sender, NULL))
  {
    debug(D_SEND_THREAD, 1, "pthread", "Can't initialise thread hello sender");
    return false;
  }
  if (pthread_create(&th2, NULL, neighbour_sender, NULL))
  {
    pthread_cancel(th1);
    debug(D_SEND_THREAD, 1, "pthread", "Can't initialise thread neighbour sender");
    return false;
  }
  pthread_detach(th1);
  pthread_detach(th2);
  return true;
}

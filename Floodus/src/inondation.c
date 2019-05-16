#include "inondation.h"

/**
 * @brief Variable globale contenant la liste des messages à inonder
 * 
 */
static message_t *g_floods = NULL;

/**
 * @brief On libère la mémoire d'un message
 * 
 * @param msg message dont on libère la mémoire
 */
void freemessage(message_t *msg)
{
    freeiovec(msg->content);
    freehashmap(msg->recipient);
    free(msg);
}

/**
 * @brief On libère la mémoire d'un message et de ses suivants.
 * 
 * @param msg message dont on libère la mémoire et ses suivants
 */
static void freedeepmessage(message_t *msg)
{
    message_t *tmp = msg;
    message_t *tmp2 = msg;
    while (tmp != NULL)
    {
        tmp2 = tmp->next;
        freemessage(tmp);
        tmp = tmp2;
    }
}

/**
 * @brief On libère toute la mémoire prise par l'inondation
 * 
 */
void free_inondation()
{
    freedeepmessage(g_floods);
    g_floods = NULL;
}

/**
 * @brief On crée un objet de type message_t
 * 
 * @param dest couple ip-port de celui qui a envoyé le tlv data reçu
 * @param id id de l'originaire du message
 * @param nonce nonce du message envoyé
 * @param type type du message
 * @param content struct iovec du contenu du message envoyé
 * @return message_t* structure construite avec toutes les données correspondantes
 */
static message_t *create_message(ip_port_t dest, u_int64_t id, uint32_t nonce, uint8_t type, data_t *content)
{
    struct timespec tc = {0};
    int rc = 0;
    message_t *res = malloc(sizeof(message_t));
    if (res == NULL)
    {
        debug(D_INOND, 1, "create_message", "création res -> problème de malloc");
        return NULL;
    }
    memset(res, 0, sizeof(message_t));
    data_t *cont_copy = copy_iovec(content);
    if (cont_copy == NULL)
    {
        debug(D_INOND, 1, "create_message", "copy du contenu -> problème de malloc");
        free(res);
        return NULL;
    }
    hashmap_t *recipient = init_map();
    if (recipient == NULL)
    {
        debug(D_INOND, 1, "create_message", "problème de création de hashmap");
        free(res);
        freeiovec(cont_copy);
        return NULL;
    }
    lock(&g_neighbours);
    node_t *neighbour = map_to_list(g_neighbours.content);
    unlock(&g_neighbours);
    node_t *tmp = neighbour;
    while (neighbour != NULL)
    {
        neighbour_t voisin = {0};
        memmove(&voisin, neighbour->value->iov_base, sizeof(neighbour_t));
        if (is_more_than_two(voisin.long_hello) == false && voisin.id != id && memcmp(&dest, neighbour->key, sizeof(ip_port_t)) != 0)
        {
            insert_map(neighbour->key, neighbour->key, recipient);
        }
        neighbour = neighbour->next;
    }
    freedeepnode(tmp);
    rc = clock_gettime(CLOCK_MONOTONIC, &tc);
    if (rc < 0)
    {
        debug(D_CONTROL, 1, "create_message -> erreur clock_gettime", strerror(errno));
        freehashmap(recipient);
        freeiovec(cont_copy);
        free(res);
        return NULL;
    }
    tc.tv_sec += 1;
    res->content = cont_copy;
    res->count = 0;
    res->id = id;
    res->next = NULL;
    res->nonce = nonce;
    res->recipient = recipient;
    res->send_time = tc;
    res->type = type;
    debug_hex(D_INOND, 0, "create_message -> return res", res, sizeof(message_t));
    return res;
}

/**
 * @brief Comparaison de deux struct timespec.
 * 
 * @param ta premier temps
 * @param tb deuxième temps
 * @return int Renvoie un entier inférieur, égal, ou supérieur à zéro, si 'ta' est respectivement inférieure, égale ou supérieur à 'tb'.  
 */
int compare_time(struct timespec ta, struct timespec tb)
{
    if (ta.tv_sec < tb.tv_sec)
    {
        return -1;
    }
    if (ta.tv_sec > tb.tv_sec)
    {
        return 1;
    }
    if (ta.tv_nsec < tb.tv_nsec)
    {
        return -1;
    }
    if (ta.tv_nsec > tb.tv_nsec)
    {
        return 1;
    }
    return 0;
}

/**
 * Enlève le sender de la liste des voisins 
 */
static bool_t remove_sender(ip_port_t dest, message_t *tmp)
{
    data_t sender_iovec = {&dest, sizeof(dest)};
    int res = remove_map(&sender_iovec, tmp->recipient);
    debug_int(D_INOND, 0, "remove_sender -> l'emetteur est enlevé des inondés", res);
    return res;
}

/**
 * @brief On regarde si on envoie déjà le message ayant pour identifiant (id, nonce).
 * On considère le message comme un acquittement, et on enlève le 'sender' de la liste des voisins à inonder.
 * 
 * @param id id du data
 * @param nonce nonce du data
 * @return le message s'il le contient NULL sinon
 */
static message_t *contains_message(u_int64_t id, uint32_t nonce)
{
    message_t *tmp = g_floods;
    while (tmp != NULL)
    {
        if (tmp->id == id && tmp->nonce == nonce)
            return tmp;
        tmp = tmp->next;
    }
    debug(D_INOND, 0, "contains_message", "on n'envoie pas le message");
    return tmp;
}

/**
 * @brief Insert le message dans la liste des messages de telle façon à garder l'ordre des messages
 * 
 * @param msg message à insérer
 * @return bool_t '1' si l'insertion a eu lieu, '0' sinon.
 */
static bool_t insert_message(message_t *msg)
{
    message_t *child = g_floods;
    message_t *father = child;
    while (child != NULL && compare_time(child->send_time, msg->send_time) < 0)
    {
        father = child;
        child = child->next;
    }
    if (father == child)
    {
        g_floods = msg;
        msg->next = father;
        debug(D_INOND, 0, "insert_message", "insertion en première position");
        return true;
    }
    father->next = msg;
    msg->next = child;
    debug(D_INOND, 0, "insert_message", "ajout de la node");
    return true;
}

/**
 * @brief Traitement d'un tlv data, et ajout d'une node d'inondation en cas de besoin.
 * 
 * @param dest couple ip-port de celui qui a envoyé le tlv data
 * @param id sender_id du message
 * @param nonce nonce du message
 * @param type type du message
 * @param content struct iovec contenant le message et sa taille
 * @return bool_t '1' si le traitement a bien été fait, '0' sinon.
 */
bool_t add_message(ip_port_t dest, u_int64_t id, uint32_t nonce, uint8_t type, data_t *content)
{
    message_t *msg = create_message(dest, id, nonce, type, content);
    if (msg == NULL)
    {
        debug(D_INOND, 1, "add_message", "problème de création d'un message_t");
        return false;
    }
    return insert_message(msg);
}

/**
 * @brief On récupère le temps qu'il reste avant le prochain message à inonder
 * 
 * @param tm struct timespec à remplir
 * @return bool_t '1' si 'tm' a été rempli, '0' sinon.
 */
bool_t get_nexttime(struct timespec *tm)
{
    long one_sec_in_nsec = 1000000000;
    if (g_floods == NULL)
    {
        tm->tv_sec = 10;
        tm->tv_nsec = 0;
        return true;
    }
    struct timespec tc = {0};
    int rc = clock_gettime(CLOCK_MONOTONIC, &tc);
    if (rc < 0)
    {
        debug(D_CONTROL, 1, "get_nexttime -> erreur clock_gettime", strerror(errno));
        return false;
    }
    long diff_sec = g_floods->send_time.tv_sec - tc.tv_sec;
    long diff_nsec = g_floods->send_time.tv_nsec - tc.tv_nsec;
    if (diff_sec < 0)
    {
        tm->tv_nsec = 0;
        tm->tv_sec = 0;
    }
    else
    {
        if (diff_nsec < 0)
        {
            if (diff_sec > 0)
            {
                diff_nsec = one_sec_in_nsec + diff_nsec;
                diff_sec--;
            }
            else
            {
                diff_sec = 0;
                diff_nsec = 0;
            }
        }
        tm->tv_nsec = diff_nsec;
        tm->tv_sec = diff_sec;
    }
    return true;
}

/**
 * @brief Envoie de tous les tlv go_away à ceux qui n'ont pas acquitté le message
 * 
 * @param msg message qui a été inondé
 * @return bool_t '1' si l'envoi s'est bien passé, '0' sinon.
 */
static bool_t flood_goaway(message_t *msg)
{
    node_t *list = map_to_list(msg->recipient);
    node_t *tmp = list;
    bool_t no_error = true;
    char message[] = "L'utilisateur n'a pas acquité le message [00000000,0000]";
    snprintf(message, strlen(message) + 1, "L'utilisateur n'a pas acquité le message [%8.lx,%4.x]", msg->id, msg->nonce);
    while (tmp != NULL)
    {
        if (update_neighbours(tmp, 2, message) == false)
            no_error = false;
        tmp = tmp->next;
    }
    freedeepnode(list);
    debug_int(D_INOND, 0, "flood_goaway -> erreurs : ", no_error);
    return no_error;
}

/**
 * @brief On procède à l'inondation sur un message.
 * 
 * @param msg message à inonder
 * @return bool_t '1' si le message a été inondé.
 * '0' si on a envoyé des go_away, et donc que le message n'est pas à rajouté dans la liste des messages à inonder.
 */
bool_t flood_message(message_t *msg)
{
    struct timespec tc = {0};
    if (msg->count > COUNT_INOND)
    {
        flood_goaway(msg);
        debug(D_INOND, 0, "flood_message", "envoi des goaway");
        return false;
    }
    node_t *list = map_to_list(msg->recipient);
    node_t *tmp = list;
    int rc = 0;
    data_t tlv = {0};
    if (!data(&tlv, msg->id, msg->nonce, msg->type,
              (uint8_t *)msg->content->iov_base, msg->content->iov_len))
    {
        debug(D_INOND, 1, "flood_message", "erreur data");
        return false;
    }
    while (tmp != NULL)
    {
        ip_port_t dest = {0};
        memmove(&dest, tmp->value->iov_base, sizeof(ip_port_t));
        if (is_symetric(dest))
        {
            add_tlv(dest, &tlv);
            rc += 1;
        }
        else
        {
            data_t ipport_iovec = {&dest, sizeof(ip_port_t)};
            remove_map(&ipport_iovec, msg->recipient);
        }
        tmp = tmp->next;
    }
    free(tlv.iov_base);
    freedeepnode(list);
    msg->count++;
    double two_pow_c = pow((double)2, (double)msg->count);
    int add_time = (int)((rand() % (int)two_pow_c) + two_pow_c);
    rc = clock_gettime(CLOCK_MONOTONIC, &tc);
    if (rc >= 0) {
      msg->send_time = tc;
    }

    msg->send_time.tv_sec += add_time;
    debug_int(D_INOND, 0, "flood_message -> envoi des datas, nombre d'envoi", rc);
    return true;
}

/**
 * @brief Boucle principal qui procède à l'inondation de tous les messages qui sont arrivés à maturation.
 * 
 * @return bool_t '1', si tout s'est bien passé, '0' sinon.
 */
bool_t launch_flood()
{
    if (g_floods == NULL)
    {
        debug(D_INOND, 0, "launch_flood", "floods vide");
        return true;
    }
    message_t *msg = NULL;
    struct timespec tc = {0};
    int rc = clock_gettime(CLOCK_MONOTONIC, &tc);
    if (rc < 0)
    {
        debug(D_CONTROL, 1, "launch_flood -> erreur clock_gettime", strerror(errno));
        return false;
    }
    rc = 0;
    debug_int(D_INOND, 0, "compare_time -> tc et g_floods", compare_time(tc, g_floods->send_time));
    while (g_floods != NULL && compare_time(tc, g_floods->send_time) >= 0)
    {
        msg = g_floods;
        g_floods = g_floods->next;
        if (flood_message(msg) == false)
        {
            freemessage(msg);
        }
        else
        {
            insert_message(msg);
        }
        rc += 1;
    }
    debug_int(D_INOND, 0, "launch_flood -> nombre de messages faits", rc);
    return true;
}

/**
 * @brief Envoie d'un acquittement pour le message reçu.
 * 
 * @param dest ip-port du destinataire
 * @param sender_id id du message
 * @param nonce nonce du message
 * @return bool_t '1' si l'acquittement a été ajouter, '0' sinon.
 */
static bool_t send_ack(ip_port_t dest, uint64_t sender_id, u_int32_t nonce)
{
    data_t ack_iovec = {0};
    if (!ack(&ack_iovec, sender_id, nonce))
    {
        debug(D_INOND, 1, "send_ack", "problème d'envoi de l'acquitement");
        return false;
    }
    int rc = add_tlv(dest, &ack_iovec);
    free(ack_iovec.iov_base);
    if (rc == false)
    {
        debug(D_INOND, 1, "send_ack", "problème d'ajout du tlv ack");
        return false;
    }
    debug(D_INOND, 0, "send_ack", "traitement du tlv data effectué");
    return true;
}

/**
 * @brief Fonction qui vient faire l'action correspondante à un data pour l'inondation.
 * Si la lecture du tlv s'est bien passé, le champs 'head_read' sera modifié pour pointer vers le tlv suivant.
 * 
 * @param dest couple ip-port correspondant à la source du tlv
 * @param data Structure iovec contenant une suite de tlv.
 * @param head_read tête de lecture sur le tableau contenu dans la struct iovec.
 * @return bool_t renvoie '1' si tout s'est bien passé, '0' si on a rien fait ou s'il y a eu un problème.
 */
bool_t apply_tlv_data(ip_port_t dest, data_t *data, size_t *head_read)
{
    if (*head_read >= data->iov_len)
    {
        debug(D_INOND, 1, "apply_tlv_data", "head_read >= data->iov_len");
        return false;
    }
    uint8_t length = 0;
    memmove(&length, data->iov_base + *head_read, sizeof(u_int8_t));
    *head_read += 1;
    if (is_symetric(dest) == false)
    {
        *head_read += length;
        debug(D_INOND, 1, "apply_tlv_data", "destinataire non symétrique");
        return true;
    }
    if (length < 14) //taille d'un tlv data avec au moins 1 uint8_t dans le champs message
    {
        *head_read += length;
        debug(D_INOND, 1, "apply_tlv_data", "taille trop petite");
        return false;
    }
    u_int64_t sender_id = 0;
    memmove(&sender_id, data->iov_base + *head_read, sizeof(u_int64_t));
    *head_read += sizeof(u_int64_t);
    length -= sizeof(u_int64_t);
    u_int32_t nonce = 0;
    memmove(&nonce, data->iov_base + *head_read, sizeof(u_int32_t));
    *head_read += sizeof(u_int32_t);
    length -= sizeof(u_int32_t);
    u_int8_t type = 0;
    memmove(&type, data->iov_base + *head_read, sizeof(u_int8_t));
    *head_read += sizeof(u_int8_t);
    length -= sizeof(u_int8_t);
    data_t content = {data->iov_base + *head_read, length};
    bool_t rc = true;
    message_t *tmp = NULL;
    if ((tmp = contains_message(sender_id, nonce)) == NULL)
    {
        rc = add_message(dest, sender_id, nonce, type, &content);
        if (rc == false)
        {
            debug(D_INOND, 1, "apply_tlv_data", "problème d'ajout du message");
            *head_read += length;
            return false;
        }
        rc = traitment_data(sender_id, type, content);
        if (!rc)
            debug(D_INOND, 1, "apply_tlv_data", "problème de traitement du message");
    }
    else
    {
        remove_sender(dest, tmp);
        debug(D_INOND, 0, "add_message", "message en cours d'envoi");
    }
    *head_read += length;
    rc = send_ack(dest, sender_id, nonce);
    return rc;
}

/**
 * @brief Fonction qui vient faire l'action correspondante à un ack pour l'inondation.
 * Si la lecture du tlv s'est bien passé, le champs 'head_read' sera modifié pour pointer vers le tlv suivant.
 * 
 * @param dest couple ip-port correspondant à la source du tlv
 * @param data Structure iovec contenant une suite de tlv.
 * @param head_read tête de lecture sur le tableau contenu dans la struct iovec.
 * @return bool_t renvoie '1' si tout s'est bien passé, '0' si on a rien fait ou s'il y a eu un problème.
 */
bool_t apply_tlv_ack(ip_port_t dest, data_t *data, size_t *head_read)
{
    if (*head_read >= data->iov_len)
    {
        debug(D_INOND, 1, "apply_tlv_ack", "head_read >= data->iov_len");
        return false;
    }
    uint8_t length = 0;
    memmove(&length, data->iov_base + *head_read, sizeof(u_int8_t));
    *head_read += 1;
    if (length != 12) // taille du tlv ack
    {
        *head_read += length;
        debug(D_INOND, 1, "apply_tlv_ack", "taille trop petite");
        return false;
    }
    u_int64_t sender_id = 0;
    memmove(&sender_id, data->iov_base + *head_read, sizeof(u_int64_t));
    *head_read += sizeof(u_int64_t);
    u_int32_t nonce = 0;
    memmove(&nonce, data->iov_base + *head_read, sizeof(u_int32_t));
    *head_read += sizeof(u_int32_t);

    message_t *tmp = NULL;
    if ((tmp = contains_message(sender_id, nonce)) != NULL)
    {
        remove_sender(dest, tmp);
        debug(D_INOND, 0, "apply_tlv_ack", "message non en mémoire");
    }
    debug(D_INOND, 0, "apply_tlv_ack", "mise à jour du message");
    return true;
}

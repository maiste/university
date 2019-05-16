/**
 * @file reader.c
 * @author Floodus
 * @brief Module s'occupant de tout ce qui est lecture d'un datagramme
 * 
 */

#include "reader.h"

typedef bool_t (*tlv_function_t)(ip_port_t, data_t *, size_t *);

/**
 * @brief Hashmap contenant les messages ancillaires
 * 
 * clé : ip_port_t du destinataire
 * valeur : contenu du message ancillaire en in6_addr
 * 
 */
hashmap_t *g_ancillary = NULL;

/**
 * @brief Fonction appelé lors de la lecture d'un tlv 'pad1'
 * 
 * @param dest couple ip-port de celui qui a envoyé le tlv
 * @param data strcut iovec contenant l'ensemble des données reçues
 * @param head_read tête de lecture de la struct iovec 'data'
 * @return bool_t '1' si la lecture a abouti, et la tête de lecture est déplacé sur le prochain tlv. '0' sinon.
 */
static bool_t tlv_call_pad1(ip_port_t dest, data_t *data, size_t *head_read)
{
    (void)dest; //enlève le warning

    if (((u_int8_t *)data->iov_base)[*head_read] != 0)
    {
        debug(D_READER, 1, "tlv_call_pad1", "mauvais type");
        return false;
    }
    *head_read += 1;
    debug(D_READER, 0, "tlv_call_pad1", "lecture du pad1");
    return true;
}

/**
 * @brief Fonction appelé lors de la lecture d'un tlv 'padn'
 * 
 * @param dest couple ip-port de celui qui a envoyé le tlv
 * @param data strcut iovec contenant l'ensemble des données reçues
 * @param head_read tête de lecture de la struct iovec 'data'
 * @return bool_t '1' si la lecture a abouti, et la tête de lecture est déplacé sur le prochain tlv. '0' sinon.
 */
static bool_t tlv_call_padn(ip_port_t dest, data_t *data, size_t *head_read)
{
    (void)dest; //enlève le warning

    if (((u_int8_t *)data->iov_base)[*head_read] != 1)
    {
        debug(D_READER, 1, "tlv_call_padn", "mauvais type");
        return false;
    }
    *head_read += 1;
    u_int8_t len = ((u_int8_t *)data->iov_base)[*head_read];
    if (*head_read + len >= data->iov_len)
    {
        debug(D_READER, 1, "tlv_call_padn", "taille du message non correspondante");
        *head_read = data->iov_len;
        return false;
    }
    *head_read += len + 1;
    debug(D_READER, 0, "tlv_call_padn", "lecture du padn");
    return true;
}

/**
 * @brief Fonction appelé lors de la lecture d'un tlv 'hello'
 * 
 * @param dest couple ip-port de celui qui a envoyé le tlv
 * @param data strcut iovec contenant l'ensemble des données reçues
 * @param head_read tête de lecture de la struct iovec 'data'
 * @return bool_t '1' si la lecture a abouti, et la tête de lecture est déplacé sur le prochain tlv. '0' sinon.
 */
static bool_t tlv_call_hello(ip_port_t dest, data_t *data, size_t *head_read)
{
    if (((u_int8_t *)data->iov_base)[*head_read] != 2)
    {
        debug(D_READER, 1, "tlv_call_hello", "mauvais type");
        return false;
    }
    *head_read += 1;
    u_int8_t len = ((u_int8_t *)data->iov_base)[*head_read];
    if (*head_read + len >= data->iov_len)
    {
        debug(D_READER, 1, "tlv_call_hello", "taille du message non correspondante");
        *head_read = data->iov_len;
        return false;
    }
    bool_t res = apply_tlv_hello(dest, data, head_read);
    if (res == false)
    {
        debug(D_READER, 1, "tlv_call_hello", "problème application du tlv hello");
        return res;
    }
    debug(D_READER, 0, "tlv_call_hello", "traitement du tlv hello effectué");
    return true;
}

/**
 * @brief Fonction appelé lors de la lecture d'un tlv 'neighbour'
 * 
 * @param dest couple ip-port de celui qui a envoyé le tlv
 * @param data strcut iovec contenant l'ensemble des données reçues
 * @param head_read tête de lecture de la struct iovec 'data'
 * @return bool_t '1' si la lecture a abouti, et la tête de lecture est déplacé sur le prochain tlv. '0' sinon.
 */
bool_t tlv_call_neighbour(ip_port_t dest, data_t *data, size_t *head_read)
{
    (void)dest; //enlève le warning

    if (((u_int8_t *)data->iov_base)[*head_read] != 3)
    {
        debug(D_READER, 1, "tlv_call_neighbour", "mauvais type");
        return false;
    }
    *head_read += 1;
    u_int8_t len = ((u_int8_t *)data->iov_base)[*head_read];
    if (*head_read + len >= data->iov_len)
    {
        debug(D_READER, 1, "tlv_call_neighbour", "taille du message non correspondante");
        *head_read = data->iov_len;
        return false;
    }
    if (!is_neighbour(dest))
    {
        debug(D_READER, 1, "tlv_call_neighbour", "Source inconnue");
        *head_read += len;
        return false;
    }
    bool_t res = apply_tlv_neighbour(data, head_read);
    if (res == false)
    {
        debug(D_READER, 1, "tlv_call_neighbour", "problème application du tlv neighbour");
        return res;
    }
    debug(D_READER, 0, "tlv_call_neighbour", "traitement du tlv neighbour effectué");
    return true;
}

/**
 * @brief Fonction appelé lors de la lecture d'un tlv 'data'
 * 
 * @param dest couple ip-port de celui qui a envoyé le tlv
 * @param data strcut iovec contenant l'ensemble des données reçues
 * @param head_read tête de lecture de la struct iovec 'data'
 * @return bool_t '1' si la lecture a abouti, et la tête de lecture est déplacé sur le prochain tlv. '0' sinon.
 */
bool_t tlv_call_data(ip_port_t dest, data_t *data, size_t *head_read)
{
    if (((u_int8_t *)data->iov_base)[*head_read] != 4)
    {
        debug(D_READER, 1, "tlv_call_data", "mauvais type");
        return false;
    }
    *head_read += 1;
    u_int8_t len = ((u_int8_t *)data->iov_base)[*head_read];
    if (*head_read + len >= data->iov_len)
    {
        debug(D_READER, 1, "tlv_call_data", "taille du message non correspondante");
        *head_read = data->iov_len;
        return false;
    }
    if (!is_neighbour(dest))
    {
        debug(D_READER, 1, "tlv_call_data", "Source inconnue");
        *head_read += len;
        return false;
    }
    bool_t res = apply_tlv_data(dest, data, head_read);
    if (res == false)
    {
        debug(D_READER, 1, "tlv_call_data", "problème application du tlv data");
        return res;
    }
    debug(D_READER, 0, "tlv_call_data", "traitement du tlv data effectué");
    return true;
}

/**
 * @brief Fonction appelé lors de la lecture d'un tlv 'ack'
 * 
 * @param dest couple ip-port de celui qui a envoyé le tlv
 * @param data strcut iovec contenant l'ensemble des données reçues
 * @param head_read tête de lecture de la struct iovec 'data'
 * @return bool_t '1' si la lecture a abouti, et la tête de lecture est déplacé sur le prochain tlv. '0' sinon.
 */
bool_t tlv_call_ack(ip_port_t dest, data_t *data, size_t *head_read)
{
    if (((u_int8_t *)data->iov_base)[*head_read] != 5)
    {
        debug(D_READER, 1, "tlv_call_ack", "mauvais type");
        return false;
    }
    *head_read += 1;
    u_int8_t len = ((u_int8_t *)data->iov_base)[*head_read];
    if (*head_read + len >= data->iov_len)
    {
        debug(D_READER, 1, "tlv_call_ack", "taille du message non correspondante");
        *head_read = data->iov_len;
        return false;
    }
    bool_t res = apply_tlv_ack(dest, data, head_read);
    if (res == false)
    {
        debug(D_READER, 1, "tlv_call_ack", "problème application du tlv ack");
        return res;
    }
    debug(D_READER, 0, "tlv_call_ack", "traitement du tlv ack effectué");
    return true;
}

/**
 * @brief Fonction appelé lors de la lecture d'un tlv 'goaway'
 * 
 * @param dest couple ip-port de celui qui a envoyé le tlv
 * @param data strcut iovec contenant l'ensemble des données reçues
 * @param head_read tête de lecture de la struct iovec 'data'
 * @return bool_t '1' si la lecture a abouti, et la tête de lecture est déplacé sur le prochain tlv. '0' sinon.
 */
bool_t tlv_call_goaway(ip_port_t dest, data_t *data, size_t *head_read)
{
    if (((u_int8_t *)data->iov_base)[*head_read] != 6)
    {
        debug(D_READER, 1, "tlv_call_goaway", "mauvais type");
        return false;
    }
    *head_read += 1;
    u_int8_t len = ((u_int8_t *)data->iov_base)[*head_read];
    if (*head_read + len >= data->iov_len)
    {
        debug(D_READER, 1, "tlv_call_goaway", "taille du message non correspondante");
        *head_read = data->iov_len;
        return false;
    }
    debug_hex(D_READER, 0, "tlv_call_goaway", data->iov_base + *head_read + 1, len);
    bool_t res = apply_tlv_goaway(dest, data, head_read);
    if (res == false)
    {
        debug(D_READER, 1, "tlv_call_goaway", "problème application du tlv goaway");
        return res;
    }
    debug(D_READER, 0, "tlv_call_goaway", "traitement du tlv goaway effectué");
    return true;
}

/**
 * @brief Fonction appelé lors de la lecture d'un tlv 'warning'
 * 
 * @param dest couple ip-port de celui qui a envoyé le tlv
 * @param data strcut iovec contenant l'ensemble des données reçues
 * @param head_read tête de lecture de la struct iovec 'data'
 * @return bool_t '1' si la lecture a abouti, et la tête de lecture est déplacé sur le prochain tlv. '0' sinon.
 */
bool_t tlv_call_warning(ip_port_t dest, data_t *data, size_t *head_read)
{
    (void)dest; //enlève le warning

    if (((u_int8_t *)data->iov_base)[*head_read] != 7)
    {
        debug(D_READER, 1, "tlv_call_warning", "mauvais type");
        return false;
    }
    *head_read += 1;
    u_int8_t len = ((u_int8_t *)data->iov_base)[*head_read];
    if (*head_read + len >= data->iov_len)
    {
        debug(D_READER, 1, "tlv_call_warning", "taille du message non correspondante");
        *head_read = data->iov_len;
        return false;
    }
    *head_read += 1;
    char *content = malloc(len + 1);
    if (content == NULL)
    {
        debug(D_READER, 1, "tlv_call_warning", "impossible d'afficher le message");
        *head_read += len;
        return false;
    }
    memmove(content, data->iov_base + *head_read, len);
    content[len] = '\0';
    debug(D_READER, 1, "tlv_call_warning -> message", content);
    free(content);
    *head_read += len;
    debug(D_READER, 0, "tlv_call_warning", "traitement du tlv warning effectué");
    return true;
}

/**
 * @brief tableau des différentes fonctions de traitements des tlvs
 * 
 */
tlv_function_t tlv_function_call[NB_TLV] = {

    tlv_call_pad1,
    tlv_call_padn,
    tlv_call_hello,
    tlv_call_neighbour,
    tlv_call_data,
    tlv_call_ack,
    tlv_call_goaway,
    tlv_call_warning

};

/**
 * @brief Initialisation de g_ancillary
 * 
 * @return bool_t '1' si init bien passé, '0' sinon.
 */
bool_t init_ancillary(void)
{
    g_ancillary = init_map();
    if (g_ancillary == NULL)
    {
        debug(D_READER, 1, "init_ancillary", "impossible d'init g_ancillary");
        return false;
    }
    debug(D_READER, 0, "init_ancillary", "init g_ancillary");
    return true;
}

/**
 * @brief Free de g_ancillary
 * 
 */
void free_ancillary(void)
{
    freehashmap(g_ancillary);
    g_ancillary = NULL;
}

/**
 * @brief Lecture d'une suite de tlvs
 * 
 * @param dest Couple ip-port de celui qui a émis les tlvs reçus
 * @param tlvs tlvs reçus
 */
static void read_tlv(ip_port_t dest, data_t *tlvs)
{
    size_t head_reader = 0;
    u_int8_t type = 0;
    while (head_reader < tlvs->iov_len)
    {
        type = ((u_int8_t *)tlvs->iov_base)[head_reader];
        if (type >= NB_TLV)
        {
            debug_int(D_READER, 0, "read_tlv -> type inconnu de tlv", type);
            head_reader += 1;
            if (head_reader >= tlvs->iov_len)
                break;
            head_reader += ((u_int8_t *)tlvs->iov_base)[head_reader];
            continue;
        }
        debug_int(D_READER, 0, "read_tlv -> commencement du traitement du tlv", type);
        tlv_function_call[type](dest, tlvs, &head_reader);
    }
    debug(D_READER, 0, "read_tlv", "fin de lecture du tlv");
}

/**
 * @brief On s'occupe de lire les données sur la socket 'g_socket'
 * 
 * @return ssize_t nombre de données lues, '-1' s'il y a une erreur
 */
ssize_t read_msg(void)
{
    // sockaddr_in6 pour le destinataire
    struct sockaddr_in6 recv = {0};
    socklen_t recvlen = sizeof(recv);

    // initialisation du premier struct iovec
    uint8_t header[RDHDRLEN] = {0};
    struct iovec header_ivc = {0};
    header_ivc.iov_base = header;
    header_ivc.iov_len = RDHDRLEN;

    // initialisation du deuxième struct iovec
    uint8_t req[READBUF] = {0};
    struct iovec corpus = {0};
    corpus.iov_base = req;
    corpus.iov_len = READBUF;

    //initialisation message de control
    union {
        struct cmsghdr hdr;
        unsigned char cmsgbuf[CMSG_SPACE(sizeof(struct in6_pktinfo))];
    } u;
    memset(&u, 0, sizeof(u));
    struct cmsghdr *cmsg = NULL;
    struct in6_pktinfo *info = NULL;

    // initialisation du struct msghdr
    data_t content[2] = {header_ivc, corpus};
    struct msghdr reader = {0};
    reader.msg_name = &recv;
    reader.msg_namelen = recvlen;
    reader.msg_iov = content;
    reader.msg_iovlen = 2;
    reader.msg_control = (struct cmsghdr *)u.cmsgbuf;
    reader.msg_controllen = sizeof(u.cmsgbuf);

    ssize_t rc = 0;
    // attente active si la socket est pas dispo meme après un select
    while (1)
    {
        debug(D_WRITER, 0, "read_msg", "turn");
        rc = recvmsg(g_socket, &reader, 0);
        if (rc >= 0)
            break;
        if (errno == EINTR ||
            (errno != EWOULDBLOCK && errno != EAGAIN))
        {
            debug(D_WRITER, 1, "read_msg -> reception non effectué", strerror(errno));
            return -1;
        }
    }
    // verification des 4 premiers octets
    header_ivc = content[0];
    uint8_t expected[2] = {93, 2};
    if (memcmp(header_ivc.iov_base, expected, 2) != 0)
    {
        debug(D_READER, 0, "read_msg", "les champs magic et version ne correspondent pas");
        return 0;
    }
    uint16_t len_msg = 0;
    memmove(&len_msg, ((uint8_t *)header_ivc.iov_base) + 2, 2);
    len_msg = ntohs(len_msg);
    if (len_msg != rc - RDHDRLEN)
    {
        debug(D_READER, 1, "read_msg", "taille lue et taille attendue différente");
        return 0;
    }
    //traitement message ancillaire
    cmsg = CMSG_FIRSTHDR(&reader);
    while (cmsg != NULL)
    {
        if ((cmsg->cmsg_level == IPPROTO_IPV6) &&
            (cmsg->cmsg_type == IPV6_PKTINFO))
        {
            info = (struct in6_pktinfo *)CMSG_DATA(cmsg);
            break;
        }
        cmsg = CMSG_NXTHDR(&reader, cmsg);
    }

    // traitement des données reçues
    ip_port_t ipport = {0};
    memmove(&ipport.port, &((struct sockaddr_in6 *)reader.msg_name)->sin6_port, sizeof(ipport.port));
    memmove(ipport.ipv6, &((struct sockaddr_in6 *)reader.msg_name)->sin6_addr, sizeof(ipport.ipv6));
    content[1].iov_len = len_msg;

    if (info == NULL)
    {
        debug(D_READER, 1, "read_msg", "IPV6_PKTINFO non trouvé");
    }
    else
    {
        data_t ipport_ivc = {&ipport, sizeof(ip_port_t)};
        data_t info_ivc = {info, sizeof(struct in6_pktinfo)};
        struct in6_pktinfo info_cmp = {0};
        data_t info_cmp_ivc = {&info_cmp, sizeof(info_cmp)};
        if (get_map(&ipport_ivc, &info_cmp_ivc, g_ancillary))
        {
            if (compare_iovec(&info_ivc, &info_cmp_ivc) != 0)
            {
                debug(D_READER, 1, "read_msg", "Mauvaise provenance par rapport à l'envoi");
                debug(D_WRITER, 0, "read_msg", "lecture d'un datagramme");
                return rc;
            }
        }
        else
        {
            int rc = insert_map(&ipport_ivc, &info_ivc, g_ancillary);
            debug_int(D_READER, 0, "read_msg -> ajout d'un message ancillaire", rc);
        }
    }

    read_tlv(ipport, &content[1]);
    debug(D_WRITER, 0, "read_msg", "lecture d'un datagramme");
    return rc;
}

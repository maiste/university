/**
 * @file writer.c
 * @author Floodus
 * @brief Module correspondant à toutes les fonctionnalités d'écriture sur le réseau.
 * 
 */

#include "writer.h"

/**
 * @brief structure contenu dans le tampon
 * - 'dest' est la destination du message
 * - 'tlvs' est un tableau de tlv à envoyer
 * - 'tlvlen' est le nombre d'éléments contenus dans 'tlvs'
 * - 'next' est la node suivante dans le tableau
 */
typedef struct buffer_node_t
{
    ip_port_t dest;
    data_t *tlvs;
    size_t tlvlen;
    struct buffer_node_t *next;
} buffer_node_t;

/**
 * @brief Buffer d'écriture indexé par :
 * - la key est un ipport
 * - la value est un tableau de tlv
 */
static buffer_node_t *g_write_buf = NULL;

/**
 *  @brief 
 * Cadenas bloquant l'accès concurrent au buffer
 * de messages
 */
static pthread_mutex_t g_lock_buff = PTHREAD_MUTEX_INITIALIZER;

static void lock_buff(void)
{
    pthread_mutex_lock(&g_lock_buff);
}

static void unlock_buff(void)
{
    pthread_mutex_unlock(&g_lock_buff);
}

/**
 * @brief On nettoie le buffer (on le free)
 * 
 */
void free_writer()
{
    debug(D_WRITER, 0, "free_writer", "clear buffer");
    lock_buff();
    buffer_node_t *tmp = NULL;
    while (g_write_buf != NULL)
    {
        tmp = g_write_buf;
        g_write_buf = g_write_buf->next;
        free(tmp->tlvs);
        free(tmp);
    }
    g_write_buf = NULL;
    unlock_buff();
}

/**
 * @brief Fonction d'envoi des tlvs à une adresse contenue dans 'dest'.
 * 
 * @param dest destination du message
 * @param tlvs contenu du message à envoyer
 * @param tlvs_len taille du contenu
 * @return bool_t '1' si le message s'est bien envoyé, '0' sinon.
 */
bool_t send_tlv(ip_port_t dest, data_t *tlvs, size_t tlvs_len)
{
    struct sockaddr_in6 destination = {0};
    destination.sin6_family = AF_INET6;
    destination.sin6_port = dest.port;
    memmove(&destination.sin6_addr, dest.ipv6, sizeof(dest.ipv6));
    struct iovec header_ivc = {0};
    u_int8_t header[4] = {93, 2, 0, 0};
    u_int16_t total_tlvs_len = 0;
    for (size_t i = 0; i < tlvs_len; i++)
    {
        total_tlvs_len += tlvs[i].iov_len;
    }
    total_tlvs_len = htons(total_tlvs_len);
    memmove(header + 2, &total_tlvs_len, 2);
    header_ivc.iov_base = header;
    header_ivc.iov_len = 4;
    data_t *content = malloc((tlvs_len + 1) * sizeof(data_t));
    if (content == NULL)
    {
        debug(D_WRITER, 1, "send_tlv", "problème de malloc -> content");
        return false;
    }
    content[0] = header_ivc;
    for (size_t i = 0; i < tlvs_len; i++)
    {
        content[i + 1] = tlvs[i];
    }
    struct msghdr msg = {0};
    msg.msg_name = &destination;
    msg.msg_namelen = sizeof(struct sockaddr_in6);
    msg.msg_iov = content;
    msg.msg_iovlen = tlvs_len + 1;

    // message ancillaire
    struct in6_pktinfo info = {0};
    struct cmsghdr *cmsg;

    data_t dest_ivc = {&dest, sizeof(dest)};
    data_t info_ivc = {&info, sizeof(info)};
    int rc = get_map(&dest_ivc, &info_ivc, g_ancillary);
    if (rc)
    {
        debug(D_WRITER, 0, "send_tlv", "ajout du message ancillaire");
        union {
            struct cmsghdr hdr;
            unsigned char cmsgbuf[CMSG_SPACE(sizeof(struct in6_pktinfo))];
        } u;
        memset(&u, 0, sizeof(u));
        msg.msg_control = u.cmsgbuf;
        msg.msg_controllen = sizeof(u.cmsgbuf);
        cmsg = CMSG_FIRSTHDR(&msg);
        cmsg->cmsg_level = IPPROTO_IPV6;
        cmsg->cmsg_type = IPV6_PKTINFO;
        cmsg->cmsg_len = CMSG_LEN(sizeof(struct in6_pktinfo));
        memcpy(CMSG_DATA(cmsg), &info, sizeof(struct in6_pktinfo));
    }
    while (1)
    {
        debug(D_WRITER, 0, "send_tlv", "écriture du message dans la socket");
        int rc = sendmsg(g_socket, &msg, 0);
        if (rc >= 0)
            break;
        if (errno == EINTR ||
            (errno != EWOULDBLOCK && errno != EAGAIN))
        {
            if (errno == EINVAL)
                errno = ENETDOWN;
            free(content);
            debug(D_WRITER, 1, "send_tlv -> envoi non effectué", strerror(errno));
            return false;
        }
    }
    for (size_t i = 0; i < tlvs_len + 1; i++)
    {
        char buf[] = "send_tlv -> content [0000]";
        snprintf(buf, strlen(buf) + 1, "send_tlv -> content [%.4ld]", i);
        debug_hex(D_WRITER, 0, buf, content[i].iov_base, content[i].iov_len);
    }
    free(content);
    debug_int(D_WRITER, 0, "send_tlv -> demande envoyée", rc);
    return true;
}

/**
 * @brief On rajoute le tlv en argument à node
 * 
 * @param tlv tlv à ajouter
 * @param node node à remplir
 * @return bool_t '1' si l'ajout s'est bien passé, '0' sinon.
 */
static bool_t complete_node_tlv(data_t *tlv, buffer_node_t *node)
{
    if (node == NULL)
    {
        debug(D_WRITER, 1, "complete_node_tlv", "node = (null)");
        return false;
    }
    data_t *ntlv = copy_iovec(tlv);
    if (ntlv == NULL)
    {
        debug(D_WRITER, 1, "complete_node_tlv", "problème de malloc -> copy de tlv");
        return false;
    }
    size_t size = sizeof(data_t) * (node->tlvlen + 1);
    data_t *content = malloc(size);
    if (content == NULL)
    {
        freeiovec(ntlv);
        debug(D_WRITER, 1, "complete_node_tlv", "cannot allow memory pour content");
        return false;
    }
    for (size_t i = 0; i < node->tlvlen; i++)
    {
        content[i] = node->tlvs[i];
    }
    content[node->tlvlen] = *ntlv;
    free(ntlv);
    free(node->tlvs);
    node->tlvs = content;
    node->tlvlen += 1;
    debug(D_WRITER, 0, "complete_node_tlv", "ajout bien effectué");
    return true;
}

/**
 * @brief Est ce que 'tlv' peut être ajouté à la node 'node' du buffer pour envoyer à 'dest'?
 * Si oui, on ajoute le tlv.
 * 
 * @param dest ip-port du destinataire
 * @param tlv tlv à rajouter
 * @param node node du buffer
 * @return bool_t on renvoie '1' si l'ajout est possible sur la node en paramètre, '0' sinon.
 */
static bool_t can_add_tlv(ip_port_t dest, data_t *tlv, buffer_node_t *node)
{
    if (node == NULL)
    {
        debug(D_WRITER, 1, "can_add_tlv", "node = (null)");
        return false;
    }
    if (memcmp(&(node->dest).ipv6, &dest.ipv6, sizeof(dest.ipv6)) != 0 ||
        (node->dest).port != dest.port)
    {
        debug(D_WRITER, 0, "can_add_tlv", "node ne correspond pas au destinataire");
        return false;
    }
    size_t total_len = 0;
    for (size_t i = 0; i < node->tlvlen; i++)
    {
        total_len += node->tlvs[i].iov_len;
    }
    if (total_len + tlv->iov_len > MAX_PER_TLV)
    {
        debug(D_WRITER, 0, "can_add_tlv", "taille max de tlv atteint");
        return false;
    }
    debug(D_WRITER, 0, "can_add_tlv", "ajout possible");
    return true;
}

/**
 * @brief ajout d'une node à la suite de 'father' initialisée avec 'dest' et 'tlv'
 * 
 * @param father node précédente
 * @param dest ip-port de destination
 * @param tlv tlv à envoyer
 * @return bool_t '1' si la node a été ajouté, '0' sinon.
 */
static bool_t add_next_node(buffer_node_t *father, ip_port_t dest, data_t *tlv)
{
    buffer_node_t *next_node = malloc(sizeof(buffer_node_t));
    if (next_node == NULL)
    {
        debug(D_WRITER, 1, "add_next_node", "erreur de malloc next_node");
        return false;
    }
    data_t *ntlv = copy_iovec(tlv);
    if (ntlv == NULL)
    {
        free(next_node);
        debug(D_WRITER, 1, "add_next_node", "problème de copie de tlv");
        return false;
    }
    memset(next_node, 0, sizeof(buffer_node_t));
    next_node->dest = dest;
    next_node->tlvs = ntlv;
    next_node->tlvlen = 1;
    next_node->next = NULL;
    if (father == NULL)
    {
        g_write_buf = next_node;
        debug(D_WRITER, 0, "add_next_node", "initialisation première next_node du buffer");
        return true;
    }
    next_node->next = father->next;
    father->next = next_node;
    debug(D_WRITER, 0, "add_next_node", "ajout d'une next_node au buffer");
    return true;
}

/**
 * @brief On ajoute le tlv 'tlv' dans le buffer d'envoi.
 * 
 * @param dest ip-port du destinataire
 * @param tlv tlv à envoyer
 * @return bool_t '1' si on a bien ajouté le tlv au buffer, '0' sinon.
 */
bool_t add_tlv(ip_port_t dest, data_t *tlv)
{
    if (tlv == NULL)
    {
        debug(D_WRITER, 1, "add_tlv", "tlv = (null)");
        return false;
    }
    lock_buff();
    buffer_node_t *father = g_write_buf;
    buffer_node_t *child = g_write_buf;
    int rc = 0;
    while (child != NULL && (rc = can_add_tlv(dest, tlv, child)) != true)
    {
        father = child;
        child = child->next;
    }
    if (rc)
    {
        if (complete_node_tlv(tlv, child))
        {
            debug(D_WRITER, 0, "add_tlv", "ajout bien effectué");
            unlock_buff();
            return true;
        }
        debug(D_WRITER, 1, "add_tlv", "ajout non effectué");
        unlock_buff();
        return false;
    }
    rc = add_next_node(father, dest, tlv);
    unlock_buff();
    if (rc)
    {
        debug(D_WRITER, 0, "add_tlv", "ajout bien effectué");
        return true;
    }
    debug(D_WRITER, 1, "add_tlv", "ajout non effectué");
    return false;
}

/**
 * @brief Permet de déterminer si le buffer d'écriture est vide
 * 
 * @return bool_t '1' s'il est vide, '0' sinon.
 */
bool_t buffer_is_empty()
{
    lock_buff();
    bool_t res = (g_write_buf == NULL);
    unlock_buff();
    return res;
}

/**
 * @brief Fonction de calcul du pmtu
 * 
 * @param dest ip-port de destination
 * @return u_int32_t pmtu
 */
u_int32_t get_pmtu(ip_port_t dest)
{
    int one = 1;
    int s = socket(AF_INET6, SOCK_DGRAM, 0);
    if (s < 0)
    {
        debug(D_WRITER, 1, "get_pmtu -> impossible de créer la socket", strerror(errno));
        errno = 0;
        return 1000;
    }
    int rc = setsockopt(s, IPPROTO_IPV6, IPV6_DONTFRAG,
                        &one, sizeof(one));
    if (rc < 0)
    {
        close(s);
        debug(D_WRITER, 1, "get_pmtu -> setsockopt IPV6_DONTFRAG", strerror(errno));
        errno = 0;
        return 1000;
    }
    rc = setsockopt(s, IPPROTO_IPV6, IPV6_MTU_DISCOVER,
                    &one, sizeof(one));
    if (rc < 0)
    {
        close(s);
        debug(D_WRITER, 1, "get_pmtu -> setsockopt IPV6_MTU_DISCOVER", strerror(errno));
        errno = 0;
        return 1000;
    }
    struct sockaddr_in6 sin6 = {0};
    sin6.sin6_family = AF_INET6;
    sin6.sin6_port = dest.port;
    memmove(&sin6.sin6_addr, dest.ipv6, sizeof(dest.ipv6));
    rc = connect(s, (struct sockaddr *)&sin6, sizeof(sin6));
    if (rc < 0)
    {
        close(s);
        debug(D_WRITER, 1, "get_pmtu -> impossible de se connecter", strerror(errno));
        errno = 0;
        return 1000;
    }
    u_int16_t pmtu = USHRT_MAX;
    u_int8_t buf[USHRT_MAX] = {0};
    buf[0] = 93;
    buf[1] = 2;
    u_int16_t tmp = htons(pmtu - 4);
    memmove(buf+2, &tmp, 2);
    rc = send(s, buf, pmtu, 0);
    if (rc < 0)
    {
        if (errno == EMSGSIZE)
        {
            struct ip6_mtuinfo mtuinfo;
            socklen_t infolen = sizeof(mtuinfo);
            rc = getsockopt(s, IPPROTO_IPV6, IPV6_PATHMTU,
                            &mtuinfo, &infolen);
            if (rc >= 0)
            {
                /* On met a jour le PMTU */
                pmtu = mtuinfo.ip6m_mtu;
            }
            else
            {
                /* On n'a pas réussi à déterminer le PMTU */
                close(s);
                debug(D_WRITER, 1, "get_pmtu -> echec de calcul du pmtu", strerror(errno));
                errno = 0;
                return 1000;
            }
        }
        else
        {
            close(s);
            debug(D_WRITER, 1, "get_pmtu -> send echec", strerror(errno));
            errno = 0;
            return 1000;
        }
    }
    close(s);
    pmtu -= 50;
    debug_int(D_WRITER, 0, "get_pmtu", pmtu);
    errno = 0;
    return pmtu;
}

/**
 * @brief On envoie ce qui est en tete du buffer
 * 
 * @return bool_t '1' si tout se passe bien, '0' sinon.
 */
bool_t send_buffer_tlv()
{
    int res = 0;
    lock_buff();
    if (g_write_buf == NULL)
    {
        unlock_buff();
        debug(D_WRITER, 1, "send_buffer_tlv", "le buffer est null");
        return false;
    }
    size_t pmtu = get_pmtu(g_write_buf->dest);
    size_t sendlen = 0;
    size_t ind = 0;
    for (; ind < g_write_buf->tlvlen; ind++)
    {
        size_t add_len = g_write_buf->tlvs[ind].iov_len;
        if (sendlen + add_len <= pmtu)
            sendlen += add_len;
        else
            break;
    }
    res = send_tlv(g_write_buf->dest, g_write_buf->tlvs, ind);
    if (!res)
    {
        debug(D_WRITER, 1, "send_buffer_tlv", "1er envoi non effectué");
        unlock_buff();
        return false;
    }
    // envoie du reste des tlvs, à modifer dans les extensions
    if (ind < g_write_buf->tlvlen)
    {
        res = send_tlv(g_write_buf->dest, g_write_buf->tlvs + ind - 1, g_write_buf->tlvlen - ind + 1);
        if (res == false)
        {
            // possiblement, il faudrait enlever de la node tous les tlvs qu'on a déjà envoyé
            debug(D_WRITER, 1, "send_buffer_tlv", "2eme envoi non effectué");
            unlock_buff();
            return false;
        }
    }
    for (size_t i = 0; i < g_write_buf->tlvlen; i++)
    {
        free(g_write_buf->tlvs[i].iov_base);
    }
    free(g_write_buf->tlvs);
    buffer_node_t *tmp = g_write_buf;
    g_write_buf = g_write_buf->next;
    free(tmp);
    debug_int(D_WRITER, 0, "send_buffer_tlv -> envoi tlv", res);
    unlock_buff();
    return res;
}

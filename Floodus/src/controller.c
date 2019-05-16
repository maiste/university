/**
 * @file controller.c
 * @author Floodus
 * @brief Module s'occupant de faire la liaison entre le reader, le writer et l'inondation
 * 
 * 
 */

#include "controller.h"

/**
 * @brief Variable globale correspodant à une socket
 * 
 */
u_int32_t g_socket = 1;

/**
 * @brief Permet d'arrêter la boucle proprement
 */
static bool_t run = true;

/**
 * @brief Stop la boucle principal
 */
void stop_program(void)
{
    run = false;
}

/**
 * @brief Ferme la socket
 */
void close_sock(void)
{
    close(g_socket);
}

/**
 * @brief Création de la socket d'écriture et de lecture.
 * On s'occupe de rendre la socket non-bloquante.
 * 
 * @param port port pour le bind
 * @return int '0' si l'opération réussie, un nombre négatif sinon (dépend du type d'erreur).
 */
int create_socket(uint16_t port)
{
    int one = 1;
    int rc = 0;
    int s = socket(AF_INET6, SOCK_DGRAM, 0);
    if (s < 0)
    {
        rc = errno;
        debug(D_CONTROL, 1, "create_socket -> erreur de création de socket", strerror(errno));
        errno = rc;
        return -1;
    }
    struct sockaddr_in6 server = {0};
    server.sin6_family = AF_INET6;
    server.sin6_port = htons(port);
    socklen_t server_len = sizeof(server);
    rc = bind(s, (struct sockaddr *)&server, server_len);
    if (rc < 0)
    {
        close(s);
        rc = errno;
        debug(D_CONTROL, 1, "create_socket -> erreur de bind", strerror(errno));
        errno = rc;
        return -2;
    }
    rc = getsockname(s, (struct sockaddr *)&server, &server_len);
    if (rc < 0)
    {
        close(s);
        rc = errno;
        debug(D_CONTROL, 1, "create_socket -> erreur getsockname", strerror(errno));
        errno = rc;
        return -3;
    }
    debug_int(D_CONTROL, 0, "create_socket -> port", ntohs(server.sin6_port));
    rc = fcntl(s, F_GETFL);
    if (rc < 0)
    {
        close(s);
        debug(D_CONTROL, 1, "create_socket", "recupération des modes de la socket impossible");
        return -4;
    }
    rc = fcntl(s, F_SETFL, rc | O_NONBLOCK);
    if (rc < 0)
    {
        close(s);
        debug(D_CONTROL, 1, "create_socket", "changement des modes de la socket impossible");
        return -4;
    }
    rc = setsockopt(s, IPPROTO_IPV6, IPV6_RECVPKTINFO, &one, sizeof(one));
    if (rc < 0)
    {
        close(s);
        debug(D_CONTROL, 1, "create_socket", "changement des options de la socket impossible pour IPV6_RECVPKTINFO");
        return -4;
    }
    g_socket = s;
    return 0;
}

/**
 * @brief On lance la boucle d'itérations principale du programme.
 * 
 */
void launch_program()
{
    int rc = 0;
    int nb_fd = 0;
    struct timespec zero = {0, 0};
    struct timespec tm = {0};
    int count = 0;
    while (run)
    {
        fd_set readfds;
        fd_set writefds;
        FD_ZERO(&readfds);
        FD_ZERO(&writefds);
        FD_SET(g_socket, &readfds);
        FD_SET(STDIN_FILENO, &readfds);

        int tmp = 0;
        if (!buffer_is_empty())
            FD_SET(g_socket, &writefds);
        get_nexttime(&tm);
        if ((nb_fd = pselect(g_socket + 1, &readfds, &writefds, NULL, &tm, NULL)) > 0)
        {
            if (FD_ISSET(STDIN_FILENO, &readfds))
            {
                int code = handle_input();
                if (code == 1)
                    run = false;
            }

            if (FD_ISSET(g_socket, &readfds))
            {
                rc = (int)read_msg();
                tmp = (!tmp) ? errno : tmp;

                if (rc < 0)
                    debug(D_CONTROL, 1, "launch_program", "message non lu -> lancer debug reader pour savoir");
                else
                    debug_int(D_CONTROL, 0, "launch_program -> taille lue et interprétée", rc);
            }
            if (FD_ISSET(g_socket, &writefds))
            {
                rc = send_buffer_tlv();
                tmp = (!tmp) ? errno : tmp;

                if (!rc)
                    debug(D_CONTROL, 1, "launch_program", "message non envoyé");
                else
                    debug(D_CONTROL, 0, "launch_program", "envoie d'un message");
            }
        }
        tmp = (!tmp && errno != ENOENT) ? errno : tmp;
        if ((tmp == ENETDOWN || tmp == ENETRESET || tmp == ENETUNREACH || tmp == ENONET) && count < MAX_NETWRK_LOOP)
        {
            char ch[] = "nombre de boucles restant : [0]";
            snprintf(ch, strlen(ch) + 1, "nombre de boucles restant : [%d]", MAX_NETWRK_LOOP - count);

            set_in_red();
            wprintw(get_panel(), "[intel] Try reach network : tour restant %i\n", MAX_NETWRK_LOOP - count);
            restore();

            debug(D_CONTROL, 1, "launch_program -> problème de réseau", ch);
            sleep(1);
            count += 1;
            continue;
        }
        if (nb_fd < 0 || tmp != 0)
        {
            debug(D_CONTROL, 1, "launch_program -> problem du pselect", strerror(errno));
            return;
        }
        if (compare_time(tm, zero) <= 0 || nb_fd == 0)
        {
            if (launch_flood())
                debug(D_CONTROL, 0, "launch_program", "inondation");
            else
                debug(D_CONTROL, 1, "launch_program", "problème d'inondation");
        }
        count = 0;
    }
}

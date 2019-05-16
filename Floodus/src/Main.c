/**
 * @file Main.c
 * @author Floodus
 * @brief Fichier s'occupant de l'exécutation du programme
 * 
 */

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <netdb.h>
#include <errno.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <time.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <signal.h>

#include "TLV.h"
#include "debug.h"
#include "voisin.h"
#include "writer.h"
#include "reader.h"
#include "send_thread.h"
#include "controller.h"
#include "view.h"
#include "make_demand.h"

#define D_MAIN 1

/**
 * @brief
 * En cas de ctrl+c stoppe l'ensemble du
 * programme et free les structures
 */
static void sig_int(int sig)
{
    if (sig == SIGINT)
    {
        stop_program();
    }
}

/**
 * @brief Fonction initialisant tout.
 * 
 */
static void initializer(void)
{
    int rc = init_sender();
    if (!rc)
    {
        debug(D_MAIN, 1, "initializer", "initialisation des threads impossible");
        exit(1);
    }
    rc = init_neighbours();
    if (!rc)
    {
        debug(D_MAIN, 1, "initializer", "initialisation des neighbours impossible");
        destroy_thread();
        exit(1);
    }
    rc = init_ancillary();
    if (!rc)
    {
        debug(D_MAIN, 1, "initializer", "initialisation des ancillaires impossible");
        destroy_thread();
        free_neighbours();
        exit(1);
    }
    rc = init_big_data();
    if (!rc)
    {
        debug(D_MAIN, 1, "initializer", "initialisation des big_data impossible");
        destroy_thread();
        free_neighbours();
        free_ancillary();
        exit(1);
    }
    signal(SIGINT, sig_int);
    handle_input();
}

/**
 * @brief Fonction faisant le ménage à la fin du programme.
 * 
 */
static void finisher(void)
{
    debug(D_MAIN, 0, "finisher", "finish");
#ifndef D_LOGFILE
    if (DEBUG)
        sleep(30);
#endif
    int rc = 1;
    leave_network();
    destroy_thread();
    while (!buffer_is_empty() && rc)
    {
        rc = send_buffer_tlv();
    }
    free_ancillary();
    free_big_data();
    free_neighbours();
    free_inondation();
    free_writer();
    close_sock();
#ifndef D_LOGFILE
    if (DEBUG)
        sleep(30);
#endif
    end_graph();
}

/**
 * @brief initialisation du serveur.
 * Le commande de lancement peut prendre 2 arguments.
 * Si les deux arguments sont présents simultanémant :
 * le premier argument correspondra au nom dns de la destination
 * le deuxième argument correspondra au port de la destination
 * 
 * @param argc nombre d'arguments de la commande
 * @param argv tableau des arguments de la commande
 * @return int valeur de retour
 */
int main(int argc, char *argv[])
{
    srand(time(NULL));
    int id_file = rand();
    log_load(id_file);
    init_graph();
    char *port = "1212";
    char *default_dest = "jch.irif.fr";
    if (argc >= 3)
    {
        default_dest = argv[1];
        port = argv[2];
    }
    printf("%s - %s - %s\n", argv[0], default_dest, port);
    int rc = create_socket(0);
    if (rc < 0)
    {
        if (rc == -1)
            perror("Main -> Erreur de création de socket ");
        if (rc == -2)
            perror("Main -> Erreur de bind ");
        if (rc == -3)
            perror("Main -> Erreur de récupération des informations ");
        if (rc == -4)
            perror("Main -> Modification des modes de la socket impossible ");
        printf("Main : Problème de connexion");
        exit(1);
    }
    initializer();
    rc = send_hello(default_dest, port);
    if (rc >= 0)
    {
        launch_program();
    }
    finisher();
    log_close(id_file);
    return 0;
}

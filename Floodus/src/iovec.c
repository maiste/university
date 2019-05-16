/**
 * @file iovec.c
 * @author Floodus
 * @brief Module contenant toutes les fonctionnalités sur la manipulation des struct iovec
 * 
 */

#include "iovec.h"

/**
 * @brief Fonction pour libérer la mémoire d'une struct iovec et de son contenu.
 * 
 * @param data pointeur vers la struct iovec à libérer
 */
void freeiovec(data_t *data)
{
    if (data == NULL)
    {

        debug(D_IOVEC, 1, "freeiovec", "data : (null)");
        return;
    }
    free(data->iov_base);
    free(data);
    debug(D_IOVEC, 0, "freeiovec", "libération de la mémoire");
}

/**
 * @brief Creer une structure iovec contenant une copie de ses arguments.
 * 
 * @param content contenu de la struct iovec
 * @param content_len taille du contenu
 * @return data_t* renvoie NULL si on a un problème de malloc, une struct iovec contenant les arguments sinon.
 */
data_t *create_iovec(void *content, size_t content_len)
{
    data_t *data = malloc(sizeof(data_t));
    if (data == NULL)
    {
        debug(D_IOVEC, 1, "create_iovec", "problème de malloc pour data");
        return NULL;
    }
    memset(data, 0, sizeof(data_t));
    void *ncontent = malloc(content_len);
    if (ncontent == NULL)
    {
        debug(D_IOVEC, 1, "create_iovec", "problème de malloc pour ncontent");
        free(data);
        return NULL;
    }
    memmove(ncontent, content, content_len);
    data->iov_base = ncontent;
    data->iov_len = content_len;
    debug(D_IOVEC, 0, "create_iovec", "création de la struct iovec");
    return data;
}

/**
 * @brief On crée une copie de la struct iovec et de son contenu donnée en paramêtre
 * 
 * @param data struct iovec à copier
 * @return struct iovec* copie du paramêtre
 */
data_t *copy_iovec(data_t *data)
{
    if (data == NULL)
    {
        debug(D_IOVEC, 1, "copy_iovec", "data : (null)");
        return NULL;
    }
    data_t *copy = create_iovec(data->iov_base, data->iov_len);
    if (copy == NULL)
    {
        debug(D_IOVEC, 1, "copy_iovec", "problem de creation de la copie");
        return NULL;
    }
    debug(D_IOVEC, 0, "copy_iovec", "renvoie de la copie");
    return copy;
}

/**
 * @brief Fonction de comparaison du contenu de deux struct iovec.
 * 
 * @param data1 donnée 1
 * @param data2 donnée 2
 * @return int Renvoie un entier inférieur, égal, ou supérieur à zéro, si data1 est respectivement inférieure, égale ou supérieur à data2.  
 */
int compare_iovec(data_t *data1, data_t *data2)
{
    if (data1 == NULL && data2 == NULL)
    {
        debug_int(D_IOVEC, 0, "compare_iovec : deux structures NULL", 0);
        return 0;
    }
    if (data1 == NULL)
    {
        debug_int(D_IOVEC, 0, "compare_iovec : data1 NULL", -1);
        return -1;
    }
    if (data2 == NULL)
    {
        debug_int(D_IOVEC, 0, "compare_iovec : data2 NULL", 1);
        return 1;
    }
    if (data1->iov_len < data2->iov_len)
    {
        debug_int(D_IOVEC, 0, "compare_iovec : taille de data1 inférieure", -1);
        return -1;
    }
    if (data2->iov_len < data1->iov_len)
    {
        debug_int(D_IOVEC, 0, "compare_iovec : taille de data2 inféreieure", 1);
        return 1;
    }
    int res = memcmp(data1->iov_base, data2->iov_base, data1->iov_len);
    debug_int(D_IOVEC, 0, "compare_iovec : comparaison de bit", res);
    return res;
}

/**
 * @brief Afficher le contenu d'une struct iovec.
 * 
 * @param data Struct iovec à afficher.
 */
void print_iovec(data_t *data)
{
    printf("Size iovec : %ld\nContent : ", data->iov_len);
    for (size_t i = 0; i < data->iov_len; i++)
    {
        printf("%.2x ", ((u_int8_t *)data->iov_base)[i]);
    }
    printf("\n");
}

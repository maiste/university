/*
 * @brief
 * Fichier fournissant des fonctions pour
 * récupérer les données d'une pthread_var_t
 *
 * @author Floodus
 */

#include "pthread_var.h"

/**
 * @brief
 * Bloque le mutex de la structure
 *
 * @param g_lock variable contenant le mutex
 * 
 * @return bool_t '1' si tout s'est bien passé, '0' sinon
 */
bool_t lock(pthread_var_t *g_lock)
{
  int rc = 0;
  rc = pthread_mutex_lock(&g_lock->locker);
  if (rc)
  {
    debug(D_PTHREAD, 1, "lock -> rc", strerror(rc));
    return false;
  }
  return true;
}

/**
 * @brief
 * Débloque le mutex de la structure
 *
 * @param g_lock variable contenant le mutex
 * 
 * @return bool_t '1' si tout s'est bien passé, '0' sinon 
 */
bool_t unlock(pthread_var_t *g_lock)
{
  int rc = 0;
  rc = pthread_mutex_unlock(&g_lock->locker);
  if (rc)
  {
    debug(D_PTHREAD, 1, "unlock -> rc", strerror(rc));
    return false;
  }
  return true;
}

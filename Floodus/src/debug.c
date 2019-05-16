/**
 * @file debug.c
 * @author Floodus
 * @brief Module implémentant toutes les fonctions de debug
 * 
 */

#include "debug.h"

#ifdef D_LOGFILE
/**
 * @brief File descripteur du fihcier de log
 * 
 */
int g_fd_log = 2;

#endif

/**
 * @brief Fonction de chargement du fichier de log
 * 
 * @return bool_t '1' si le chargement a été effectué, '0' sinon
 */
bool_t log_load(int id)
{
  (void)id;
#ifdef D_LOGFILE
  int fd = open(FILE_LOG, O_WRONLY | O_CREAT | O_APPEND, 0777);
  if (fd < 0)
  {
    perror("log_load -> problème de chargement du fichier log");
    return false;
  }
  char str[] = "\nDébut de log [0000000000]\n";
  snprintf(str, strlen(str) + 1, "\nDébut de log [%i]\n", id);
  ssize_t res = write(fd, str, strlen(str));
  if (res < 0)
  {
    perror("log_load -> problème d'écriture dans le fichier log");
    close(fd);
    return false;
  }
  g_fd_log = fd;
#endif
  return true;
}

/**
 * @brief Fonction de cloture du fichier de log
 * 
 */
void log_close(int id)
{
#ifndef D_LOGFILE
  (void)id;
  return;
#else
  char str[] = "Fin de log [0000000000]\n";
  snprintf(str, strlen(str) + 1, "Fin de log [%i]\n", id);
  write(g_fd_log, str, strlen(str));
  close(g_fd_log);
#endif
}

/**
 * @brief
 * Message de debug général
 * @param flag flag de debug de la fonction appelante
 * @param error permet de marquer une erreur
 * @param name le nom de la constante ou la chaine vide
 * @param msg le message à afficher
 */
void debug(uint8_t flag, uint8_t error, char *name, const char *msg)
{
#ifndef D_LOGFILE
  if (DEBUG && flag)
  {
    if (error)
      set_in_red();
    else
      set_in_green();

    wprintw(get_panel(), "[Debug] Str %s : %s\n", name, msg);
    restore();
  }
#else
  if (DEBUG && flag)
  {
    char s[] = "Erreur";
    if (!error)
    {
      s[0] = 'V';
      s[1] = '\0';
    }
    dprintf(g_fd_log, "[%s] Str %s : %s\n", s, name, msg);
  }
#endif
}

/**
 * @brief
 * Message de debug général et quitte
 * @param flag flag de debug de la fonction appelante
 * @param error permet de marquer une erreur
 * @param name le nom de la chaine
 * @param msg le message
 * @param exit_code le code d'erreur
 */
void debug_and_exit(uint8_t flag, uint8_t error, char *name, const char *msg, int exit_code)
{
#ifndef D_LOGFILE
  if (DEBUG && flag)
  {
    if (error)
      set_in_red();
    else
      set_in_green();
    wprintw(get_panel(), "[Debug] Str %s : %s\n", name, msg);
    restore();
    exit(exit_code);
  }
#else
  if (DEBUG && flag)
  {
    char s[] = "Erreur";
    if (!error)
    {
      s[0] = 'V';
      s[1] = '\0';
    }
    dprintf(g_fd_log, "[%s] Str %s : %s\n", s, name, msg);
    exit(exit_code);
  }
#endif
}

/**
 * @brief
 * Affiche une requête sous forme hexadécimale
 * @param flag flag de debug de la fonction appelante
 * @param error permet de marquer une erreur
 * @param name le nom de la requête
 * @param data le tableau d'octets
 * @param data_len la taille des données
 */
void debug_hex(uint8_t flag, uint8_t error, char *name, void *data, int data_len)
{
#ifndef D_LOGFILE
  if (DEBUG && flag)
  {
    if (error)
      set_in_red();
    else
      set_in_green();

    wprintw(get_panel(), "[Debug] Hexa %s : ", name);
    for (int i = 0; i < data_len; i++)
    {
      wprintw(get_panel(), "%.2x ", ((uint8_t *)data)[i]);
    }
    wprintw(get_panel(), "\n");
    restore();
  }
#else
  if (DEBUG && flag)
  {
    char s[] = "Erreur";
    if (!error)
    {
      s[0] = 'V';
      s[1] = '\0';
    }
    dprintf(g_fd_log, "[%s] Hexa %s : ", s, name);
    for (int i = 0; i < data_len; i++)
    {
      dprintf(g_fd_log, "%.2x ", ((uint8_t *)data)[i]);
    }
    dprintf(g_fd_log, "\n");
  }
#endif
}

/**
 * @brief
 * Affiche une requête sous forme hexadécimale et quitte
 * @param flag flag de debug de la fonction appelante
 * @param error permet de marquer une erreur 
 * @param name le nom de la requête
 * @param data le tableau d'octets
 * @param data_len la taille des données
 * @param exit_code le code d'erreur
 */
void debug_hex_and_exit(uint8_t flag, uint8_t error, char *name, void *data, int data_len, int exit_code)
{
#ifndef D_LOGFILE
  if (error)
    set_in_red();
  else
    set_in_green();

  if (DEBUG && flag)
  {
    wprintw(get_panel(), "[Debug] Hexa %s : ", name);
    for (int i = 0; i < data_len; i++)
    {
      wprintw(get_panel(), "%.2x ", ((uint8_t *)data)[i]);
    }
    wprintw(get_panel(), "\n");
    restore();
    exit(exit_code);
  }
#else
  if (DEBUG && flag)
  {
    char s[] = "Erreur";
    if (!error)
    {
      s[0] = 'V';
      s[1] = '\0';
    }
    dprintf(g_fd_log, "[%s] Hexa %s : ", s, name);
    for (int i = 0; i < data_len; i++)
    {
      dprintf(g_fd_log, "%.2x ", ((uint8_t *)data)[i]);
    }
    dprintf(g_fd_log, "\n");
    exit(exit_code);
  }
#endif
}

/**
 * @brief
 * Affiche un int
 * @param flag flag de debug de la fonction appelante
 * @param error permet de marquer une erreur
 * @param name le nom de l'int
 * @param rc l'int à afficher
 */
void debug_int(uint8_t flag, uint8_t error, char *name, int rc)
{
#ifndef D_LOGFILE
  if (DEBUG && flag)
  {
    if (error)
      set_in_red();
    else
      set_in_green();

    wprintw(get_panel(), "[Debug] Int %s: %d\n", name, rc);
    restore();
  }
#else
  if (DEBUG && flag)
  {
    char s[] = "Erreur";
    if (!error)
    {
      s[0] = 'V';
      s[1] = '\0';
    }
    dprintf(g_fd_log, "[%s] Int %s: %d\n", s, name, rc);
  }
#endif
}

/**
 * @brief
 * Affiche un int et quitte
 * @param flag flag de debug de la fonction appelante
 * @param error permet de marquer une erreur
 * @param name le nom de l'int
 * @param rc l'int
 * @param exit_code le code d'erreur
 */
void debug_int_and_exit(uint8_t flag, uint8_t error, char *name, int rc, int exit_code)
{
#ifndef D_LOGFILE
  if (DEBUG && flag)
  {
    if (error)
      set_in_red();
    else
      set_in_green();

    wprintw(get_panel(), "[Debug] Int %s : %d\n", name, rc);
    restore();
    exit(exit_code);
  }
#else
  if (DEBUG && flag)
  {
    char s[] = "Erreur";
    if (!error)
    {
      s[0] = 'V';
      s[1] = '\0';
    }
    dprintf(g_fd_log, "[%s] Int %s : %d\n", s, name, rc);
    exit(exit_code);
  }
#endif
}

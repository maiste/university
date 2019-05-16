#include "make_demand.h"
#include "inondation.h"
#include "controller.h"
#include "view.h"

static char buf[BUF_LEN];
static int pos = 0;

static char surname[MAX_SURNAME];
static int sur_len = MAX_SURNAME;

static WINDOW *top, *bottom, *top_panel, *bot_panel;

/**
 * ## Aide ##
 */

WINDOW *get_panel()
{
  return top_panel;
}

void set_in_red()
{
  wattron(top_panel, COLOR_PAIR(RED_COL));
}

void set_in_green()
{
  wattron(top_panel, COLOR_PAIR(GREEN_COL));
}

void set_in_blue()
{
  wattron(top_panel, COLOR_PAIR(BLUE_COL));
}

void restore()
{
  wattroff(top_panel, COLOR_PAIR(RED_COL));
  wattroff(top_panel, COLOR_PAIR(GREEN_COL));
  wattroff(top_panel, COLOR_PAIR(BLUE_COL));
  wrefresh(top_panel);
}

/**
 *  ## Gestion du buffer ##
 */

/**
 * @brief
 * Remet le buffer à vide
 */
static void empty_buff()
{
  memset(buf, 0, BUF_LEN);
  pos = 0;
}

/**
 * @brief
 * Récupère l'entrée du buffer
 * @param max la taille max que l'utilisateur peut
 * envoyer
 * @return 1 en cas de \n
 */
static int get_input(int max)
{
  char c;
  if ((c = getch()) != ERR)
  {
    if (pos < max && isprint(c))
    {
      buf[pos] = c;
      pos++;
    }
    else if (pos != 0 && c == '\n')
    {
      buf[pos] = '\0';
      return 1;
    }
    else if (c == 7)
    {
      pos = (pos - 1 >= 0) ? pos - 1 : 0;
      buf[pos] = '\0';
    }
  }
  return 0;
}

/**
 * @brief Fonction d'affichage d'un message donnée en argument. Cette fonction n'utilise pas de fonctions de debug.
 * 
 * @param content contenu du message (peut ne pas se terminer par '\0')
 * @param content_len taille du contenu
 */
void print_data(u_int8_t *content, int content_len)
{
  uint8_t *display_msg = malloc(content_len + 1);
  if (display_msg == NULL)
  {
    stop_program();
    return;
  }
  memset(display_msg, 0, content_len + 1);
  memcpy(display_msg, content, content_len);

  wprintw(top_panel, "%s\n", display_msg);

  wrefresh(top_panel);
  free(display_msg);
}

/**
 * @brief 
 * Affiche le buffer
 */
static void print_buff()
{
  werase(bot_panel);
  wprintw(bot_panel, "> %s", buf);
  wrefresh(bot_panel);
}

/**
 * @brief
 * Envoie le buffer
 */
static void send_buffer()
{
  int content_len = pos + sur_len + 3;
  uint8_t *content = malloc(content_len);
  if (content == NULL)
    return;
  memcpy(content, surname, sur_len);
  content[sur_len] = ':';
  content[sur_len + 1] = ' ';
  memcpy(content + sur_len + 2, buf, pos);
  set_in_blue();
  print_data(content, content_len - 1);
  restore();
  add_my_message(content, content_len - 1);
  free(content);
}

/**
 * @brief
 * Set surname
 */
static void set_surname()
{
  char *new_surname = buf + 3;
  int size = (strlen(new_surname) < MAX_SURNAME) ? strlen(new_surname) : (MAX_SURNAME - 1);
  memset(surname, 0, sur_len);
  memcpy(surname, new_surname, size);
  sur_len = size;
}

/**
 * @brief
 * Affiche les interfaces et leur ip4 ou 6
 */
static void print_my_ips()
{
  struct ifaddrs *ifaddr, *p_ifaddr;
  struct sockaddr_in6 sin6 = {0};
  struct sockaddr_in sin4 = {0};
  char ip[INET6_ADDRSTRLEN] = {0};

  getifaddrs(&p_ifaddr);
  ifaddr = p_ifaddr;
  while (ifaddr != NULL)
  {
    if (ifaddr->ifa_addr == NULL)
    {
      ifaddr = ifaddr->ifa_next;
      continue;
    }
    if (ifaddr->ifa_addr->sa_family == AF_INET6)
    {
      sin6 = *((struct sockaddr_in6 *)ifaddr->ifa_addr);
      getnameinfo((struct sockaddr *)&sin6,
                  sizeof(struct sockaddr_in6), ip,
                  INET6_ADDRSTRLEN, NULL, 0, NI_NUMERICHOST);

      wprintw(top_panel, "   |- %s => ip6: %s\n", ifaddr->ifa_name, ip);
    }
    else if (ifaddr->ifa_addr->sa_family == AF_INET)
    {
      sin4 = *((struct sockaddr_in *)ifaddr->ifa_addr);
      getnameinfo((struct sockaddr *)&sin4,
                  sizeof(struct sockaddr_in), ip,
                  INET6_ADDRSTRLEN, NULL, 0, NI_NUMERICHOST);

      wprintw(top_panel, "   |- %s => ip4: %s\n", ifaddr->ifa_name, ip);
    }

    ifaddr = ifaddr->ifa_next;
  }
  freeifaddrs(p_ifaddr);
}

/**
 * @brief
 * Print intel about the system
 */
static void print_intel()
{
  struct sockaddr_in6 sin6 = {0};
  uint32_t size = sizeof(struct sockaddr_in6);
  if (getsockname(g_socket, (struct sockaddr *)&sin6, &size) == 0)
  {
    set_in_blue();
    wprintw(top_panel, "[info]\n|-ip\n");
    print_my_ips();
    wprintw(top_panel, "|-port => %u\n", ntohs(sin6.sin6_port));
    wprintw(top_panel, "|-surname => %s\n", surname);
    restore();
  }

  restore();
}

/**
 * @brief
 * Renvoie la commande à faire 
 */
static int handle_cmd()
{
  if (pos > 1 && buf[0] == '/')
  {
    if (buf[1] == 'q')
    {
      return 1;
    }
    else if (buf[1] == 'c')
    {
      set_in_blue();
      wprintw(top_panel, "[connect] %s\n", buf + 3);
      if (!try_connect_pair(buf + 3))
      {
        set_in_red();
        wprintw(top_panel, "[error] can't reach peer\n");
      }
      restore();
      return 2;
    }
    else if (buf[1] == 's')
    {
      set_surname();
      set_in_blue();
      wprintw(top_panel, "[surname] %s\n", surname);
      restore();
      return 2;
    }
    else if (buf[1] == 'i')
    {
      print_intel();
      return 2;
    }
  }
  return 0;
}

/**
 * @brief
 * Gestion de l'entrée
 * @return 
 */
int handle_input()
{
  int max = BUF_LEN - sur_len - 3;
  if (get_input(max))
  {
    int code = handle_cmd();
    if (code)
    {
      empty_buff();
      print_buff();
      return code;
    }
    else
    {
      send_buffer();
      empty_buff();
    }
  }
  print_buff();
  return 0;
}

/**
   ## Memoire ##
 */

/**
 * @brief
 * Initialise le module graphique
 */
void init_graph()
{
  initscr(); // Lance ncurses
  noecho();  // Ne duplique pas les touches
  cbreak();  // Vide le tampon automatiquement

  top = subwin(stdscr, LINES * 3 / 4, COLS, 0, 0);
  top_panel = subwin(top, (LINES * 3 / 4) - 2, COLS - 2, 1, 1);
  bottom = subwin(stdscr, (LINES / 4), COLS, (LINES * 3 / 4), 0);
  bot_panel = subwin(bottom, (LINES / 4) - 2, COLS - 2, (LINES * 3 / 4) + 1, 1);

  box(top, ACS_VLINE, ACS_HLINE);
  box(bottom, ACS_VLINE, ACS_HLINE);

  nodelay(stdscr, TRUE);
  scrollok(top_panel, TRUE);
  scrollok(bot_panel, TRUE);
  curs_set(0);
  keypad(stdscr, TRUE);

  start_color();
  use_default_colors();
  init_pair(COL, COLOR_WHITE, COLOR_BLACK);
  init_pair(RED_COL, COLOR_RED, COLOR_BLACK);
  init_pair(GREEN_COL, COLOR_GREEN, COLOR_BLACK);
  init_pair(BLUE_COL, COLOR_BLUE, COLOR_BLACK);

  sur_len = 6;
  memcpy(surname, "Nobody", sur_len);

  wrefresh(top);
  wrefresh(bottom);
  wrefresh(top_panel);
  wrefresh(bot_panel);

  empty_buff();
}

/**
 * @brief
 * Nettoie la mémoire graphique
 */
void end_graph()
{
  delwin(top);
  delwin(top_panel);
  delwin(bottom);
  delwin(bot_panel);
  endwin();
  reset_shell_mode();
}

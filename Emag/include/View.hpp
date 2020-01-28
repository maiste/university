/*
 * Interface View
 * Betend - Marais
 */

#include <sstream>

#include "std_h.hpp"
#include "Move.hpp"
#include "Plateau.hpp"

#ifndef VIEW
#define VIEW

class View {
public:
  virtual ~View (){};
  virtual void display_header(void) const = 0;
  virtual string ask_name(void) const = 0;
  virtual void display_board(Plateau* p) const = 0;
  virtual void display_str_board(vector<string>& p) = 0;
  virtual Move get_user_input(void) const = 0;
  virtual void level_up(int level) const = 0;
  virtual void game_over(int score) const = 0;
  virtual void win(int score) const = 0;
  virtual int ask_height(void) const = 0;
  virtual int ask_width(void) const = 0;
  virtual Position ask_position(void) const = 0;
  virtual char new_element(void) const = 0;
  virtual void fail_teupor(void) const = 0;
  virtual void fail_modif(void) const = 0;
  virtual string want_save(void) const = 0;
 };

#endif // View

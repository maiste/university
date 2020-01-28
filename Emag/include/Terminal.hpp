/*
 * Terminal
 * Betend - Marais
 */

#include "std_h.hpp"
#include "View.hpp"

#ifndef TERMINAL
#define TERMINAL

class Terminal : public View {
public:
  virtual void display_header(void) const;
  virtual string ask_name(void) const;
  virtual void display_board(Plateau* p) const;
  virtual void display_str_board(vector<string>& p);
  virtual Move get_user_input(void) const;
  virtual void level_up(int level) const;
  virtual void game_over(int score) const;
  virtual void win(int score) const;
  virtual int ask_height(void) const;
  virtual int ask_width(void) const;
  virtual Position ask_position(void) const;
  virtual char new_element(void) const;
  virtual void fail_teupor(void) const;
  virtual void fail_modif(void) const;
  virtual string want_save(void) const;
};

#endif // TERMINAL

/*
 * Curse view
 * Betend - Marais
 */

#include <ncurses.h>

#include "std_h.hpp"
#include "View.hpp"

#ifndef CURSE
#define CURSE

class Curse : public View {
  private:
    int h;
    int w;
    void clear_line(int y, int x, int size) const;
    void print_mid_str(string str) const;
    void print_mid_str(string start, string stop, int n) const;
    string ask_str(string question, string prompt, int y, int x) const;
    int ask_int(string question, int bound, int y, int x) const;

  public:
    Curse();
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
    virtual ~Curse();
};

#endif // CURSE

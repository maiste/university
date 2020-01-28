/*
 * Plateau
 * Betend - Marais
 */

#include <cstdlib>
#include <ctime>
#include <iostream>
#include <vector>
#include <string>

#include "std_h.hpp"
#include "Oueurj.hpp"
#include "Move.hpp"
#include "Case.hpp"
#include "NormalStreumon.hpp"
#include "LineStreumon.hpp"
#include "ColumnStreumon.hpp"
#include "RandomStreumon.hpp"
#include "Reumus.hpp"
#include "Diams.hpp"
#include "Empty.hpp"
#include "Geuchar.hpp"
#include "Teupor.hpp"
#include "CaseError.hpp"
#include "SizeError.hpp"
#include "MissingPlayer.hpp"

#ifndef PLATEAU
#define PLATEAU

class Plateau {
  private:
    Oueurj* o;
    Position depart;
    int h;
    int w;
    vector< vector<Case *> > plateau;
    bool over;
    bool win;
    vector<Streumon *> stremons;
    Position random_position(void);
    Position geuchar_move(void);
    bool plateau_same_type(int x, int y, char t);
    void collision_lcstreumons(int i, int p_x, int p_y);
  public:
    Plateau(Oueurj*, vector<string> model, int h, int w);
    Plateau(Oueurj*, vector<string> model, int h, int w, bool save);
    virtual ~Plateau();
    vector< vector<Case *> >& get_plateau(void);
    bool move_player(Move m);
    void update_board(void);
    void init_player(void);

    int get_height(void);
    int get_width(void);
    string get_player_name(void);
    int get_player_diams(void);
    int get_player_geuchars(void);
    Case * get_case(int i, int j);

    bool is_finish(void);
    bool is_win(void);
    bool is_valide(Position p);
    static bool in_obj_list(char elt);

    void to_string_vector(vector<string>& exp);
};


#endif // PLATEAU

/*
 * Builder
 * Betend - Marais
 */

#include <vector>
#include <string>

#include "std_h.hpp"
#include "Oueurj.hpp"
#include "Streumon.hpp"
#include "FileManager.hpp"
#include "View.hpp"

#ifndef BUILDER
#define BUILDER

class Builder {
private:
  string filename;
  Oueurj o;
  int nb_teupors;
  int nb_diams;
  vector<string> board;

  void define_default(void);
  bool in_board(Position p);
  bool is_ext_wall(Position p);
  bool is_type(Position p, char type);

public:
  Builder(string path, unsigned int h, int w);
  Builder(string path , vector<string> model);
  void build(void);
  static void build_game(int size, char *argv[]);
  static Builder* create_board(string path, View * v);
  static Builder* generate_board(string path);
  bool add_teupor(Position p);
  bool add_diams(Position p);
  bool add_geuchar(Position p);
  bool add_reumu(Position p);
  bool add_stremon(Position p, int type);
  bool remove_case(Position p);
  bool move_player(Position p);
  void modify_board(View *v);
};

#endif /* protected header BUILDER */

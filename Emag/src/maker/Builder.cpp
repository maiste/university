/*
 * Builder
 * Betend - Marais
 */

#include "Builder.hpp"

bool Builder::in_board(Position p) {
  return (p.getX() >= 0 && p.getX() < (int)board[0].size() &&
          p.getY() >= 0 && p.getY() < (int)board.size());
}

bool Builder::is_ext_wall(Position p) {
  return (p.getX() == 0 || p.getX() == ((int)board[0].size() -1) ||
          p.getY() == 0 || p.getY() == ((int)board.size() - 1));
}

bool Builder::is_type(Position p, char type) {
  return board[p.getY()][p.getX()] == type;
}

void Builder::define_default() {
  // Move player at the middle
  Position m = Position((int)board[0].size() / 2,
                (int)board.size() / 2);
  o.move(m);
  board[m.getY()][m.getX()] = 'J';

  // Up Border
  for(int i = 0 ; i < (int)board[0].size() ; i++) {
    board[0][i] = '#';
  }
  // Left Border
  for (int i = 0 ; i < (int)board.size() ; i++) {
    board[i][0] = '#';
  }
  // Right Border

  for (int i = 0 ; i < (int)board.size() ; i++) {
    board[i][board[0].size() - 1] = '#';
  }
  // Down Border
  for (int i = 0 ; i < (int)board[0].size() ; i++) {
    board[board.size()-1][i] = '#';
  }
}

Builder::Builder(string path, unsigned int h, int w) :
  filename{path}, o{}, nb_teupors{0}, nb_diams{0},
  board{h, std::string{}}{
  for(int i = 0; i <(int)h; i++){
    for(int j = 0; j<w ; j++){
      board[i].push_back('.');
    }
  }
  define_default();
}

Builder::Builder(string path, vector<string> model): filename{path}, o{},
  nb_teupors{0}, nb_diams{0}, board{model} {
    for(int i = 0; i<(int)board.size(); ++i){
      for(int j=0; j< (int)board[0].size(); ++j){
        if(model[i][j] == '-') { nb_teupors ++; }
        if(model[i][j] == '$') { nb_diams ++; }
        if(model[i][j] == 'J') {
          Position p(j,i);
          o.move(p);
        }
      }
    }
}

Builder* Builder::create_board (string path, View * v){
  int h = v->ask_height();
  int w = v->ask_width();
  Builder* b =  new Builder (path, h, w);
  return b;
}

Builder* Builder::generate_board (string path){
  FileManager f (path,0);
  Builder *b = new Builder(path, f.generate_model());
  return b;
}

void Builder::build() {
  FileManager f(filename, 3);
  f.write_plateau(board);
}

void Builder::build_game(int size, char* args[]) {
  if(FileManager::get_extension(args[1]) != GAME) {
    throw UnknownFile();
  }

  // Check if it's the right extension
  for (int i = 2 ; i < size ; i++) {
    if(FileManager::get_extension(args[i]) != BOARD) {
      throw UnknownFile();
    }
  }

  // Add boards
  FileManager head(args[1], 3);
  head.write_head(size-2);
  for (int i = 2 ; i < size ; i++) {
    FileManager f(args[i], args[1]);
    f.copy_in_to_out();
  }

}

bool Builder::add_teupor(Position p) {
  if(in_board(p) && is_ext_wall(p) && is_type(p, '#')) {
    board[p.getY()][p.getX()] = '-';
    nb_teupors++;
    return true;
  }
  return false;
}

bool Builder::add_diams(Position p) {
  if(in_board(p) && !is_ext_wall(p) && is_type(p, '.')) {
    board[p.getY()][p.getX()] = '$';
    nb_diams++;
    return true;
  }
  return false;
}

bool Builder::add_geuchar(Position p) {
  if(in_board(p) && !is_ext_wall(p) && is_type(p, '.')) {
    board[p.getY()][p.getX()] = '*';
    return true;
  };
  return false;
}

bool Builder::add_reumu(Position p) {
  if(in_board(p) && is_type(p, '.')) {
    board[p.getY()][p.getX()] = '#';
    return true;
  };
  return false;
}

bool Builder::remove_case(Position p) {
  if(in_board(p)) {
    if(is_ext_wall(p)) {
      if(is_type(p,'-')) {
        nb_teupors--;
        board[p.getY()][p.getX()] = '#';
      } else {
        return false;
      }
    } else if(is_type(p, 'J')){
      return false;
    } else if(is_type(p, '$')){
      nb_diams--;
      board[p.getY()][p.getX()] = '.';
    } else {
      board[p.getY()][p.getX()] = '.';
    }
    return true;
  }
  return false;
}

bool Builder::add_stremon(Position p, int type) {
  if(in_board(p) && !is_ext_wall(p) && is_type(p, '.')) {
    if(type == 0) {
      board[p.getY()][p.getX()] = '~';
    } else if (type == 1) {
      board[p.getY()][p.getX()] = 'l';
    } else if (type == 2) {
      board[p.getY()][p.getX()] = 'c';
    } else if (type == 3) {
      board[p.getY()][p.getX()] = 'r';
    } else {
      return false;
    }
    return true;
  }
  return false;
}

bool Builder::move_player(Position p) {
  if(in_board(p) &&  !is_ext_wall(p) && is_type(p, '.')) {
    Position po = o.get_position();
    board[po.getY()][po.getX()] = '.';
    o.move(p);
    board[p.getY()][p.getX()] = 'J';
    return true;
  }
  return false;
}


void Builder::modify_board(View *v){
  bool h = false;
  char elem;
  while (true) {
    v->display_str_board(board);
    elem = v->new_element();
    if(elem == 'q') {
      if(nb_teupors == 0 || nb_teupors > nb_diams) {
        v->fail_teupor();
        continue;
      } else { break; }
    }
    Position p = v->ask_position();
    switch (elem) {
      case 'J':
        h = move_player(p);
        break;
      case '~' :
        h = add_stremon(p, 0);
        break;
      case 'l':
        h = add_stremon(p, 1);
        break;
      case 'c' :
        h = add_stremon(p,2);
        break;
      case 'r':
        h = add_stremon(p, 3);
        break;
      case '#' :
        h = add_reumu(p);
        break;
      case '*':
        h = add_geuchar(p);
        break;
      case '$' :
        h = add_diams(p);
        break;
      case '-' :
        h = add_teupor(p);
        break;
      case '.' :
        h = remove_case(p);
        break;
    }
    if(!h)
      v->fail_modif();
  }
  build();
}

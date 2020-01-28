/*
 * Controller
 * Betend - Marais
 */

#include "Controller.hpp"

Controller::Controller(string path, View *v) : finalScore{0},level{1}, v{v},
  o{nullptr}, plateaux{} {
    o = new Oueurj{"Player"};
    FileManager f{path};
    if (f.get_extension(path) == SAVE){
       finalScore = f.get_score();
       level = f.get_level();
     }
    f.create_game(o, plateaux);
}

Controller::~Controller(){
  for (int i =0; i<(int) plateaux.size() ; i++)
    delete plateaux[i];
  delete o;
}

bool Controller::want_play(Move m) {
  return m != Move::LEAVE;
}


void Controller::set_view(View *v) {
  this->v = v;
}

bool Controller::play_one_level(Plateau* p) {
  p->init_player();
  bool run = true;

  while(run && !p->is_finish()) {
    Move m;
    do {
      v->display_board(p);
      m = v->get_user_input();
      run = want_play(m);
    } while(run && !p->move_player(m));
    if (run) {
      p->update_board();
    } else {
      finalScore = level * p->get_player_diams();
      save_game(p);
    }
  }

  return p->is_win();
}

void Controller::run_game(void) {
  v->display_header();
  o->set_name(v->ask_name());
  for (auto *p : plateaux){
    v->level_up(level);
    if(play_one_level(p)) {
      finalScore += (level * p->get_player_diams());
      level++;
    } else {
      v->game_over(finalScore);
      return;
    }
  }
  v->win(finalScore);
}

void Controller::save_game(Plateau * p){
    string path = v->want_save();
    if(path != ""){
      path += ".save";
      FileManager f{path,3};
      f.write_save(finalScore, level, plateaux, p);
  }
}

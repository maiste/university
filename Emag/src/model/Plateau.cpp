/*
 * Plateau
 * Betend - Marais
 */

#include "Plateau.hpp"

Plateau::Plateau(Oueurj* o, vector<string> model, int h, int w, bool verif) :
    o{o}, depart{-1,-1}, h{h}, w{w},
    plateau{
      (long unsigned int)h, vector<Case *> {(long unsigned int)w, nullptr}
    },
    over{false}, win{false}, stremons {} {
      vector<Teupor *> portes;
      vector<Diams *> diams;
      for (int i = 0 ; i <  h ; i++) {
          for (int j = 0 ; j < w ; j++) {

            switch (model[i][j]) {
              case '~' :
              {
                NormalStreumon * c = new NormalStreumon(j,i);
                plateau[i][j] = c;
                stremons.push_back(c);
                break;
              }
              case 'l': {
                LineStreumon* c = new LineStreumon(j,i);
                plateau[i][j] = c;
                stremons.push_back(c);
                break;
              }
              case 'c' : {
                ColumnStreumon* c = new ColumnStreumon(j,i);
                plateau[i][j] = c;
                stremons.push_back(c);
                break;
              }
              case 'r': {
                RandomStreumon* c = new RandomStreumon(j,i);
                plateau[i][j] = c;
                stremons.push_back(c);
                break;
              }
              case '.':
              {
                Empty* c = new Empty;
                plateau[i][j] = c;
                break;
              }
              case '$':
              {
                Diams* c = new Diams;
                plateau[i][j] = c;
                diams.push_back(c);
                break;
              }
              case '*':
              {
                Geuchar* c =  new Geuchar;
                plateau[i][j] = c;
                break;
              }
              case '#':
              {
                Reumus* c = new Reumus;
                plateau[i][j] = c;
                break;
              }
              case '-':
              {
                Teupor* c =  new Teupor;
                plateau[i][j] = c;
                portes.push_back(c);
                break;
              }
              case '+':
              {
                Teupor* c = new Teupor(true);
                plateau[i][j] = c;
                break;
              }
              case 'J':
              {
                depart = Position(j,i);
                plateau[i][j] = o;
                break;
              }
              default :
                throw CaseError();
            }

          }
      }
      if(depart.getX() == -1 || depart.getY()==-1)
        throw MissingPlayer();

      if(verif && (portes.size()>diams.size() || portes.size() == 0))
        throw SizeError();

      for(int i=0; i<(int)portes.size() && i<(int) diams.size(); i++){
        diams[i]->add_porte(portes[i]);
      }
}

Plateau::Plateau(Oueurj* o, vector<string> model, int h, int w): Plateau{o, model, h, w, true}{};

Position Plateau::random_position() {
  Position p = o->get_position();
  int x = (rand() % this->w) - p.getX();
  int y = (rand() % this->h) - p.getY();
  return Position(x,y);
}

Position Plateau::geuchar_move() {
  srand(time(nullptr));
  Position p = o->get_position();
  Position n = random_position();
  while(!is_valide(p+n)) {
    n = random_position();
  }
  o->consume_geuchar();
  return n;
}


Plateau::~Plateau(){
  for (int i = 0; i < h; i++)
    for (int j = 0; j < w ; j ++) {
      if (plateau[i][j]->type() != 'J') { delete plateau[i][j]; }
    }
}

// Moves the player if the move is valid
bool Plateau::move_player(Move m) {
  if (m != Move::GEUCHARS || o->nb_geuchars() != 0) {
    Position n = (m == Move::GEUCHARS) ? geuchar_move() : m;
    Position p = o->get_position() + n;
    if(is_valide(p)) {
      if(plateau[p.getY()][p.getX()]->type() == '+') {
        win = true;
        over = true;
      } else if(plateau[p.getY()][p.getX()]->type() == '*') {
        o->add_geuchar();
      } else if(plateau[p.getY()][p.getX()]->type() == '$') {
        o->add_diams();
      }

      delete plateau[p.getY()][p.getX()];
      plateau[o->get_position().getY()][o->get_position().getX()] = new Empty;
      o->move(p);
      plateau[p.getY()][p.getX()] = o;
      return true;
    }
  }
  return false;
}

bool Plateau::plateau_same_type(int x,int y,char t) {
 return plateau[y][x]->type() == t;
}

// Updates the board by moving the Streumons
void Plateau::update_board(void) {

  for(int i = 0; i < (int) stremons.size(); i++){
    vector<Move> coups;
    Streumon * s = stremons[i];
    int x = s->get_pos().getX();
    int y = s->get_pos().getY();
    s->potential_move(coups, o->get_position());

    for(auto l=coups.begin(); l!=coups.end(); ++l){
      Position p = s->get_pos() + *l;
      int p_x = p.getX();
      int p_y = p.getY();

      if(plateau[p_y][p_x]->type() == 'J') {
        over = true;
        return;
      }

      if(is_valide(p) && !plateau_same_type(p_x, p_y,'$') && !plateau_same_type(p_x, p_y,'+')){
        s->execute_move(*l);
        delete(plateau[p_y][p_x]);
        plateau[p_y][p_x] = s;
        plateau[y][x] = new Empty;
        break;
      }

      if((plateau_same_type(p_x,p_y,'c') && s->type() == 'l') ||
          (plateau_same_type(p_x,p_y,'l') && s->type() == 'c')){
            collision_lcstreumons(i,p_x,p_y);
            break;
          }
    }
  }
}

void Plateau::collision_lcstreumons(int i, int p_x, int p_y){
  int x = stremons[i]->get_pos().getX();
  int y = stremons[i]->get_pos().getY();
  // Removing the 2 stremons from the streumons vector
  int pos;
  for(int j = 0; j<(int) stremons.size(); j++){
    if (stremons[j] == plateau[p_y][p_x]){
      pos = j;
      break;
    }
  }
  stremons.erase(stremons.begin()+pos);
  if(pos < i)
    i-=1;
  stremons.erase(stremons.begin()+i);

  // Creating a reumus at the place of their encounter
  delete   plateau[p_y][p_x];
  plateau[p_y][p_x] = new Reumus;
  delete plateau[y][x];
  plateau[y][x] = new Empty;
}


void Plateau::init_player() {
  o->reset_diams();
  o->set_position(depart.getX(), depart.getY());
}

int Plateau::get_height(void){
  return h;
}

int Plateau::get_width(void){
  return w;
}

string Plateau::get_player_name(void) {
  return o->get_name();
}

int Plateau::get_player_diams(void) {
  return o->nb_diams();
}

int Plateau::get_player_geuchars(void) {
  return o->nb_geuchars();
}

Case* Plateau::get_case(int i,int j){
  return plateau[i][j];
}

bool Plateau::is_finish(){
  return over;
}

bool Plateau::is_win() {
  return win;
}

bool Plateau::in_obj_list(char elt) {
  int size = 11;
  char obj[size] = {
    'J',
    '~',
    'l',
    'c',
    'r',
    '#',
    '*',
    '$',
    '.',
    '-',
    'q'
  };
  for (int i = 0 ; i<size ; i++) {
    if(obj[i] == elt) { return true; }
  }
  return false;
}

bool Plateau::is_valide(Position p){
  if (p.getX() < 0 || p.getX() >= this->w ||
      p.getY() < 0 || p.getY() >= this->h) {
        return false;
  }
  return plateau[p.getY()][p.getX()]->action();
}

void Plateau::to_string_vector(vector<string>& exp) {
  exp.resize(h, string((unsigned int)w, ' '));
  for(int i = 0 ; i < h ; i++) {
    for (int j = 0 ; j < w ; j++) {
      exp[i][j] = plateau[i][j]->type();
    }
  }
}

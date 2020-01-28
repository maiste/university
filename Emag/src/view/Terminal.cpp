/*
 * View
 * Betend - Marais
 */

#include "Terminal.hpp"

void Terminal::display_header(void) const {
  cout << "****************************" << endl;
  cout << "** Bienvenue aventurier ! **" << endl;
  cout << "**  by Marie  &  Etienne  **" << endl;
  cout << "****************************" << endl;
  cout << endl;
}

string Terminal::ask_name(void) const {
  string name = "";
  while (name == "") {
    cout << "Entre ton nom d'aventurier : ";
    getline(cin,name);
  }
  cout << endl;
  return name;
}

Move stringToMove(std::string str) {
    if(str == "z")      return UP;
    if(str == "s")      return DOWN;
    if(str == "q")      return LEFT;
    if(str == "d")      return RIGHT;
    if(str == "a")     return UP_LEFT;
    if(str == "e")     return UP_RIGHT;
    if(str == "w")     return DOWN_LEFT;
    if(str == "x")     return DOWN_RIGHT;
    if(str == "g")      return GEUCHARS;
    if(str == "l")      return LEAVE;
    return ERROR;
}


void Terminal::display_board(Plateau* p) const {
  cout << "*******************" << endl;
  cout << "Aventurier: " << p->get_player_name() << endl;
  cout << "Geuchar(s): " << p->get_player_geuchars() << endl;
  cout << "Diam(s): " << p->get_player_diams() << endl;
  cout << "*******************" << endl;
  for (int i = 0 ; i < p->get_height() ; i++) {
    for (int j = 0 ; j < p->get_width() ; j++) {
      cout << p->get_case(i,j)->type() << " ";
    }
    cout << endl;
  }
  cout << endl;
}
void Terminal::display_str_board(vector<string>& p) {
 for (auto i = p.begin(); i != p.end(); ++i)
          cout << *i << endl;
}


Move Terminal::get_user_input() const {
  string input;
  Move m;
  do {
    cout << "Actions :" << endl;
    cout << "-> z: haut, s: bas q: gauche, d: droite, "
         << "a: haut gauche, e: haut droit, w: bas gauche, x: bas droit" << endl;
    cout << "-> g: geuchar aléatoire, l pour quitter" << endl;
    cout << "gp>";
    getline(cin,input);
  } while((m = stringToMove(input)) == ERROR);
  cout << endl;
  return m;
}

void Terminal::level_up(int level) const {
  cout << "* Bravo aventurier ! Tu as survécu à un niveau de plus ! *" << endl;
  cout << "* Tu es au niveau " << level << " *" << endl << endl;
}

void Terminal::game_over(int score) const {
  cout << "-- Perdu !!! Tu gagnes quand même " << score << " dollar(s)... --" << endl
       << endl;
}

void Terminal::win(int score) const {
  cout << "++ Bravo Indiana Jones ! Tu remportes la somme de " << score << " dollar(s) ! ++" << endl
       << endl ;
}

int Terminal::ask_height(void) const {
  int h;
  string input = "";
  bool asking;
  do {
    asking = false;
    cout << "Quelle est la hauteur du plateau ?" << endl;
    cout << "Note que cela doit être supérieur à 2" << endl;
    cout << "gc>hauteur> ";
    getline(cin, input); // Secure with getline();
    stringstream out(input);
    if(!(out >> h)) asking = true;
  } while (asking || h <= 2);
  return h;
}

int Terminal::ask_width(void) const {
  int w;
  string input = "";
  bool asking;
  do {
    asking = false;
    cout << "Quelle est la largeur du plateau ?" << endl;
    cout << "Elle doit être supérieure à 3." << endl;
    cout << "gc>largeur> ";
    getline(cin, input);
    stringstream out(input);
    if(!(out >> w)) asking = true;
  } while (asking || w <= 3);
  return w;
}

Position Terminal::ask_position(void) const{
  int x, y;
  string input = "";
  bool asking;
  do{
    asking = false;
    cout << "Quelle est la position de l'élément ? " << endl;
    cout << "Le choix est un nombre supérieur ou égal à 0" << endl;

    cout << "gc>abscisse> ";
    getline(cin, input);        // Secure with getline()
    stringstream x_out(input);
    if(!(x_out >> x)) { asking = true ; continue; }

    cout << "gc>ordonnée> ";
    getline(cin, input);
    stringstream y_out(input);
    if(!(y_out >> y)) { asking = true; }
  } while (asking || (x < 0 && y < 0));
  return Position(x,y);
}

char Terminal::new_element(void) const {
  char elem;
  string input = "";
  do{
    cout << "Que veux-tu faire ?" << endl;
    cout << "Modifier J, insérer ~, r, l, c, #, *, $, -, nettoyer la case avec . ou quitter avec q" <<endl;
    cout << "gc>élément> ";
    getline(cin, input);    // Secure with getline
    if(input.length() != 1) continue;
    elem = input[0];
  } while (!Plateau::in_obj_list(elem)) ;
  return elem;
}

void Terminal::fail_teupor(void) const {
  cout << "Plus de teupors que de diams. Il faut en avoir autant ou moins !" << endl;
}

void Terminal::fail_modif(void) const {
  cout << "!! Ta modification a echoué !!" << endl;
}

string Terminal::want_save(void) const {
  string name;
  cout << "Avant de quitter, veux-tu sauvegarder ta progression ?" << '\n';
  cout << "Si oui, entre le nom du fichier dans lequel tu veux enregistrer ton avancée, sinon tape entrée" << '\n';
  getline(cin, name);
  return name;
}

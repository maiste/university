/*
 * Curse
 * Betend - Marais
 */

#include "Curse.hpp"

void Curse::clear_line(int y, int x, int size) const {
  for (int i = 0 ; i < size ; i++) {
    move(y,x+i);
    printw(" ");
  }
  refresh();
}

void Curse::print_mid_str(string str) const {
  clear();
  move(h/2, w/2 - (str.size()/2));
  printw("%s", str.c_str());
  getch();
  refresh();
}

void Curse::print_mid_str(string start, string stop, int n) const {
  clear();
  move(h/2, w/2 - ((start.size() + stop.size()) /2));
  printw("%s %d %s", start.c_str(), n, stop.c_str());
  getch();
  refresh();
}



string Curse::ask_str(string question, string prompt, int y, int x) const {
  string input = "";
  int c;
  while(true) {
    move(y,x);
    printw("%s", question.c_str());
    move(y+1, x);
    printw("%s", prompt.c_str());
    refresh();

    c = getch();
    if (c == '\n'){
      break;
    } else if(c == 127) { // Backspace
      if(input.size() > 0) { input.pop_back(); }
      clear_line(y+1,0, w);
    } else {
      input.push_back(c);
    }
    move(y+1,x+prompt.size()+1);
    printw("%s", input.c_str());
    refresh();
  }
  clear_line(y+1,0, w);
  return input;
}

int Curse::ask_int(string question, int bound, int y, int x) const {
  int res;
  string input = "";
  bool asking;

  do {
    asking = false;
    input = ask_str(question, "*" ,y ,x);
    stringstream converter(input);
    if(!(converter >> res)) asking = true;
  } while(asking || res <= bound);
  return res;
}



Curse::Curse() {
  initscr();
  noecho(); // Remove chars
  cbreak(); // Get sig
  curs_set(0); // Remove cursor

  getmaxyx(stdscr,h,w);
}

void Curse::display_header(void) const {
  string avent = "** Bienvenue aventurier ! **";
  string danger = "/!\\ Prepare-toi a affronter tous les dangers /!\\";

  print_mid_str(avent);
  print_mid_str(danger);
}

string Curse::ask_name(void) const {
  clear();
  string question = "ENTRE TON NOM D'AVENTURIER";
  string prompt = "* ";
  string input = "";
  int h_offset = h/2;
  int w_offset = w/2 - question.size()/2;
  while(input == "") {
    input = ask_str(question, prompt, h_offset, w_offset);
  }
  return input;
}

void Curse::display_board(Plateau* p) const {
  int h_offset = (h/2) - (2 * p->get_height()/2) ;
  int w_offset = p->get_width() / 2 ;

  clear();
  move(h_offset-2, w_offset);
  printw("|>   EMAG    <|");

  w_offset = 2;

  for (int i = 0 ; i < p->get_height() ; i++) {
    for (int j = 0 ; j < p->get_width() ; j++) {
      move(2*i + h_offset, 2*j);
      printw("%c",p->get_case(i,j)->type());
    }
  }

  w_offset = w_offset + 1 + (2*p->get_width());

  move(h_offset, w_offset);
  printw("------");
  move(h_offset + 1, w_offset);
  printw("* Aventurier : %s", p->get_player_name().c_str());
  move(h_offset+2, w_offset);
  printw("* Geuchar(s) : %d", p->get_player_geuchars());
  move(h_offset+3, w_offset);
  printw("* Diam(s) : %d", p->get_player_diams());
  move(h_offset+4, w_offset);
  printw("------");
  refresh();
}

void Curse::display_str_board(vector<string>& p) {
  clear();
  int h_offset = (h/2) - (2 * p.size()) ;
  int w_offset = (w/2) - (2 * p[0].size());  ;
  for (int i = 0 ; i < (int)p.size() ; i++) {
    for (int j = 0 ; j < (int)p[0].size() ; j++) {
      move(2*i + h_offset, 2*j + w_offset);
      printw("%c",p[i][j]);
    }
  }

}

Move Curse::get_user_input(void) const {
  while(true) {
    switch(getch()) {
      case 'z': return UP;
      case 's': return DOWN;
      case 'q': return LEFT;
      case 'd': return RIGHT;
      case 'a': return UP_LEFT;
      case 'e': return UP_RIGHT;
      case 'w': return DOWN_LEFT;
      case 'x': return DOWN_RIGHT;
      case 'g': return GEUCHARS;
      case 'l': return LEAVE;
    }
  }
}

void Curse::level_up(int level) const {
  string start = "++ Bravo !! Tu passes au niveau";
  string stop = "! ++";
  print_mid_str(start, stop, level);
}

void Curse::game_over(int score) const {
  string start = "-- Desole tu as perdu... Tu repars avec";
  string stop = "dollar(s)... --";
  string the_end = "THE END";
  print_mid_str(start, stop, score);
  print_mid_str(the_end);
}

void Curse::win(int score) const {
  string start = "++ Bravo Indiana Jones ! Tu remportes le pactole avec";
  string stop = "dollar(s) ! ++";
  string the_end = "THE END";
  print_mid_str(start, stop, score);
  print_mid_str(the_end);
}

int Curse::ask_height(void) const {
  clear();
  string question = "Hauteur de votre plateau ? (au moins 3)";
  int w_offset = w/2 - question.size()/2;
  return ask_int(question, 2, h/2, w_offset);
}

int Curse::ask_width(void) const {
  clear();
  string question = "Largeur de votre plateau ? (au moins 4)";
  int w_offset = w/2 - question.size()/2;
  return ask_int(question, 3, h/2, w_offset);
}

Position Curse::ask_position(void) const {
  int x, y;
  string question_1 = "Abscisse de l'objet ?";
  string question_2 = "Ordonnee de l'objet ?";
  int w_offset = w/2 - question_1.size()/2;
  x = ask_int(question_1, -1, h-2, w_offset);
  y = ask_int(question_2, -1, h-2, w_offset);
  return Position(x,y);
};

char Curse::new_element(void) const {
  char elt;
  string input;
  string question = "Modifier J, inserer ~, r, l, c, #, *, $, -, nettoyer la case avec . ou quitter avec q ?";
  string prompt = "* ";
  int w_offset = w/2 - question.size()/2;

  do {
    input = ask_str(question, prompt, h-2, w_offset);
    if(input.length() != 1) continue;

    elt = input[0];
  } while(!Plateau::in_obj_list(elt));
  clear_line(h-2, 0, w);
  return elt;
}

void Curse::fail_teupor(void) const {
  string fail = "/!\\ Il manque une teupor ou il y a plus de teupors que de diams !";
  print_mid_str(fail);
}

void Curse::fail_modif(void) const {
  string fail = "/!\\ La modification demandee n'est pas realisable !";
  print_mid_str(fail);
}

string Curse::want_save(void) const {
  clear();
  string question = "Si tu souhaites sauvegarder ta progression, entre le nom du fichier dans lequel tu veux enregistrer ton avancee, sinon presse entree.";
  string prompt = "* ";
  int w_offset = w/2 - question.size()/2;

  return ask_str(question, prompt, h/2, w_offset);
}

Curse::~Curse() {
  curs_set(1);
  endwin();
}

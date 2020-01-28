/*
 * RandomStreumon
 * Betend - Marais
 */

#include "RandomStreumon.hpp"

RandomStreumon::RandomStreumon() {};

RandomStreumon::RandomStreumon(int i, int j) : Streumon{i,j} {}

void RandomStreumon::execute_move(const Move m) {
  update_pos(m);
}

void RandomStreumon::potential_move(vector<Move> & coup, const Position& p) {
  coup.resize(1);

  srand(time(nullptr));
  int choice = rand() % 8;

  switch(choice) {
    case 0: coup[0] = Move::UP; return;
    case 1: coup[0] = Move::DOWN; return;
    case 2: coup[0] = Move::RIGHT; return;
    case 3: coup[0] = Move::LEFT; return;
    case 4: coup[0] = Move::UP_LEFT; return;
    case 5: coup[0] = Move::UP_RIGHT; return;
    case 6: coup[0] = Move::DOWN_LEFT; return;
    default: coup[0] = Move::DOWN_RIGHT; return;
  }
}

bool RandomStreumon::action(void) {
  return false;
}

char RandomStreumon::type(void) {
  return 'r';
}

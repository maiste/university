/*
 * ColumnStreumon
 * Betend - Marais
 */

#include "ColumnStreumon.hpp"

ColumnStreumon::ColumnStreumon() {}

ColumnStreumon::ColumnStreumon(int i, int j) : Streumon{i,j} {}

void ColumnStreumon::execute_move(const Move m) {
  update_pos(m);
}

void ColumnStreumon::potential_move(vector<Move> & coup, const Position& p) {
  coup.resize(2);
  coup[0] = Move::UP;
  coup[1] = Move::DOWN;
  sort_by_dist(coup,p);
}

bool ColumnStreumon::action(void) {
  return false;
}

char ColumnStreumon::type(void) {
  return 'c';
}

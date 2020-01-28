/*
 * LineStreumon
 * Betend - Marais
 */

#include "LineStreumon.hpp"

LineStreumon::LineStreumon() {}

LineStreumon::LineStreumon(int i, int j) : Streumon{i,j} {}

void LineStreumon::execute_move(const Move m) {
  update_pos(m);
}

void LineStreumon::potential_move(vector<Move> & coup, const Position& p) {
  coup.resize(2);
  coup[0] = Move::LEFT;
  coup[1] = Move::RIGHT;
  sort_by_dist(coup,p);
}

bool LineStreumon::action(void) {
  return false;
}

char LineStreumon::type(void) {
  return 'l';
}

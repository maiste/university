/*
 * NormalStreumon
 * Betend - Marais
 */

#include "NormalStreumon.hpp"

NormalStreumon::NormalStreumon() : Streumon{} {}

NormalStreumon::NormalStreumon(int i,int j) : Streumon{i,j}{}

void NormalStreumon::execute_move(const Move m) {
  update_pos(m);
}

// Arranges the potentials move of the stremon by increasing distance between him and the player at position p
void NormalStreumon::potential_move(vector<Move>& coups, const Position& p){
  coups.resize(8);
  coups[0]= UP;
  coups[1]= DOWN;
  coups[2]= LEFT;
  coups[3]= RIGHT;
  coups[4]= UP_LEFT;
  coups[5]= UP_RIGHT;
  coups[6]= DOWN_LEFT;
  coups[7]= DOWN_RIGHT;
  sort_by_dist(coups, p);
}

bool NormalStreumon::action(void) {
  return false;
}

char NormalStreumon::type(void) {
  return '~';
}

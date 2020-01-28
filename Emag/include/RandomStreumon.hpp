/*
 * RandomStreumon
 * Betend - Marais
 */
#include <cstdlib>
#include <ctime>

#include "std_h.hpp"
#include "Streumon.hpp"

#ifndef RANDOM
#define RANDOM

class RandomStreumon  : public Streumon {
public:
  RandomStreumon();
  RandomStreumon(int i, int j);
  virtual void execute_move(const Move m);
  virtual void potential_move(vector<Move>& coups, const Position& p);

  virtual bool action(void);
  virtual char type(void);
};

#endif // RANDOM

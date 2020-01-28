/*
 * ColumnStreumon
 * Betend - Marais
 */

#include "std_h.hpp"
#include "Streumon.hpp"

#ifndef COLUMN
#define COLUMN

class ColumnStreumon  : public Streumon {
public:
  ColumnStreumon();
  ColumnStreumon(int i, int j);
  virtual void execute_move(const Move m);
  virtual void potential_move(vector<Move>& coups, const Position& p);

  virtual bool action(void);
  virtual char type(void);
};

#endif // COLUMN

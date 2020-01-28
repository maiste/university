/*
 * Position
 * Betend - Marais
 */

#include <cmath>

#include "std_h.hpp"
#include "Move.hpp"
#include "MoveError.hpp"

#ifndef POSITION
#define POSITION

class Position {
private:
  int x;
  int y;

public:
  Position(void);
  Position(int, int);
  Position(Move m);
  virtual ~Position();
  int getX(void) const;
  int getY(void) const;
  void moveX(int x);
  void moveY(int y);
  void move(int x, int y);
  double get_dist(Position p) const;

  Position operator+(const Position&) const;
};

#endif // POSITION

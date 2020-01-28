/*
 * Position
 * Betend - Marais
 */

#include "Position.hpp"

Position::Position() : x{0}, y{0} {}

Position::Position(int x, int y) : x{x}, y{y} {}

Position::Position(Move m) {
  switch(m) {
    case UP:
      x = 0;
      y = -1;
      break;
    case DOWN:
      x = 0;
      y = 1;
      break;
    case LEFT:
      x = -1;
      y = 0;
      break;
    case RIGHT:
      x = 1;
      y = 0;
      break;
    case UP_LEFT:
      x = -1;
      y = -1;
      break;
    case UP_RIGHT:
      x = 1;
      y = -1;
      break;
    case DOWN_LEFT:
      x = -1;
      y = 1;
      break;
    case DOWN_RIGHT:
      x = 1;
      y = 1;
      break;
    default:
      throw MoveError();
  }
}

Position::~Position(){}

int Position::getX(void) const {
  return this->x;
}

int Position::getY(void) const {
  return this->y;
}


void Position::moveX(int x) {
  this->x = x;
}

void Position::moveY(int y) {
  this->y = y;
}

void Position::move(int x, int y) {
  moveX(x);
  moveY(y);
}

Position Position::operator+(const Position& p) const {
  return Position(
            this->x + p.x,
            this->y + p.y
      );
}

double Position::get_dist(Position p) const {
  return sqrt( pow(x - p.getX(), 2) + pow(y - p.getY(), 2) );
}

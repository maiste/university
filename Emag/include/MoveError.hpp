/*
 * MoveError
 * Betend - Marais
 */

#include <exception>

#include "std_h.hpp"

#ifndef MOVE_EXPT
#define MOVE_EXPT

class MoveError : public exception {
public:
  MoveError();
};

#endif /* protected header MOVE_EXPT */

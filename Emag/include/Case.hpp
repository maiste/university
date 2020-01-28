/*
 * Interface case
 * Betend - Marais
 */

#include "std_h.hpp"

#ifndef CASE
#define CASE

class Case {
public:
  virtual ~Case() {};
  virtual char type(void) = 0;
  virtual bool action(void) = 0;
};

#endif

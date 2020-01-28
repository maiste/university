/*
 * SizeError
 * Betend - Marais
 */

#include <exception>

#include "std_h.hpp"

#ifndef SIZE_ERROR
#define SIZE_ERROR

class SizeError : public exception {
  public:
    SizeError();
};

#endif /* protected header SIZE_ERROR */

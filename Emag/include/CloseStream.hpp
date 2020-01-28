/*
 * CloseStream
 * Betend - Marais
 */

#include <exception>

#include "std_h.hpp"

#ifndef CLOSESTREAM
#define CLOSESTREAM

class CloseStream : public exception {
  public:
    CloseStream();
};

#endif /* protected header CLOSESTREAM */

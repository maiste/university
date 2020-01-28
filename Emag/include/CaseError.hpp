/*
 * CaseError
 * Betend - Marais
 */

#include <exception>

#include "std_h.hpp"

#ifndef CASE_EXPT
#define CASE_EXPT

class CaseError : public exception {
public:
  CaseError();
};

#endif

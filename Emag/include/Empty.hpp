/*
 * Empty
 * Betend - Marais
 */

#include "std_h.hpp"
#include "Case.hpp"

#ifndef EMPTY
#define EMPTY

class Empty : public Case {
  public :
    Empty ();
    virtual char type(void);
    virtual bool action(void);
};

#endif //EMPTY

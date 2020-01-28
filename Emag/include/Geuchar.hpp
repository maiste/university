/*
 * Geuchar
 * Betend - Marais
 */

#include "std_h.hpp"
#include "Case.hpp"

#ifndef GEUCHAR
#define GEUCHAR

class Geuchar : public Case {
  public :
    Geuchar ();
    virtual char type(void);
    virtual bool action(void);
};

#endif //GEUCHAR

/*
 * Reumus
 * Betend - Marais
 */

#include "std_h.hpp"
#include "Case.hpp"

#ifndef REUMUS
#define REUMUS

class Reumus : public Case {
  public :
    Reumus ();
    virtual char type(void);
    virtual bool action(void);
};

#endif //REUMUS

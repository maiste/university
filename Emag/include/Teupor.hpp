/*
 * Teupor
 * Betend - Marais
 */

#include "std_h.hpp"
#include "Case.hpp"

#ifndef TEUPOR
#define TEUPOR

class Teupor : public Case {
  private :
    bool status;
    void open ();
  public :
    Teupor();
    Teupor(bool status);
    bool get_status();
    virtual char type(void);
    virtual bool action(void);

    friend class Diams;
};

#endif //TEUPOR

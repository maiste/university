/*
 * Diams
 * Betend - Marais
 */

#include "std_h.hpp"
#include "Case.hpp"
#include "Teupor.hpp"

#ifndef DIAMS
#define DIAMS

class Plateau;

class Diams : public Case {
  private :
    Teupor * porte;
    void add_porte(Teupor * p);
  public :
    Diams ();
    virtual char type(void);
    virtual bool action(void);
  friend Plateau;

};

#endif //DIAMS

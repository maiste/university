/*
 * Oueurj
 * Betend - Marais
 */

#include <iostream>
#include <list>

#include "std_h.hpp"
#include "Position.hpp"
#include "Case.hpp"
#include "Geuchar.hpp"

#ifndef JOUEUR
#define JOUEUR

class Plateau;

class Oueurj : public Case {
  private:
    string name;
    Position pos;
    int diams;
    int geuchars;
    void set_position(int i, int j);
    void reset_diams(void);

  public:
    Oueurj(string name);
    Oueurj(void);

    int nb_diams(void) const;
    int nb_geuchars(void);
    Position& get_position(void);
    void set_name(string);
    string get_name (void) const;

    void consume_geuchar(void);
    void move(const Position& p);
    void add_diams(void);
    void add_geuchar(void);

    virtual bool action(void);
    virtual char type(void);

  friend Plateau;
};

#endif // JOUEUR

/*
 * Interface Streumon
 * Betend Marais
 */

#include <vector>

#include "std_h.hpp"
#include "Position.hpp"
#include "Case.hpp"

#ifndef MONSTRE
#define MONSTRE

class Streumon : public Case {
private:
  Position pos;

public:
  Streumon() : pos{} {}
  Streumon(int i, int j) : pos{i,j} {}

  Position& get_pos(void){
    return this->pos;
  }

  void update_pos(const Position& p) {
    this->pos.move(p.getX(), p.getY());
  }

  void update_pos(const Move m){
    this->pos = pos + m;
  }

  void sort_by_dist(vector<Move>& coups, const Position& p){
    Position tmp = this->get_pos();
    double dist [coups.size()];
    double tampon;
    Move mv_tampon;
    for (int i=0; i<(int)coups.size(); i++){
      tmp=this->get_pos() + coups[i];
      dist[i] = tmp.get_dist(p);
    }

    for(int i=0; i< (int) coups.size() - 1; i++){
      if( dist[i] > dist[i+1]){
        tampon = dist[i+1];
        dist[i+1] = dist[i];
        dist[i] = tampon;
        mv_tampon = coups[i+1];
        coups[i] = coups[i+1];
        coups[i+1] = mv_tampon;
        i=-1;
      }
    }
  }

  virtual void execute_move(const Move m) = 0;
  virtual void potential_move(vector<Move >& coups, const Position& p) = 0;
  virtual bool action(void) = 0;
  virtual char type(void) = 0;
};


#endif // MONSTRE

/*
 * Oueurj
 * Betend - Marais
 */

#include "Oueurj.hpp"


Oueurj::Oueurj(string name) : name{name}, pos{-1,-1}, diams{0}, geuchars{0} {}
Oueurj::Oueurj(void) : Oueurj{"Player1"} {}


int Oueurj::nb_diams(void) const {
  return this->diams;
}

int Oueurj::nb_geuchars(void) {
  return this->geuchars;
}

Position& Oueurj::get_position(void) {
  return this->pos;
}

string Oueurj::get_name(void) const {
  return this->name;
}


void Oueurj::set_position (int i, int j){
  pos.move(i,j);
}

void Oueurj::set_name(string name) {
  this->name = name;
}

void Oueurj::consume_geuchar(void) {
  if(this->geuchars > 0) {
    this->geuchars--;
  }
}

void Oueurj::reset_diams(void) {
  this->diams = 0;
}

void Oueurj::move(const Position& p) {
  this->pos.move(p.getX(), p.getY());
}

void Oueurj::add_diams(void) {
  this->diams++;
}

void Oueurj::add_geuchar(void) {
  this->geuchars++;
}

bool Oueurj::action(void) {
  return false;
}

char Oueurj::type(void) {
  return 'J';
}

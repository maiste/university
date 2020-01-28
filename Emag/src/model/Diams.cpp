/*
 * Diams
 * Betend - Marais
 */

#include "Diams.hpp"

Diams::Diams(): porte(nullptr){}

char Diams::type () {
  return '$';
}

// Picking a Diams opens the associate door
bool Diams::action () {
  if(porte != nullptr) {
    porte->open();
  }
  return true;
}

// Associates a door to the current instance
void Diams::add_porte(Teupor * p){
  porte = p;
}

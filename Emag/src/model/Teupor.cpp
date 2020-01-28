/*
 * Teupor
 * Betend - Marais
 */

#include "Teupor.hpp"

Teupor::Teupor(): status(false){}

Teupor::Teupor(bool b): status(b){}

char Teupor::type() {
  if(status)
    return '+';
  return '-';
}

bool Teupor::action() {
  if (status) {
    // action -> getting out of the board
    return true;
  }
  else
    return false;
}

// The door opens
void Teupor::open() {
  status = true;
}

// Return the actual state of the door : False if it's close, True is it's open
bool Teupor::get_status(){
  return status;
}

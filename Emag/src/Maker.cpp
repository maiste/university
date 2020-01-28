/*
 * Maker
 * Betend - Marais
 */

#include "Maker.hpp"
#include "Curse.hpp"


int Maker::run_creator(int argc, char * argv[]) {
 try {
    if(argc == 2){
      Terminal v;
      Builder *b;

      FileManager::check_right_ext(argv[1], Ext::BOARD);

      if(exists(argv[1])) {
        b = Builder::generate_board(argv[1]);
      } else {
        b = Builder::create_board(argv[1], &v);
      }

      b->modify_board(&v);
      delete b;
    } else {
      Builder::build_game(argc, argv);
    }
  } catch (UnknownFile& e) {
    cout << "Wrong path or not .game or .board extension!" << endl;
    return 1;
  } catch (CloseStream& e) {
    cout << "Closed stream" << endl;
    return 1;
  } catch (SizeError& e) {
    cout << "Wrong Size Board" << endl;
    return 1;
  }
  return 0;
}


int main(int argc, char* argv[]) {
  if (argc <= 1){
    cout << "You need to specify a game file" << endl;
    return 1;
  }

  return Maker::run_creator(argc, argv);
 }

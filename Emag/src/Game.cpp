/*
 * Game
 * Betend - Marais
 */

#include "Curse.hpp"
#include "Game.hpp"

int Game::run(int argc, char* argv[]) {
  string path = string(argv[2]);
  try {
    if(string(argv[1]) == "t") {
      Terminal v;
      Controller c(path, &v);
      c.run_game();
    } else if(string(argv[1]) == "n") {
      Curse v;
      Controller c (path, &v);
      c.run_game();
    } else {
      cout << "Mauvais argument pour le type de la vue : t ou n" << endl;
      return 1;
    }
  } catch (UnknownFile& e) {
    cout << "Chemin inconnu ou extension fichier différente de .board ou .save"
         << endl;
    return 1;
  } catch (CaseError& e) {
    cout << "Fichier corrompu : case inconnue dans la construction du plateau" << endl;
    return 1;
  } catch (SizeError& e) {
    cout << "Fichier corrompu : erreur dans la génération des plateaux" << endl;
    return 1;
  } catch (MissingPlayer& e) {
    cout << "Fichier corrompu : absence de joueur" << endl;
    return 1;
  }

  return 0;
}


int main(int argc, char* argv[]) {
  if (argc != 3 ) {
    cout << "Spécifiez un mode [t|n] et un fichier de jeu" << endl;
    return 1;
  }

  return Game::run(argc, argv);
 }

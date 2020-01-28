/*
 * Controller
 * Betend - Marais
 */


#include "std_h.hpp"
#include "UnknownFile.hpp"
#include "View.hpp"
#include "Plateau.hpp"
#include "FileManager.hpp"
#include "Builder.hpp"

#ifndef CONTROLLER
#define CONTROLLER

class Controller {
  private:
    int finalScore;
    int level;
    View* v;
    Oueurj *o;
    vector<Plateau*> plateaux;
    bool want_play(Move m);
    bool play_one_level(Plateau* p);
  public:
    Controller(string, View* v);
    virtual ~Controller();
    void set_view(View * v);
    void run_game(void);
    void save_game(Plateau* p);
};

#endif // CONTROLLER

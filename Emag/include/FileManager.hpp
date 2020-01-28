/*
 * FileManager
 * Betend - Marais
 */

#include <iostream>
#include <fstream>
#include <string>
#include <vector>

#include "std_h.hpp"
#include "ext.hpp"
#include "UnknownFile.hpp"
#include "CloseStream.hpp"
#include "Oueurj.hpp"
#include "Plateau.hpp"

#ifndef FILEMANAGER
#define FILEMANAGER

class FileManager {
  private:
    string inpath;
    string outpath;
    ifstream fin;
    ofstream fout;
    void fill_plateau(Oueurj *o, int pos, vector<Plateau *>& plateaux, bool b);
    int get_nb_plateaux(void);
    int get_width(void);
    int get_height(void);
  public:
    FileManager(string inpath);
    FileManager(string inpath, string outpath);
    FileManager(string path, int type);
    virtual ~FileManager();
    static Ext get_extension(string path);
    static void check_right_ext(string path, Ext ext);
    vector<string> generate_model(void);;
    void create_game(Oueurj *o,vector<Plateau *>& plateaux);
    void copy_in_to_out(void);
    void write_head(int head);
    void write_plateau(vector<string>& plateau);
    void write_save(int score, int level, vector<Plateau *>&, Plateau *);
    int get_score(void);
    int get_level(void);
};

bool exists(const char*);
#endif /* protected header FILEMANAGER */

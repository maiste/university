/*
 * FileManager
 * Betend - Marais
 */

#include "FileManager.hpp"

int FileManager::get_score(void){
  int s = 0;
  fin >> s;
  if(s < 0) throw SizeError();
  return s;
}

int FileManager::get_level(void){
  int h = 0;
  fin >> h;
  if(h < 1) throw SizeError();
  return h;
}

int FileManager::get_nb_plateaux(void) {
  int nb = 0;
  fin >> nb;
  if(nb <= 0) throw SizeError();
  return nb;
}

int FileManager::get_height(void) {
  int h = 0;
  fin >> h;
  if(h <= 2) throw SizeError();
  return h;
}

int FileManager::get_width(void) {
  int w = 0;
  fin >> w;
  if(w <= 3) throw SizeError();
  return w;
}

vector<string> FileManager::generate_model(void){
  int h = get_height();
  int w = get_width();
  string line;
  vector<string> model{(long unsigned int)h, ""};
  for (int i = 0 ; i  < h ; i++) {
    fin >> line;
    if((int)line.size() != w) throw SizeError();
    model[i] = line;
  }
  return model;
}

void FileManager::fill_plateau(Oueurj*o, int pos, vector<Plateau *>& p, bool b) {
  vector<string> model = generate_model();
  p[pos] = new Plateau(o,model,(int) model.size(),(int) model[0].size(), !b);
}

FileManager::FileManager(string inpath) : FileManager(inpath,0) {}

FileManager::FileManager(string inpath, string outpth){
    inpath = inpath;
    outpath = outpth;
    fin.open(inpath);
    fout.open(outpath,  ios_base::app);
    if(!fin.is_open() && fout.is_open()) {
      fout.close();
      throw UnknownFile();
    } else if(fin.is_open() && !fout.is_open()) {
      fout.close();
      throw UnknownFile();
    } else if(!fin.is_open() && !fout.is_open()) {
      throw UnknownFile();
    }
  }

FileManager::FileManager(string path, int type) {
  if(type == 0) {
    inpath = path;
    fin.open(inpath);
    if(!fin.is_open()) throw UnknownFile();
  } else if(type == 1) {
    outpath = path;
    fout.open(outpath, ios_base::app);
    if(!fout.is_open()) throw CloseStream();
  } else if(type == 3){
    outpath = path;
    fout.open(outpath, ios_base::trunc);
  } else {
    throw CloseStream();
  }
}

FileManager::~FileManager(){}

Ext FileManager::get_extension(string path) {
  string dot = ".";
  size_t index = path.rfind(dot);
  if(index != string::npos) {
    string ext = path.substr(index+1);
    if(ext == "board") return BOARD;
    if(ext == "game") return GAME;
    if(ext == "save") return SAVE;
  }
  return UNKNOWN;
}

void FileManager::check_right_ext(string path, Ext e) {
  if(get_extension(path) != e) {
    throw UnknownFile();
  }
}

void FileManager::create_game(Oueurj *o, vector<Plateau *>& plateaux) {
  Ext extension = get_extension(inpath);
  int nb;
  if(extension == BOARD) {
    nb = 1;
  } else if (extension == GAME || extension == SAVE){
    nb = get_nb_plateaux();
  } else {
    throw UnknownFile();
  }
  plateaux.resize(nb);
  for(int i = 0; i < nb ; i++) {
    fill_plateau(o, i, plateaux, extension == SAVE);
  }
}

void FileManager::write_plateau(vector<string>& plateau) {
  if(!fout.is_open()) { throw CloseStream(); }
  fout << plateau.size() << endl ;
  fout << plateau[0].size() <<endl;
  for(auto& line : plateau) {
    fout << line <<endl;
  }
}

void FileManager::write_save(int s, int lvl, vector<Plateau *>& plateaux, Plateau * p) {
  if(!fout.is_open()) { throw CloseStream(); }
  int pos = 0;
  for(int i = 0; i <(int)plateaux.size(); i++){
    if (plateaux[i] == p){
      pos = i;
      break;
    }
  }
  fout << s << endl;
  fout << lvl << endl;
  fout << plateaux.size() - pos << endl;

  for(int i=pos; i<(int) plateaux.size(); i++){
    vector<string> str_plateau;
    plateaux[i]->to_string_vector(str_plateau);
    write_plateau(str_plateau);
  }
}

void FileManager::copy_in_to_out(void) {
  string line;
  while(fin>>line) {
    fout << line <<endl;
  }
}

void FileManager::write_head(int head) {
  if(!fout.is_open()) {
    throw CloseStream();
  }
  fout << head <<endl;
}

bool exists(const char * name) {
  ifstream f(name);
  bool b = f.good();
  return b;
}

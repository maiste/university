#ifndef OUT_H
#define OUT_H

#include <stdio.h>
#include <stdlib.h>
#include <SDL2/SDL.h>
#include <SDL_image.h>
#include <jpeglib.h>
#include <png.h>
#include <dirent.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <errno.h>

#include"m_image.h"

image *save_image_as(image* img, char * path);
short save_image(image* img);
short save_secure(image* img);
short remove_secure(image * img);
void remove_tmp_file(char * filename);
void clean_secure();
#endif

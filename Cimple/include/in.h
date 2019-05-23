#ifndef IN_H
#define IN_H
#include"m_image.h"

#include <stdio.h>
#include <stdlib.h>
#include <SDL2/SDL.h>
#include <SDL_image.h>
#include <unistd.h>
#include <dirent.h>
#include "m_image.h"

image* load_image(char* path); /*load an image*/
short check_tmp();

#endif

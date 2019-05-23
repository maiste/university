#ifndef WINDOW_H
#define WINDOW_H

#include "m_image.h"
#include "in.h"

struct frame {
  image * image;
  SDL_Window * window;
  SDL_Renderer * renderer;
};

typedef struct frame frame;

frame * init_frame(char * path);
void free_frame(frame * target);
short update_frame(frame * target, char * path );

#endif

#ifndef M_FRAME_H
#define M_FRAME_H

#include <SDL2/SDL.h>
#include "m_image.h"

#define MAX_W  2000
#define MAX_H  2000
#define MAX_R  500
	short truncate_image(image * target , SDL_Rect zone;);
	short resize_workspace(image * target,int witdh , int height );
	short resize_image(image * target, int width, int height);


#endif

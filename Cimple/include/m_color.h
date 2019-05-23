#ifndef M_COLOR_H
#define M_COLOR_H

#include <SDL2/SDL.h>
#include "m_image.h"
short negative_filter(image * img, SDL_Rect rect);
short black_and_white_filter(image * img, SDL_Rect rect);
short grey_filter(image * img,SDL_Rect rect );
short replace_color(image * img, SDL_Rect rect , SDL_Color origin_color,SDL_Color target_color,int margin);
short color_zone(image * img ,SDL_Rect rect , SDL_Color color);
short light_filter(image * img , SDL_Rect rect , int percent );
short contrast(image*, SDL_Rect rect, int percent);
#endif

#ifndef M_IMAGE_H
#define M_IMAGE_H

#define _GNU_SOURCE
#include <SDL2/SDL.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

typedef struct image image;
image *new_img(char *path);
char *get_img_name(image *img);
char *get_img_path(image *img);
char *get_img_ext(image *img);
SDL_Surface *get_img_surface(image *img);
char * get_full_image_path(image * image);
short set_img_name(image *img, char *name);
short set_img_path(image *img, char *path);
short set_img_ext(image *img, char *ext);
short set_img_surface(image *img, SDL_Surface *texture);
image * copy_image(image * ref);
void free_image(image *image);

#endif

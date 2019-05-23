	#include "m_frame.h"

/**
 * @brief
 * Returns the maximum of two numbers
 *
 * @param a first number
 * @param b second number
 * @return maximum of two integers
 */

static int get_int_max(int a, int b){
	if (a > b) return a;
	return b;
}

/**
 * @brief
 * Returns the minimum of two numbers
 *
 * @param a first number
 * @param b second number
 * @return minimum of two integers
 */

static int get_int_min(int a, int b){
	if (a < b) return a;
	return b;
}

/**
 * @brief
 * Truncates the image
 *
 * @param target image
 * @param x1 x coordinate of the first summit
 * @param y1 y coordinate of the first summit
 * @param x2 x coordinate of the second summit
 * @param y2 y coordinate of the second summit
 * @return 1 if succeded, 0 if failed
 */

short truncate_image(image *target, SDL_Rect rect){
	if (target == NULL) {
		fprintf(stderr, "Error : image is not initialised \n");
		return 0;
	}
	SDL_Surface *surface = get_img_surface(target);
	// check if surface is null
	if (surface == NULL) {
		fprintf(stderr, "Error : surface is not initialised \n");
		return 0;
	}
	int x_min = get_int_min(rect.x, rect.x + rect.w);
	int y_min = get_int_min(rect.y, rect.y + rect.h);
	int x_max = get_int_max(rect.x, rect.x + rect.w);
	int y_max = get_int_max(rect.y, rect.y + rect.h);
	if (rect.h == 0 || rect.w == 0 ||
	    x_min < 0 || y_min < 0 || x_max > surface->w || y_max > surface->h) {
		fprintf(stderr, "Error : invalid coordinates \n");
		return 0;
	}
	SDL_Rect     zone = {.x = x_min, .y = y_min, .w = x_max - x_min, .h = y_max - y_min};
	SDL_Surface *new_surface;
	new_surface = SDL_CreateRGBSurfaceWithFormat(0, zone.w, zone.h, 32, surface->format->format);
	if (SDL_MUSTLOCK(new_surface) == SDL_TRUE) SDL_LockSurface(new_surface);
	if (SDL_MUSTLOCK(surface) == SDL_TRUE) SDL_LockSurface(surface);
	Uint32 *pixels_ref = surface->pixels;
	Uint32 *pixels_new = new_surface->pixels;
	for (int i = 0; i < zone.h; i++) {
		for (int j = 0; j < zone.w; j++) {
			SDL_Color c_ref = {0};
			SDL_GetRGBA(pixels_ref[(i + zone.y) * surface->w + (j + zone.x)], new_surface->format, &c_ref.r, &c_ref.g, &c_ref.b, &c_ref.a);
			Uint32 new_color = SDL_MapRGBA(new_surface->format, c_ref.r, c_ref.g, c_ref.b, c_ref.a);
			pixels_new[i * new_surface->w + j] = new_color;
		}
	}
	SDL_UnlockSurface(new_surface);
	SDL_UnlockSurface(surface);
	if (set_img_surface(target, new_surface) == 0) {
		fprintf(stderr, "Error : surface can not be set\n");
		return 0;
	}
	return 1;
}

/**
 * @brief
 * Resizes the workspace, width and height may be negative
 *
 * @param target image
 * @param width_p the value added to the width
 * @param height_p the value added to the height
 * @return 1 if succeded, 0 if failed
 */

short resize_workspace(image *target, int width_p, int height_p){
	if (target == NULL) {
		fprintf(stderr, "Error : image is not initialised \n");
		return 0;
	}
	SDL_Surface *surface = get_img_surface(target);
	if (surface == NULL) {
		fprintf(stderr, "Error : surface is not initialised\n");
		return 0;
	}
	int width_new = surface->w + width_p;
	int height_new = surface->h + height_p;
	if (width_new <= 0 || height_new <= 0) {
		fprintf(stderr, "Error : can not resize\n");
		return 0;
	}
	SDL_Surface *new_surface;
	new_surface = SDL_CreateRGBSurfaceWithFormat(0, width_new, height_new, 32, surface->format->format);
	if (SDL_MUSTLOCK(new_surface) == SDL_TRUE) SDL_LockSurface(new_surface);
	if (SDL_MUSTLOCK(surface) == SDL_TRUE) SDL_LockSurface(surface);
	Uint32 *pixels_ref = surface->pixels;
	Uint32 *pixels_new = new_surface->pixels;
	for (int i = 0; i < height_new; i++) {
		for (int j = 0; j < width_new; j++) {
			Uint32 new_color;
			if (i > surface->h - 1 || j > surface->w - 1)
				new_color = SDL_MapRGBA(surface->format, 0, 0, 0, 255);
			else{
				SDL_Color c_ref = {0};
				SDL_GetRGBA(pixels_ref[i * surface->w + j], new_surface->format, &c_ref.r, &c_ref.g, &c_ref.b, &c_ref.a);
				new_color = SDL_MapRGBA(surface->format, c_ref.r, c_ref.g, c_ref.b, c_ref.a);
			}
			pixels_new[i * width_new + j] = new_color;
		}
	}
	SDL_UnlockSurface(new_surface);
	SDL_UnlockSurface(surface);
	if (set_img_surface(target, new_surface) == 0) {
		fprintf(stderr, "Error : surface can not be set\n");
		return 0;
	}
	return 1;
}

/**
 * @brief
 * Resizes the image
 *
 * @param target image
 * @param width new width
 * @param height new height
 * @return 1 if succeded, 0 if failed
 */


short resize_image(image *target, int width, int height){
	if (width <= 0 || height <= 0) {
		fprintf(stderr, "Error : can not resize to negative value\n");
		return 0;
	}
	if (target == NULL) {
		fprintf(stderr, "Error : image is not initialised \n");
		return 0;
	}
	SDL_Surface *surface = get_img_surface(target);
	if (surface == NULL) {
		fprintf(stderr, "Error : surface is not initialised \n");
		return 0;
	}
	SDL_Surface *new_surface;
	new_surface = SDL_CreateRGBSurfaceWithFormat(0, width, height, 32, surface->format->format);
	if (new_surface == NULL) {
		fprintf(stderr, "Error : new surface not created\n");
		return 0;
	}
	SDL_SetSurfaceBlendMode(surface, SDL_BLENDMODE_NONE);
	if (SDL_BlitScaled(surface, NULL, new_surface, NULL) != 0) {
		SDL_FreeSurface(new_surface);
		fprintf(stderr, "Error : blitscale failed\n");
		return 0;
	}
	if (set_img_surface(target, new_surface) == 0) {
		fprintf(stderr, "Error : surface can not be set\n");
		return 0;
	}
	return 1;
}

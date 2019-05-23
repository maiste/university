#include "m_transform.h"

static Uint32 *buffer = NULL;
static int     buffer_h = 0;
static int     buffer_w = 0;

/**
 * Applies vertical or horizontal symmetry on a surface
 *
 * @param target target image
 * @param vertical 1 for vertical symmetry, 0 for horizontal symmetry
 * @return 1 if success, 0 if failed
 */

short symmetry(image *target, short vertical){
	if (target == NULL) {
		fprintf(stderr, "Error : null argument in symmetry\n");
		return 0;
	}
	SDL_Surface *img = get_img_surface(target);
	if (img == NULL) {
		fprintf(stderr, "Error : null surface in symmetry\n");
		return 0;
	}
	if (vertical != 0 && vertical != 1) {
		fprintf(stderr, "Error : wrong option argument in symmetry\n");
		return 0;
	}
	SDL_Surface *new_surface;
	new_surface = SDL_CreateRGBSurfaceWithFormat(0, img->w, img->h, 32, img->format->format);
	if (new_surface == NULL) {
		fprintf(stderr, "Error : can't create texture from surface\n");
		return 0;
	}
	if (SDL_MUSTLOCK(new_surface) == SDL_TRUE) SDL_LockSurface(new_surface);
	if (SDL_MUSTLOCK(img) == SDL_TRUE) SDL_LockSurface(img);
	Uint32 *pixels_ref = img->pixels;
	Uint32 *pixels_test = new_surface->pixels;
	int     height = new_surface->h;
	int     width = new_surface->w;
	// Vertical symmetry
	if (vertical == 1)
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				SDL_Color c_ref = {0};
				SDL_GetRGBA(pixels_ref[i * width + j], new_surface->format, &c_ref.r, &c_ref.g, &c_ref.b, &c_ref.a);
				Uint32 new_color = SDL_MapRGBA(new_surface->format, c_ref.r, c_ref.g, c_ref.b, c_ref.a);
				pixels_test[i * new_surface->w + width - j - 1] = new_color;
			}
		}
	// Horizontal symmetry
	if (vertical == 0)
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				SDL_Color c_ref = {0};
				SDL_GetRGBA(pixels_ref[i * width + j], new_surface->format, &c_ref.r, &c_ref.g, &c_ref.b, &c_ref.a);
				Uint32 new_color = SDL_MapRGBA(new_surface->format, c_ref.r, c_ref.g, c_ref.b, c_ref.a);
				pixels_test[(height - i - 1) * new_surface->w + j] = new_color;
			}
		}
	SDL_UnlockSurface(new_surface);
	SDL_UnlockSurface(img);
	if (set_img_surface(target, new_surface) == 0) {
		fprintf(stderr, "Error : surface not set\n");
		return 0;
	}
	return 1;
}

/**
 * Rotates the image clockwise
 *
 * @param target image to work with
 * @param angle a positive miltiple of 90,the angle to rotate the surface
 * @param rev 1 if reversed rotation, 0 otherwise
 * @return 1 if success, 0 if failed, -1 if surface wasn't created
 */

short rotate(image *target, int angle, short rev){
	if (target == NULL) {
		fprintf(stderr, "Error : null image in rotate\n");
		return 0;
	}
	if (rev != 0 && rev != 1) {
		fprintf(stderr, "Error : invalid rev argument\n");
		return 0;
	}
	SDL_Surface *img = get_img_surface(target);
	if (target == NULL) {
		fprintf(stderr, "Error : null surface in rotate\n");
		return 0;
	}
	if(rev==1){
		angle=angle+180;
	}
	// if image is not changed (i.e. 360 degrees)
	if ((angle / 90) % 4 == 0)
		return 1;
	// create a surface to be filled
	SDL_Surface *new_surface;
	int          mod = (angle / 90) % 4;
	if (mod % 2 == 1)
		new_surface = SDL_CreateRGBSurfaceWithFormat(0, img->h, img->w, 32, img->format->format);
	if (mod % 2 == 0)
		new_surface = SDL_CreateRGBSurfaceWithFormat(0, img->w, img->h, 32, img->format->format);
	if (new_surface == NULL) {
		fprintf(stderr, "Error : can't create texture from surface\n");
		return 0;
	}
	if (SDL_MUSTLOCK(new_surface) == SDL_TRUE) SDL_LockSurface(new_surface);
	if (SDL_MUSTLOCK(img) == SDL_TRUE) SDL_LockSurface(img);
	Uint32 *pixels_ref = img->pixels;
	Uint32 *pixels_test = new_surface->pixels;
	// when image is turned once clockwise
	if (mod==1)
		for (int i = 0; i < img->h; i++) {
			for (int j = 0; j < img->w; j++) {
				SDL_Color c_ref = {0};
				SDL_GetRGBA(pixels_ref[i * img->w + j], new_surface->format, &c_ref.r, &c_ref.g, &c_ref.b, &c_ref.a);
				Uint32 new_color = SDL_MapRGBA(new_surface->format, c_ref.r, c_ref.g, c_ref.b, c_ref.a);
				pixels_test[j * new_surface->w + new_surface->w - i - 1] = new_color;
			}
		}
	// when image is turned twice clockwise
	if (mod == 2)
		for (int i = 0; i < img->h; i++) {
			for (int j = 0; j < img->w; j++) {
				SDL_Color c_ref = {0};
				SDL_GetRGBA(pixels_ref[i * img->w + j], new_surface->format, &c_ref.r, &c_ref.g, &c_ref.b, &c_ref.a);
				Uint32 new_color = SDL_MapRGBA(new_surface->format, c_ref.r, c_ref.g, c_ref.b, c_ref.a);
				pixels_test[(new_surface->h - i - 1) * new_surface->w + new_surface->w - j - 1] = new_color;
			}
		}
	// when image is turned three times clockwise
	if (mod==3)
		for (int i = 0; i < img->h; i++) {
			for (int j = 0; j < img->w; j++) {
				SDL_Color c_ref = {0};
				SDL_GetRGBA(pixels_ref[i * img->w + j], new_surface->format, &c_ref.r, &c_ref.g, &c_ref.b, &c_ref.a);
				Uint32 new_color = SDL_MapRGBA(new_surface->format, c_ref.r, c_ref.g, c_ref.b, c_ref.a);
				pixels_test[(new_surface->h - j - 1) * new_surface->w + i] = new_color;
			}
		}
	SDL_UnlockSurface(new_surface);
	SDL_UnlockSurface(img);
	if (set_img_surface(target, new_surface) == 0) {
		fprintf(stderr, "Error : surface not set\n");
		return 0;
	}
	return 1;
}

/**
 * @brief
 * Free the buffer that stores copy
 */
static void free_buffer(){
	if (buffer != NULL) {
		Uint32 *tmp = buffer;
		buffer = NULL;
		free(tmp);
	}
}

/**
 * @brief
 * Tell if the pixel is inside the image
 */
static short is_in_image(int x, int y, int w, int h){
	return (0 < y) && (y < h) && (0 < x) && (x < w);
}

/*
 * @brief
 * Copy an area into the buffer
 *
 * @param img image on which you have to copy
 * @param SDL_Rect area where you want to copy
 * @return 0 in case of error 1 else
 */
short copy(image *img, SDL_Rect area){
	if (img == NULL) {
		fprintf(stderr, "Error : null image in copy\n");
		return 0;
	}
	SDL_Surface *current = get_img_surface(img);
	if (current == NULL) {
		fprintf(stderr, "Error : null surface in copy\n");
		return 0;
	}
	free_buffer();
	buffer_h = area.h;
	buffer_w = area.w;
	buffer = malloc(area.w * area.h * sizeof(Uint32));
	if (buffer == NULL) {
		fprintf(stderr, "Error : buffer copy error\n");
		return 0;
	}
	if (SDL_MUSTLOCK(current) == SDL_TRUE) SDL_LockSurface(current);
	Uint32 *pixels = current->pixels;

	for (int i = 0; i < area.h; i++) {
		for (int j = 0; j < area.w; j++) {
			buffer[i * area.w + j] = pixels[(area.x + j) + ((area.y + i) * current->w)];
		}
	}
	SDL_UnlockSurface(current);
	return 1;
}

/*
 * @brief
 * Cut an area into the buffer
 *
 * @param img image on which you have to copy
 * @param SDL_Rect area where you want to copy
 * @return 0 in case of error 1 else
 */
short cut(image *img, SDL_Rect area){
	if (img == NULL) {
		fprintf(stderr, "Error : null image in cut\n");
		return 0;
	}
	SDL_Surface *current = get_img_surface(img);
	if (current == NULL) {
		fprintf(stderr, "Error : null surface in cut\n");
		return 0;
	}
	free_buffer();
	buffer_h = area.h;
	buffer_w = area.w;
	buffer = malloc(area.w * area.h * sizeof(Uint32));
	if (buffer == NULL) {
		fprintf(stderr, "Error : buffer cut error\n");
		return 0;
	}
	if (SDL_MUSTLOCK(current) == SDL_TRUE) SDL_LockSurface(current);
	Uint32 *pixels = current->pixels;

	for (int i = 0; i < area.h; i++) {
		for (int j = 0; j < area.w; j++) {
			buffer[i * area.w + j] = pixels[(area.x + j) + ((area.y + i) * current->w)];
			pixels[(area.x + j) + ((area.y + i) * current->w)] = 0; // Couleur noire
		}
	}
	SDL_UnlockSurface(current);
	return 1;
}

/**
 * @brief
 * Paste buffer onto img->surface
 * @param img image to paste onto
 * @param x y coordinates
 */
short paste(image *img, int x, int y){
	if (buffer == NULL) return 1;
	if (img == NULL) {
		fprintf(stderr, "Error : null image in paste\n");
		return 0;
	}
	SDL_Surface *current = get_img_surface(img);
	if (current == NULL) {
		fprintf(stderr, "Error : null surface in paste\n");
		return 0;
	}
	if (SDL_MUSTLOCK(current) == SDL_TRUE) SDL_LockSurface(current);
	Uint32 *pixels = current->pixels;
	for (int i = 0; i < buffer_h; i++) {
		for (int j = 0; j < buffer_w; j++) {
			if (is_in_image(x + j, y + i, current->w, current->h))
				pixels[(x + j) + ((y + i) * current->w)] = buffer[i * buffer_w + j];
		}
	}
	SDL_UnlockSurface(current);
	return 1;
}

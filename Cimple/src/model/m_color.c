#include "m_color.h"

#include <math.h>


/**
 * Change all surface pixels with negative filter
 *
 * @param image * img , pointer to an image structure representing an image
 * @param SDL_Rect , represent the area coordinates on the target surface
 * @return 0 if changes failed , 1 if all changes done.
 */

short negative_filter(image *img, SDL_Rect rect){
	if (img == NULL) {
		fprintf(stderr, "Error : image is not initialised \n");
		return 0;
	}
	SDL_Surface *surface = get_img_surface(img);
	SDL_Surface *neg_surface = SDL_CreateRGBSurfaceWithFormat(0, surface->w, surface->h, 32, surface->format->format);
	if (neg_surface == NULL) {
		fprintf(stderr, "Error : surface can not be set \n");
		return 0;
	}

	if (SDL_MUSTLOCK(neg_surface) == 1) SDL_LockSurface(neg_surface);
	if (SDL_MUSTLOCK(surface) == 1) SDL_LockSurface(surface);

	Uint32 *src_pixels = surface->pixels;
	Uint32 *dest_pixels = neg_surface->pixels;

	for (int i = 0; i < neg_surface->h; i++) {
		for (int j = 0; j < neg_surface->w; j++) {
			SDL_Color c = {0};
			SDL_GetRGBA(src_pixels[i * surface->w + j], neg_surface->format, &c.r, &c.g, &c.b, &c.a);
			if (i >= rect.y && i < rect.y + rect.h && j < rect.x + rect.w && j >= rect.x) {
				SDL_Color c = {0};
				SDL_GetRGBA(src_pixels[i * surface->w + j], neg_surface->format, &c.r, &c.g, &c.b, &c.a);
				Uint32 c_neg = SDL_MapRGBA(neg_surface->format, 255 - c.r, 255 - c.g, 255 - c.b, c.a);
				dest_pixels[i * neg_surface->w + j] = c_neg;
			}
			else
				dest_pixels[i * neg_surface->w + j] = SDL_MapRGBA(neg_surface->format, c.r, c.g, c.b, c.a);
		}
	}

	for (int i = rect.y; i < rect.y + rect.h; i++) {
		for (int j = rect.x; j < rect.x + rect.w; j++) {
			SDL_Color c = {0};
			SDL_GetRGBA(src_pixels[i * surface->w + j], neg_surface->format, &c.r, &c.g, &c.b, &c.a);
			Uint32 c_neg = SDL_MapRGBA(neg_surface->format, 255 - c.r, 255 - c.g, 255 - c.b, c.a);
			dest_pixels[i * neg_surface->w + j] = c_neg;
		}
	}

	SDL_UnlockSurface(neg_surface);
	SDL_UnlockSurface(surface);
	set_img_surface(img, neg_surface);

	return 1;
}

/**
 * Change all surface pixels with black and white filter
 *
 * @param image * img , pointer to an image structure representing an image
 * @param SDL_Rect , represent the area coordinates on the target surface
 * @return 0 if changes failed , 1 if all changes done.
 */

short black_and_white_filter(image *img, SDL_Rect rect){
	if (img == NULL) {
		fprintf(stderr, "Error : image is not initialised \n");
		return 0;
	}
	SDL_Surface *surface = get_img_surface(img);
	SDL_Surface *bnw_surface = SDL_CreateRGBSurfaceWithFormat(0, surface->w, surface->h, 32, surface->format->format);
	if (bnw_surface == NULL) {
		fprintf(stderr, "Error : surface can not be set\n");
		return 0;
	}

	if (SDL_MUSTLOCK(bnw_surface) == 1) SDL_LockSurface(bnw_surface);
	if (SDL_MUSTLOCK(surface) == 1) SDL_LockSurface(surface);

	Uint32 *src_pixels = surface->pixels;
	Uint32 *dest_pixels = bnw_surface->pixels;

	for (int i = 0; i < bnw_surface->h; i++) {
		for (int j = 0; j < bnw_surface->w; j++) {
			SDL_Color c = {0};
			SDL_GetRGBA(src_pixels[i * surface->w + j], bnw_surface->format, &c.r, &c.g, &c.b, &c.a);
			if (i >= rect.y && i < rect.y + rect.h && j < rect.x + rect.w && j >= rect.x) {
				int    gray_scale = (int)c.r * 0.2125 + c.g * 0.7154 + c.b * 0.0721;     /*CIE 709 recommandation for grayscale*/
				Uint32 c_bnw;
				if (gray_scale < 128)
					c_bnw = SDL_MapRGBA(bnw_surface->format, 0, 0, 0, c.a);
				else
					c_bnw = SDL_MapRGBA(bnw_surface->format, 255, 255, 255, c.a);
				dest_pixels[i * bnw_surface->w + j] = c_bnw;
			}
			else
				dest_pixels[i * bnw_surface->w + j] = SDL_MapRGBA(bnw_surface->format, c.r, c.g, c.b, c.a);
		}
	}

	SDL_UnlockSurface(bnw_surface);
	SDL_UnlockSurface(surface);
	set_img_surface(img, bnw_surface);

	return 1;
}

/**
 * Change all surface pixels with grey filter
 *
 * @param image * img , pointer to image structure representing an image
 * @param SDL_Rect , represent the area coordinates on the target surface
 * @return 0 if changes failed , 1 if all changes done.
 */

short grey_filter(image *img, SDL_Rect rect){
	if (img == NULL) {
		fprintf(stderr, "Error : image is not initialised \n");
		return 0;
	}

	SDL_Surface *surface = get_img_surface(img);
	SDL_Surface *gray_surface = SDL_CreateRGBSurfaceWithFormat(0, surface->w, surface->h, 32, surface->format->format);
	if (gray_surface == NULL) {
		fprintf(stderr, "Error : surface can not be set\n");
		return 0;
	}

	if (SDL_MUSTLOCK(gray_surface) == 1) SDL_LockSurface(gray_surface);
	if (SDL_MUSTLOCK(surface) == 1) SDL_LockSurface(surface);

	Uint32 *src_pixels = surface->pixels;
	Uint32 *dest_pixels = gray_surface->pixels;

	for (int i = 0; i < gray_surface->h; i++) {
		for (int j = 0; j < gray_surface->w; j++) {
			SDL_Color c = {0};
			SDL_GetRGBA(src_pixels[i * surface->w + j], gray_surface->format, &c.r, &c.g, &c.b, &c.a);
			if (i >= rect.y && i < rect.y + rect.h && j < rect.x + rect.w && j >= rect.x) {
				int    gray_scale = (int)((c.r * 0.2125) + (c.g * 0.7154) + (c.b * 0.0721));    /*CIE 709 recommandation for grayscale*/
				Uint32 c_gray = SDL_MapRGBA(gray_surface->format, gray_scale, gray_scale, gray_scale, c.a);
				dest_pixels[i * gray_surface->w + j] = c_gray;
			}
			else
				dest_pixels[i * gray_surface->w + j] = SDL_MapRGBA(gray_surface->format, c.r, c.g, c.b, c.a);
		}
	}

	SDL_UnlockSurface(gray_surface);
	SDL_UnlockSurface(surface);
	set_img_surface(img, gray_surface);

	return 1;
}

/**
 * calculates the percentage of distance between two colors
 * @param SDL_Color current_color , color to compare
 * @param SDL_Color origin_color , color to compare
 * @param int margin , margin percentage
 * @return short , 0 if the distance between the two colors is bigger than the percentage , 1 if the distance is acceptable
 */

static short margin_colors(SDL_Color current_color, SDL_Color origin_color, int margin){
	if (margin == 0) return 1;
	double delta_r = current_color.r - origin_color.r;
	double delta_g = current_color.g - origin_color.g;
	double delta_b = current_color.b - origin_color.b;
	double r_canal = (current_color.r + origin_color.r) / 2;
	double eucli_dist = sqrt(((2 + (r_canal / 256)) * delta_r * delta_r)
	                         + (4 * (delta_g * delta_g))
	                         + ((2 + ((255 - r_canal) / 256)) * delta_b * delta_b));
	double margin_pourcent = (eucli_dist / (sqrt((255 * 255) * 15))) * 100;
	if (margin_pourcent <= (double)margin) return 1;
	return 0;
}

/**
 * Change surface pixels color with an another (neighboring) color
 *
 * @param image * img , pointer to image structure representing an image
 * @param SDL_Rect , represent the area coordinates on the target surface
 * @param SDL_Color origin , represent the color to replace
 * @param SDL_Color target_color , represent the color to apply
 * @param int margin , represent the percentage of color proximity level
 * @return 0 if changes failed , 1 if all changes done.
 */

short replace_color(image *img, SDL_Rect rect, SDL_Color origin_color, SDL_Color target_color, int margin){
	if (img == NULL) {
		fprintf(stderr, "Error : image is not initialised \n");
		return 0;
	}
	SDL_Surface *surface = get_img_surface(img);
	SDL_Surface *repl_surface = SDL_CreateRGBSurfaceWithFormat(0, surface->w, surface->h, 32, surface->format->format);
	if (repl_surface == NULL) {
		fprintf(stderr, "Error : surface can not be set\n");
		return 0;
	}

	if (SDL_MUSTLOCK(repl_surface) == SDL_TRUE) SDL_LockSurface(repl_surface);
	if (SDL_MUSTLOCK(surface) == SDL_TRUE) SDL_LockSurface(surface);

	Uint32 *src_pixels = surface->pixels;
	Uint32 *dest_pixels = repl_surface->pixels;
	for (int i = 0; i < repl_surface->h; i++) {
		for (int j = 0; j < repl_surface->w; j++) {
			SDL_Color c = {0};
			SDL_GetRGBA(src_pixels[i * surface->w + j], repl_surface->format, &c.r, &c.g, &c.b, &c.a);
			if (i >= rect.y && i < rect.y + rect.h && j < rect.x + rect.w && j >= rect.x) {
				Uint32 repl_c;
				if (margin_colors(c, origin_color, margin) == 1)
					repl_c = SDL_MapRGBA(repl_surface->format, target_color.r, target_color.g, target_color.b, target_color.a);
				else
					repl_c = src_pixels[i * surface->w + j];
				dest_pixels[i * repl_surface->w + j] = repl_c;
			}
			else
				dest_pixels[i * repl_surface->w + j] = SDL_MapRGBA(repl_surface->format, c.r, c.g, c.b, c.a);
		}
	}

	SDL_UnlockSurface(repl_surface);
	SDL_UnlockSurface(surface);
	set_img_surface(img, repl_surface);

	return 1;
}

/**
 * Fill rectangle on image surface
 *
 * @param img * img , pointer to image structure representing an image
 * @param SDL_color , represents the color to apply in the chosen area
 * @param SDL_Rect rect , represent the area coordinates on the target surface
 * @return 0 if changes failed , 1 if all changes done.
 */
short color_zone(image *img, SDL_Rect rect, SDL_Color color){
	if (img == NULL) {
		fprintf(stderr, "Error : image is not initialised \n");
		return 0;
	}
	SDL_Surface *surface = get_img_surface(img);
	SDL_Surface *zone_surface = SDL_CreateRGBSurfaceWithFormat(0, surface->w, surface->h, 32, surface->format->format);
	if (zone_surface == NULL) {
		fprintf(stderr, "Error : surface can not be set\n");
		return 0;
	}
	if (SDL_BlitSurface(surface, NULL, zone_surface, NULL) != 0) {
		fprintf(stderr, "Error : can not blit surface");
		return 0;
	}

	if (SDL_MUSTLOCK(zone_surface) == 1) SDL_LockSurface(zone_surface);
	/*if(SDL_MUSTLOCK(img) == 1) SDL_LockSurface(surface);*/

	Uint32 r_color = SDL_MapRGBA(zone_surface->format, color.r, color.g, color.b, color.a);
	SDL_FillRect(zone_surface, &rect, r_color);

	SDL_UnlockSurface(zone_surface);

	set_img_surface(img, zone_surface);

	return 1;
}

/**
 * Keep color between 0 and 255
 */
static Uint8 keep_format(int color){
	if (color < 0)
		return 0;
	if (color > 255)
		return 255;
	return color;
}

/**
 * Change lumosity on image surface
 *
 * @param img * img , pointer to image structure representing an image
 * @param int percentage , degree of luminosity percentage
 * @param SDL_Rect rect , represent the area coordinates on the target surface
 * @return 0 if changes failed , 1 if all changes done.
 */


short light_filter(image *img, SDL_Rect rect, int percent){
	if (img == NULL) {
		fprintf(stderr, "Error : image is not initialised \n");
		return 0;
	}

	SDL_Surface *surface = get_img_surface(img);
	SDL_Surface *light_surface = SDL_CreateRGBSurfaceWithFormat(0, surface->w, surface->h, 32, surface->format->format);
	if (light_surface == NULL) {
		fprintf(stderr, "Error : surface can not be set\n");
		return 0;
	}

	if (SDL_MUSTLOCK(surface) == 1) SDL_UnlockSurface(surface);
	if (SDL_MUSTLOCK(light_surface) == 1) SDL_UnlockSurface(light_surface);

	Uint32 *src_pixels = surface->pixels;
	Uint32 *dest_pixels = light_surface->pixels;

	for (int i = 0; i < light_surface->h; i++) {
		for (int j = 0; j < light_surface->w; j++) {
			SDL_Color c = {0};
			SDL_GetRGBA(src_pixels[i * surface->w + j], light_surface->format, &c.r, &c.g, &c.b, &c.a);
			if (i >= rect.y && i < rect.y + rect.h && j < rect.x + rect.w && j >= rect.x) {
				Uint32 light_c = SDL_MapRGBA(light_surface->format, keep_format(c.r + percent), keep_format(c.g + percent), keep_format(c.b + percent), c.a);
				dest_pixels[i * light_surface->w + j] = light_c;
			}
			else
				dest_pixels[i * light_surface->w + j] = SDL_MapRGBA(light_surface->format, c.r, c.g, c.b, c.a);
		}
	}

	SDL_UnlockSurface(light_surface);
	SDL_UnlockSurface(surface);
	if (!set_img_surface(img, light_surface))
		return 0;
	return 1;
}

/**
 * Get the color for contrast transformation
 */
static Uint32 get_new_pixel(SDL_Color c, SDL_PixelFormat *format, double contrast){
	// ((colour scale - median color) * percent contrast + median color) * scale)
	Uint8 red = keep_format((int)((((c.r / 255.0) - 0.5) * contrast + 0.5) * 255.0));
	Uint8 blue = keep_format((int)((((c.g / 255.0) - 0.5) * contrast + 0.5) * 255.0));
	Uint8 green = keep_format((int)((((c.b / 255.0) - 0.5) * contrast + 0.5) * 255.0));
	return SDL_MapRGBA(format, red, blue, green, c.a);
}

/**
 * @brief
 * Change image contrast
 * @param img image with surface
 * @param rect zone where apply contrast
 * @param percent rate of contrast
 */
short contrast(image *img, SDL_Rect rect, int percent){
	if (img == NULL) {
		fprintf(stderr, "Error : image is not initialised \n");
		return 0;
	}

	SDL_Surface *surface = get_img_surface(img);
	SDL_Surface *new_surface = SDL_CreateRGBSurfaceWithFormat(0, surface->w, surface->h, 32, surface->format->format);
	if (new_surface == NULL) {
		fprintf(stderr, "Error : surface can not be set\n");
		return 0;
	}

	if (SDL_MUSTLOCK(surface) == 1) SDL_UnlockSurface(surface);
	if (SDL_MUSTLOCK(new_surface) == 1) SDL_UnlockSurface(new_surface);


	// Calculate amount of contrast
	double contrast = pow((100.0 + percent) / 100.0, 2);

	Uint32 *src_pixels = surface->pixels;
	Uint32 *new_pixels = new_surface->pixels;
	for (int i = 0; i < new_surface->h; i++) {
		for (int j = 0; j < new_surface->w; j++) {
			SDL_Color c = {0};
			SDL_GetRGBA(src_pixels[i * surface->w + j], surface->format, &c.r, &c.g, &c.b, &c.a);
			if (i >= rect.y && i < rect.y + rect.h && j < rect.x + rect.w && j >= rect.x) {
				Uint32 contrast_pixel = get_new_pixel(c, surface->format, contrast);
				new_pixels[i * surface->w + j] = contrast_pixel;
			}
			else
				new_pixels[i * surface->w + j] = SDL_MapRGBA(surface->format, c.r, c.g, c.b, c.a);
		}
	}


	SDL_UnlockSurface(new_surface);
	SDL_UnlockSurface(surface);
	if (!set_img_surface(img, new_surface))
		return 0;
	return 1;
}

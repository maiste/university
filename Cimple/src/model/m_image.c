#include "m_image.h"

struct image {
	char *       name;
	char *       save_path;
	char *       extension;
	SDL_Surface *surface;
};



/**
 * @brief Normalize a path with UNIX
 * @param path the original path
 * @return the new path
 */
static char *normalize_path(char *path){
	int   size = strlen(path);
	char *n_path;
	if (size < 2 || path[0] == '/' || (path[0] == '.' && path[1] == '/')) {
		n_path = malloc(size + 1);
		if (n_path == NULL)
			return NULL;
		memcpy(n_path, path, size + 1);
	}
	else {
		n_path = malloc(size + 3);
		if (n_path == NULL)
			return NULL;
		n_path[0] = '.';
		n_path[1] = '/';
		memcpy(n_path + 2, path, size + 1);
	}
	return n_path;
}

/**
 * Fills the struct image elements according to path
 *
 * @param init_path string containing the full path to the image (i.e. /toto/tata/image.png)
 * @param path pointer to write the path to (i.e. /toto/tata)
 * @param name pointer to write the image name
 * @param ext pointer to write the image extension
 * @return extension of the image
 */

static short break_full_path(char *init_path, char **path, char **name, char **ext){
	char *slash_p = memrchr(init_path, '/', strlen(init_path));
	if (slash_p == NULL) {
		fprintf(stderr, "Error : invalid path\n");
		return 0;
	}
	char *dot_p = memrchr(init_path, '.', strlen(init_path));
	if (dot_p == NULL) {
		fprintf(stderr, "Error : path extension not found\n");
		return 0;
	}
	//path
	char *new_path = malloc((slash_p - init_path) + 1);
	if (new_path == NULL) {
		fprintf(stderr, "Error : new path\n");
		return 0;
	}
	memcpy(new_path, init_path, (slash_p - init_path) + 1);
	new_path[slash_p - init_path] = '\0';
	*path = new_path;
	//name
	char *new_name = malloc(dot_p - slash_p);
	if (new_name == NULL) {
		fprintf(stderr, "Error : new path name\n");
		free(new_path);
		return 0;
	}
	memcpy(new_name, slash_p + 1, dot_p - slash_p - 1);
	new_name[dot_p - slash_p - 1] = '\0';
	*name = new_name;
	//extension
	char *new_ext = malloc(strlen(dot_p));
	if (new_ext == NULL) {
		fprintf(stderr, "Error : new path extension");
		free(new_name);
		free(new_path);
		return 0;
	}
	memcpy(new_ext, dot_p + 1, strlen(dot_p));
	new_ext[strlen(dot_p)] = '\0';
	*ext = new_ext;
	return 1;
}

/**
 * Creates a brand new image
 *
 * @param path string containing the path to the image
 * @return new image
 */

image *new_img(char *path){
	// checking if it's a valid path
	if (path == NULL) {
		fprintf(stderr, "Error : path not valid\n");
		return NULL;
	}
	char *n_path = normalize_path(path);
	if (n_path == NULL) {
		fprintf(stderr, "Error : cannot normalize path\n");
		return NULL;
	}
	// allocate memory
	image *new = malloc(sizeof(struct image));
	if (new == NULL) {
		fprintf(stderr, "Error : image not initialized\n");
		free(n_path);
		return NULL;
	}
	memset(new, 0, sizeof(image));
	if (!break_full_path(n_path, &new->save_path, &new->name, &new->extension)) {
		fprintf(stderr, "Error : image not initialized\n");
		free(new);
		free(n_path);
		return NULL;
	}
	new->surface = NULL;
	free(n_path);
	return new;
}

/**
 * Gets the image name
 *
 * @param img the image to work with
 * @return image name
 */

char *get_img_name(image *img){
	if (img == NULL || img->name == NULL) {
		fprintf(stderr, "Error : cannot find image name\n");
		return NULL;
	}
	return img->name;
}

/**
 * Gets the image path
 *
 * @param img the image to work with
 * @return image path
 */

char *get_img_path(image *img){
	if (img == NULL || img->save_path == NULL) {
		fprintf(stderr, "Error : cannot find image\n");
		return NULL;
	}
	return img->save_path;
}

/**
 * Gets the image extension
 *
 * @param img the image to work with
 * @return image extension
 */

char *get_img_ext(image *img){
	if (img == NULL || img->extension == NULL) {
		fprintf(stderr, "Error : image extension failed\n");
		return NULL;
	}
	return img->extension;
}

/**
 * Gets the surface of the image
 *
 * @param img the image to work with
 * @return SDL_Surface element of the structure
 */

SDL_Surface *get_img_surface(image *img){
	if (img == NULL || img->surface == NULL) {
		fprintf(stderr, "Error : cannot find image\n");
		return NULL;
	}
	return img->surface;
}

/**
 * Return full path of the image
 *
 * @param img the image to work with
 * @return string with the full path (i.e. /toto/tata/imagr.png)
 */

char *get_full_image_path(image *image){
	if (image == NULL) {
		fprintf(stderr, "Error : image is not initialized");
		return NULL;
	}
	int   size_fullpath = strlen(image->extension) + strlen(image->save_path) + strlen(image->name) + 3;
	char *fullpath = malloc(size_fullpath);
	if (fullpath == NULL) {
		fprintf(stderr, "Error : cannot allocate fullpath\n");
		return NULL;
	}
	snprintf(fullpath, size_fullpath, "%s/%s.%s", image->save_path, image->name, image->extension);
	return fullpath;
}

/**
 * Sets a new image name
 *
 * @param img the image to work with
 * @param name new name
 * @return <0 if failed, >=0 if succeed
 */

short set_img_name(image *img, char *name){
	if (img == NULL || name == NULL) {
		fprintf(stderr, "Error : image cannot be find or is not initialized correctly\n");
		return 0;
	}
	char *tmp = img->name;
	img->name = name;
	free(tmp);
	if (img->name == NULL) {
		free(img);
		return 0;
	}
	return 1;
}

/**
 * Sets the image path
 *
 * @param img the image to work with
 * @param name new path
 * @return <0 if failed, >=0 if succeed
 */

short set_img_path(image *img, char *path){
	if (img == NULL || path == NULL) {
		fprintf(stderr, "Error : image cannot be find or is not initialized correctly\n");
		return 0;
	}
	char *tmp = img->save_path;
	img->save_path = path;
	free(tmp);
	if (img->save_path == NULL) {
		free(img);
		return 0;
	}
	return 1;
}

/**
 * Sets a new extension for the image
 *
 * @param img the image to work with
 * @param name new extension
 * @return <0 if failed, >=0 if succeed
 */

short set_img_ext(image *img, char *ext){
	if (img == NULL || ext == NULL) {
		fprintf(stderr, "Error : image cannot be find or extension is not initialized correctly\n");
		return 0;
	}
	char *tmp = img->extension;
	img->extension = ext;
	free(tmp);
	if (img->extension == NULL) {
		free(img);
		return 0;
	}
	return 1;
}

/**
 * Sets a new surface for the image
 *
 * @param img the image to work with
 * @param name new SDL_Surface
 * @return <0 if failed, >=0 if succeed
 */

short set_img_surface(image *img, SDL_Surface *surface){
	if (img == NULL || surface == NULL) {
		fprintf(stderr, "Error : image cannot be find or surface is not initialized correctly\n");
		return 0;
	}
	SDL_Surface *image_to_free = img->surface;
	img->surface = surface;
	if (image_to_free != NULL)
		SDL_FreeSurface(image_to_free);
	return 1;
}

/**
 * Returns a copy of an image
 *
 * @param img the image to work with
 * @return pointer to the new image
 */

image *copy_image(image *ref){
	char *path = get_full_image_path(ref);
	if (path == NULL) {
		fprintf(stderr, "Error : copy failed\n");
		return NULL;
	}
	image *copy = new_img(path);
	if (copy == NULL) {
		fprintf(stderr, "Error : copy failed\n");
		return NULL;
	}
	SDL_Surface *copy_surface = SDL_ConvertSurface(ref->surface, ref->surface->format, 0);
	if (copy_surface == NULL) {
		fprintf(stderr, "Error : copy failed\n");
		free_image(copy);
		return NULL;
	}
	copy->surface = copy_surface;
	return copy;
}

/**
 * Frees the image structure
 *
 * @param img the image free
 */

void free_image(image *image){
	if (image != NULL) {
		if (image->surface != NULL) SDL_FreeSurface(image->surface);
		if (image->extension != NULL) free(image->extension);
		if (image->save_path != NULL) free(image->save_path);
		if (image->name != NULL) free(image->name);
		free(image);
	}
}

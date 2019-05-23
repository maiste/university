 #include "in.h"

/**
 * Allow to load an image referenced by
 * [path]
 */
image *load_image(char *path){
	image *img = new_img(path);
	if (img == NULL) {
		fprintf(stderr, "Error : wrong image path.\n");
		return NULL;
	}
	SDL_Surface *tmp = IMG_Load(path);
	if (tmp == NULL) {
		fprintf(stderr, "Error : can't load image.\n");
		free_image(img);
		return NULL;
	}
	SDL_PixelFormat *format = SDL_AllocFormat(SDL_PIXELFORMAT_RGBA8888);
	if (format == NULL) {
		fprintf(stderr, "Error : can't allocate format.\n");
		SDL_FreeSurface(tmp);
		free_image(img);
		return NULL;
	}
	SDL_Surface *copy = SDL_ConvertSurface(tmp, format, 0);
	SDL_FreeSurface(tmp);
	if (copy == NULL) {
		fprintf(stderr, "Error : can't convert image.\n");
		SDL_FreeFormat(format);
		return NULL;
	}
	set_img_surface(img, copy);
	SDL_FreeFormat(format);
	return img;
}

short check_tmp(){
	char *tmp_dir = "/tmp/cimpletmp/";
	DIR * dir = opendir(tmp_dir);
	int   ret = 0;
	if (dir == NULL) {
		printf("No file saved\n");
		return ret;
	}
	struct dirent *current;
	while ((current = readdir(dir)) != NULL) {
		if (memcmp(current->d_name, "./", 2) == 0 ||
		    memcmp(current->d_name, "../", 3) == 0)
			continue;
		ret += 1;
		printf("%s%s", tmp_dir, current->d_name);
	}
	closedir(dir);
	return ret;
}

#include "m_tests.h"

static void compare_surface(SDL_Surface *test_surface, SDL_Surface *ref_surface, int diff){
	SDL_PixelFormat *pixel_format = SDL_AllocFormat(SDL_PIXELFORMAT_RGBA8888);
	if (pixel_format == NULL) {
		perror("PixelFormat");
		exit(1);
	}
	assert_int_equal(ref_surface->w, test_surface->w);
	assert_int_equal(ref_surface->h, test_surface->h);
	Uint32 *pixels_ref = ref_surface->pixels;
	Uint32 *pixels_test = test_surface->pixels;
	SDL_LockSurface(test_surface);
	SDL_LockSurface(ref_surface);
	int min, max;
	for (int i = 0; i < ref_surface->h; i++) {
		for (int j = 0; j < ref_surface->w; j++) {
			SDL_Color c_ref = {0};
			SDL_Color c_test = {0};
			SDL_GetRGBA(pixels_ref[i * ref_surface->w + j], pixel_format, &c_ref.r, &c_ref.g, &c_ref.b, &c_ref.a);
			SDL_GetRGBA(pixels_test[i * test_surface->w + j], pixel_format, &c_test.r, &c_test.g, &c_test.b, &c_test.a);
			min = c_ref.r - diff;
			if (min < 0) min = 0;
			max = c_ref.r + diff;
			if (max > 255) max = 255;
			assert_in_range(c_test.r, min, max);
			min = c_ref.g - diff;
			if (min < 0) min = 0;
			max = c_ref.g + diff;
			if (max > 255) max = 255;
			assert_in_range(c_test.g, min, max);
			min = c_ref.b - diff;
			if (min < 0) min = 0;
			max = c_ref.b + diff;
			if (max > 255) max = 255;
			assert_in_range(c_test.b, min, max);
		}
	}
	SDL_UnlockSurface(test_surface);
	SDL_UnlockSurface(ref_surface);
	SDL_FreeFormat(pixel_format);
}

static void negative_filter_test(void **state){
	//Loading of the reference image and the image to test
	image *test_image = load_image("tests/m_test/test_image.png");
	image *ref_image = load_image("tests/m_test/test_image_negative.png");
	//Loading corresponding surfaces
	SDL_Surface *ref_surface = get_img_surface(ref_image);
	//Applying the negative effect
	SDL_Rect rect = {.x = 0, .y = 0, .w = ref_surface->w, .h = ref_surface->h};
	negative_filter(test_image, rect);
	SDL_Surface *test_surface = get_img_surface(test_image);
	//Launching test
	compare_surface(test_surface, ref_surface, 15);
	//Closing images
	free_image(test_image);
	free_image(ref_image);
}

static void black_and_white_filter_test(void **state){
	//Loading of the reference image and the image to test
	image *test_image = load_image("tests/m_test/test_image.png");
	image *ref_image = load_image("tests/m_test/test_image_bnw.png");
	//Loading corresponding surfaces
	SDL_Surface *ref_surface = get_img_surface(ref_image);
	//Applying the black and white filter
	SDL_Rect rect = {.x = 0, .y = 0, .w = ref_surface->w, .h = ref_surface->h};
	black_and_white_filter(test_image, rect);
	SDL_Surface *test_surface = get_img_surface(test_image);
	//Launching test
	compare_surface(test_surface, ref_surface, 0);
	//Closing images
	free_image(test_image);
	free_image(ref_image);
}

static void grey_filter_test(void **state){
	//Loading of the reference image and the image to test
	image *test_image = load_image("tests/m_test/test_image.png");
	image *ref_image = load_image("tests/m_test/test_image_grayscale.png");
	//Loading corresponding surfaces
	SDL_Surface *ref_surface = get_img_surface(ref_image);
	//Applying the grayscale effect
	SDL_Rect rect = {.x = 0, .y = 0, .w = ref_surface->w, .h = ref_surface->h};
	grey_filter(test_image, rect);
	SDL_Surface *test_surface = get_img_surface(test_image);
	//Launching test
	compare_surface(test_surface, ref_surface, 10);
	//Closing images
	free_image(test_image);
	free_image(ref_image);
}

static void color_zone_test(void **state){
	//Loading the image to test
	image *test_image = load_image("tests/m_test/test_image.png");
	//Draw an orange rectangle
	SDL_Color color = {.r = 255, .g = 165, .b = 0, .a = 255};
	SDL_Rect  rect = {.x = 10, .y = 10, .w = 40, .h = 40};
	color_zone(test_image, rect, color);
	SDL_Surface *test_surface = get_img_surface(test_image);
	//Test the image
	SDL_PixelFormat *pixel_format = SDL_AllocFormat(SDL_PIXELFORMAT_RGBA8888);
	if (pixel_format == NULL) {
		perror("PixelFormat");
		exit(1);
	}
	Uint32 *pixels_test = test_surface->pixels;
	SDL_LockSurface(test_surface);
	for (int i = rect.y; i < rect.h; i++) {
		for (int j = rect.x; j < rect.w; j++) {
			SDL_Color c_test = {0};
			SDL_GetRGBA(pixels_test[i * test_surface->w + j], pixel_format, &c_test.r, &c_test.g, &c_test.b, &c_test.a);
			assert_int_equal(c_test.r, color.r);
			assert_int_equal(c_test.g, color.g);
			assert_int_equal(c_test.b, color.b);
			assert_int_equal(c_test.a, color.a);
		}
	}
	SDL_UnlockSurface(test_surface);
	SDL_FreeFormat(pixel_format);
	free_image(test_image);
}

static void symmetry_test(void **state){
	//Loading the images for tests
	image *test_image = load_image("tests/m_test/test_image.png");
	image *ref_image = load_image("tests/m_test/test_image.png");
	//Loading corresponding surfaces
	SDL_Surface *    ref_surface = get_img_surface(ref_image);
	SDL_Surface *    test_surface;
	SDL_PixelFormat *pixel_format = SDL_AllocFormat(SDL_PIXELFORMAT_RGBA8888);
	if (pixel_format == NULL) {
		perror("PixelFormat");
		exit(1);
	}
	Uint32 *pixels_ref = ref_surface->pixels;
	Uint32 *pixels_test;
	//Testing vertical symmetry
	symmetry(test_image, 1);
	test_surface = get_img_surface(test_image);
	assert_int_equal(ref_surface->w, test_surface->w);
	assert_int_equal(ref_surface->h, test_surface->h);
	SDL_LockSurface(test_surface);
	SDL_LockSurface(ref_surface);
	pixels_test = test_surface->pixels;
	for (int i = 0; i < test_surface->h; i++) {
		for (int j = 0; j < test_surface->w; j++) {
			SDL_Color c_test = {0};
			SDL_Color c_ref = {0};
			SDL_GetRGBA(pixels_ref[i * test_surface->w + j], pixel_format, &c_ref.r, &c_ref.g, &c_ref.b, &c_ref.a);
			SDL_GetRGBA(pixels_test[i * test_surface->w + (test_surface->w - j - 1)], pixel_format, &c_test.r, &c_test.g, &c_test.b, &c_test.a);
			assert_int_equal(c_test.r, c_ref.r);
			assert_int_equal(c_test.g, c_ref.g);
			assert_int_equal(c_test.b, c_ref.b);
			assert_int_equal(c_test.a, c_ref.a);
		}
	}
	free_image(test_image);
	//Testing horizontal symmetry
	test_image = load_image("tests/m_test/test_image.png");
	symmetry(test_image, 0);
	test_surface = get_img_surface(test_image);
	SDL_LockSurface(test_surface);
	assert_int_equal(ref_surface->w, test_surface->w);
	assert_int_equal(ref_surface->h, test_surface->h);
	pixels_test = test_surface->pixels;
	for (int i = 0; i < test_surface->h; i++) {
		for (int j = 0; j < test_surface->w; j++) {
			SDL_Color c_test = {0};
			SDL_Color c_ref = {0};
			SDL_GetRGBA(pixels_ref[i * test_surface->w + j], pixel_format, &c_ref.r, &c_ref.g, &c_ref.b, &c_ref.a);
			SDL_GetRGBA(pixels_test[(test_surface->h - i - 1) * test_surface->w + j], pixel_format, &c_test.r, &c_test.g, &c_test.b, &c_test.a);
			assert_int_equal(c_test.r, c_ref.r);
			assert_int_equal(c_test.g, c_ref.g);
			assert_int_equal(c_test.b, c_ref.b);
			assert_int_equal(c_test.a, c_ref.a);
		}
	}
	//Closing everything
	SDL_UnlockSurface(test_surface);
	SDL_UnlockSurface(ref_surface);
	SDL_FreeFormat(pixel_format);
	free_image(test_image);
	free_image(ref_image);
}

static void rotate_test(void **state){
	//Loading the images for tests
	image *test_image = load_image("tests/m_test/test_image.png");
	image *ref_image = load_image("tests/m_test/test_image.png");
	//Loading corresponding surfaces
	SDL_Surface *    ref_surface = get_img_surface(ref_image);
	SDL_PixelFormat *pixel_format = SDL_AllocFormat(SDL_PIXELFORMAT_RGBA8888);
	if (pixel_format == NULL) {
		perror("PixelFormat");
		exit(1);
	}
	if (SDL_MUSTLOCK(ref_surface) == SDL_TRUE) SDL_LockSurface(ref_surface);
	//Testing rotate
	rotate(test_image, 90, 0);
	SDL_Surface *test_surface = get_img_surface(test_image);
	if (SDL_MUSTLOCK(test_surface) == SDL_TRUE) SDL_LockSurface(test_surface);
	Uint32 *pixels_ref = ref_surface->pixels;
	Uint32 *pixels_test = test_surface->pixels;
	//Checking dimensions
	assert_int_equal(test_surface->w, ref_surface->h);
	assert_int_equal(test_surface->h, ref_surface->w);
	//Checking if pixels moved correctly
	for (int i = 0; i < ref_surface->h; i++) {
		for (int j = 0; j < ref_surface->w; j++) {
			SDL_Color c_test = {0};
			SDL_Color c_ref = {0};
			SDL_GetRGBA(pixels_ref[i * ref_surface->w + j], pixel_format, &c_ref.r, &c_ref.g, &c_ref.b, &c_ref.a);
			SDL_GetRGBA(pixels_test[j * ref_surface->h + ref_surface->h - i - 1], pixel_format, &c_test.r, &c_test.g, &c_test.b, &c_test.a);
			assert_int_equal(c_test.r, c_ref.r);
			assert_int_equal(c_test.g, c_ref.g);
			assert_int_equal(c_test.b, c_ref.b);
			assert_int_equal(c_test.a, c_ref.a);
		}
	}
	SDL_UnlockSurface(test_surface);
	SDL_UnlockSurface(ref_surface);
	SDL_FreeFormat(pixel_format);
	free_image(test_image);
	free_image(ref_image);
}

static void resize_workspace_test(void **state){
	//Loading the image to test
	image *test_image = load_image("tests/m_test/test_image.png");
	//Loading corresponding surface
	SDL_Surface *test_surface = get_img_surface(test_image);
	int          w_before, h_before, w_after, h_after;
	int          width = 100, height = 100;
	w_before = test_surface->w;
	h_before = test_surface->h;
	//Resizing
	resize_workspace(test_image, width, height);
	//Launching test
	test_surface = get_img_surface(test_image);
	w_after = test_surface->w;
	h_after = test_surface->h;
	assert_int_equal(w_after, w_before + width);
	assert_int_equal(h_after, h_before + height);
	//Closing image
	free_image(test_image);
}

static void new_img_test(void **state){
	image *img = new_img("tests/m_test/test_image.png");
	assert_non_null(img);
}

// m_*_test functions test the getters and setters for a struct image fields

static void m_img_surface_test(void **state){
	image *      img = new_img("tests/m_test/test_image.png");
	SDL_Surface *surface = SDL_CreateRGBSurface(0, 100, 100, 32, 0, 0, 0, 0);
	set_img_surface(img, surface);
	assert_ptr_equal(surface, get_img_surface(img));
	free_image(img);
}

static void m_img_name_test(void **state){
	image *img = new_img("tests/m_test/test_image.png ");
	char * new_name = malloc(8 * sizeof(char));
	if (new_name != NULL) {
		memcpy(new_name, "newname", 8);
		set_img_name(img, new_name);
		assert_string_equal(new_name, get_img_name(img));
	}
	free_image(img);
}

static void m_img_path_test(void **state){
	image *img = new_img("tests/m_test/test_image.png");
	char * new_path = malloc(12 * sizeof(char));
	if (new_path != NULL) {
		memcpy(new_path, "tests/hello", 12);
		set_img_path(img, new_path);
		assert_string_equal(new_path, get_img_path(img));
	}
	free_image(img);
}

static void m_img_ext_test(void **state){
	image *img = new_img("tests/m_test/test_image.png");
	char * new_ext = malloc(8 * sizeof(char));
	if (new_ext != NULL) {
		memcpy(new_ext, "new_ext", 8);
		set_img_ext(img, new_ext);
		assert_string_equal(new_ext, get_img_ext(img));
	}
	free_image(img);
}

int run_m_tests(){
	const struct CMUnitTest tests[] = {
		unit_test(negative_filter_test),
		unit_test(black_and_white_filter_test),
		unit_test(grey_filter_test),
		unit_test(color_zone_test),
		unit_test(rotate_test),
		unit_test(symmetry_test),
		unit_test(resize_workspace_test),
		unit_test(new_img_test),
		unit_test(m_img_surface_test),
		unit_test(m_img_name_test),
		unit_test(m_img_path_test),
		unit_test(m_img_ext_test)
	};
	return cmocka_run_group_tests(tests, NULL, NULL);
}

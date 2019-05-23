#include "io_test.h"
#define WIDTH  300
#define HEIGHT 400

static void test_h_and_w(SDL_Surface *reference, SDL_Surface *subject){
	assert_int_equal(reference->h, subject->h);
	assert_int_equal(reference->w, subject->w);
}

static void test_pixel(SDL_Surface *reference, SDL_Surface *subject, int margin){
	Uint32 *         pixels_ref = reference->pixels;
	Uint32 *         pixels_sub = subject->pixels;
	SDL_PixelFormat *format = SDL_AllocFormat(SDL_PIXELFORMAT_RGBA8888);
	int i, j;
	if (format == NULL) {
		fprintf(stderr, "Error : can't init format!\n");
		exit(1);
	}
	SDL_LockSurface(reference);
	SDL_LockSurface(subject);
	for (i = 0; i < HEIGHT; i++) {
		for (j = 0; j < WIDTH; j++) {
			SDL_Color c_sub = {0}, c_ref = {0};
			SDL_GetRGBA(pixels_sub[i * WIDTH + j], format,
			            &c_sub.r, &c_sub.g, &c_sub.b, &c_sub.a);
			SDL_GetRGBA(pixels_ref[i * WIDTH + j], format,
			            &c_ref.r, &c_ref.g, &c_ref.b, &c_ref.a);
			assert_in_range(c_sub.r, c_ref.r - margin, c_ref.r + margin);
		}
	}
	SDL_UnlockSurface(reference);
	SDL_UnlockSurface(subject);
	SDL_FreeFormat(format);
}

static void input_test(void **state){
	image *      img_ref = load_image("tests/io_test/io_test.bmp");
	image *      img_sub = load_image("tests/io_test/io_test.jpg");
	SDL_Surface *reference = get_img_surface(img_ref);
	SDL_Surface *subject = get_img_surface(img_sub);
	test_h_and_w(reference, subject);
	test_pixel(reference, subject, 15);
	free_image(img_ref);
	free_image(img_sub);
}

int run_io_test(){
	const struct CMUnitTest tests[] = {
		cmocka_unit_test(input_test)
	};
	return cmocka_run_group_tests(tests, NULL, NULL);
}

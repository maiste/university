#include "event.h"

/**
 * -- Local variables
 */

static frame *frame_buffer[MAX_BUFFER];
static int    cursor = -1;



/**
 * --- Helpers ---
 */

/**
 * @brief
 * Return 1 if the rect is non_empty
 */
short non_empty(SDL_Rect rect){
	return rect.x || rect.y || rect.h || rect.w;
}

/**
 * @brief
 * Return 1 if the user has click
 */
short has_click(SDL_Point point){
	return point.x != -1;
}

/**
 * Static function to return
 * a standard rect
 */
static void standard_rect(SDL_Rect *origin){
	if (origin->w < 0) {
		origin->x += origin->w;
		origin->w = -origin->w;
	}
	if (origin->h < 0) {
		origin->y += origin->h;
		origin->h = -origin->h;
	}
}

/**
 * Test if windowID is the same as cursor window ID
 *
 * @param windowID to compare with
 */
static short same_window(int id){
	return ((Uint32)id) == SDL_GetWindowID(frame_buffer[cursor]->window);
}

/**
 * --- Selection ---
 */

/**
 *  Draw selection
 */
int draw_select(SDL_Rect selection){
	frame *current = frame_buffer[cursor];

	SDL_Color    select = {255, 0, 0, 255};
	SDL_Surface *surface = get_img_surface(current->image);
	SDL_Texture *texture = NULL;

	if (surface == NULL) {
		fprintf(stderr, "Error : can't get current surface\n");
		return 0;
	}

	if (SDL_RenderClear(current->renderer)) {
		fprintf(stderr, "Error : can't clear renderer\n");
		return 0;
	}

	if ((texture = SDL_CreateTextureFromSurface(current->renderer, surface)) == NULL) {
		fprintf(stderr, "Error : can't convert surface into texture\n");
		return 0;
	}

	SDL_SetRenderTarget(current->renderer, texture);
	SDL_RenderCopy(current->renderer, texture, NULL, NULL);

	if (non_empty(selection)) {
		SDL_SetRenderDrawColor(current->renderer, select.r, select.g, select.b, select.a);
		SDL_RenderDrawRect(current->renderer, &selection);
	}

	SDL_SetRenderDrawColor(current->renderer, 0,0,0,255);
	SDL_SetRenderTarget(current->renderer, NULL);
	SDL_RenderPresent(current->renderer);

	return 1;
}

/**
 * @brief
 * Get point location
 * @return the point with a negative x in case of
 * quit
 */
SDL_Point get_point(){
	SDL_Event event = {0};
	SDL_Point point;
	memset(&point, 0, sizeof(SDL_Point));
	short run = 1;
	SDL_RaiseWindow(frame_buffer[cursor]->window);
	SDL_SetWindowGrab(frame_buffer[cursor]->window, SDL_TRUE);

	while (run) {
		SDL_WaitEvent(&event);

		// Key is pressed for leaving
		if (event.type == SDL_KEYDOWN && event.key.keysym.sym == SDLK_q) {
			point.x = -1;
			run = 0;
			printf("Cancel selection\n");
		}
		else if (same_window(event.window.windowID) &&
		         event.type == SDL_MOUSEBUTTONDOWN) {
			point.x = event.button.x;
			point.y = event.button.y;
			printf("Point x: %d, y: %d\n", point.x, point.y);
			run = 0;
		}
	}

	SDL_SetWindowGrab(frame_buffer[cursor]->window, SDL_FALSE);
	return point;
}

/**
 * @brief
 * Launch a selection in the current window
 * @return an SDL_Rect with non empty fields
 */
SDL_Rect get_select_array(){
	SDL_Event event;
	SDL_Rect  rect;
	memset(&rect, 0, sizeof(SDL_Rect));
	short run = 1;
	SDL_RaiseWindow(frame_buffer[cursor]->window);
	SDL_SetWindowGrab(frame_buffer[cursor]->window, SDL_TRUE);

	while (run) {
		SDL_WaitEvent(&event);

		// Key is pressed
		if (event.type == SDL_KEYDOWN) {
			if (event.key.keysym.sym == SDLK_q) {
				memset(&rect, 0, sizeof(SDL_Rect));
				run = 0;
				printf("\nCancel selection\n");
			}
			else if (non_empty(rect) && event.key.keysym.sym == SDLK_v)
				run = 0;
			else if (non_empty(rect) && event.key.keysym.sym == SDLK_f)
				memset(&rect, 0, sizeof(SDL_Rect));
		}

		// Mouse is in motion
		else if (same_window(event.window.windowID) &&
		         event.type == SDL_MOUSEBUTTONDOWN) {
			rect.x = event.button.x;
			rect.y = event.button.y;
			rect.h = 0;
			rect.w = 0;
		}
		else if (same_window(event.window.windowID) &&
		         event.type == SDL_MOUSEMOTION &&
		         (event.motion.state & SDL_BUTTON_LEFT)) {
			rect.h += event.motion.yrel;
			rect.w += event.motion.xrel;
		}

		printf("\rRect x:%d, y:%d, w:%d, h:%d      ", rect.x, rect.y, rect.w, rect.h);

		// Draw the selection
		if (!draw_select(rect)) {
			memset(&rect, 0, sizeof(SDL_Rect));
			run = 0;
		}
	}
	SDL_SetWindowGrab(frame_buffer[cursor]->window, SDL_FALSE);
	standard_rect(&rect);
	SDL_Rect empty_rect = {0};
	draw_select(empty_rect);
	printf("\n");
	return rect;
}

/**
 * --- Getters and move options ---
 */

/**
 * Get the frame at buffer position
 * @return NULL in case of empty buffer
 */
frame *get_cursor_buffer(){
	if (cursor == -1)
		return NULL;
	return frame_buffer[cursor];
}

/**
 * Get first free buffer
 * @return the position of free buffer
 */
int get_free_buffer(){
	for (int i = 0; i < MAX_BUFFER; i++) {
		if (frame_buffer[i] == NULL)
			return i;
	}
	return -1;
}

/**
 * Move to first non_empty buffer
 */
void moveto_first_buffer(){
	cursor = -1;
	for (int i = 0; i < MAX_BUFFER; i++) {
		if (frame_buffer[i] != NULL) {
			cursor = i;
			return;
		}
	}
}

/**
 * @brief
 * Move cursor to frame
 * @param pos new position
 * @return -1 in case of wrong id
 * 0 in case of NULL frame
 * 1 else
 */
int moveto_buffer(int pos){
	if (pos < 0 || pos > MAX_BUFFER)
		return -1;
	if (frame_buffer[pos] == NULL)
		return 0;
	cursor = pos;
	SDL_RaiseWindow(frame_buffer[cursor]->window);
	return 1;
}

/**
 * --- Frame buffer manager --
 */

/**
 * Init a new frame at free position
 * @param path path to the image
 * @return 0 in case of failure
 */
int new_frame(char *path){
	int pos = get_free_buffer();
	if (pos == -1) {
		fprintf(stderr, "Error : can't open more buffers. Max : %d\n", MAX_BUFFER);
		return 0;
	}

	frame *current = init_frame(path);
	if (current == NULL)
		return 0;
	frame_buffer[pos] = current;
	cursor = pos;
	return 1;
}

/**
 * @brief
 * Print current buffer
 */
void print_frame(){
	for (int i = 0; i < MAX_BUFFER; i++) {
		if (frame_buffer[i] != NULL)
			printf("Window id : %d | picture : %s\n", i, get_img_name(frame_buffer[i]->image));
		else
			printf("Window id : %d | not open window \n", i);
	}
	printf("Actual buffer : %d\n", cursor);
}

/**
 * @brief
 * Move the current buffer to new pos
 * @param target new position
 * @return 1 in case of error else 0
 */
short move_current_to(int target){
	if (cursor == -1 || (target < 0 || target > 9)) {
		fprintf(stderr, "Error : unauthorized buffer\n");
		return 1;
	}
	if (frame_buffer[target] == NULL) {
		frame_buffer[target] = frame_buffer[cursor];
		frame_buffer[cursor] = NULL;
	}
	else {
		image *tmp = frame_buffer[target]->image;
		frame_buffer[target]->image = frame_buffer[cursor]->image;
		frame_buffer[cursor]->image = NULL;
		free_image(tmp);
		free_frame_buffer(cursor);
	}
	cursor = target;
	return 0;
}

/**
 * @brief
 * Check if the current is still stable
 */
void check_current_frame(){
	if (cursor != -1) {
		frame *current = frame_buffer[cursor];
		if (current == NULL || current->image == NULL ||
		    get_img_surface(current->image) == NULL) {
			free_frame_buffer(cursor);
			frame_buffer[cursor] = NULL;
			moveto_first_buffer();
		}
	}
}

/**
 * Delete a buffer and move cursor
 * to next non_empty position
 */
void free_frame_buffer(int i){
	if (cursor != -1 && i == -1) {
		free_frame(frame_buffer[cursor]);
		frame_buffer[cursor] = NULL;
	}
	else if (i >= 0 && i < MAX_BUFFER && frame_buffer[i] != NULL) {
		free_frame(frame_buffer[i]);
		frame_buffer[i] = NULL;
	}
	moveto_first_buffer();
}

/**
 * Delete the content of th] entiere buffer
 */
void free_frames(){
	for (int i = 0; i < MAX_BUFFER; i++) {
		free_frame_buffer(i);
	}
}

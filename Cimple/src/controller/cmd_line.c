#include "cmd_line.h"


static short cmd_function_handler(cmd *command);



static char *string_cpy(char *s){
	char *str = malloc(sizeof(char) * (strlen(s) + 1));
	if ((str = memcpy(str, s, strlen(s) + 1)) == NULL) {
		fprintf(stderr, "Error : memory copy failed\n");
		return NULL;
	}
	str[strlen(s)] = '\0';
	return str;
}

static int string_to_int(char *str){
	int i;
	sscanf(str, "%d", &i);
	return i;
}

/**
 * Call black_and_white model function in m_color.c to modify and apply modifications by calling view function
 * Call select function in event.c when -a option isn't present
 * @param cmd * command , pointer to a command structure
 * @return 0 if changes failed , 1 if changes done.
 */

static short handler_cmd_bnw(cmd *command){
	frame *f = get_cursor_buffer();
	if (f == NULL) return 0;
	image *img = f->image;

	SDL_Rect rect = {0, 0, get_img_surface(img)->w, get_img_surface(img)->h};

	if (strcmp(command->args[1], "") == 0) {
		rect = get_select_array();
		if (non_empty(rect) != 1) return 1;
	}


	if (black_and_white_filter(img, rect) != 1) return 0;
	if (update_frame(f, NULL) != 1) return 0;
	return 1;
}

/**
 * Call greyscale model function in m_color.c to modify pixels and apply modifications by calling view function
 * Call select function in event.c when -a option isn't present
 *
 * @param cmd * command , pointer to a command structure
 * @return 0 if changes failed , 1 if changes done.
 */

static short handler_cmd_greyscale(cmd *command){
	frame *f = get_cursor_buffer();
	if (f == NULL) return 0;
	image *img = f->image;

	SDL_Rect rect = {0, 0, get_img_surface(img)->w, get_img_surface(img)->h};

	if (strcmp(command->args[1], "") == 0) {
		rect = get_select_array();
		if (non_empty(rect) != 1) return 1;
	}

	if (grey_filter(img, rect) != 1) return 0;
	if (update_frame(f, NULL) != 1) return 0;
	return 1;
}

/**
 * Call negative model function in m_color.c to modify pixels and apply modifications by calling view function
 * Call select function in event.c when -a option isn't present
 *
 * @param cmd * command , pointer to a command structure
 * @return 0 if changes failed , 1 if changes done.
 */

static short handler_cmd_negative(cmd *command){
	frame *f = get_cursor_buffer();
	if (f == NULL) return 0;
	image *img = f->image;

	SDL_Rect rect = {0, 0, get_img_surface(img)->w, get_img_surface(img)->h};

	if (strcmp(command->args[1], "") == 0) {
		rect = get_select_array();
		if (non_empty(rect) != 1) return 1;
	}

	if (negative_filter(img, rect) != 1) return 0;
	if (update_frame(f, NULL) != 1) return 0;
	return 1;
}

/**
 * Call contrast model function in m_color.c to modify pixels and apply modifications by calling view function
 * Call select function in event.c when -a option isn't present
 *
 * @param cmd * command , pointer to a command structure
 * @return 0 if changes failed , 1 if changes done.
 */

static short handler_cmd_contrast(cmd *command){
	frame *f = get_cursor_buffer();
	if (f == NULL) return 0;
	image *img = f->image;
	if (strcmp(command->args[2], "") == 0) {
		fprintf(stderr, "Error : command [%s], please enter a percent rate\n", command->args[0]);
		return 0;
	}
	int percent = string_to_int(command->args[2]);

	SDL_Rect rect = {0, 0, get_img_surface(img)->w, get_img_surface(img)->h};

	if (strcmp(command->args[1], "") == 0) {
		rect = get_select_array();
		if (non_empty(rect) != 1) return 1;
	}

	if (contrast(img, rect, percent) != 1) return 0;
	if (update_frame(f, NULL) != 1) return 0;
	return 1;
}

/**
 * Call light model function in m_color.c to modify pixels and apply modifications by calling view function
 * Call select function in event.c when -a option isn't present
 *
 * @param cmd * command , pointer to a command structure
 * @return 0 if changes failed , 1 if changes done.
 */

static short handler_cmd_light(cmd *command){
	frame *f = get_cursor_buffer();
	if (f == NULL) return 0;
	image *img = f->image;
	if (strcmp(command->args[2], "") == 0) {
		fprintf(stderr, "Error : command [%s], please enter a percent rate\n", command->args[0]);
		return 0;
	}
	int      percent = string_to_int(command->args[2]);
	SDL_Rect rect = {0, 0, get_img_surface(img)->w, get_img_surface(img)->h};

	if (strcmp(command->args[1], "") == 0) {
		rect = get_select_array();
		if (non_empty(rect) != 1) return 1;
	}
	if (light_filter(img, rect, percent) != 1) return 0;
	if (update_frame(f, NULL) != 1) return 0;
	return 1;
}

/**
 * Call replace model function in m_color.c to modify pixels and apply modifications by calling view function
 * Call select function in event.c when -a option isn't present
 *
 * @param cmd * command , pointer to a command structure
 * @return 0 if changes failed , 1 if changes done.
 */

static short handler_cmd_replace(cmd *command){
	frame *f = get_cursor_buffer();
	if (f == NULL) return 0;
	image *img = f->image;

	int percent = 1;
	int orig_r = string_to_int(command->args[4]);
	int orig_g = string_to_int(command->args[5]);
	int orig_b = string_to_int(command->args[6]);
	int orig_a = string_to_int(command->args[7]);
	int targ_r = string_to_int(command->args[8]);
	int targ_g = string_to_int(command->args[9]);
	int targ_b = string_to_int(command->args[10]);
	int targ_a = string_to_int(command->args[11]);

	if (strcmp(command->args[1], "-m") == 0)
		percent = string_to_int(command->args[2]);

	SDL_Color origin_color = {orig_r, orig_g, orig_b, orig_a};
	SDL_Color target_color = {targ_r, targ_g, targ_b, targ_a};

	SDL_Rect rect = {0, 0, get_img_surface(img)->w, get_img_surface(img)->h};

	if (strcmp(command->args[3], "") == 0) {
		rect = get_select_array();
		if (non_empty(rect) != 1) return 1;
	}

	if (replace_color(img, rect, origin_color, target_color, percent) != 1) return 0;
	if (update_frame(f, NULL) != 1) return 0;
	return 1;
}

/**
 * Call colorzone function in m_color.c and apply the modification by calling view function
 * Call select function in event.c when -a option isn't present
 *
 * @param cmd * command , pointer to a command structure
 * @return 0 if changes failed , 1 if changes done.
 */

static short handler_cmd_fill(cmd *command){
	frame *f = get_cursor_buffer();
	if (f == NULL) return 0;
	image *img = f->image;

	int col_r = string_to_int(command->args[2]);
	int col_g = string_to_int(command->args[3]);
	int col_b = string_to_int(command->args[4]);
	int col_a = string_to_int(command->args[5]);

	SDL_Rect  rect = {0, 0, get_img_surface(img)->w, get_img_surface(img)->h};
	SDL_Color color = {col_r, col_g, col_b, col_a};

	if (strcmp(command->args[1], "") == 0) {
		rect = get_select_array();
		if (non_empty(rect) != 1) return 1;
	}

	if (color_zone(img, rect, color) != 1) return 0;
	if (update_frame(f, NULL) != 1) return 0;
	return 1;
}

/**
 * Call copy function in m_transform.c and apply the modification by calling view function
 * Call select function in event.c when -a option isn't present
 *
 * @param cmd * command , pointer to a command structure
 * @return 0 if changes failed , 1 if changes done.
 */

static short handler_cmd_copy(cmd *command){
	frame *f = get_cursor_buffer();
	if (f == NULL) return 0;
	image *img = f->image;

	SDL_Rect rect = {0, 0, get_img_surface(img)->w, get_img_surface(img)->h};

	if (strcmp(command->args[1], "") == 0) {
		rect = get_select_array();
		if (non_empty(rect) != 1) return 1;
	}

	if (copy(img, rect) != 1) return 0;
	if (update_frame(f, NULL) != 1) return 0;
	return 1;
}

/**
 * Call cut function in m_transform.c and apply the modification by calling view function
 * Call select function in event.c when -a option isn't present
 *
 * @param cmd * command , pointer to a command structure
 * @return 0 if changes failed , 1 if changes done.
 */

static short handler_cmd_cut(cmd *command){
	frame *f = get_cursor_buffer();
	if (f == NULL) return 0;
	image *img = f->image;

	SDL_Rect rect = {0, 0, get_img_surface(img)->w, get_img_surface(img)->h};

	if (strcmp(command->args[1], "") == 0) {
		rect = get_select_array();
		if (non_empty(rect) != 1) return 1;
	}

	if (cut(img, rect) != 1) return 0;
	if (update_frame(f, NULL) != 1) return 0;
	return 1;
}

/**
 * Call paste function in m_transform.c and apply the modification by calling view function
 * Call select point function in event.c when -a option isn't present
 *
 * @param cmd * command , pointer to a command structure
 * @return 0 if changes failed , 1 if changes done.
 */

static short handler_cmd_paste(cmd *command){
	frame *f = get_cursor_buffer();
	if (f == NULL) return 0;
	image *img = f->image;

	int x = 0, y = 0;
	if (strcmp(command->args[1], "") == 0) {
		SDL_Point p = get_point();
		if (has_click(p) != 1) return 1;
		x = p.x;
		y = p.y;
	}
	if (paste(img, x, y) != 1) return 0;
	if (update_frame(f, NULL) != 1) return 0;
	return 1;
}

/**
 * Call symmetry function in m_transform.c and apply the modification by calling view function
 *
 * @param cmd * command , pointer to a command structure
 * @return 0 if changes failed , 1 if changes done.
 */

static short handler_cmd_symmetry(cmd *command){
	frame *f = get_cursor_buffer();
	if (f == NULL) return 0;
	image *img = f->image;

	int mode = 0;

	if (strcmp(command->args[1], "v") == 0) mode = 1;
	if (symmetry(img, mode) != 1) return 0;
	if (update_frame(f, NULL) != 1) return 0;
	return 1;
}

/**
 * Call resize function in m_frame.c and apply the modification by calling view function
 * Resize the workspace or the current image
 *
 * @param cmd * command , pointer to a command structure
 * @return 0 if changes failed , 1 if changes done.
 */

static short handler_cmd_resize(cmd *command){
	frame *f = get_cursor_buffer();
	if (f == NULL) return 0;
	image *img = f->image;
	short  n;
	int    width = string_to_int(command->args[2]);
	int    height = string_to_int(command->args[3]);

	if (strcmp(command->args[1], "workspace") == 0) {
		if (get_img_surface(f->image)->w + width >= MAX_W ||
		    get_img_surface(f->image)->h + height >= MAX_H ||
		    width > MAX_R || height > MAX_R) {
			fprintf(stderr, "Error : command [%s] , dimensions out of bounds\n", command->name);
			return 0;
		}
		n = resize_workspace(img, width, height);
	}
	else n = resize_image(img, width, height);
	if (n != 1) return 0;
	if (update_frame(f, NULL) != 1) return 0;
	return 1;
}

/**
 * Call rotate function in m_frame.c and apply the modification by calling view function
 *
 * @param cmd * command , pointer to a command structure
 * @return 0 if changes failed , 1 if changes done.
 */

static short handler_cmd_rotate(cmd *command){
	frame *f = get_cursor_buffer();
	if (f == NULL) return 0;
	image *img = f->image;
	int    n;
	int    angle = string_to_int(command->args[2]);
	if (strcmp(command->args[1], "-r") == 0) n = rotate(img, angle, 1);
	else n = rotate(img, angle, 0);
	update_frame(f, NULL);
	return n;
}

/**
 * Call truncation function in m_frame.c and apply the modification by calling view function
 *
 * @param cmd * command , pointer to a command structure
 * @return 0 if changes failed , 1 if changes done.
 */


static short handler_cmd_truncate(cmd *command){
	frame *f = get_cursor_buffer();
	if (f == NULL) return 0;
	image *img = f->image;

	SDL_Rect rect = {0, 0, get_img_surface(img)->w, get_img_surface(img)->h};

	if (strcmp(command->args[1], "") == 0) {
		rect = get_select_array();
		if (non_empty(rect) != 1) return 1;
	}
	else {
		int x1 = string_to_int(command->args[2]);
		int y1 = string_to_int(command->args[3]);
		int x2 = string_to_int(command->args[4]);
		int y2 = string_to_int(command->args[5]);
		rect.x = x1;
		rect.y = y1;
		rect.w = x2 - x1;
		rect.h = y2 - y1;
	}

	if (truncate_image(img, rect) != 1) return 0;
	if (update_frame(f, NULL) != 1) return 0;
	return 1;
}

/**
 * Print frame list by calling function in event.c
 *
 * @param cmd * command , pointer to a command structure
 * @return 1
 */

short handler_cmd_list_buff(cmd *command){
	(void)command;
	print_frame();
	return 1;
}

/**
 * Print help , display command formats
 *
 * @param cmd * command , pointer to a command structure
 * @return 1
 */


short handler_cmd_help(cmd *command){
	(void)command;
	printf("symmetry < v | h > \n");
	printf("rotate [-r] angle \n");
	printf("copy [-a]\n");
	printf("cut [-a]\n");
	printf("paste [-a]\n");
	printf("truncate [-s origin_x origin_y end_x end_y]\n");
	printf("resize < workspace | image > width height\n");
	printf("negative [-a]\n");
	printf("bnw [-a]\n");
	printf("greyscale [-a]\n");
	printf("fill [-a] red green blue alpha\n");
	printf("replace [-m percent] [-a] red green blue alpha red green blue alpha\n");
	printf("contrast [-a] percent\n");
	printf("light [-a] percent\n");
	printf("load [-w window_id] path\n");
	printf("save [-p path]\n");
	printf("list_buffer\n");
	printf("switch_buffer window_id\n");
	printf("move_buffer window_id\n");
	printf("help\n");
	printf("quit [-w window_id]\n");
	printf("apply_script path\n");
	printf("edit_script path\n");
	printf("bundle regex command\n");
	return 1;
}

/**
 * Quit Cimple program , or close a specific frame by window id
 *
 * @param cmd * command , pointer to a command structure
 * @return 0 if changes failed , 1 if changes done.
 */

static short handler_cmd_quit(cmd *command){
	if (strcmp(command->args[1], "-w") != 0) {
		clean_secure();
		free_frames();
		printf("CIMPLE PHOTO EDITOR ----> SHUT DOWN\n");
		return 2;
	}
	int index = string_to_int(command->args[2]);
	if (moveto_buffer(index) != 1) {
		fprintf(stderr, "Error  : command [quit], invalid window id \n");
		return 0;
	}
	char *s1 = get_img_name(get_cursor_buffer()->image);
	char *s2 = get_img_ext(get_cursor_buffer()->image);
	char *path = malloc(strlen(s1) + strlen(s2) + 1);
	sprintf(path, "%s.%s", s1, s2);
	remove_tmp_file(path);
	free(path);
	free_frame_buffer(index);
	return 1;
}

/**
 * Change the current frame by calling view function in event.c
 *
 * @param cmd * command , pointer to a command structure
 * @return 0 if change failed , 1 if change done .
 */

static short handler_cmd_switch_buff(cmd *command){
	short s = moveto_buffer(string_to_int(command->args[1]));
	if (s != 1) {
		if (s == -1) fprintf(stderr, "Error : command[%s], invalid window id , index out of bound ( [0;10] ) \n", command->name);
		if (s == 0) fprintf(stderr, "Error : command[%s], invalid window id , window isn't initialised \n", command->name);
		return 0;
	}
	return s;
}

/**
 * Move current buffer to another window
 * full or not with an image.
 *
 * @param cmd * command, pointer to a command structure
 * @return 0 if the change failed else 1
 */
static short handler_cmd_move_buffer(cmd *command){
	frame *f = get_cursor_buffer();
	if (f == NULL) return 0;
	short s = move_current_to(string_to_int(command->args[1]));
	if (s != 0) return 0;
	if (update_frame(get_cursor_buffer(), NULL) != 1) return 0;
	return 1;
}

/**
 * Call load function in in.c and open an image in a frame by calling view function
 *
 * @param cmd * command , pointer to a command structure
 * @return 0 if load failed , 1 if load done.
 */

static short handler_cmd_load(cmd *command){
	if (strcmp(command->args[1], "-w") != 0) {
		if (new_frame(command->args[3]) != 0) return 0;
	}
	else{
		int index = string_to_int(command->args[2]);
		if (moveto_buffer(index) != 1) {
			fprintf(stderr, "Error : command[load], invalid window id \n");
			return 0;
		}
		if (update_frame(get_cursor_buffer(), command->args[3]) != 1) return 0;
	}
	return 1;
}

/**
 * Create (or edit if it exists already) a script at given path
 *
 * @param cmd * command , pointer to a command structure
 * @return 0 if save failed , 1 if save done.
 */

static short handler_cmd_edit_script(cmd *command){
	char *editor = getenv("EDITOR");
	if (editor == NULL) {
		fprintf(stderr, "Error : NULL editor, check yout $EDITOR variable\n");
		return 0;
	}
	printf("Entering [%s] editor\n", editor);
	if (!fork())
		execlp(editor, editor, command->args[1], NULL);
	wait(NULL);
	printf("Exited editor\n");
	printf("\e[1;1H\e[2J");
	return 1;
}

/**
 * Call save function in out.c and apply the modification by calling view function if image format change
 *
 * @param cmd * command , pointer to a command structure
 * @return 0 if save failed , 1 if save done.
 */

static short handler_cmd_save(cmd *command){
	frame *f = get_cursor_buffer();
	if (f == NULL) {
		fprintf(stderr, "Error : command [%s], no window founded , please load an image\n", command->name);
		return 0;
	}
	image *img = f->image;

	if (strcmp(command->args[1], "-p") == 0) {
		image *new_img = save_image_as(img, command->args[2]);
		if (new_img == NULL) {
			fprintf(stderr, "Error : command [%s], error while saving the image\n", command->args[0]);
			return 0;
		}
		f->image = new_img;
		if (update_frame(f, NULL)) {
			remove_secure(img);
			printf("Save as %s\n", get_full_image_path(f->image));
			free(img);
			return 1;
		}
		return 0;
	}
	if (save_image(img) != 0) return 0;
	return 1;
}

/**
 * Apply a script located at given path to the current window
 *
 * @param cmd * command , pointer to a command structure
 * @return 0 if save failed , 1 if save done.
 */

static short handler_cmd_apply_script(cmd *command){
	frame *f = get_cursor_buffer();
	if (f == NULL) {
		fprintf(stderr, "Error : command [%s], no window founded , please load an image\n", command->name);
		return 0;
	}
	char * script_path = command->args[1];
	FILE * script = fopen(script_path, "r");
	char * line;
	size_t bufsize = 64;
	if (script == NULL) {
		fprintf(stderr, "Error : could not open script file\n");
		return 1;
	}
	int pos = 0;
	while ((pos = getdelim(&line, &bufsize, ';', script)) != -1) {
		char *comline = malloc(strlen(line) - 1);
		memcpy(comline, line, strlen(line) - 1);
		if (pos <= 1) break;
		if (comline[0] == '\n') comline++;
		cmd *c = parse_line(comline);
		if (c != NULL) {
			int rc = cmd_function_handler(c);
			if (rc == 0) {
				fprintf(stderr, "Error : command [%s] could not be applied\n", c->name);
				free_cmd(c);
				fclose(script);
				free(comline);
				if (line) free(line);
				return 1;
			}
			free(comline);
			free_cmd(c);
			check_current_frame();
		}
		else {
			fprintf(stderr, "Error : could not parse line\n");
			fclose(script);
			free(comline);
			if (line) free(line);
			return 1;
		}
	}
	check_current_frame();
	fclose(script);
	if (line) free(line);

	return update_frame(f, NULL);
}

/**
 * Apply action to a set of files
 *
 * @param cmd * command , pointer to a command structure
 * @return 0 if change failed , 1 if change done .
 */
static short handler_cmd_bundle(cmd *command){
	node *list = find_expr("./", command->args[1]);
	node *current = list;
	int   rc = 0;
	cmd * real_cmd = get_real_cmd(command->args[2]);
	if (real_cmd == NULL) {
		free_cmd(real_cmd);
		free_all(list);
		return 0;
	}
	while (current != NULL) {
		if (new_frame(current->value) == 0) {
			free_cmd(real_cmd);
			free_all(list);
			return 0;
		}
		rc = cmd_function_handler(real_cmd);
		if (rc == 0) {
			free_cmd(real_cmd);
			free_all(list);
			return 0;
		}
		frame *f = get_cursor_buffer();
		if (f == NULL || save_image(f->image) == 0) {
			free_cmd(real_cmd);
			free_all(list);
			return 0;
		}
		free_frame_buffer(-1);
		current = current->next;
	}
	free_all(list);
	return 1;
}

/**
 * Redirection to a specific handler function by the help of command name
 *
 * @param cmd pointer contains all command informations
 * @return 0 if the command will execute with sucess , 1 if an error has occured
 */


static short cmd_function_handler(cmd *command){
	if (strcmp(command->name, "bnw") == 0) return handler_cmd_bnw(command);
	if (strcmp(command->name, "copy") == 0) return handler_cmd_copy(command);
	if (strcmp(command->name, "cut") == 0) return handler_cmd_cut(command);
	if (strcmp(command->name, "contrast") == 0) return handler_cmd_contrast(command);
	if (strcmp(command->name, "greyscale") == 0) return handler_cmd_greyscale(command);
	if (strcmp(command->name, "fill") == 0) return handler_cmd_fill(command);
	if (strcmp(command->name, "help") == 0) return handler_cmd_help(command);
	if (strcmp(command->name, "light") == 0) return handler_cmd_light(command);
	if (strcmp(command->name, "list_buffer") == 0) return handler_cmd_list_buff(command);
	if (strcmp(command->name, "load") == 0) return handler_cmd_load(command);
	if (strcmp(command->name, "move_buffer") == 0) return handler_cmd_move_buffer(command);
	if (strcmp(command->name, "negative") == 0) return handler_cmd_negative(command);
	if (strcmp(command->name, "paste") == 0) return handler_cmd_paste(command);
	if (strcmp(command->name, "quit") == 0) return handler_cmd_quit(command);
	if (strcmp(command->name, "replace") == 0) return handler_cmd_replace(command);
	if (strcmp(command->name, "resize") == 0) return handler_cmd_resize(command);
	if (strcmp(command->name, "rotate") == 0) return handler_cmd_rotate(command);
	if (strcmp(command->name, "save") == 0) return handler_cmd_save(command);
	if (strcmp(command->name, "switch_buffer") == 0) return handler_cmd_switch_buff(command);
	if (strcmp(command->name, "symmetry") == 0) return handler_cmd_symmetry(command);
	if (strcmp(command->name, "truncate") == 0) return handler_cmd_truncate(command);
	if (strcmp(command->name, "apply_script") == 0) return handler_cmd_apply_script(command);
	if (strcmp(command->name, "edit_script") == 0) return handler_cmd_edit_script(command);
	if (strcmp(command->name, "bundle") == 0) return handler_cmd_bundle(command);
	fprintf(stderr, "Error command [%s] : current command unrecognized\n", command->name);
	return 0;
}

/**
 * Load all image file present in tmp_file if cimple_tmp directory isn't empty
 * when user start running the program.
 *
 * @return n
 */

static void load_tmp_file(){
	DIR *d = opendir("/var/tmp/cimpletmp");
	if (d == NULL) return;
	struct dirent *dir_iter;
	while ((dir_iter = readdir(d)) != NULL) {
		if (memcmp(dir_iter->d_name, ".", 2) == 0 || memcmp(dir_iter->d_name, "..", 3) == 0)
			continue;

		char *dir_path = "/var/tmp/cimpletmp/";
		char *path = malloc(strlen(dir_path) + strlen(dir_iter->d_name) + 4);
		sprintf(path, "%s%s", dir_path, dir_iter->d_name);

		if (new_frame(path) != 1) {
			fprintf(stderr, "Error : cannot load %s from load_tmp_file directory\n", dir_iter->d_name);
			remove(path);
			free(path);
		}
		free(path);
	}
}

/**
 * Loop on user command input and call parse function to build command structure and give it
 * to the command handler function
 *
 * @return n
 */


short cimple_handler(){
	int       n = 0;
	SDL_Event event;
	load_tmp_file();
	while (1) {
		char *cmd_line = getcmdline();
		if (cmd_line == NULL) continue;
		cmd *command = parse_line(string_cpy(cmd_line));
		if (command != NULL) {
			while (SDL_PollEvent(&event));       // empty event queue
			n = cmd_function_handler(command);
			if (n == 2)
				return 0;
			free(cmd_line);
			free_cmd(command);
			frame *f;
			if ((f = get_cursor_buffer()) != NULL)
				save_secure(f->image);
		}
	}
	return n;
}

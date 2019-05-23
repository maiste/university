#include "parse.h"

struct cmd_info {
	char *name;
	int   len;
	short args_type [LEN_MAX];
	char *option [LEN_MAX];
};

static struct cmd_info info_tab[LEN_INFO] = {
	{.name = "bnw",           .len = LEN_BNW,         .args_type = {0, 1      }, .option = {"", "-a"}},
	{.name = "copy",          .len = LEN_COPY,        .args_type = {0, 1      }, .option = {"", "-a"}},
	{.name = "contrast",      .len = LEN_CONSTRAST,   .args_type = {0, 1, POURCR}, .option = {"", "-a", ""}},
	{.name = "cut",           .len = LEN_CUT,         .args_type = {0, 1      }, .option = {"", "-a"}},
	{.name = "greyscale",     .len = LEN_GREYS,       .args_type = {0, 1      }, .option = {"", "-a"}},
	{.name = "help",          .len = LEN_HELP,        .args_type = {0},.option = {""}},
	{.name = "fill",          .len = LEN_FILL,        .args_type = {0, 1, PIXEL, PIXEL, PIXEL, PIXEL}, .option = {"", "-a", "", "", "", ""}},
	{.name = "light",         .len = LEN_LIGHT,       .args_type = {0, 1, POURCR}, .option = {"", "-a", ""}},
	{.name = "list_buffer",   .len = LEN_LIST_BUFFER, .args_type = {0},.option = {""}},
	{.name = "load",          .len = LEN_LOAD,        .args_type = {0, 2, NUMBER, STRING}, .option = {"", "-w", "", ""}},
	{.name = "negative",      .len = LEN_NEG,         .args_type = {0, 1      }, .option = {"", "-a"}},
	{.name = "move_buffer",   .len = LEN_MOVE_BUFFER, .args_type = {0, NUMBER }, .option = {"", ""  }},
	{.name = "paste",         .len = LEN_PASTE,       .args_type = {0, 1      }, .option = {"", "-a"}},
	{.name = "quit",          .len = LEN_QUIT,        .args_type = {0, 2, NUMBER}, .option = {"", "-w", ""}},
	{.name = "replace",       .len = LEN_REPLACE,     .args_type = {0, 2, POURC, 1, PIXEL, PIXEL, PIXEL, PIXEL, PIXEL, PIXEL, PIXEL, PIXEL}, .option = {"", "-m", "", "-a", "", "", "", "", "", "", "", ""}},
	{.name = "resize",        .len = LEN_RESIZE,      .args_type = {0, VIEW, RELATIV, RELATIV}, .option = {"", "", "", ""}},
	{.name = "rotate",        .len = LEN_ROTATE,      .args_type = {0, 1, ANGLE}, .option = {"", "-r", ""}},
	{.name = "save",          .len = LEN_SAVE,        .args_type = {0, 2, STRING}, .option = {"", "-p", ""}},
	{.name = "switch_buffer", .len = LEN_SWITCH,      .args_type = {0, NUMBER }, .option = {"", ""  }},
	{.name = "symmetry",      .len = LEN_SYM,         .args_type = {0, SYMTYPE}, .option = {"", ""  }},
	{.name = "truncate",      .len = LEN_TRUNCATE,    .args_type = {0, 5, NUMBER, NUMBER, NUMBER, NUMBER}, .option = {"", "-s", "", "", "", ""}},
	{.name = "apply_script",  .len = LEN_SCRIPT,      .args_type = {0, STRING }, .option = {"", ""  }},
	{.name = "edit_script",   .len = LEN_SCRIPT,      .args_type = {0, STRING }, .option = {"", ""  }},
	{.name = "bundle",        .len = LEN_BUNDLE,      .args_type = {0, STRING, STRING}, .option = {"", "", ""}}
};

/**
 * Print the error message according to a type and a flags
 *
 * @param type short containing the token associated with the error flags
 * @param flags short containing the error flags
 * @param cmd_name pointer to the command name
 * @param str pointer to the command argument
 * @return value of the error flags
 */

short msg_error(short type, int flags, char *cmd_name, char *str){
	switch (flags) {
	case EINVA:                                              /*INVALID ARGUMENT*/
		if (type == SYMTYPE) fprintf(stderr, "Error command [%s]: invalid argument '%s', please enter 'v' for vertical or 'h for horizontal\n", cmd_name, str);
		else if (type == VIEW) fprintf(stderr, "Error command [%s] , invalid argument '%s' please enter 'workspace' or 'image' for resize one of them\n", cmd_name, str);
		else fprintf(stderr, "Error command [%s]: invalid argument '%s'\n", cmd_name, str);
		break;

	case EMSG:                                               /*MISSING ARGUMENT*/
		fprintf(stderr, "Error command [%s]: missing arguments\n", cmd_name);
		break;

	case ENUMV:                                              /*WRONG NUMERIC VALUE*/
		if (type == RELATIV) fprintf(stderr, "Error command [%s]: invalid arguments '%s', please enter numeric value \n", cmd_name, str);
		if (type == PIXEL) fprintf(stderr, "Error command [%s]: invalid arguments '%s', please enter numeric value between 0 and 255\n", cmd_name, str);
		if (type == NUMBER) fprintf(stderr, "Error command [%s]: invalid arguments '%s', please enter positive numeric value \n", cmd_name, str);
		if (type == POURC) fprintf(stderr, "Error command [%s]: invalid arguments '%s', please enter a numeric value between 0 and 100\n", cmd_name, str);
		if (type == ANGLE) fprintf(stderr, "Error command [%s]: invalid argument '%s', please enter a multiple of 90\n", cmd_name, str);
		if (type == POURCR) fprintf(stderr, "Error command [%s]: invalid arguments '%s', please enter a numeric value between -100 and 100\n", cmd_name, str);
		break;

	case EFFORM:                                             /*INVALID FILE FORMAT*/
		if (type == EXT) fprintf(stderr, "Error command [%s]: invalids argument '%s', please enter a valid image extension\n", cmd_name, str);
		break;

	case EOPT:                                               /*INVALID OPTION*/
		fprintf(stderr, "Error command [%s]: invalid arguments '%s', please enter a valid command option\n", cmd_name, str);
		break;

	case EUNKN:                                              /*UNKNOW ARGUMENT*/
		fprintf(stderr, "Error command [%s] : command not found\n", cmd_name);
		break;
	}
	return flags;
}

/**
 * Copy the pointer contents in a another
 *
 * @param s string to copy
 * @return str new pointer containing the copy of s contents
 */

char *string_cpy(char *s){
	if (s == NULL) return NULL;
	char *str = malloc(sizeof(char) * (strlen(s) + 1));
	if ((str = memcpy(str, s, strlen(s) + 1)) == NULL) {
		fprintf(stderr, "Error : memory copy failed\n");
		return NULL;
	}
	str[strlen(s)] = '\0';
	return str;
}

void multiple_free(int n, ...){
	va_list valist;
	int     i;
	va_start(valist, n);
	for (i = 0; i < n; i++) {
		char *s = va_arg(valist, char *);
		if (s != NULL) free(s);
	}
	va_end(valist);
}

/**
 * Search the command index in info_tab
 *
 * @param str pointer to the command name
 * @return return the command index info_tab if str is finded on it , -1 if the search isn't successful
 */

short find_index(char *str){
	if (str == NULL) return -1;
	int i;
	for (i = 0; i < LEN_INFO; i++) {
		if (strcmp(str, info_tab[i].name) == 0) return i;
	}
	return -1;
}

/**
 * Allocate the memory needed for the struct cmd
 * @return cmd pointer
 */

cmd *alloc_cmd(){
	cmd *command = malloc(sizeof(cmd));
	if (command == NULL) return NULL;

	command->name = NULL;
	command->args = NULL;
	command->size = 0;

	return command;
}

/**
 * Free cmd structure
 * @void
 */

void free_cmd(cmd *command){
	if (command == NULL) return;
	if (command->name != NULL) free(command->name);
	int i;
	for (i = 0; i < (command->size) - 1; i++) {
		if (command->args[i] != NULL && strlen(command->args[i]) != 0) free(command->args[i]);
	}
	if (command->args != NULL) free(command->args);
	free(command);
}

/**
 * Initialise the command -> args tab with empty string
 *
 * @param pointer to the command structure
 * @void
 */

void set_cmd_args(cmd *command){
	int i;
	for (i = 0; i < command->size - 1; i++) {
		command->args[i] = "";
	}
	command->args [command->size - 1] = NULL;
}

/**
 * Initialise the command structure
 *
 * @param command pointer to the command structure
 * @param str pointer to the command name
 * @return the command index in info_tab if it's not fail ,
 *	else return -2 if memory allocation failed -1 if the index is not found
 */

short init_cmd(cmd *command, char *str){
	short index;
	if ((index = find_index(str)) == -1) return index;
	if ((command->name = string_cpy(str)) == NULL) return -2;
	command->size = info_tab[index].len;
	command->args = malloc(command->size * (sizeof(char *)));
	if (command->args == NULL) {
		fprintf(stderr, "Error : memory allocation failed\n");
		return -2;
	}
	set_cmd_args(command);
	if ((command->args[0] = string_cpy(str)) == NULL) return -2;
	return index;
}

short is_natural(char *str){
	int i;
	int n = sscanf(str, "%u", &i);
	return (n == 1 && i >= 0) ? 0 : ENUMV;
}

short is_option(char *str){
	if (str == NULL || strlen(str) != 2 || str[0] != '-') return EOPT;
	return isalpha(str[1]) ?  0 : EOPT;
}

short is_option_flags(short index){
	return (index > 0 && index < 11) ? 1 : 0;
}

short is_angle(char *str){
	int i;
	int n = sscanf(str, "%u", &i);
	return (n == 1 && i % 90 == 0 && i >= 0) ? 0 : ENUMV;
}

short is_relative(char *str){
	int i;
	int n = sscanf(str, "%d", &i);
	return (n == 1) ? 0 : ENUMV;
}

short is_pixel(char *str){
	int i;
	int n = sscanf(str, "%u", &i);
	return (n == 1 && i <= 255 && i >= 0) ? 0 : ENUMV;
}

short is_view(char *str){
	return (strcmp(str, "workspace") == 0 || strcmp(str, "image") == 0) ? 0 : EINVA;
}

short is_symtype(char *str){
	return (strcmp(str, "v") == 0 || strcmp(str, "h") == 0) ? 0 : EINVA;
}

short is_extension(char *str){
	return (strcmp(str, "png") == 0 ||
	        strcmp(str, "jpeg") == 0 ||
	        strcmp(str, "gif") == 0 ||
	        strcmp(str, "bmp") == 0) ? 0 : EFFORM;
}

short is_pourcent(char *str){
	int i;
	int n = sscanf(str, "%u", &i);
	return (n == 1 && i <= 100 && i >= 0) ? 0 : ENUMV;
}

short is_pourcent_relative(char *str){
	int i;
	int n = sscanf(str, "%d", &i);
	return (n == 1 && i <= 100 && i >= -100) ? 0 : ENUMV;
}

/**
 * Check if an option is the one excepted or exist in the command specification
 *
 * @param index short represent the command index in info_tab
 * @param i short represent the current position in info_tab.option
 * @param arg pointer to the current argument in the command line
 * @return 0 if the option exist in the command specification ,
 *		  else 1 if any option matched with arg
 */
short check_option(short index, short i, char *arg){
	while (i < info_tab[index].len - 1)
		if (strcmp(arg, info_tab[index].option[i++]) == 0) return 0;
	return 1;
}

/**
 * Check if a command argument is a right token
 *
 * @param flag represent the correct token value
 * @param cmd_name pointer to the command name
 * @return print an error and return an ERRFLAGS if the command args is wrong
 *		   else return 0 (no wrong value)
 */

short check_token(short flags, char *cmd_name, char *arg){
	if (strlen(arg) == 0) return msg_error(0, EMSG, cmd_name, NULL);
	if (flags == STRING && strlen(arg) == 0) return msg_error(0, EMSG, cmd_name, NULL);
	if (flags == RELATIV && is_relative(arg)) return msg_error(RELATIV, ENUMV, cmd_name, arg);
	if (flags == POURCR && is_pourcent_relative(arg)) return msg_error(POURCR, ENUMV, cmd_name, arg);
	if (flags == NUMBER && is_natural(arg)) return msg_error(NUMBER, ENUMV, cmd_name, arg);
	if (flags == PIXEL && is_pixel(arg)) return msg_error(PIXEL, ENUMV, cmd_name, arg);
	if (flags == POURC && is_pourcent(arg)) return msg_error(POURC, ENUMV, cmd_name, arg);
	if (flags == EXT && is_extension(arg)) return msg_error(EXT, EFFORM, cmd_name, arg);
	if (flags == VIEW && is_view(arg)) return msg_error(VIEW, EINVA, cmd_name, arg);
	if (flags == SYMTYPE && is_symtype(arg)) return msg_error(SYMTYPE, EINVA, cmd_name, arg);
	if (flags == ANGLE && is_angle(arg)) return msg_error(ANGLE, ENUMV, cmd_name, arg);
	return 0;
}

/**
 * Check each command arguments
 *
 * @param command pointer to the command structure
 * @return short : 0 if all command arguments are ok , else return the first ERRFLAGS associated to
 *				   to the iterated command argument
 */

short check_arguments(cmd *command){
	if (command == NULL) return EINVA;
	int   i;
	short n, index, flags;
	index = find_index(command->name);

	for (i = 1; i < info_tab[index].len - 1; i++) {
		flags = info_tab[index].args_type[i];

		if (is_option_flags(flags) && strlen(command->args[i]) == 0) {
			i += flags - 1;
			continue;
		}
		if (is_option_flags(flags) && strcmp(command->args[i], info_tab[index].option[i]) != 0)
			return msg_error(0, EOPT, command->name, command->args[i]);
		if ((n = check_token(info_tab[index].args_type[i], command->name, command->args[i])) != 0) return n;
	}
	if (command->size > info_tab[index].len) {
		fprintf(stderr, "Error command [%s] : too much arguments \n", command->name);
		return EINVA;
	}
	return 0;
}

/**
 * Build command -> args
 *
 * @param command pointer to the command structure
 * @param s pointer to the string founded after the command name (the arguments)
 * @param index short representing the command index in info_tab
 * @return  0 if the construction doesn't fail ,
 *		   else EINVA if the command line contains too much arguments , 1 if s copy failed
 */

short build_args(cmd *command, char *s, short index){
	if (s == NULL || command == NULL) return 0;
	int   i = 1;
	char *str = string_cpy(s);
	if (str == NULL) return 1;
	char *space = " ";
	char *token = "";
	while (token != NULL) {                                               /*while an argument exist*/
		if (i != 1) token = strtok(NULL, space);
		else token = strtok(str, space);

		if (token != NULL) {
			while (is_option_flags(info_tab[index].args_type[i]) == 1) {                                                      /*while the correct token type correspond to an option*/
				if (is_option(token)) i += info_tab[index].args_type[i];                                                      /*the current argument isn't an option -> jump */
				else if (is_option(token) == 0 && check_option(index, i + 1, token) == 0) i += info_tab[index].args_type[i];  /*the current argument is an option but not the one execepted*/
				else break;
			}

			if (i >= (command->size) - 1) {
				if (str != NULL) free(str);
				fprintf(stderr, "Error command [%s] : too much arguments \n", command->name);
				return EINVA;
			}

			if ((command->args[i++] = string_cpy(token)) == NULL) return 1; /*add the current argument to command -> args */
		}
	}
	if (str != NULL) free(str);
	return 0;
}

/**
 * Build the command structure associated to the command line
 *
 * @param pointer to the command line
 * @return pointer to command structure if it's not fail , else NULL
 *
 */

cmd *parse_line(char *line){
	cmd * command = NULL;
	short index, i = 0;
	command = alloc_cmd();
	char *s = NULL;

	if ((s = string_cpy(line)) == NULL) {
		free_cmd(command);
		return NULL;
	}

	char *space = " ";
	char *token = string_cpy(strtok(s, space));
	char *s1 = string_cpy(strtok(NULL, ""));

	if ((index = init_cmd(command, token)) < 0) {                  /*command initialisation failed*/
		if (index == -1 && token != NULL) {
			msg_error(0, EUNKN, token, NULL);
			ERRPARSE = EUNKN;
		}
		free_cmd(command);
		multiple_free(3, s1, s, token);
		return NULL;
	}

	if ((i = build_args(command, s1, index)) >= 1) { /*command-> args construction failed*/
		if (i == EINVA) {
			check_arguments(NULL);                   /*case where command line containts to much arguments*/
			ERRPARSE = EINVA;
		}
		free_cmd(command);
		multiple_free(3, s1, s, token);
		return NULL;
	}

	if ((i = check_arguments(command)) > 0) {
		ERRPARSE = i;
		free_cmd(command);
		multiple_free(3, s1, s, token);
		return NULL;
	}
	multiple_free(3, s1, s, token);
	ERRPARSE = 0;
	return command;
}

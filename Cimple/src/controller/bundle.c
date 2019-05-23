#include "bundle.h"

/**
 *  Test if it's a regular file
 *
 * @param path the file
 * @return 1 if true else 0
 */
static int is_file(char *path){
	struct stat st = {0};
	stat(path, &st);
	return S_ISREG(st.st_mode);
}

/**
 * Test if it's a right expression according to the p
 * pattern
 * @param string string to compare with
 * @param pattern regex
 * @return 1 if regex match 0 else
 */
static short is_good_exp(char *string, char *pattern){
	int     res = 0;
	regex_t regex = {0};

	if (regcomp(&regex, pattern, REG_NOSUB | REG_EXTENDED) != 0)
		return 0;
	if (regexec(&regex, string, 0, NULL, 0) == 0) {
		printf("-> %s\n", string);
		res = 1;
	}
	regfree(&regex);
	return res;
}

/**
 * Return a list of file match, else NULL
 *
 * @param path in which search
 * @param pattern the pattern to test
 */
node *find_expr(char *path, char *pattern){
	node *list = NULL;
	DIR * dir = opendir(path);
	if (dir == NULL)
		return NULL;
	struct dirent *current;

	while ((current = readdir(dir)) != NULL) {
		if (memcmp(current->d_name, "./", 2) == 0 ||
		    memcmp(current->d_name, "../", 3) == 0)
			continue;
		if (is_file(current->d_name) && is_good_exp(current->d_name, pattern))
			list = insert_head(list, current->d_name);
	}
	closedir(dir);
	return list;
}

/**
 * Transform a code into a cmd
 *
 * @param cmd the command to transform
 * @return NULL if it failed
 */
cmd *get_real_cmd(char *command){
	if (strcmp(command, "bnw") == 0)
		return parse_line("bnw -a");
	if (strcmp(command, "negative") == 0)
		return parse_line("negative -a");
	if (strcmp(command, "greyscale") == 0)
		return parse_line("greyscale -a");
	fprintf(stderr, "Error: command %s doesn't exist\n", command);
	return NULL;
}

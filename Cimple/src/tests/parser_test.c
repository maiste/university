#include <stdio.h>
#include <stdlib.h>
#include "parser_test.h"
#include "parse.h"
#include "err_flags.h"



static char *symmetry_tab[7] = {"symmetry v", "symmetry h", "symmetry -r 90", "symmetry -v", "symmetry 98", "symmetry hello", NULL};
static char *sym_argstab[][5] = {
	{"symmetry", "v", NULL},
	{"symmetry", "h", NULL},
	{NULL},
	{NULL},
	{NULL},
	{NULL},
	{NULL}
};
static short sym_err_tab[7] = {0, 0, EINVA, EINVA, EINVA, EINVA, 0};  // checked


static char *rotate_tab[8] = {"rotate -r 90", "rotate 90", "rotate 100", "rotate -r 10", "rotate 0", "rotate -90", "rotate -r -10", NULL};
static char *rot_argstab[][5] = {
	{"rotate", "-r", "90", NULL},
	{"rotate", "",   "90", NULL},
	{NULL},
	{NULL},
	{"rotate", "",   "0",  NULL},
	{NULL},
	{NULL},
	{NULL}
};
static short rot_err_tab[8] = {0, 0, ENUMV, ENUMV, 0, ENUMV, ENUMV, 0};  // checked


static char *truncate_tab[5] = {"truncate", "truncate -s 123 0 123 0", "truncate -s aze 100 zez 10", "truncate -s 100 230", NULL};
static char *trunc_argstab[][10] = {
	{"truncate", "",   "",    "",  "",    "",  NULL},
	{"truncate", "-s", "123", "0", "123", "0", NULL},
	{NULL},
	{NULL},
	{NULL}
};
static short trunc_err_tab[5] = {0, 0, ENUMV, EMSG, 0};



static char *resize_tab[7] = {"resize 100 100", "resize workspace", "resize workspace 100", "resize workspace 100 100", "resize image", "resize image 100 100", NULL};
static char *res_argstab[][6] = {
	{NULL },
	{NULL },
	{NULL },
	{"resize", "workspace", "100", "100", NULL},
	{NULL },
	{"resize", "image", "100", "100", NULL},
	{NULL }
};
static short res_err_tab[7] = {EINVA, EMSG, EMSG, 0, EMSG, 0, 0};

static char *fill_tab[7] = {"fill 100 190 100 0", "fill -a 100 100 100", "fill -a 257 200 200 200", "fill 200 200 200", "fill its a joke", "fill -a 100 190 100 0", NULL};
static char *fill_argstab[][10] = {
	{"fill", "",   "100", "190", "100", "0", NULL},
	{NULL},
	{NULL},
	{NULL},
	{NULL},
	{"fill", "-a", "100", "190", "100", "0", NULL},
	{NULL}
};
static short fill_err_tab[7] = {0, EMSG, ENUMV, EMSG, ENUMV, 0, 0};

static char *replace_tab[5] = {"replace -a 100 200 123 12 123 239 12 9",      "replace -m 20 -a 200",
	                           "replace -m 199 -a 200 250 30 200 22 11 9 10", "replace -m 19 -a -200 250 30 200 22 11 9 10", NULL};
static char *rep_argstab[][15] = {
	{"replace", "", "", "-a", "100", "200", "123", "12", "123", "239", "12", "9", NULL},
	{NULL},
	{NULL},
	{NULL},
	{NULL}
};
static short rep_err_tab[5] = {0, EMSG, ENUMV, ENUMV, 0};

static char *save_tab[5] = {"save", "save ./", "save -p hjg", "save -g pick", NULL};
static char *save_argstab[][5] = {
	{"save", "",   "",    NULL},
	{NULL},
	{"save", "-p", "hjg", NULL},
	{NULL},
	{NULL}
};
static short save_err_tab[5] = {0, EINVA, 0, EOPT, 0};

static char *load_tab[5] = {"load ./noexist.png", "load -w 2 image.png", "load ", "load image.zzz", NULL};
static char *load_argstab[][5] = {
	{"load", "",   "",  "./noexist.png", NULL},
	{"load", "-w", "2", "image.png",     NULL},
	{NULL},
	{"load", "",   "",  "image.zzz",     NULL},
	{NULL}
};
static short load_err_tab[5] = {0, 0, EMSG, 0, 0};

static char *negative_tab[6] = {"negative -a 232", "negative -a", "negative", "negative   ", "negative 223", NULL};
static char *neg_argstab[][6] = {
	{NULL },
	{"negative", "-a", NULL},
	{"negative", "", NULL},
	{"negative", "", NULL},
	{NULL },
	{NULL }
};
static short neg_err_tab[6] = {EINVA, 0, 0, 0, EINVA, 0};

static char *bnw_tab[6] = {"bnw -a 232", "bnw -a ", "bnw", "bnw    ", "bnw 223", NULL};
static char *bnw_argstab[][6] = {
	{NULL },
	{"bnw", "-a", NULL},
	{"bnw", "", NULL},
	{"bnw", "", NULL},
	{NULL },
	{NULL }
};
static short bnw_err_tab[6] = {EINVA, 0, 0, 0, EINVA, 0};

static char *greyscale_tab[6] = {"greyscale -a 232", "greyscale -a ", "greyscale", "greyscale    ", "greyscale 223", NULL};
static char *grey_argstab[][6] = {
	{NULL },
	{"greyscale", "-a", NULL},
	{"greyscale", "", NULL},
	{"greyscale", "", NULL},
	{NULL },
	{NULL }
};
static short grey_err_tab[6] = {EINVA, 0, 0, 0, EINVA, 0};

static char *switch_tab[5] = {"switch_buffer zer", "switch_buffer -1  ", "switch_buffer", "switch_buffer 3", NULL};
static char *switch_argstab[][5] = {
	{NULL },
	{NULL },
	{NULL },
	{"switch_buffer", "3", NULL},
	{NULL }
};
static short switch_err_tab[5] = {ENUMV, ENUMV, EMSG, 0, 0};

static char *list_buffer[4] = {"list_buffer -a ", "list_buffer 10", "  list_buffer ", NULL};
static char *listb_argstab[][5] = {
	{NULL },
	{NULL },
	{"list_buffer", NULL},
	{NULL }
};
static short listb_err_tab[4] = {EINVA, EINVA, 0, 0};



static void test_parse_line(char **tab, char *str, int len){
	int  i;
	cmd *command = parse_line(str);
	if (tab[0] == NULL) assert_null(command);
	else {
		assert_non_null(command->args);
		for (i = 0; i < len; i++) {
			if (tab[i] == NULL) assert_null(command->args[i]);
			else assert_string_equal(tab[i], command->args[i]);
		}
	}
	free_cmd(command);
}

static void test_parse_error(short err, char *str){
	assert_int_equal(err, ERRPARSE);
}

static void input_test(void **state){
	int i = 0;
	while (symmetry_tab[i] != NULL) {
		test_parse_line(sym_argstab[i], symmetry_tab[i], LEN_SYM);
		test_parse_error(sym_err_tab[i], symmetry_tab[i]);
		i++;
	}
	i = 0;
	while (rotate_tab[i] != NULL) {
		test_parse_line(rot_argstab[i], rotate_tab[i], LEN_ROTATE);
		test_parse_error(rot_err_tab[i], rotate_tab[i]);
		i++;
	}
	i = 0;
	while (truncate_tab[i] != NULL) {
		test_parse_line(trunc_argstab[i], truncate_tab[i], LEN_TRUNCATE);
		test_parse_error(trunc_err_tab[i], truncate_tab[i]);
		i++;
	}
	i = 0;
	while (resize_tab[i] != NULL) {
		test_parse_line(res_argstab[i], resize_tab[i], LEN_RESIZE);
		test_parse_error(res_err_tab[i], resize_tab[i]);
		i++;
	}
	i = 0;
	while (fill_tab[i] != NULL) {
		test_parse_line(fill_argstab[i], fill_tab[i], LEN_FILL);
		test_parse_error(fill_err_tab[i], fill_tab[i]);
		i++;
	}
	i = 0;
	while (replace_tab[i] != NULL) {
		test_parse_line(rep_argstab[i], replace_tab[i], LEN_REPLACE);
		test_parse_error(rep_err_tab[i], replace_tab[i]);
		i++;
	}
	i = 0;
	while (negative_tab[i] != NULL) {
		test_parse_line(neg_argstab[i], negative_tab[i], LEN_NEG);
		test_parse_error(neg_err_tab[i], negative_tab[i]);
		i++;
	}
	i = 0;
	while (save_tab[i] != NULL) {
		test_parse_line(save_argstab[i], save_tab[i], LEN_SAVE);
		test_parse_error(save_err_tab[i], save_tab[i]);
		i++;
	}
	i = 0;
	while (load_tab[i] != NULL) {
		test_parse_line(load_argstab[i], load_tab[i], LEN_LOAD);
		test_parse_error(load_err_tab[i], load_tab[i]);
		i++;
	}
	i = 0;
	while (greyscale_tab[i] != NULL) {
		test_parse_line(grey_argstab[i], greyscale_tab[i], LEN_GREYS);
		test_parse_error(grey_err_tab[i], greyscale_tab[i]);
		i++;
	}
	i = 0;
	while (bnw_tab[i] != NULL) {
		test_parse_line(bnw_argstab[i], bnw_tab[i], LEN_BNW);
		test_parse_error(bnw_err_tab[i], bnw_tab[i]);
		i++;
	}
	i = 0;
	while (switch_tab[i] != NULL) {
		test_parse_line(switch_argstab[i], switch_tab[i], LEN_SWITCH);
		test_parse_error(switch_err_tab[i], switch_tab[i]);
		i++;
	}
	i = 0;
	while (list_buffer[i] != NULL) {
		test_parse_line(listb_argstab[i], list_buffer[i], LEN_LIST_BUFFER);
		test_parse_error(listb_err_tab[i], list_buffer[i]);
		i++;
	}
}

int run_parser_test(){
	const struct CMUnitTest tests[] = {
		cmocka_unit_test(input_test)
	};
	return cmocka_run_group_tests(tests, NULL, NULL);
}

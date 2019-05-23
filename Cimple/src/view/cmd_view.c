#include "cmd_view.h"


/**
 * @brief
 * Prompt the commandline and return
 * the content
 */
char *getcmdline(){
	char *s = (char *)NULL;
	s = readline("Cimple# ");
	if (s != NULL && strlen(s) != 0) {
		add_history(s);
		return s;
	}
	return NULL;
}

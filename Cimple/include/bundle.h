#ifndef BUNDLE_H
#define BUNDLE_H

#include <stdio.h>
#include <stdlib.h>
#include <regex.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <dirent.h>

#include "list.h"
#include "parse.h"

node *find_expr(char *path,char *pattern);
cmd *get_real_cmd(char *cmd);

#endif // BUNDLE_H
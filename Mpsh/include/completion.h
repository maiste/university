#ifndef COMPLETION_H
#define COMPLETION_H

#include <sys/types.h>

short is_exec(mode_t mod, uid_t prop, uid_t origu, gid_t grp, gid_t origg);
short is_readable(mode_t mod, uid_t prop, uid_t origu, gid_t grp, gid_t origg);
char ** completion(const char *text, int start, int end);

#endif

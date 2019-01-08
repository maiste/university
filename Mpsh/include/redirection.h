#ifndef REDIRECTION_H
#define REDIRECTION_H

int execute_with_pipe(piped_cmd *chain, short background);
void update_cmd_with_path(char ** exe);
short update_stdin(char *exe_stdin);
short update_stdout(char *exe_stdout);
short update_stderr(char *exe_stderr);
short update_cmd(cmd *exe);
short execute_cmd(cmd *exe, short last);


#endif

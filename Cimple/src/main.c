#include "stdio.h"
#include "cmd_line.h"

int main(){
	if (SDL_Init(SDL_INIT_VIDEO) != 0)
		return 1;
	if (SDL_SetHint(SDL_HINT_VIDEO_X11_NET_WM_PING, "0") != SDL_TRUE)
		// Avoid GNOME Freeze
		return 1;
	int rc = cimple_handler();
	SDL_Quit();
	return rc;
}

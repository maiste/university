## REPOSITORIES

BUILD = build/
TESTS = tests/

## SRC

SRC_FOLDER = src/
VIEW_FOLDER = src/view/
MOD_FOLDER = src/model/
CONTR_FOLDER = src/controller/
TESTS_FOLDER = src/tests/

SRC_MAIN = main.c
SRC_VIEW = cmd_view.c \
	   window.c   \
	   event.c
SRC_MOD = in.c		  		\
	  out.c 	  		\
	  m_color.c 			\
	  m_frame.c 			\
	  m_transform.c 		\
		m_image.c \
		parse.c    \
		list.c
    
SRC_CONTR = cmd_line.c \
						bundle.c
SRC_TESTS = io_test.c			\
			parser_test.c \
			m_tests.c 	\
	    main_test.c


# SRC_FILES

SRCP_MAIN = $(addprefix $(SRC_FOLDER), $(SRC_MAIN))
SRCP_VIEW = $(addprefix $(VIEW_FOLDER), $(SRC_VIEW))
SRCP_MOD = $(addprefix $(MOD_FOLDER), $(SRC_MOD))
SRCP_CONTR = $(addprefix $(CONTR_FOLDER), $(SRC_CONTR))
SRCP_TESTS = $(addprefix $(TESTS_FOLDER), $(SRC_TESTS))


# OBJS_FILES

OBJ_MAIN = $(SRCP_MAIN:$(SRC_FOLDER)%.c=$(BUILD)%.o)
OBJ_VIEW = $(SRCP_VIEW:$(SRC_FOLDER)%.c=$(BUILD)%.o)
OBJ_MOD = $(SRCP_MOD:$(SRC_FOLDER)%.c=$(BUILD)%.o)
OBJ_CONTR = $(SRCP_CONTR:$(SRC_FOLDER)%.c=$(BUILD)%.o)
OBJ_TESTS = $(SRCP_TESTS:$(TESTS_FOLDER)%.c=$(BUILD)%.o)
OBJS = $(OBJ_MAIN) $(OBJ_VIEW) $(OBJ_MOD) $(OBJ_CONTR)

# TESTS LIST
TESTS_DEPS = $(OBJ_MOD)

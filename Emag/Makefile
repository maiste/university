###################
#    Emag Make    #
# Betend - Marais #
###################


# ** VARIABLES **

# Gcc++
CC=g++
FLAGS=--std=c++11 -Wall
GAME_TARGET=gp
MAKER_TARGET=gc

# Folders
BUILD=build/
INC=-I include/
LIBS=$(shell pkg-config ncurses --cflags --libs)

SRC_D=src/
MODEL_D=src/model/
VIEW_D=src/view/
CONTROLLER_D=src/controller/
EXCEPT_D=src/exception/

MAKER_D=src/maker/

# Files
GAME=Game.cpp
MODEL=Plateau.cpp \
		Oueurj.cpp \
		NormalStreumon.cpp \
		ColumnStreumon.cpp \
		LineStreumon.cpp \
		RandomStreumon.cpp \
		Position.cpp \
		Diams.cpp \
		Geuchar.cpp \
		Empty.cpp \
		Reumus.cpp \
		Teupor.cpp
VIEW=Terminal.cpp \
		 Curse.cpp
CONTROLLER=Controller.cpp \
					 FileManager.cpp
EXCEPT=MoveError.cpp \
			 SizeError.cpp \
			 CaseError.cpp \
			 UnknownFile.cpp \
			 MissingPlayer.cpp \
			 CloseStream.cpp

GAME_MAKER=Maker.cpp
MAKER=Builder.cpp

# Files list
SRC_GAME=$(addprefix $(SRC_D), $(GAME))
SRC_MODEL=$(addprefix $(MODEL_D), $(MODEL))
SRC_VIEW=$(addprefix $(VIEW_D), $(VIEW))
SRC_CONTROLLER=$(addprefix $(CONTROLLER_D), $(CONTROLLER))
SRC_EXCEPT=$(addprefix $(EXCEPT_D), $(EXCEPT))

SRC_GAME_MAKER=$(addprefix $(SRC_D), $(GAME_MAKER))
SRC_MAKER=$(addprefix $(MAKER_D), $(MAKER))

# Generate .o targets
OBJ_GAME=$(SRC_GAME:$(SRC_D)%.cpp=$(BUILD)%.o)
OBJ_MODEL=$(SRC_MODEL:$(SRC_D)%.cpp=$(BUILD)%.o)
OBJ_VIEW=$(SRC_VIEW:$(SRC_D)%.cpp=$(BUILD)%.o)
OBJ_EXCEPT=$(SRC_EXCEPT:$(SRC_D)%.cpp=$(BUILD)%.o)
OBJ_CONTROLLER=$(SRC_CONTROLLER:$(SRC_D)%.cpp=$(BUILD)%.o)

OBJS_GAME=$(OBJ_GAME) $(OBJ_MODEL) $(OBJ_VIEW) $(OBJ_CONTROLLER) $(OBJ_EXCEPT)


OBJ_GAME_MAKER=$(SRC_GAME_MAKER:$(SRC_D)%.cpp=$(BUILD)%.o)
OBJ_MAKER=$(SRC_MAKER:$(SRC_D)%.cpp=$(BUILD)%.o)

OBJS_MAKER=$(OBJ_GAME_MAKER) $(OBJ_MODEL) $(OBJ_CONTROLLER) $(OBJ_EXCEPT) $(OBJ_MAKER) $(OBJ_VIEW)

# ** RULES **

# Make
all: $(GAME_TARGET) $(MAKER_TARGET)

# Rule for game
$(GAME_TARGET): $(OBJS_GAME)
	@printf "== linking %s ==\n" $@
	$(CC) $(FLAGS) $(INC) -o $@ $^ $(LIBS)
	@printf "\n"

# Rule for maker
$(MAKER_TARGET): $(OBJS_MAKER)
	@printf "== linking %s ==\n" $@
	$(CC) $(FLAGS) $(INC) -o $@ $^ $(LIBS)
	@printf "\n"

# Rule to build .o
$(BUILD)%.o : $(SRC_D)%.cpp
	@mkdir -p $(dir $@)
	@printf "CC %s\n" $<
	@$(CC) $(FLAGS) $(INC) -c -o $@ $< $(LIBS)


# Clean all files
clean :
	@printf "\n=== clean ===\n"
	rm -rf $(BUILD) $(GAME_TARGET) $(MAKER_TARGET)

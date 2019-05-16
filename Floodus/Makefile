NAME = floodus
ZIP_FILE = MARAIS_DURAND_$(NAME).zip
SRCDIR = src/
BIN = bin/
DOCS = doc/
INCL = include/
COVERAGE = coverage.html/

FILES := $(shell find $(SRCDIR) -name '*.c')
OBJ:= $(FILES:$(SRCDIR)%.c=$(BIN)%.o)

CC = gcc
NCURSES = $(shell pkg-config ncurses --libs)
FLAGS = -Wall -Wextra -Werror -fprofile-arcs -ftest-coverage
LDLIBS = -pthread -D_REENTRANT -lm -D_GNU_SOURCE $(NCURSES)

.PHONY: all
all:
	@printf "%s\n" $(OBJ)

$(NAME): $(OBJ)
	@printf "[\e[1;34mEn cours\e[0m] Assemblement\n"
	$(CC) -o $(NAME) $(FLAGS) -I $(INCL) $(OBJ) $(LDLIBS)
	@printf "[\e[1;32mOK\e[0m] Assemblement finie\n"

$(BIN)%.o: $(SRCDIR)%.c
	@mkdir -p $(dir $@)
	$(CC) -c $(FLAGS) -I $(INCL) -o $@ $< $(LDLIBS)

.PHONY: debug
debug: FLAGS += -D D_FLAG
debug: $(NAME)

.PHONY: log
log: FLAGS += -D D_FLAG -D D_LOGFILE
log: $(NAME)

.PHONY: clean
clean:
	@printf "[\e[1;34mEn cours\e[0m] Suppression des binaires\n"
	@rm -rf $(BIN)
	@rm -rf $(COVERAGE)
	@rm -rf coverage.info
	@printf "[\e[1;32mOK\e[0m] Suppression finie\n"

.PHONY: cleandoc
cleandoc:
	@printf "[\e[1;34mEn cours\e[0m] Suppression de la documentation\n"
	@rm -rf $(DOCS)
	@printf "[\e[1;32mOK\e[0m] Suppression finie\n"

.PHONY: cleanall
cleanall: clean cleandoc
	@rm -rf $(NAME)

.PHONY: re
re: cleanall $(NAME)
	@rm -rf $(ZIP_FILE)

.PHONY: doc
doc: cleandoc
	@printf "[\e[1;34mEn cours\e[0m] Création de la documentation\n"
	@doxygen documentation
	@printf "[\e[1;32mOK\e[0m] Création finie\n"

.PHONY: zip
zip:
	@printf "[\e[1;34mEn cours\e[0m] Début du zippage\n"
	@zip -r $(ZIP_FILE) README.md Makefile $(SRCDIR) documentation $(INCL) MARAIS_DURAND_rapport.pdf
	@printf "[\e[1;32mOK\e[0m] Zippage finie\n"

.PHONY: coverage
coverage:
	@printf "[\e[1;34mEn cours\e[0m] Création du rapport de code coverage\n"
	@rm -rf coverage coverage.info
	@lcov -c --directory bin --output-file coverage.info
	@genhtml coverage.info --output-directory $(COVERAGE)
	@printf "[\e[1;32mOK\e[0m] Rapport dans coverage\n"


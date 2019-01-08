LDLIBS=-lreadline -lfl -lm
EXEC=main
BUILD=build
CC=gcc
CFLAGS=-W -Wall -g -Iinclude -Iinclude/interne_cmd -Iinclude/parser -I$(BUILD)/parser
LEX = flex
YACC = bison -d

all:
	mkdir -p $(BUILD)
	mkdir -p $(BUILD)/parser
	make $(EXEC)

$(BUILD)/parser/mpsh.tab.c: src/parser/mpsh.y $(BUILD)/cmd.o
	$(YACC) -o $(BUILD)/parser/mpsh.tab.c src/parser/mpsh.y

$(BUILD)/parser/lex.yy.c: src/parser/mpsh.l $(BUILD)/cmd.o
	$(LEX) -o $(BUILD)/parser/lex.yy.c --header-file=$(BUILD)/parser/lex.yy.h src/parser/mpsh.l

$(BUILD)/struct_utils.o: src/parser/struct_utils.c
	$(CC) $(CFLAGS) -o $(BUILD)/struct_utils.o -c src/parser/struct_utils.c $(LDLIBS)

$(BUILD)/nodes.o: src/parser/nodes.c
	$(CC) $(CFLAGS) -o $(BUILD)/nodes.o -c src/parser/nodes.c $(LDLIBS)

$(BUILD)/cmd.o: src/parser/cmd.c
	$(CC) $(CFLAGS) -o $(BUILD)/cmd.o -c src/parser/cmd.c $(LDLIBS)

$(BUILD)/arith.o: src/parser/arith.c
	$(CC) $(CFLAGS) -o $(BUILD)/arith.o -c src/parser/arith.c $(LDLIBS)

$(BUILD)/handle.o: src/handle.c
	$(CC) $(CFLAGS) -o $(BUILD)/handle.o -c src/handle.c $(LDLIBS)

$(BUILD)/redirection.o: src/redirection.c
	$(CC) $(CFLAGS) -o $(BUILD)/redirection.o -c src/redirection.c $(LDLIBS)

$(BUILD)/completion.o: src/completion.c
	$(CC) $(CFLAGS) -o $(BUILD)/completion.o -c src/completion.c $(LDLIBS)

$(BUILD)/env_list.o: src/env_list.c
	$(CC) $(CFLAGS) -o $(BUILD)/env_list.o -c src/env_list.c $(LDLIBS)

$(BUILD)/environment.o: src/environment.c
	$(CC) $(CFLAGS) -o $(BUILD)/environment.o -c src/environment.c $(LDLIBS)

$(BUILD)/cd.o: src/interne_cmd/cd.c
	$(CC) $(CFLAGS) -o $(BUILD)/cd.o -c src/interne_cmd/cd.c $(LDLIBS)

$(BUILD)/alias.o: src/interne_cmd/alias.c
	$(CC) $(CFLAGS) -o $(BUILD)/alias.o -c src/interne_cmd/alias.c $(LDLIBS)

$(BUILD)/unalias.o: src/interne_cmd/unalias.c
	$(CC) $(CFLAGS) -o $(BUILD)/unalias.o -c src/interne_cmd/unalias.c $(LDLIBS)

$(BUILD)/export.o: src/interne_cmd/export.c
	$(CC) $(CFLAGS) -o $(BUILD)/export.o -c src/interne_cmd/export.c $(LDLIBS)

$(BUILD)/set.o: src/interne_cmd/set.c
	$(CC) $(CFLAGS) -o $(BUILD)/set.o -c src/interne_cmd/set.c $(LDLIBS)

$(BUILD)/unset.o: src/interne_cmd/unset.c
	$(CC) $(CFLAGS) -o $(BUILD)/unset.o -c src/interne_cmd/unset.c $(LDLIBS)

$(BUILD)/type.o: src/interne_cmd/type.c
	$(CC) $(CFLAGS) -o $(BUILD)/type.o -c src/interne_cmd/type.c $(LDLIBS)

$(BUILD)/umask.o: src/interne_cmd/umask.c
	$(CC) $(CFLAGS) -o $(BUILD)/umask.o -c src/interne_cmd/umask.c $(LDLIBS)

$(BUILD)/history.o: src/interne_cmd/history.c
	$(CC) $(CFLAGS) -o $(BUILD)/history.o -c src/interne_cmd/history.c $(LDLIBS)

$(BUILD)/builtin.o: src/interne_cmd/builtin.c
	$(CC) $(CFLAGS) -o $(BUILD)/builtin.o -c src/interne_cmd/builtin.c $(LDLIBS)

$(BUILD)/complete.o: src/interne_cmd/complete.c
	$(CC) $(CFLAGS) -o $(BUILD)/complete.o -c src/interne_cmd/complete.c $(LDLIBS)

$(BUILD)/signal_handler.o: src/signal_handler.c
	$(CC) $(CFLAGS) -o $(BUILD)/signal_handler.o -c src/signal_handler.c $(LDLIBS)

main: src/main.c $(BUILD)/parser/mpsh.tab.c $(BUILD)/parser/lex.yy.c $(BUILD)/redirection.o $(BUILD)/arith.o $(BUILD)/struct_utils.o $(BUILD)/nodes.o $(BUILD)/cmd.o $(BUILD)/env_list.o $(BUILD)/environment.o $(BUILD)/cd.o $(BUILD)/alias.o $(BUILD)/export.o $(BUILD)/history.o $(BUILD)/builtin.o $(BUILD)/completion.o $(BUILD)/umask.o $(BUILD)/type.o $(BUILD)/unalias.o $(BUILD)/set.o $(BUILD)/unset.o $(BUILD)/signal_handler.o $(BUILD)/handle.o $(BUILD)/complete.o
	$(CC) $(CFLAGS) -o mpsh $(BUILD)/env_list.o $(BUILD)/arith.o $(BUILD)/struct_utils.o $(BUILD)/nodes.o $(BUILD)/cmd.o $(BUILD)/environment.o $(BUILD)/history.o $(BUILD)/complete.o $(BUILD)/umask.o $(BUILD)/type.o $(BUILD)/cd.o $(BUILD)/alias.o $(BUILD)/export.o $(BUILD)/set.o $(BUILD)/unset.o $(BUILD)/completion.o $(BUILD)/builtin.o $(BUILD)/redirection.o $(BUILD)/unalias.o $(BUILD)/handle.o $(BUILD)/signal_handler.o  $(BUILD)/parser/mpsh.tab.c $(BUILD)/parser/lex.yy.c src/main.c $(LDLIBS)

install: all
	mkdir -p ~/mpsh
	mkdir -p ~/mpsh/man/man1
	cp mpsh ~/mpsh
	install -m 644 man/mpsh.1.gz ~/mpsh/man/man1

clean:
	rm -rf $(BUILD)

mrproper: clean
	rm -rf ~/mpsh
	rm -rf mpsh

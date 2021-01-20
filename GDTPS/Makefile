# +------------------------------+
# | Good Duck Transfert Protocol |
# | Version: 1.0                 |
# | RFC: draft-6.4.1             |
# | Authors: Marais-Viau         |
# +------------------------------+

VERSION="1.0"
SERVER_PORT ?= "1027"
GDTP_port ?= "1027"
GDTP_addr ?= "127.0.0.1"
GDTP_udp_port ?= "7201"
DEBUG ?= "no"

all: compile_client compile_server

version:
	@printf "Compiler for version %s.\n" $(VERSION)

compile_common:
	@printf "Compile extern library.\n"
	javac -cp src src/common/*.java

compile_server: compile_common
	@printf "Compile the server.\n"
	javac -cp src src/server/*.java

compile_client: compile_common
	@printf "Compile the client.\n"
	javac -cp src/lanterna-3.0.4.jar:src src/client/*.java

server: compile_server
	@printf "Run server on port $(SERVER_PORT).\n\n"
	@java -cp src server.Server $(SERVER_PORT)

client: compile_client
	@printf "Run client.\n"
	java -cp src/lanterna-3.0.4.jar:src client.Client  $(DEBUG) $(GDTP_addr) $(GDTP_port) $(GDTP_udp_port)

clean_common:
	@printf "Clean_common.\n"
	rm -rf src/common/*.class

clean_client:
	@printf "Clean client.\n"
	rm -rf src/client/{*.class,gui/*.class}

clean_server:
	@printf "Clean server. \n"
	rm -rf src/server/*.class

clean: clean_client clean_server clean_common



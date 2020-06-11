all: compile

compile:
	dune build
	ln -sf _build/default/src/genconcat.exe ./genconcat

clean:
	rm -rf ./genconcat
	dune clean

SRC = Maze.java SchapireDriver.java

all:	$(SRC)
	javac -d . $(SRC)
	java probabalistic_reasoning.SchapireDriver

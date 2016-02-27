package probabalistic_reasoning;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;


public class Maze {
	final static Charset ENCODING = StandardCharsets.UTF_8;

        public static final int DIMENSION = 4;

	// A few useful constants to describe actions
	public static int[] NORTH = {0, 1};
	public static int[] EAST = {1, 0};
	public static int[] SOUTH = {0, -1};
	public static int[] WEST = {-1, 0};
        public static int[] NONE = {0, 0};	
	public int width;
	public int height;

        public static int actions[][] = { NORTH, EAST, SOUTH, WEST, NONE };
        public static char colorSet[] = { 'r', 'g', 'b', 'y' };

	private char[][] grid;
        private char[][] colors;

        public Random rand = new Random(0);

	public static Maze readFromFile(String filename) {
		Maze m = new Maze();

		try {
			List<String> lines = readFile(filename);
			m.height = lines.size();

			int y = 0;
			m.grid = new char[m.height][];
			for (String line : lines) {
				m.width = line.length();
				m.grid[m.height - y - 1] = new char[m.width];
				for (int x = 0; x < line.length(); x++) {
					// (0, 0) should be bottom left, so flip y as 
					//  we read from file into array:
					m.grid[m.height - y - 1][x] = line.charAt(x);
				}
				y++;

				// System.out.println(line.length());
	                }

                        m.colors = new char[m.height][m.width];
                        m.colorIn();
                        System.out.println(m);
			return m;
		} catch (IOException E) {
			return null;
		}

              
	}

	private static List<String> readFile(String fileName) throws IOException {
		Path path = Paths.get(fileName);
		return Files.readAllLines(path, ENCODING);
	}

	public char getChar(int x, int y) {
		return grid[y][x];
	}

        public char getColor(int x, int y) {
                return colors[y][x];
        }

	// is the location x, y on the map, and also a legal floor tile (not a wall)?
	public boolean isLegal(int x, int y) {
		// on the map
		if(x >= 0 && x < width && y >= 0 && y < height) {
			// and it's a floor tile, not a wall tile:
			return getChar(x, y) == '.';
		}
		return false;
	}

        public int[] getPath(int len) {
          int[] path = new int[len];
          int xCurr, yCurr;
          int xNext, yNext;
          int randVal;
          int[] rMove;

          // Get the starting position
          do {
            xCurr = rand.nextInt(width);
            yCurr = rand.nextInt(height);
          } while (! isLegal(xCurr, yCurr));

          // Put these in the first position in the path
          path[0] = XYtoState(xCurr, yCurr);

          for (int i = 1; i < len; i++) {
            // Find the next legal move (randomly)
            do {
              rMove = actions[rand.nextInt(5)];
              xNext = xCurr + rMove[0];
              yNext = yCurr + rMove[1];
            } while (! isLegal(xNext, yNext));

            path[i] = XYtoState(xNext, yNext);
            xCurr = xNext;
            yCurr = yNext;

          }

          return path;
        }

        public char[] getColorPath(int[] path) {
          int[] XY;
          int rVal;
          char[] cPath = new char[path.length];
          char correct;
          char perceived;

          for (int i = 0; i < path.length; i++) {
            XY = StatetoXY(path[i]);
            correct = colors[XY[1]][XY[0]];

            rVal = rand.nextInt(100);

            if (rVal < 88) {  // Give the correct value 88% of the time
              perceived = correct;
            } else {
              do {            // Randomly choose one of the other three options
                perceived = colorSet[rand.nextInt(4)];
              } while (perceived == correct);
            }

            cPath[i] = perceived;
          }
          return cPath;
        }
	
        public char[] getCorrectColorPath(int[] path) {
          int[] XY;
          int rVal;
          char[] cPath = new char[path.length];

          for (int i = 0; i < path.length; i++) {
            XY = StatetoXY(path[i]);
            cPath[i] = colors[XY[1]][XY[0]];
          }
          return cPath;
        }

        public static int[] StatetoXY(int state) {
          int[] pair = new int[2];
          pair[0] = state % DIMENSION;
          pair[1] = state / DIMENSION;

          return pair;
        }

        public static int XYtoState(int x, int y) {
          return (y * DIMENSION + x);
        }
	public void colorIn() {
          int rVal = -1;
          for (int y = 0; y < width; y++) {
            for (int x = 0; x < height; x++) {
              if (getChar(x, y) == '.') {
                rVal = rand.nextInt(4);

                switch(rVal) {
                  case 0:   colors[y][x] = 'r';
                            break;
                  case 1:   colors[y][x] = 'g';
                            break;
                  case 2:   colors[y][x] = 'b';
                            break;
                  case 3:   colors[y][x] = 'y';
                            break;
                  default:  colors[y][x] = 'x';
                }
              } else {
                colors[y][x] = '#';
              }
            }
          }
        }

	public String toString() {
		String s = "Maze:\n";
		for (int y = 0; y < height; y++) {
                        s += "\t";
			for (int x = 0; x < width; x++) {
				s += grid[y][x];
			}
			s += "\n";
		}
                s += "\nColors:\n";
              
                s += "\t";
                for (int y = 0; y < height; y++) {
                  for (int x = 0; x < width; x++) {
                    s += colors[y][x];
                  }
                  s += "\n\t";
                }
		return s;
	}

	public static void main(String args[]) {
		Maze m = Maze.readFromFile("simple.maz");
		System.out.println(m);
	}

}

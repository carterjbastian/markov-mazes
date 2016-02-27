package probabalistic_reasoning;

public class SchapireDriver {
  
  // Variables
  private static final int dimension = 4;
  public static Maze m;
  
  // File with the maze in it
  public static final String mazeFile = "./simple1.maz";


  public static void main(String[] args) {
    int[] randomPath;
    char[] correctColors;
    char[] observedColors;

    m = Maze.readFromFile(mazeFile);
    randomPath = m.getPath(10);
    correctColors = m.getCorrectColorPath(randomPath);
    observedColors = m.getColorPath(randomPath);
    
    printPath(randomPath, observedColors, correctColors);

  }

  public static void printPath(int[] path, char[] observed, char[] colors) {
    int[] XY;
    for (int i = 0; i < path.length; i++) {
      XY = Maze.StatetoXY(path[i]);
      System.out.println("Step " + (i+1) + ": (" + XY[0] + ", " + XY[1] + ")\tE = " + observed[i] + " (correct = " + colors[i] + ")");
    }
  }
}

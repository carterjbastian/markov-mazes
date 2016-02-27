package probabalistic_reasoning;

import java.util.Arrays;
import java.text.DecimalFormat;
public class SchapireDriver {
  // Variables
  private static final int mazeDimension = 4;
  public static Maze m;
  public static final int pathLen = 10;

  // File with the maze in it
  public static final String mazeFile = "./simple1.maz";


  public static class SchapireProblem {
    public int mazeDimension;
    public int stateCount;
    public Maze m;
    DecimalFormat f = new DecimalFormat("#0.000"); 
    public int[] randomPath;
    public char[] correctColors;
    public char[] evidence;

    public double[][] transitionModel;
    public double[][] transitionTranspose;

    public double[][] rSensorModel;
    public double[][] bSensorModel;
    public double[][] ySensorModel;
    public double[][] gSensorModel;

    public double[] forwardInitialCondition;


    public SchapireProblem(Maze maze, int dim, int pLen) {
      mazeDimension = dim;
      m = maze;
      stateCount = dim * dim;

      randomPath = m.getPath(pLen);
      correctColors = m.getCorrectColorPath(randomPath);
      evidence = m.getColorPath(randomPath);

      printPath();

    }

    /**
     * Builds the transition and sensor models for the Schapire Problem
     */
    public void setUp() {
      double initProb;
      char c; // Color
      int[] XY; // For transitioning between state number and maze location
      int state;
      double[][] model;

      int legalMoves;
      double moveProb;
      int nextState;

      /* Build the sensor Models */
      // Sensor model for red
      c = 'r';
      model = new double[stateCount][stateCount]; // Initialized to zeros
      for (state = 0; state < stateCount; state++) {
        XY = Maze.StatetoXY(state);
        if (m.getColor(XY[0], XY[1]) == c) // This state is the given color
          model[state][state] = 0.88;
        else // The state is not the given color
          model[state][state] = 0.04;
      }
      rSensorModel = model;

      // Sensor model for blue
      c = 'b';
      model = new double[stateCount][stateCount]; // Initialized to zeros
      for (state = 0; state < stateCount; state++) {
        XY = Maze.StatetoXY(state);
        if (m.getColor(XY[0], XY[1]) == c) // This state is the given color
          model[state][state] = 0.88;
        else // The state is not the given color
          model[state][state] = 0.04;
      }
      bSensorModel = model;

      // Sensor model for green
      c = 'g';
      model = new double[stateCount][stateCount]; // Initialized to zeros
      for (state = 0; state < stateCount; state++) {
        XY = Maze.StatetoXY(state);
        if (m.getColor(XY[0], XY[1]) == c) // This state is the given color
          model[state][state] = 0.88;
        else // The state is not the given color
          model[state][state] = 0.04;
      }
      gSensorModel = model;

      // Sensor model for yellow
      c = 'y';
      model = new double[stateCount][stateCount]; // Initialized to zeros
      for (state = 0; state < stateCount; state++) {
        XY = Maze.StatetoXY(state);
        if (m.getColor(XY[0], XY[1]) == c) // This state is the given color
          model[state][state] = 0.88;
        else // The state is not the given color
          model[state][state] = 0.04;
      }
      ySensorModel = model;

      // Sanity Check
      System.out.println("Red Sensor Model:");
      printMatrix(rSensorModel, stateCount);
      System.out.println("Blue Sensor Model:");
      printMatrix(bSensorModel, stateCount);
      System.out.println("Green Sensor Model:");
      printMatrix(gSensorModel, stateCount);
      System.out.println("Yellow Sensor Model:");
      printMatrix(ySensorModel, stateCount);



      /* Build the transition Model */
      // For each state in the system
      transitionModel = new double[stateCount][stateCount];
      for (state = 0; state < stateCount; state++) {
        legalMoves = 0;
        moveProb = 0.0;
        XY = Maze.StatetoXY(state); // Get the (x,y) coordinates of this state

        // Find out how many available actions there are
        for (int[] action : Maze.actions)
          if (m.isLegal(XY[0] + action[0], XY[1] + action[1]))
            legalMoves += 1;

        if (legalMoves == 0) // Edge case of state being on a wall surrounded by walls
          moveProb = 0.0;
        else
          moveProb = 1.0 / legalMoves;

        // Fill in with probability of transitioning from state -> nextState
        for (int[] action : Maze.actions) {
          if (m.isLegal(XY[0] + action[0], XY[1] + action[1])) {
            nextState = Maze.XYtoState(XY[0] + action[0], XY[1] + action[1]);
            transitionModel[state][nextState] = moveProb;
          }
        }
      }

      /* Sanity Check */
      System.out.println("Transition Table: ");
      printMatrix(transitionModel, stateCount);

      /* Build its transpose (for Filtering equation) */
      // This loop has god-awful locality. Sean Smith would kill me.
      transitionTranspose = new double[stateCount][stateCount];
      for (int y = 0; y < stateCount; y++)
        for (int x = 0; x < stateCount; x++)
          transitionTranspose[y][x] = transitionModel[x][y]; 

      System.out.println("Transpose of Transition Table: ");
      printMatrix(transitionTranspose, stateCount);


      /* Build the forward initial condition */
      initProb = 1.0 / stateCount;          // All states are equi-probable 
      forwardInitialCondition = new double[stateCount];
      Arrays.fill(forwardInitialCondition, initProb);
      System.out.println("\nInitial Condition:");
      printMessage(forwardInitialCondition, mazeDimension);
    }

    public void printPath() {
      int[] XY;
      for (int i = 0; i < randomPath.length; i++) {
        XY = Maze.StatetoXY(randomPath[i]);
        System.out.println("Step " + (i+1) + ": (" + XY[0] + ", " + XY[1] + 
            ")\tE = " + evidence[i] + " (correct = " + correctColors[i] + ")");
      }
    }

    public void printMatrix(double[][] matrix, int dimension) {
      System.out.print("[");
      for (int y = 0; y < dimension; y++) {
        System.out.print("\t");
        for (int x = 0; x < dimension; x++) {
          System.out.print(f.format(matrix[y][x]) + ", ");
        }
        System.out.print("\n");
      }
      System.out.println("]\n");
    }
  
    public void printMessage(double[] message, int dimension) {
      System.out.print("[");
      int l = dimension;
      for (int i = 0; i < message.length; i++) {
        if (l == dimension) {
          System.out.print("\n\t");
          l = 0;
        }

        System.out.print(f.format(message[i]) + ", ");
        l++;
      }
      System.out.print("\n]\n");
    }
  
  }


  public static void main(String[] args) {
    m = Maze.readFromFile(mazeFile);
    SchapireProblem prob = new SchapireProblem(m, mazeDimension, pathLen);
    prob.setUp();
  }

}

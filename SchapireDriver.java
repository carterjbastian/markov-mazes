package probabalistic_reasoning;

import java.util.Arrays;
import java.text.DecimalFormat;
import java.util.LinkedList;

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
    public double[] backwardInitialCondition;

    public double[][] viterbiMessages; // Holds probabilities of optimal path to that state (at that step)
    public int[][] viterbiBackchains; // Holds the backchain accross steps

    public SchapireProblem(Maze maze, int dim, int pLen) {
      mazeDimension = dim;
      m = maze;
      stateCount = dim * dim;

      randomPath = m.getPath(pLen);
      correctColors = m.getCorrectColorPath(randomPath);
      evidence = m.getColorPath(randomPath);

      viterbiMessages = new double[pLen][stateCount];
      viterbiBackchains = new int[pLen][stateCount];

      printPath();
      setUp();
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

      /* Build the backward initial condtion */
      backwardInitialCondition = new double[stateCount];
      Arrays.fill(backwardInitialCondition, 1.0);
      System.out.println("\nInitial Condition (Backward):");
      printMessage(backwardInitialCondition, mazeDimension);

      /* Build the forward initial condition */
      initProb = 1.0 / stateCount;          // All states are equi-probable 
      forwardInitialCondition = new double[stateCount];
      Arrays.fill(forwardInitialCondition, initProb);
      System.out.println("\nInitial Condition (Forward):");
      printMessage(forwardInitialCondition, mazeDimension);

    }

    /**
     * Implements Markov Chain Filtering to produce probability distributions
     */
    public void solveFiltering() {
      double[][] sensorModel = null;
      double[] forwardMessage;
      int[] XY;

      // Initialize with the initial probability distribution
      forwardMessage = forwardInitialCondition;

      for (int step = 0; step < randomPath.length; step++) {
        // Select the appropriate sensor model based on the evidence
        switch (evidence[step]) {
          case 'r':   sensorModel = rSensorModel;
                      break;
          case 'b':   sensorModel = bSensorModel;
                      break;
          case 'g':   sensorModel = gSensorModel;
                      break;
          case 'y':   sensorModel = ySensorModel;
                      break;
        }
        
        // f_1:t+1 = alpha * SensorModel_t+1 * TransitionTranspose * f_1:t
        forwardMessage = normalize(
                          matrixMultiply(sensorModel, 
                            matrixMultiply(transitionTranspose, forwardMessage)));

        // Step one is the starting point for viterbi optimal-path finding
        if (step == 0)
          viterbiMessages[0] = forwardMessage;

        System.out.println("\nFiltered Distribuition at step " + (step+1) + ": ");
        System.out.println("(Real location is state " + randomPath[step] + ")");
        printMessage(forwardMessage, mazeDimension);

      }
    }

    /**
     * Implements the Viterbi Algorithm to find the optimal path
     */
    public void solveBestPath() {
      int maxState, nextStateBack; 
      double maxVal;
      double tempVal;
      double[][] sensorModel = null;

      LinkedList<Integer> optimalPath = new LinkedList<Integer>();

      int currState, lastState;



      // Setup the path lengths
      Arrays.fill(viterbiBackchains[0], -1);

      // Loop through each step in the path
      for (int step = 1; step < randomPath.length; step++) {
        double[] viterbiMessage = new double[stateCount];

        // Loop through each possible current state to fill out the viterbi message
        for (currState = 0; currState < stateCount; currState++) {
          maxState = -1;
          maxVal = 0.0;

          // Loop through each possible last state
          for (lastState = 0; lastState < stateCount; lastState++) {
            // Get the probability of an optimal path to the last state 
            // times the probability of transitioning from the last state to the current
            tempVal = viterbiMessages[step - 1][lastState] * transitionModel[lastState][currState];

            if (tempVal > maxVal) {
              maxVal = tempVal;
              maxState = lastState;
            }
          }
          
          // Update this index in the viterbiMessage
          viterbiMessage[currState] = maxVal;
          viterbiBackchains[step][currState] = maxState;
        }

        // Having the probabilities correctly in the viterbiMessage,
        // Find and apply the sensor model, normalize, and save in the global variable

        // Select the appropriate sensor model based on the evidence
        switch (evidence[step]) {
          case 'r':   sensorModel = rSensorModel;
                      break;
          case 'b':   sensorModel = bSensorModel;
                      break;
          case 'g':   sensorModel = gSensorModel;
                      break;
          case 'y':   sensorModel = ySensorModel;
                      break;
        }

        viterbiMessages[step] = normalize(
                                  matrixMultiply(sensorModel,
                                    viterbiMessage));
      }

      // Loop through the last viterbiMessage to find the starting point for backchaining
      maxState = -1;
      maxVal = -1.0;
      for (int i = 0; i < stateCount; i++) {
        tempVal = viterbiMessages[randomPath.length - 1][i];
        if (tempVal > maxVal) {
          maxVal = tempVal;
          maxState = i;
        }
      }

      currState = maxState;
      optimalPath.addFirst(currState);
      for (int step = randomPath.length - 1; step > 0; step--) {
        nextStateBack = viterbiBackchains[step][currState];
        optimalPath.addFirst(nextStateBack);
        currState = nextStateBack;
      }

      // Print results
      System.out.println("Found optimal path with probability " + maxVal + ":");
      System.out.println(optimalPath);
    }

    /**
     * Implements Forward-Backward Algorithm to produce probability distributions
     */
    public void solveSmoothing() {
      // We have to store the filtering approximations
      double[][] fVals = new double[randomPath.length][stateCount];


      double[][] sensorModel = null;
      double[] forwardMessage;
      double[] backwardMessage;
      double[] distribution;
      int[] XY;

      // Initialize with the initial probability distribution
      forwardMessage = forwardInitialCondition;
      backwardMessage = backwardInitialCondition;

      // Forward part of forward-backwards
      for (int step = 0; step < randomPath.length; step++) {
        // Select the appropriate sensor model based on the evidence
        switch (evidence[step]) {
          case 'r':   sensorModel = rSensorModel;
                      break;
          case 'b':   sensorModel = bSensorModel;
                      break;
          case 'g':   sensorModel = gSensorModel;
                      break;
          case 'y':   sensorModel = ySensorModel;
                      break;
        }
        
        // f_1:t+1 = alpha * SensorModel_t+1 * TransitionTranspose * f_1:t
        forwardMessage = normalize(
                          matrixMultiply(sensorModel, 
                            matrixMultiply(transitionTranspose, forwardMessage)));

        fVals[step] = forwardMessage; // Store this approximation



      }

      // Backwards part of forward-backwards
      for (int step = randomPath.length - 1; step >= 0; step--) {
        distribution = normalize(messageMultiply(fVals[step], backwardMessage));

        // Print the results at this step
        System.out.println("\nSmoothed Distribuition at step " + (step+1) + ": ");
        System.out.println("(Real location is state " + randomPath[step] + ")");
        printMessage(distribution, mazeDimension);

        // Select the appropriate sensor model based on the evidence
        switch (evidence[step]) {
          case 'r':   sensorModel = rSensorModel;
                      break;
          case 'b':   sensorModel = bSensorModel;
                      break;
          case 'g':   sensorModel = gSensorModel;
                      break;
          case 'y':   sensorModel = ySensorModel;
                      break;
        }

        backwardMessage = matrixMultiply(transitionModel, 
                            matrixMultiply(sensorModel,backwardMessage));
      }
    }

    public static double[] messageMultiply(double[]m1, double[]m2) {
      double[] result = new double[m1.length];

      for (int i = 0; i < m1.length; i++)
        result[i] = m1[i] * m2[i];

      return result;
    }
    
    public static double[] matrixMultiply(double[][]matrix, double[]message) {
      double[] result = new double[message.length];
      double rowSum = 0;
    
      // Loop through each row in the matrix
      for (int y = 0; y < message.length; y++) {
        rowSum = 0;
        for (int x = 0; x < message.length; x++) {
          rowSum += (matrix[y][x] * message[x]);
        }
        result[y] = rowSum;
      }
      return result;
    }

    public static double[] normalize(double[] message) {
      double[] normalized = new double[message.length];
      double sum = 0;

      for (double val : message)
        sum += val;

      for (int i = 0; i < message.length; i++)
        normalized[i] = message[i] / sum;

      return normalized;
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
    prob.solveFiltering();

    prob.solveSmoothing();
    prob.solveBestPath();
  }

}

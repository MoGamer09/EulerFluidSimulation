import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Simulates a fluid inside a grid.
 * Use SimulationStep(); to calculate next state.
 */
public class Simulation {
    private double oldTime;
    public double fps;
    private final Queue<Double> fpsBuffer = new LinkedList<Double>();
    private final int fpsAverage = 10;

    public static final double OUTFLOW_THRESHOLD = 0;
    public static final double SINGLE_FLOW_THRESHOLD = 0;
    private static final int VERTICAL_FLOW = 1;
    private static final int HORIZONTAL_FLOW = 0;
    private static final int DEFAULT_CELL = 0;
    private static final int INFLOW_CELL = 1;
    private static final int OBSTACLE_CELL = 2;

    public SimulationSceneInfo sceneInfo;

    public int rows;
    public int cols;

    public double[][][] flows; //Stores "outflowVectors" per cell (right, down)
    public double[][][] oldFlows; //for witching efficiently
    boolean useTmpFlows = false;
    public double[][][] cellColors;
    public double[][] divergence; //Divergence per cell
    public double[][][] summedFlows;
    public double[][][] summedOutflows;
    public int[][] cellType;
    public double[][] pressure;

    public double inflowDirX = 1;
    public double inflowDirY = -1;

    public double mouseX = 0;
    public double mouseY = 0;

    public int[][] neighbours;

    ExecutorService executorService = Executors.newFixedThreadPool(2);
    
    public FluidParticle[] particles;

    /**
     * Initializes a Simulation with the given scene info containing the basic settings for a simulation.
     *
     * @param sceneInfo Basic information about this simulation's scene.
     */
    public Simulation(SimulationSceneInfo sceneInfo) {
        changeScene(sceneInfo);
        
        flows = new double[cols][rows][2];
        oldFlows = new double[cols][rows][2];
        cellColors = new double[cols][rows][3];
        divergence = new double[cols][rows];
        summedFlows = new double[cols][rows][2];
        summedOutflows = new double[cols][rows][2];
        cellType = new int[cols][rows];
        pressure = new double[cols][rows];
        neighbours = new int[cols][rows];
        particles = new FluidParticle[(rows*cols)];

        //initialize flows
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                if (y < rows - 1)
                    flows[x][y][HORIZONTAL_FLOW] = 0;
                if (x < cols - 1)
                    flows[x][y][VERTICAL_FLOW] = 0;
                cellColors[x][y][0] = sceneInfo.preWarmColors[0];
                cellColors[x][y][1] = sceneInfo.preWarmColors[1];
                cellColors[x][y][2] = sceneInfo.preWarmColors[2];
            }
        }


        for(int i=0; i<particles.length; i++) {
            Random rand = new Random();
            particles[i] = new FluidParticle(rand.nextInt(sceneInfo.cols),rand.nextInt(sceneInfo.rows),rand.nextInt(1000));
        }

        updateNeighbourCounts();
    }

    public FluidParticle randParticle() {
        Random rand = new Random();
        return new FluidParticle(rand.nextInt(sceneInfo.cols),rand.nextInt(sceneInfo.rows),1000);
    }

    /**
     * Cleanup before garbage collection.
     */
    public void clean()
    {
        executorService.shutdown();    
    }

    /**
     * Changes scenes.
     * Does not work correctly when switching simulation sizes!
     *
     * @param sceneInfo The new scene to simulate
     */
    public void changeScene(SimulationSceneInfo sceneInfo) {
        cols = sceneInfo.cols;
        rows = sceneInfo.rows;
        this.sceneInfo = sceneInfo;
    }

    public boolean isCellFull(int x, int y) {
        return false;
        //return cellColors[x][y][0] > 220;
    }

    /**
     * Updates the divergence list by adding up flows to left and bottom and then subtracting flows coming from left and top
     */
    private void calculateDivergence() {
        try {
            Future<?> future1 = executorService.submit(() -> {
                for (int y = 0; y < rows; y++) {
                    for (int x = y % 2; x < cols; x+=2) {
                        calculateDivergenceSingle(x, y);
                    }
                }
            });
            Future<?> future2 = executorService.submit(() -> {
                for (int y = 0; y < rows; y++) {
                    for (int x = (y + 1) % 2; x < cols; x+=2) {
                        calculateDivergenceSingle(x, y);
                    }
                }
            });

            future1.get();
            future2.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Calculates the divergence of a specific cell
     * @param x The cells horizontal position.
     * @param y The cells vertical position.
     */
    private void calculateDivergenceSingle(int x, int y) {
        if (cellType[x][y] == INFLOW_CELL) {
            divergence[x][y] = 0;
            return;
        }

        double divergenceX = -flows[x][y][HORIZONTAL_FLOW];
        double divergenceY = -flows[x][y][VERTICAL_FLOW];
        if (x > 0) {
            divergenceX += flows[x - 1][y][HORIZONTAL_FLOW];
        }
        if (y > 0) {
            divergenceY += flows[x][y - 1][VERTICAL_FLOW];
        }

        divergence[x][y] = divergenceX + divergenceY;
    }

    /**
     * Updates a single cell by the rules of the Gauss-Seidel-Projection by simply distributing
     * the divergence across cell outflows to neighbouring cells.
     *
     * @param x        The x coordinate of the cell to update.
     * @param y        The y coordinate of the cell to update.
     * @param oldFlows A snapshot of the flows before the start of the update to base the calculations off of.
     */
    private void projectGaussSeidelSingle(int x, int y, double[][][] oldFlows) {
        var deltaVel = getDeltaVel(x, y);
        if (cellType[x][y] != DEFAULT_CELL) return;

        if (x < cols - 1) {
            var neighbourDelta = getDeltaVel(x + 1, y);
            double delta = 0;
            //if (!isCellFull(x, y) && deltaVel < 0 || !isCellFull(x + 1, y) && deltaVel > 0)
            delta += deltaVel;
            //if (!isCellFull(x + 1, y) && delta > 0 || !isCellFull(x, y) && deltaVel < 0)
            delta -= neighbourDelta;

            flows[x][y][HORIZONTAL_FLOW] = Math.min(oldFlows[x][y][HORIZONTAL_FLOW] + delta, 2);
        }
        if (y < rows - 1) {
            var neighbourDelta = getDeltaVel(x, y + 1);
            double delta = 0;
            if (!isCellFull(x, y) && deltaVel < 0 || !isCellFull(x, y + 1) && deltaVel > 0)
                delta += deltaVel;
            if (!isCellFull(x, y + 1) && deltaVel > 0 || !isCellFull(x, y) && deltaVel < 0)
                delta -= neighbourDelta;

            flows[x][y][VERTICAL_FLOW] = Math.min(oldFlows[x][y][VERTICAL_FLOW] + delta, 2);
        }

        //flows[x][y][HORIZONTAL_FLOW] = Math.min(flows[x][y][HORIZONTAL_FLOW], 2);
        //flows[x][y][VERTICAL_FLOW] = Math.min(flows[x][y][VERTICAL_FLOW], 2);
    }

    /**
     * Calculates the velocity each flow of a cell has to be changed by to push back the divergence.
     *
     * @param x The x coordinate of the cell.
     * @param y The x coordinate of the cell.
     * @return The velocity as a double that has to be distributed onto the cells four flow vectors.
     */
    private double getDeltaVel(int x, int y) {
        int numberOfDirectNeighbours = 4;
        if (x == 0 || isCellFull(x - 1, y))
            numberOfDirectNeighbours--;
        if (x >= cols - 1 || isCellFull(x + 1, y))
            numberOfDirectNeighbours--;
        if (y == 0 || isCellFull(x, y - 1))
            numberOfDirectNeighbours--;
        if (y >= rows - 1 || isCellFull(x, y + 1))
            numberOfDirectNeighbours--;
        return divergence[x][y] / numberOfDirectNeighbours;
    }

    /**
     * Updates the flows array to counteract the divergence by calling ProjectGaussSeidelSingle on each cell.
     */
    public void projectGaussSeidel() {
        double[][][] tmp = oldFlows;
        oldFlows = flows;
        flows = tmp;
        
        try {
            Future<?> future1 = executorService.submit(() -> {
                for (int y = 0; y < rows; y++) {
                    for (int x = y % 2; x < cols; x += 2) {
                        flows[x][y][0] = 0;
                        flows[x][y][1] = 0;
                        projectGaussSeidelSingle(x, y, oldFlows);
                    }
                }
            });
            Future<?> future2 = executorService.submit(() -> {
                for (int y = 0; y < rows; y++) {
                    for (int x = (y + 1) % 2; x < cols; x += 2) {
                        flows[x][y][0] = 0;
                        flows[x][y][1] = 0;
                        projectGaussSeidelSingle(x, y, oldFlows);
                    }
                }
            });

            future1.get();
            future2.get();
        } catch (Exception e) {
            e.printStackTrace();
        } 
        
        useTmpFlows = !useTmpFlows;
    }

    /*
    Unnötigster code jemals geschrieben

    private double[][][] Clone3DArray(double[][][] arr) {
        double[][][] res = new double[arr.length][arr[0].length][arr[0][HORIZONTAL_FLOW].length];
        for (int y = 0; y < arr.length; y++) {
            for (int x = 0; x < arr[0].length; x++) {
                System.arraycopy(arr[y][x], 0, res[y][x], 0, arr[0][HORIZONTAL_FLOW].length);
            }
        }
        return res;
    }

    private double[][] Clone2DArray(double[][] arr) {
        double[][] res = new double[arr.length][arr[0].length];
        for (int y = 0; y < arr.length; y++) {
            System.arraycopy(arr[y], 0, res[y], 0, arr[0].length);
        }
        return res;
    }
    */

    /**
     * Updates the pressure array. Is not needed for the simulation.
     */
    public void calculatePressure() {
        double[][] oldPressure = pressure;
        pressure = new double[cols][rows];
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                double p = 0;
                int neighbours = 0;
                if (isCellInGrid(x + 1, y)) {
                    p += oldPressure[x + 1][y];
                    neighbours++;
                }
                if (isCellInGrid(x - 1, y)) {
                    p += oldPressure[x - 1][y];
                    neighbours++;
                }
                if (isCellInGrid(x, y + 1)) {
                    p += oldPressure[x][y + 1];
                    neighbours++;
                }
                if (isCellInGrid(x, y - 1)) {
                    p += oldPressure[x][y - 1];
                    neighbours++;
                }
                pressure[x][y] = p / neighbours - divergence[x][y] / neighbours;
            }
        }
    }

    /**
     * Updates the velocities by averaging the velocities of the cells that should point to this cell.
     * This way we pull the velocities instead if pushing them witch helps to prevent errors by overwriting data
     * because after each step a cell is completely finished with advection.
     */
    public void advectVelocities() {
        calculateSummedFlows();
        calculateSummedOutflows();
        flows = new double[cols][rows][2];
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {

                AdvectionInterpolationInfo adInfo = getAdvectionDeltas(x, y, false);

                double velTLX = getCelVel(adInfo.CellTLX, adInfo.CellTLY, HORIZONTAL_FLOW);
                double velTLY = getCelVel(adInfo.CellTLX, adInfo.CellTLY, VERTICAL_FLOW);
                double velTRX = getCelVel(adInfo.CellTRX, adInfo.CellTRY, HORIZONTAL_FLOW);
                double velTRY = getCelVel(adInfo.CellTRX, adInfo.CellTRY, VERTICAL_FLOW);
                double velBLX = getCelVel(adInfo.CellBLX, adInfo.CellBLY, HORIZONTAL_FLOW);
                double velBLY = getCelVel(adInfo.CellBLX, adInfo.CellBLY, VERTICAL_FLOW);
                double velBRX = getCelVel(adInfo.CellBRX, adInfo.CellBRY, HORIZONTAL_FLOW);
                double velBRY = getCelVel(adInfo.CellBRX, adInfo.CellBRY, VERTICAL_FLOW);

                double newVelX = -twoDimensionLerp(velTLX, velTRX, velBLX, velBRX, adInfo.tx, adInfo.ty);
                double newVelY = -twoDimensionLerp(velTLY, velTRY, velBLY, velBRY, adInfo.tx, adInfo.ty);

                if (x > 0 && x < cols - 1) {
                    flows[x - 1][y][HORIZONTAL_FLOW] += newVelX / 2.0;
                    flows[x][y][HORIZONTAL_FLOW] += newVelX / 2.0;
                } else if (x > 0) {
                    flows[x - 1][y][HORIZONTAL_FLOW] += newVelX;
                } else if (x < cols - 1) {
                    flows[x][y][HORIZONTAL_FLOW] += newVelX;
                }
                if (y > 0 && y < rows - 1) {
                    flows[x][y - 1][VERTICAL_FLOW] += newVelY / 2.0;
                    flows[x][y][VERTICAL_FLOW] += newVelY / 2.0;
                } else if (y > 0) {
                    flows[x][y - 1][VERTICAL_FLOW] += newVelY;
                } else if (y < rows - 1) {
                    flows[x][y][VERTICAL_FLOW] += newVelY;
                }

                //System.out.println(newVelX);
            }
        }
    }

    /**
     * Returns the velocity at a point in simulation space by interpolating between the four nearest cells.
     *
     * @param x The horizontal part of the coordinate.
     * @param y The vertical part of the coordinate.
     * @return Returns a velocity vector represented as a double[] with res[0] containing the horizontal part and res[1] containing the vertical part.
     */
    public double[] getVelAtPoint(double x, double y) {
        int CellTLX = (int) Math.floor(x);
        int CellTLY = (int) Math.floor(y);
        int CellTRX = (int) Math.ceil(x);
        int CellTRY = (int) Math.floor(y);
        int CellBLX = (int) Math.floor(x);
        int CellBLY = (int) Math.ceil(y);
        int CellBRX = (int) Math.ceil(x);
        int CellBRY = (int) Math.ceil(y);

        double velTLX = getCelVel(CellTLX, CellTLY, HORIZONTAL_FLOW);
        double velTLY = getCelVel(CellTLX, CellTLY, VERTICAL_FLOW);
        double velTRX = getCelVel(CellTRX, CellTRY, HORIZONTAL_FLOW);
        double velTRY = getCelVel(CellTRX, CellTRY, VERTICAL_FLOW);
        double velBLX = getCelVel(CellBLX, CellBLY, HORIZONTAL_FLOW);
        double velBLY = getCelVel(CellBLX, CellBLY, VERTICAL_FLOW);
        double velBRX = getCelVel(CellBRX, CellBRY, HORIZONTAL_FLOW);
        double velBRY = getCelVel(CellBRX, CellBRY, VERTICAL_FLOW);

        double newVelX = -twoDimensionLerp(velTLX, velTRX, velBLX, velBRX, x - Math.floor(x), y - Math.floor(y));
        double newVelY = -twoDimensionLerp(velTLY, velTRY, velBLY, velBRY, x - Math.floor(x), y - Math.floor(y));

        return new double[]{newVelX, newVelY};
    }

    /**
     * Updates the cell colors by averaging the colors of the cells that should point to the given cell by the rules of advection.
     */
    public void advectColor() {
        calculateSummedFlows();
        calculateSummedOutflows();
        double[][][] oldColors = cellColors;
        cellColors = new double[cols][rows][3];
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {

                AdvectionInterpolationInfo adInfo = getAdvectionDeltas(x, y, false);


                double[] colorTL = isCellInGrid(adInfo.CellTLX, adInfo.CellTLY) ? oldColors[adInfo.CellTLX][adInfo.CellTLY] : new double[3];
                double[] colorTR = isCellInGrid(adInfo.CellTRX, adInfo.CellTRY) ? oldColors[adInfo.CellTRX][adInfo.CellTRY] : new double[3];
                double[] colorBL = isCellInGrid(adInfo.CellBLX, adInfo.CellBLY) ? oldColors[adInfo.CellBLX][adInfo.CellBLY] : new double[3];
                double[] colorBR = isCellInGrid(adInfo.CellBRX, adInfo.CellBRY) ? oldColors[adInfo.CellBRX][adInfo.CellBRY] : new double[3];


                double cellColorR = twoDimensionLerp(colorTL[0], colorTR[0], colorBL[0], colorBR[0], adInfo.tx, adInfo.ty);
                double cellColorG = twoDimensionLerp(colorTL[1], colorTR[1], colorBL[1], colorBR[1], adInfo.tx, adInfo.ty);
                double cellColorB = twoDimensionLerp(colorTL[2], colorTR[2], colorBL[2], colorBR[2], adInfo.tx, adInfo.ty);

                cellColors[x][y] = new double[]{cellColorR, cellColorG, cellColorB};
            }
        }
    }

    /**
     * Calculates the information needed for basic advection by the flow vectors at a specific cell.
     *
     * @param x       The cells horizontal position.
     * @param y       The cells vertical position.
     * @param forward determines weather the advection process should look forward (true) or trace backwards (false).
     * @return an AdvectionInterpoationInfo object containing the advection neighbour cells and
     * the interpolation factors between them.
     */
    public AdvectionInterpolationInfo getAdvectionDeltas(int x, int y, boolean forward) {
        double dirX = summedFlows[x][y][HORIZONTAL_FLOW];
        double dirY = summedFlows[x][y][VERTICAL_FLOW];
        if (forward) {
            dirX *= -1;
            dirY *= -1;
        }
        int CellTLX = x + (int) Math.floor(dirX);
        int CellTLY = y + (int) Math.floor(dirY);
        int CellTRX = x + (int) Math.ceil(dirX);
        int CellTRY = y + (int) Math.floor(dirY);
        int CellBLX = x + (int) Math.floor(dirX);
        int CellBLY = y + (int) Math.ceil(dirY);
        int CellBRX = x + (int) Math.ceil(dirX);
        int CellBRY = y + (int) Math.ceil(dirY);

        double tX = (x + dirX) - CellTLX;
        double tY = Math.abs((y + dirY) - CellBLY);

        return new AdvectionInterpolationInfo(CellTLX, CellTLY, CellTRX, CellTRY, CellBLX, CellBLY, CellBRX, CellBRY, tX, tY);
    }


    /**
     * Returns the velocity of a cell if the cell exists and zero if the requested position is not a valid cell.
     *
     * @param x       The cells horizontal position.
     * @param y       The cells vertical position.
     * @param flowDir The direction to return. Either 0 for horizontal or 1 for vertical.
     * @return a double representing the summed flow in the requested direction.
     */
    private double getCelVel(int x, int y, int flowDir) {
        return isCellInGrid(x, y) ? summedOutflows[x][y][flowDir] : 0;
    }

    /**
     * Interpolates between 4 values representing 4 corners at (0, 1), (1, 1), (0, 0), and (1, 0).
     *
     * @param TL The value in the top left corner (0,1).
     * @param TR The value in the top right corner (1,1).
     * @param BL The value in the bottom left corner (0,0).
     * @param BR The value in the bottom right corner (1,1).
     * @param tX The horizontal interpolation factor.
     * @param tY The vertical interpolation factor.
     * @return a double interpolated between the four inputs.
     */
    public double twoDimensionLerp(double TL, double TR, double BL, double BR, double tX, double tY) {
        double topLerp = lerp(TL, TR, tX);
        double bottomLerp = lerp(BL, BR, tX);
        return lerp(bottomLerp, topLerp, tY);
    }

    /**
     * Updates the cell colors and velocities to slowly average out between their neighbours.
     */
    private void diffuse() {
        double velocityDiffuseCoefficient = 0.8;
        double colorDiffuseCoefficient = 0.8;
        double[][][] tmp = oldFlows;
        oldFlows = flows;
        double[][][] oldCellColors = cellColors;
        flows = tmp;
        cellColors = new double[cols][rows][3];
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                flows[x][y][HORIZONTAL_FLOW] = oldFlows[x][y][HORIZONTAL_FLOW] * velocityDiffuseCoefficient;
                flows[x][y][VERTICAL_FLOW] = oldFlows[x][y][VERTICAL_FLOW] * velocityDiffuseCoefficient;
                cellColors[x][y][0] = oldCellColors[x][y][0] * colorDiffuseCoefficient;
                cellColors[x][y][1] = oldCellColors[x][y][1] * colorDiffuseCoefficient;
                cellColors[x][y][2] = oldCellColors[x][y][2] * colorDiffuseCoefficient;
                for (int v = -1; v < 2; v++) {
                    for (int w = -1; w < 2; w++) {
                        int yy = y + v;
                        int xx = x + w;
                        if (v == 0 && w == 0 || !isCellInGrid(xx, yy) || cellType[xx][yy] != DEFAULT_CELL)
                            continue;

                        int neighborCount = neighbours[x][y];
                        flows[x][y][HORIZONTAL_FLOW] +=
                                oldFlows[xx][yy][HORIZONTAL_FLOW] * (1 - velocityDiffuseCoefficient) / neighborCount;
                        flows[x][y][VERTICAL_FLOW] += oldFlows[xx][yy][VERTICAL_FLOW] * (1 - velocityDiffuseCoefficient) / neighborCount;
                        cellColors[x][y][0] += oldCellColors[xx][yy][0] * (1 - colorDiffuseCoefficient) / neighborCount;
                        cellColors[x][y][1] += oldCellColors[xx][yy][1] * (1 - colorDiffuseCoefficient) / neighborCount;
                        cellColors[x][y][2] += oldCellColors[xx][yy][2] * (1 - colorDiffuseCoefficient) / neighborCount;
                    }
                }
            }
        }
    }

    /**
     * Calculates the count of neighbours (including diagonal neighbours!) a cell has respecting bounds.
     *
     * @param x The cells horizontal position.
     * @param y The cells vertical position.
     * @return an integer between 0 and 8.
     */
    public int getNeighbourCount(int x, int y) {
        int res = 9;
        for (int v = -1; v < 2; v++) {
            for (int w = -1; w < 2; w++) {
                int yy = y + v;
                int xx = x + w;
                if (v == 0 && w == 0 || !isCellInGrid(xx, yy) || cellType[xx][yy] != DEFAULT_CELL)
                    res--;
            }
        }
        return res;
    }

    private void updateNeighbourCounts() {
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                neighbours[x][y] = getNeighbourCount(x, y);
            }
        }
    }

    /**
     * Interpolates linearly between the values a and b by the factor t.
     *
     * @param a The value at (0)
     * @param b The value at (1)
     * @param t The interpolation factor between 0 and 1
     * @return the interpolated value between a and b
     */
    public double lerp(double a, double b, double t) {
        return t * b + (1 - t) * a;
    }

    /**
     * Updates the array of summed flows by adding horizontal and vertical flows from and to this cell.
     */
    private void calculateSummedFlows() {
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                double dirX;
                double dirY;
                dirX = flows[x][y][HORIZONTAL_FLOW];
                dirY = flows[x][y][VERTICAL_FLOW];
                if (x > 0)
                    dirX += flows[x - 1][y][HORIZONTAL_FLOW];
                if (y > 0)
                    dirY += flows[x][y - 1][VERTICAL_FLOW];
                dirX /= -2;
                dirY /= -2;
                summedFlows[x][y][HORIZONTAL_FLOW] = dirX;
                summedFlows[x][y][VERTICAL_FLOW] = dirY;
            }
        }
    }

    /**
     * Updates the summedOutFlows array by just adding the flows that leaves the cells. (Might be negative...)
     */
    public void calculateSummedOutflows() {
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                double dirX = 0;
                double dirY = 0;
                if (flows[x][y][HORIZONTAL_FLOW] > 0)
                    dirX += flows[x][y][HORIZONTAL_FLOW];
                if (flows[x][y][VERTICAL_FLOW] > 0)
                    dirY += flows[x][y][VERTICAL_FLOW];
                if (x > 0 && flows[x - 1][y][HORIZONTAL_FLOW] < 0)
                    dirX += flows[x - 1][y][HORIZONTAL_FLOW];
                if (y > 0 && flows[x][y - 1][VERTICAL_FLOW] < 0)
                    dirY += flows[x][y - 1][VERTICAL_FLOW];
                summedOutflows[x][y][HORIZONTAL_FLOW] = -dirX;
                summedOutflows[x][y][VERTICAL_FLOW] = -dirY;
            }
        }
    }

    /**
     * Checks if a cell is in the bounds of the simulation
     *
     * @param x The cells horizontal position.
     * @param y The cells vertical position.
     * @return true if the cell is in the grid and false otherwise.
     */
    boolean isCellInGrid(int x, int y) {
        return x >= 0 && x < cols && y >= 0 && y < rows;
    }

    /**
     * ! OBSOLETE CODE that moved color along the flows wo the cell neighbours instead of advecting them by their velocities.
     */
    private void CalculateColorMovement() {
        double[][][] oldCellColors = cellColors;
        cellColors = new double[cols][rows][3];
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                double outflow = 0;
                double oldColor = oldCellColors[x][y][0];
                //Sum up outflow
                if (y > 0) {
                    if (flows[x][y - 1][VERTICAL_FLOW] < 0) {
                        outflow += Math.abs(flows[x][y - 1][VERTICAL_FLOW]);
                    }
                }
                if (x > 0) {
                    if (flows[x - 1][y][HORIZONTAL_FLOW] < 0) {
                        outflow += Math.abs(flows[x - 1][y][HORIZONTAL_FLOW]);
                    }
                }
                if (x < cols - 1) {
                    if (flows[x][y][HORIZONTAL_FLOW] > 0) {
                        outflow += flows[x][y][HORIZONTAL_FLOW];
                    }
                }
                if (y < rows - 1) {
                    if (flows[x][y][VERTICAL_FLOW] > 0) {
                        outflow += flows[x][y][VERTICAL_FLOW];
                    }
                }

                //System.out.println(outflow);
                if (outflow <= OUTFLOW_THRESHOLD) {
                    increaseCell(x, y, oldCellColors[x][y][0]);
                    continue;
                }
                //Move Liquid
                if (y > 0 && flows[x][y - 1][VERTICAL_FLOW] < -SINGLE_FLOW_THRESHOLD) {
                    double movedColor = oldColor * (Math.abs(flows[x][y - 1][VERTICAL_FLOW]) / outflow);
                    increaseCell(x, y - 1, movedColor);
                }
                if (x > 0 && flows[x - 1][y][HORIZONTAL_FLOW] < -SINGLE_FLOW_THRESHOLD) {
                    double movedColor = oldColor * (Math.abs(flows[x - 1][y][HORIZONTAL_FLOW]) / outflow);
                    increaseCell(x - 1, y, movedColor);
                }
                if (x < cols - 1 && flows[x][y][HORIZONTAL_FLOW] > SINGLE_FLOW_THRESHOLD) {
                    double movedColor = oldColor * (Math.abs(flows[x][y][HORIZONTAL_FLOW]) / outflow);
                    increaseCell(x + 1, y, movedColor);
                }
                if (y < rows - 1 && flows[x][y][VERTICAL_FLOW] > SINGLE_FLOW_THRESHOLD) {
                    double movedColor = oldColor * (Math.abs(flows[x][y][VERTICAL_FLOW]) / outflow);
                    increaseCell(x, y + 1, movedColor);
                }

                //Fade
                //increaseCell(x, y, -0.4);
            }
        }
    }

    /**
     * ! OBSOLETE CODE that adds color to a specific cell
     *
     * @param x          The cells horizontal position.
     * @param y          The cells horizontal position.
     * @param movedColor the amount of color (red chanel only)
     */
    private void increaseCell(int x, int y, double movedColor) {
        if (cellColors[x][y][0] + movedColor > 255)
            System.out.println("Mass disappeared!  " + (cellColors[x][y][0] + movedColor - 255));
        cellColors[x][y][0] = cellColors[x][y][0] + movedColor;
    }


    /**
     * Adds a specific inflow.
     *
     * @param x       The inflows horizontal position.
     * @param y       The inflows vertical position.
     * @param amountR The inflow colors red channel.
     * @param amountG The inflow colors green channel.
     * @param amountB The inflow colors blue channel.
     * @param dirX    The inflows horizontal velocity.
     * @param dirY    The inflows vertical velocity.
     * @param noise   Decides if the inflow color should be modified slightly for better flow visibility.
     */
    public void addInflow(int x, int y, double amountR, double amountG, double amountB, double dirX, double dirY, boolean noise) {

        if (amountR + amountG + amountB > 0.1f)
            cellColors[x][y] = noise ? randomizeColor(new double[]{amountR, amountG, amountB}) : new double[]{amountR, amountG, amountB};

        if (dirX > 0)
            flows[x][y][HORIZONTAL_FLOW] = dirX / 2.0;
        else
            flows[x - 1][y][HORIZONTAL_FLOW] = dirX / 2.0;
        if (dirY > 0)
            flows[x][y][VERTICAL_FLOW] = dirY / 2.0;
        else
            flows[x][y - 1][VERTICAL_FLOW] = dirY / 2.0;
        //divergence[x][y] = 0;
        //cellType[x][y] = INFLOW_CELL;
    }

    /**
     * Randomizes a color double[3] array.
     *
     * @param color The color to modify
     * @return a double[3] array containing the modified color.
     */
    public double[] randomizeColor(double[] color) {
        Random random = new Random();
        double noise = random.nextDouble() / 2 + 0.5;
        return new double[]{noise * color[0], noise * color[1], noise * color[2]};
    }

    /**
     * Adds force between two neighbouring cells
     *
     * @param cell1X The first cells horizontal position.
     * @param cell1Y The first cells vertical position.
     * @param cell2X The second cells horizontal position.
     * @param cell2Y The second cells vertical position.
     * @param dirX   The horizontal velocity.
     * @param dirY   The vertical velocity.
     * @param random Decides if the velocity should be random.
     */
    public void addNewForceBetweenCells(int cell1X, int cell1Y, int cell2X, int cell2Y, int dirX, int dirY, boolean random) {
        Random rng = new Random();
        //Bounds Check
        if (cell1X > flows.length - 1 || cell1X < 0 || cell1Y > flows[0].length - 1 || cell1Y < 0 ||
                cell2X > flows.length - 1 || cell2X < 0 || cell2Y > flows[0].length - 1 || cell2Y < 0
        ) {
            return;
        }
        if (cell1X < cell2X) {
            flows[cell1X][cell1Y][HORIZONTAL_FLOW] = random ? rng.nextDouble() * 10 : 10;
        }
        if (cell2X < cell1X) {
            flows[cell2X][cell2Y][HORIZONTAL_FLOW] = random ? -rng.nextDouble() * 10 : -10;
        }
        if (cell1Y < cell2Y) {
            flows[cell1X][cell1Y][VERTICAL_FLOW] = random ? rng.nextDouble() * 10 : 10;
        }
        if (cell2Y < cell1Y) {
            flows[cell2X][cell2Y][VERTICAL_FLOW] = random ? -rng.nextDouble() * 10 : -10;
        }
    }

    /**
     * Updates the particles in the FluidParticle[] array, based on their velocities and lifetime.
     */
    public void moveParticles()
    {
        int mult = 2;
        for (int i = 0; i < particles.length; i++){
            FluidParticle p = particles[i];
            if (p.lifetime <= 0)
                particles[i] = randParticle();

            p.move(getVelAtPoint(p.x, p.y)[0],getVelAtPoint(p.x, p.y)[1]);
            //p.move(-pressureDerivative(p.x,p.y)[0]*0.8,-pressureDerivative(p.x,p.y)[1]*0.8);
            p.lifetime-=20;


        }
    }

    public double[] pressureDerivativeAtCell(int x, int y) {
        if(!isCellInGrid(x,y))
            return new double[]{0,0};
        double velX = 0;
        double velY = 0;
        if(isCellInGrid(x+1,y)){
            velX += pressure[x][y]-pressure[x+1][y];
        }
        if(isCellInGrid(x,y+1)) {
            velY += pressure[x][y]-pressure[x][y+1];
        }
        if(isCellInGrid(x,y-1)) {
            velY -= pressure[x][y]-pressure[x][y-1];
        }
        if(isCellInGrid(x-1,y)) {
            velX -= pressure[x][y]-pressure[x-1][y];
        }
        return new double[]{velX,velY};
    }

    public double[] pressureDerivative(double x, double y){
        double[] tl = pressureDerivativeAtCell((int) Math.floor(x), (int) Math.ceil(y));
        double[] tr = pressureDerivativeAtCell((int) Math.ceil(x), (int) Math.ceil(y));
        double[] bl = pressureDerivativeAtCell((int) Math.floor(x), (int) Math.floor(y));
        double[] br = pressureDerivativeAtCell((int) Math.ceil(x), (int) Math.floor(y));

        double velX = twoDimensionLerp(tl[0],tr[0],bl[0],br[0],(x-(int)x),(y-(int)y));
        double velY = twoDimensionLerp(tl[1],tr[1],bl[1],br[1],(x-(int)x),(y-(int)y));

        return new double[]{velX,velY};
    }

    /**
     * Does a whole simulation step including diffusion, divergence suppression, velocity and color advection and inflow addition / operation.
     * The fps are calculated based on the frequency this function is called.
     */
    public void SimulationStep() {
        calculateDivergence();

        diffuse();

        //calculatePressure();

        calculateDivergence();

        //Force steps
        for (int i = 0; i < 100; i++) {
            calculateDivergence();
            projectGaussSeidel();
        }
        calculateDivergence();

        advectVelocities();

        //Old way of color movement
        //CalculateColorMovement();

        for (int i = 0; i < 1; i++) {
            advectColor();
        }

        calculateDivergence();

        moveParticles();

        for (InflowInfo inflow : sceneInfo.inflowInfos) {
            inflow.tick(mouseX, mouseY);
            addInflow(inflow.x, inflow.y, inflow.color[0], inflow.color[1], inflow.color[2], inflow.dirX, inflow.dirY, inflow.noise);
        }

        double delta = (System.nanoTime() - oldTime);
        double currentFps = 1000000000.0 / delta;
        oldTime = System.nanoTime();
        fpsBuffer.add(currentFps);
        if (fpsBuffer.size() > fpsAverage) {
            fpsBuffer.poll();
            fps = 0;
            for (double f : fpsBuffer) {
                fps += f;
            }
            fps /= fpsAverage;
        }
    }
}



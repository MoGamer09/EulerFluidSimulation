import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Grid Panel visualizes the Simulation and its debug options.
 * It's also responsible for tracking User Input.
 */
public class GridPanel extends JPanel {

    public static final int MARGIN = 20;

    public static int WIDTH = 700;
    public static int HEIGHT = 700;

    public static double particleScalar = 2;

    public static int cellWidth;
    public static int cellHeight;

    public int[][][] cellCenters;

    public Simulation sim;

    public boolean drawDefault = true;
    public boolean drawParticles = false;
    public boolean debug = true;
    public boolean debugDirections = false;
    public boolean debugFlows = false;
    public boolean debugNumbers = false;
    public boolean debugOutflows = false;
    public boolean debugAdvectionNeighbours = false;
    public boolean debugMouseVelocity = false;
    public boolean debugHairVelocity = false;

    public int lastMouseX;
    public int lastMouseY;

    public int lastCellX; //Last cell mouse hovered over
    public int lastCellY;


    public int newCellX; //Last cell mouse hovered over
    public int newCellY;

    public int mouseCellEnterX; //Where Mouse entered last cell
    public int mouseCellEnterY;

    public double mouseVelX; //Velocity at mouse
    public double mouseVelY;


    public GridPanel(Simulation sim) {
        this.sim = sim;
        
        if(sim.cols > sim.rows) {
            cellWidth = (int) (WIDTH / (float) sim.cols - (2 * MARGIN / (float) sim.cols));
            cellHeight = cellWidth;
        }
        else {
            cellHeight = (int) (HEIGHT / (float) sim.rows - (2 * MARGIN / (float) sim.rows));
            cellWidth = cellHeight;
        }
        

        //Calculate Cell centers
        cellCenters = new int[sim.cols][sim.rows][2];
        var x = MARGIN + cellWidth / 2;
        var y = MARGIN + cellHeight / 2;
        for (int i = 0; i < sim.rows; i++) {
            for (int j = 0; j < sim.cols; j++) {
                cellCenters[j][i][0] = x;
                cellCenters[j][i][1] = y;
                x += cellWidth;
            }
            x = MARGIN + cellWidth / 2;
            y += cellHeight;
        }

        final Dimension dim = new Dimension(WIDTH, HEIGHT);
        setPreferredSize(dim);
        setMinimumSize(dim);


        this.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                super.mouseDragged(e);
                lastCellX = newCellX;
                lastCellY = newCellY;
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                newCellX = Math.max(Math.min(canvasToCellCoordinateXFloored(e.getX()), sim.cols - 1), 0);
                newCellY = Math.max(Math.min(canvasToCellCoordinateYFloored(e.getY()), sim.rows - 1), 0);

                if (lastCellX != newCellX || lastCellY != newCellY) {
                    sim.addNewForceBetweenCells(lastCellX, lastCellY, newCellX, newCellY, (lastMouseX - mouseCellEnterX) * 3000 * cellWidth, (lastMouseY - mouseCellEnterY) * 3000 * cellHeight, true);
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                super.mouseMoved(e);
                lastCellX = newCellX;
                lastCellY = newCellY;
                lastMouseX = e.getX();
                lastMouseY = e.getY();


                sim.inflowDirX = (newCellX - (sim.cols / 2)) * (2.0 / sim.cols);
                sim.inflowDirY = (newCellY - (sim.rows / 2)) * (2.0 / sim.rows);

                sim.mouseX = canvasToCellCoordinateX(lastMouseX);
                sim.mouseY = canvasToCellCoordinateY(lastMouseY);

                newCellX = Math.max(Math.min(canvasToCellCoordinateXFloored(e.getX()), sim.cols - 1), 0);
                newCellY = Math.max(Math.min(canvasToCellCoordinateYFloored(e.getY()), sim.rows - 1), 0);

                double[] mouseVel = sim.getVelAtPoint((lastMouseX - MARGIN) / (double) cellWidth, (lastMouseY - MARGIN) / (double) cellHeight);
                mouseVelX = mouseVel[0];
                mouseVelY = mouseVel[1];
            }
        });
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D graphics2D = (Graphics2D) g;

        if (!debug || drawDefault) {
            //Draw Cells
            for (int i = 0; i < sim.rows; i++) {
                for (int j = 0; j < sim.cols; j++) {
                    graphics2D.setColor(new Color((int) Math.max(Math.min(sim.cellColors[j][i][0], 255), 0), (int) Math.max(Math.min(sim.cellColors[j][i][1], 255), 0), (int) Math.max(Math.min(sim.cellColors[j][i][2], 255), 0)));
                    graphics2D.fillRect(cellCenters[j][i][0] - cellWidth / 2, cellCenters[j][i][1] - cellHeight / 2, cellWidth, cellHeight);
                    graphics2D.setColor(Color.WHITE);
                }
            }

        } else {
            graphics2D.setColor(Color.BLACK);
            graphics2D.fillRect(MARGIN, MARGIN, cellWidth * sim.cols, cellHeight * sim.rows);
        }

        if(drawParticles){
            for(FluidParticle p : sim.particles){
                graphics2D.setColor(new Color((int) Math.max(Math.min(Math.sqrt(Math.pow(p.velX,2)+Math.pow(p.velY,2))*200,255),0),0,255));
                if (p!=null) graphics2D.fillOval(cellToCanvasCoordinateX(p.x), cellToCanvasCoordinateY(p.y), p.getSize(cellWidth), p.getSize(cellHeight));
                System.out.println(cellToCanvasCoordinateX(p.x) + "  " + cellToCanvasCoordinateY(p.y));
            }
        }



        if (debug) {
            if (debugNumbers) {
                drawCellNumbers(graphics2D);
            }

            if (debugFlows) {
                drawFlowArrows(graphics2D);
            }

            if (debugDirections) {
                drawSummedFlow(graphics2D);
            }

            if (debugOutflows) {
                drawOutflows(graphics2D);
            }

            if (debugAdvectionNeighbours) {
                drawAdvectionNeighbours(graphics2D);
            }

            if (debugMouseVelocity) {
                paintArrow(graphics2D, lastMouseX, lastMouseY, (int) (mouseVelX * cellWidth * 4), (int) (mouseVelY * cellHeight * 4), Color.ORANGE);
            }

            if (debugHairVelocity) {
                drawVelocityHairs(graphics2D);
            }
            
            drawDivergence(graphics2D);
        }

        if(!debugMouseVelocity) {
            //Draw Hovered Cell
            g.setColor(Color.WHITE);
            int cellX = canvasToCellCoordinateXFloored(lastMouseX);
            int cellY = canvasToCellCoordinateYFloored(lastMouseY);
            g.drawRect(cellX * cellWidth + MARGIN, cellY * cellHeight + MARGIN, cellWidth, cellHeight);
        }
    }

    private void drawCellNumbers(Graphics2D graphics2D) {
        graphics2D.setColor(Color.WHITE);
        for (int i = 0; i < sim.rows; i++) {
            for (int j = 0; j < sim.cols; j++) {
                if (sim.cellColors[j][i][0] != 0)
                    graphics2D.drawString(String.valueOf((int) sim.cellColors[j][i][0]), cellCenters[j][i][0] - cellWidth / 2, cellCenters[j][i][1]);
            }
        }
    }

    private void drawFlowArrows(Graphics2D graphics2D) {
        for (int i = 0; i < sim.rows; i++) {
            for (int j = 0; j < sim.cols; j++) {
                if (sim.flows[j][i][0] != 0) {
                    //Draw Right Arrow
                    paintArrow(graphics2D, cellCenters[j][i][0] + (int) (cellWidth / 2f - cellWidth / 4f * sim.flows[j][i][0]),
                            cellCenters[j][i][1], (int) (cellWidth / 2f * sim.flows[j][i][0]), 0, new Color(63, 192, 135));
                }
                if (sim.flows[j][i][1] != 0) {
                    //Draw LeftArrow
                    paintArrow(graphics2D, cellCenters[j][i][0],
                            cellCenters[j][i][1] + (int) (cellHeight / 2f - cellHeight / 4f * sim.flows[j][i][1]),
                            0, (int) (cellHeight / 2f * sim.flows[j][i][1]), new Color(63, 192, 135));
                }
            }
        }
    }

    private void drawDivergence(Graphics2D graphics2D) {
        for (int i = 0; i < sim.rows; i++) {
            for (int j = 0; j < sim.cols; j++) {
                graphics2D.setColor(Color.YELLOW);
                if (sim.divergence[j][i] < 0)
                    graphics2D.setColor(Color.BLACK);
                double div = Math.abs(sim.divergence[j][i]);
                int radius = (int) (cellWidth / 8f * div);
                graphics2D.fillOval(cellCenters[j][i][0] - radius, cellCenters[j][i][1] - radius, radius * 2, radius * 2);
            }
        }
    }

    private void drawVelocityHairs(Graphics2D graphics2D) {
        int hairSteps = 30;
        for (int y = 0; y < sim.rows; y++) {
            for (int x = 0; x < sim.cols; x++) {
                double linePosX = x;
                double linePosY = y;
                for (int i = 0; i < hairSteps; i++) {
                    double velocityMagnitude = Math.sqrt(Math.pow(sim.getVelAtPoint(linePosX, linePosY)[0], 2) + Math.pow(sim.getVelAtPoint(linePosX, linePosY)[1], 2));
                    double newLinePosX = linePosX + sim.getVelAtPoint(linePosX, linePosY)[0] * 0.3;
                    double newLinePosY = linePosY + sim.getVelAtPoint(linePosX, linePosY)[1] * 0.3;
                    graphics2D.setColor(new Color((int)Math.min(velocityMagnitude * 255, 255), 0, 255));
                    graphics2D.drawLine(cellToCanvasCoordinateX(linePosX), cellToCanvasCoordinateY(linePosY),
                            cellToCanvasCoordinateX(newLinePosX), cellToCanvasCoordinateY(newLinePosY));
                    linePosX = newLinePosX;
                    linePosY = newLinePosY;
                }
            }
        }
    }

    private void drawAdvectionNeighbours(Graphics2D graphics2D) {
        graphics2D.setColor(Color.GRAY);
        //Draw Advection Neighbours
        for (int y = 0; y < sim.rows; y++) {
            for (int x = 0; x < sim.cols; x++) {
                if (lastCellX != x || lastCellY != y) {
                    continue;
                }

                AdvectionInterpolationInfo adInfo = sim.getAdvectionDeltas(x, y, false);
                double dirX = sim.summedFlows[x][y][0];
                double dirY = sim.summedFlows[x][y][1];


                double tX = (x + dirX) - adInfo.CellBLX;
                double tY = Math.abs(y + dirY) - adInfo.CellBLY;

                graphics2D.setColor(Color.BLUE);
                graphics2D.drawOval((int) ((adInfo.CellBLX + tX) * cellWidth + MARGIN + cellWidth / 2.0 - cellWidth / 10), (int) ((adInfo.CellBLY + tY) * cellHeight + MARGIN + cellHeight / 2.0 - cellHeight / 10), cellWidth / 5, cellHeight / 5);
                graphics2D.setColor(Color.GRAY);
                graphics2D.drawRect(adInfo.CellTLX * cellWidth + MARGIN, adInfo.CellTLY * cellHeight + MARGIN, cellWidth, cellHeight);
                graphics2D.drawRect(adInfo.CellTRX * cellWidth + MARGIN, adInfo.CellTRY * cellHeight + MARGIN, cellWidth, cellHeight);
                graphics2D.drawRect(adInfo.CellBLX * cellWidth + MARGIN, adInfo.CellBLY * cellHeight + MARGIN, cellWidth, cellHeight);
                graphics2D.drawRect(adInfo.CellBRX * cellWidth + MARGIN, adInfo.CellBRY * cellHeight + MARGIN, cellWidth, cellHeight);
            }
        }
    }

    private void drawOutflows(Graphics2D graphics2D) {
        for (int i = 0; i < sim.rows; i++) {
            for (int j = 0; j < sim.cols; j++) {
                double dirX;
                double dirY;
                dirX = sim.summedOutflows[j][i][0];
                dirY = sim.summedOutflows[j][i][1];
                dirX /= 2;
                dirY /= 2;
                paintArrow(graphics2D, cellCenters[j][i][0], cellCenters[j][i][1], (int) (dirX * cellWidth), (int) (dirY * cellHeight), Color.PINK);
            }
        }
    }

    private void drawSummedFlow(Graphics2D graphics2D) {
        for (int i = 0; i < sim.rows; i++) {
            for (int j = 0; j < sim.cols; j++) {
                double dirX;
                double dirY;
                dirX = sim.flows[j][i][0];
                dirY = sim.flows[j][i][1];
                if (j > 0)
                    dirX += sim.flows[j - 1][i][0];
                if (i > 0)
                    dirY += sim.flows[j][i - 1][1];
                dirX /= 2;
                dirY /= 2;

                paintArrow(graphics2D, cellCenters[j][i][0], cellCenters[j][i][1], (int) (dirX * cellWidth), (int) (dirY * cellHeight), Color.ORANGE);
            }
        }
    }

    /**
     * Converts coordinates from canvas space to cell indexes.
     * @param y The vertical coordinate to be converted.
     * @return The vertical cell index at this coordinate.
     */
    private int canvasToCellCoordinateYFloored(int y) {
        return (int) Math.floor(canvasToCellCoordinateY(y));
    }

    /**
     * Converts coordinates from canvas space to cell indexes.
     * @param x The horizontal coordinate to be converted.
     * @return The horizontal cell index at this coordinate.
     */
    private int canvasToCellCoordinateXFloored(int x) {
        return (int) Math.floor(canvasToCellCoordinateX(x));
    }

    /**
     * Converts coordinates from canvas space to simulation space.
     * @param y The vertical coordinate to be converted.
     * @return The vertical simulation coordinate at this point.
     */
    private double canvasToCellCoordinateY(int y) {
        return (y - MARGIN) / (double) cellHeight;
    }

    /**
     * Converts coordinates from canvas space to simulation space.
     * @param x The horizontal coordinate to be converted.
     * @return The horizontal simulation coordinate at this point.
     */
    private double canvasToCellCoordinateX(int x) {
        return (x - MARGIN) / (double) cellWidth;
    }

    /**
     * Converts coordinates from simulation space to canvas space.
     * @param x The horizontal coordinate to be converted.
     * @return The horizontal canvas coordinate at this point.
     */
    private int cellToCanvasCoordinateX(double x) {
        return (int) (x * cellWidth) + MARGIN;
    }

    /**
     * Converts coordinates from simulation space to canvas space.
     * @param y The vertical coordinate to be converted.
     * @return The vertical canvas coordinate at this point.
     */
    private int cellToCanvasCoordinateY(double y) {
        return (int) (y * cellHeight) + MARGIN;
    }

    /**
     * Draws an arrow from start coordinate in the direction and length of the direction vector in a specific color.
     * @param g The graphics context to paint in.
     * @param startX The start coordinates horizontal part.
     * @param startY The start coordinates vertical part.
     * @param dirX The direction vectors horizontal part.
     * @param dirY The direction vectors vertical part.
     * @param color The color of the arrow
     */
    void paintArrow(Graphics g, int startX, int startY, int dirX, int dirY, Color color) {
        g.setColor(color);
        if (dirX == 0 && dirY == 0) {
            //g.fillOval(startX - 5, startY - 5, 10, 10);
            return;
        }
        var endX = startX + dirX;
        var endY = startY + dirY;
        g.drawLine(startX, startY, endX, endY);
        //Arrow Triangle
        int[] xPoints = new int[3];
        int[] yPoints = new int[3];
        //Tip
        float tipScale = 0.3f;

        xPoints[0] = endX;
        yPoints[0] = endY;
        //Right Corner
        xPoints[1] = endX + (int) ((startX - endX) * tipScale) + (int) ((startY - endY) * tipScale);
        yPoints[1] = endY + (int) ((startY - endY) * tipScale) + (int) ((startX - endX) * -tipScale);

        //Left Corner
        xPoints[2] = endX + (int) ((startX - endX) * tipScale) - (int) ((startY - endY) * tipScale);
        yPoints[2] = endY + (int) ((startY - endY) * tipScale) - (int) ((startX - endX) * -tipScale);

        g.setColor(color);
        g.fillPolygon(xPoints, yPoints, 3);
    }
}

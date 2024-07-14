import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class InspectorPanel extends JPanel {
    private JLabel neighboursLabel;
    private GridPanel gridPanel;
    JLabel coordinateLabel;
    JLabel flowsLabel;
    JLabel divergenceLabel;
    JLabel fpsLabel;
    JLabel pressureLabel;

    public InspectorPanel(GridPanel gridPanel) {
        setLayout(new FlowLayout(FlowLayout.LEADING, 10, 5));
        setLayout(new GridLayout(7, 1));
        setPreferredSize(new Dimension(200, 500));
        this.gridPanel = gridPanel;
        coordinateLabel = new JLabel("x: " + gridPanel.lastCellX + " y: " + gridPanel.lastCellY);
        divergenceLabel = new JLabel("<html> Divergence: <br>" + gridPanel.sim.divergence[gridPanel.lastCellX][gridPanel.lastCellY]);
        fpsLabel = new JLabel("FPS: 60");
        flowsLabel = new JLabel("");
        neighboursLabel = new JLabel();
        pressureLabel = new JLabel();
        add(fpsLabel);
        add(coordinateLabel);
        add(flowsLabel);
        add(divergenceLabel);
        add(neighboursLabel);
        add(pressureLabel);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D graphics2D = (Graphics2D) g;

        fpsLabel.setText("FPS: " +  new DecimalFormat("#.0", DecimalFormatSymbols.getInstance(Locale.ENGLISH)).format(gridPanel.sim.fps) + " / " + Main.desiredFps);
        coordinateLabel.setText("x: " + gridPanel.lastCellX + " y: " + gridPanel.lastCellY);
        var flowTop = 0.0;
        var flowLeft = 0.0;
        var flowRight = gridPanel.sim.flows[gridPanel.lastCellX][gridPanel.lastCellY][0];
        flowRight = ((int)(flowRight * 1000) / 1000.0);
        var flowDown = gridPanel.sim.flows[gridPanel.lastCellX][gridPanel.lastCellY][1];
        flowDown = ((int)(flowDown * 1000) / 1000.0);
        if(gridPanel.lastCellX > 0){
            flowLeft = -gridPanel.sim.flows[gridPanel.lastCellX - 1][gridPanel.lastCellY][0];
            flowLeft = ((int)(flowLeft * 1000) / 1000.0);
        }
        if(gridPanel.lastCellY > 0){
            flowTop = -gridPanel.sim.flows[gridPanel.lastCellX][gridPanel.lastCellY - 1][1];
            flowTop = ((int)(flowTop * 1000) / 1000.0);
        }
        
        final int flowRectScale = 40;
        final int rectMiddleX = flowsLabel.getX()+ flowsLabel.getWidth() / 2 - 20;
        final int rectMiddleY = flowsLabel.getY() + flowsLabel.getHeight() / 2;
        final int textOffsetX = 15;
        graphics2D.setColor(new Color((int)Math.min(gridPanel.sim.cellColors[gridPanel.lastCellX][gridPanel.lastCellY][0],255), (int)Math.min(gridPanel.sim.cellColors[gridPanel.lastCellX][gridPanel.lastCellY][1], 255), (int)Math.min(gridPanel.sim.cellColors[gridPanel.lastCellX][gridPanel.lastCellY][2], 255)));
        graphics2D.fillRect(rectMiddleX - flowRectScale / 2, rectMiddleY - flowRectScale / 2, flowRectScale,flowRectScale);
        graphics2D.setColor(Color.BLACK);
        graphics2D.drawString( String.valueOf(flowTop), rectMiddleX - textOffsetX, rectMiddleY - flowRectScale);
        graphics2D.drawString( String.valueOf(flowDown), rectMiddleX - textOffsetX, rectMiddleY + flowRectScale);
        graphics2D.drawString( String.valueOf(flowLeft), rectMiddleX - flowRectScale - textOffsetX , rectMiddleY);
        graphics2D.drawString( String.valueOf(flowRight), rectMiddleX + flowRectScale - textOffsetX, rectMiddleY);

        divergenceLabel.setText("<html> Divergence: <br>" + gridPanel.sim.divergence[gridPanel.lastCellX][gridPanel.lastCellY]);
        neighboursLabel.setText(gridPanel.sim.neighbours[gridPanel.lastCellX][gridPanel.lastCellY] + " Neighbours");
        pressureLabel.setText(gridPanel.sim.pressure[gridPanel.lastCellX][gridPanel.lastCellY]+"");
    }
}

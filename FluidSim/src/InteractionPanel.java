import javax.swing.*;
import java.awt.*;

public class InteractionPanel extends JPanel {
    
    public static final int WIDTH = 300;
    public static final int HEIGHT = 300;
    private JPanel debugPanel;
    
    public InteractionPanel() {
        setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
        setLayout(new BorderLayout());
        Button restartButton = new Button("Restart");
        restartButton.addActionListener(e -> {
            Main.restartSimulation();
        });
        Button pauseButton = new Button("Pause");
        pauseButton.addActionListener(e -> {
            Main.pauseSimulation();
        });
        Button stepButton = new Button("Step");
        stepButton.addActionListener(e -> {
            Main.stepSimulation();
        });
        Button particleButton = new Button("Particles");
        particleButton.addActionListener(e -> {
            Main.toggleParticles();
        });
        Button debugButton = new Button("Debug");
        debugButton.addActionListener(e -> {
            Main.toggleDebug();
            debugPanel.setVisible(!debugPanel.isVisible());
        });
        JPanel scenePanel = new JPanel();
        scenePanel.setLayout(new GridLayout(1, 8));
        Button nextSceneButton = new Button("Next Scene");
        nextSceneButton.addActionListener(e -> {
            Main.nextSimulationScene();
        });
        Button previousSceneButton = new Button("Previous Scene");
        previousSceneButton.addActionListener(e -> {
            Main.previousSimulationScene();
        });
        scenePanel.add(previousSceneButton);
        scenePanel.add(nextSceneButton);
        JPanel steppingPanel = new JPanel();
        steppingPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
        steppingPanel.setLayout(new GridLayout(1, 5));
        steppingPanel.add(restartButton);
        steppingPanel.add(pauseButton);
        steppingPanel.add(stepButton);
        steppingPanel.add(particleButton);
        steppingPanel.add(debugButton);
        add(steppingPanel, BorderLayout.CENTER);
        debugPanel = new JPanel();
        debugPanel.setLayout(new GridLayout(1, 6));
        Button flowButton = new Button("Cell Flows");
        flowButton.addActionListener(e -> {
            Main.toggleDebugFlows();
        });
        Button directionsButton = new Button("Cell Directions");
        directionsButton.addActionListener(e -> {
            Main.toggleDebugDirections();
        });
        Button numbersButton = new Button("Cell Numbers");
        numbersButton.addActionListener(e -> {
            Main.toggleDebugNumbers();
        });
        Button outflowsButton = new Button("Outflows");
        outflowsButton.addActionListener(e -> {
            Main.toggleDebugOutflows();
        });
        Button advectionButton = new Button("Advection");
        advectionButton.addActionListener(e -> {
            Main.toggleDebugAdvectionNeighbours();
        });
        Button mouseVelButton = new Button("Mouse Velocity");
        mouseVelButton.addActionListener(e -> {
            Main.toggleDebugMouseVel();
        });
        Button drawDefaultButton = new Button("Draw Default");
        drawDefaultButton.addActionListener(e -> {
            Main.toggleDrawDefault();
        });
        Button hairButton = new Button("Velocity Hairs");
        hairButton.addActionListener(e -> {
            Main.toggleDebugHairs();
        });
        debugPanel.add(drawDefaultButton);
        debugPanel.add(flowButton);
        debugPanel.add(directionsButton);
        debugPanel.add(numbersButton);
        debugPanel.add(outflowsButton);
        debugPanel.add(advectionButton);
        debugPanel.add(mouseVelButton);
        debugPanel.add(hairButton);
        add(debugPanel, BorderLayout.SOUTH);
        add(scenePanel, BorderLayout.NORTH);
        setBorder(BorderFactory.createLineBorder(Color.black));

        Main.toggleDebug();
        debugPanel.setVisible(!debugPanel.isVisible());
    }
}

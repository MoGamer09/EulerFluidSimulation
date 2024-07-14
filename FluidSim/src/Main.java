import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Main {
    public static int simScaleX = 40;
    public static int simScaleY = 40;
    private static GridPanel gridPanel;
    private static JFrame window;
    private static boolean paused;
    private static boolean step;
    public static int desiredFps = 100;
    private static Timer timer;

    private static SimulationSceneInfo[] simulationSceneInfos = new SimulationSceneInfo[]{
            SimulationSceneInfo.waterFallScene,
            SimulationSceneInfo.vortexScene,
            SimulationSceneInfo.colorMixingScene,
            SimulationSceneInfo.clashScene,
            SimulationSceneInfo.movingInflowScene,
            SimulationSceneInfo.colorSplashesScene,
            SimulationSceneInfo.shootMouseScene,
            SimulationSceneInfo.multiShootMouseScene,
            SimulationSceneInfo.shootCircleScene,
    };
    private static int currentScene = 0;


    public static void showGUI(Simulation sim) {
        window = new JFrame("FLUID SIM");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gridPanel = new GridPanel(sim);
        window.add(gridPanel);
        final InteractionPanel interactionPanel = new InteractionPanel();
        window.add(interactionPanel, BorderLayout.SOUTH);
        final InspectorPanel inspectorPanel = new InspectorPanel(gridPanel);
        window.add(inspectorPanel, BorderLayout.EAST);
        window.setResizable(false);
        window.pack();
        window.setVisible(true);
        timer = new Timer(1000 / desiredFps, new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if(!paused || step)
                {
                    sim.SimulationStep();
                    step = false;
                }
                gridPanel.repaint();
                inspectorPanel.repaint();
            }
        });
        timer.start();

    }
    
    public static void main(String[] args) {
        Simulation sim = new Simulation(SimulationSceneInfo.waterFallScene);
        showGUI(sim);
    }

    public static void restartSimulation()
    {
        gridPanel.sim.clean();
        Simulation sim = new Simulation(gridPanel.sim.sceneInfo);
        timer.stop();
        window.dispose();
        showGUI(sim);
    }

    public static void pauseSimulation() {
        paused = !paused;
    }

    public static void stepSimulation() {
        step = true;
    }

    public static void toggleDebug()
    {
        gridPanel.debug = !gridPanel.debug;
    }

    public static void toggleDebugDirections()
    {
        gridPanel.debugDirections = !gridPanel.debugDirections;
    }

    public static void toggleDebugFlows()
    {
        gridPanel.debugFlows = !gridPanel.debugFlows;
    }

    public static void toggleDebugNumbers()
    {
        gridPanel.debugNumbers = !gridPanel.debugNumbers;
    }

    public static void toggleDebugOutflows() { gridPanel.debugOutflows = !gridPanel.debugOutflows;
    }

    public static void toggleDebugAdvectionNeighbours() {
        gridPanel.debugAdvectionNeighbours = !gridPanel.debugAdvectionNeighbours;
    }

    public static void toggleDebugMouseVel() {
        gridPanel.debugMouseVelocity = !gridPanel.debugMouseVelocity;
    }

    public static void toggleDebugHairs() {
        gridPanel.debugHairVelocity = !gridPanel.debugHairVelocity;
    }

    public static void toggleDrawDefault() {
        gridPanel.drawDefault = !gridPanel.drawDefault;
    }

    public static void nextSimulationScene() {
        currentScene = (currentScene + 1) % simulationSceneInfos.length;
        gridPanel.sim.changeScene(simulationSceneInfos[currentScene]);
        restartSimulation();
    }

    public static void previousSimulationScene() {
        currentScene = (currentScene - 1) % simulationSceneInfos.length;
        if(currentScene == -1)
            currentScene = simulationSceneInfos.length - 1;
        gridPanel.sim.changeScene(simulationSceneInfos[currentScene]);
        restartSimulation();
    }

    public static void toggleParticles() {
        gridPanel.drawParticles = !gridPanel.drawParticles;
    }
}
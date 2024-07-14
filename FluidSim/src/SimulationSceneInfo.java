import java.util.Random;

public class SimulationSceneInfo {
    public int rows;
    public int cols;
    public double[] preWarmColors;
    public InflowInfo[] inflowInfos;


    public SimulationSceneInfo(int cols, int rows, InflowInfo[] inflowInfos, double[] preWarmColors) {
        this.rows = rows;
        this.cols = cols;
        this.inflowInfos = inflowInfos;
        this.preWarmColors = preWarmColors;
    }

    public SimulationSceneInfo(int cols, int rows, InflowInfo[] inflowInfos) {
        this.rows = rows;
        this.cols = cols;
        this.inflowInfos = inflowInfos;
        this.preWarmColors = new double[3];
    }


    //---------------- PRESETS -------------------------------

    public static SimulationSceneInfo shootMouseScene = new SimulationSceneInfo(50, 50,
            new InflowInfo[]{
                    new InflowInfo(50 / 2, 50 / 2, 0, 0, new double[]{180, 235, 28}, true, (info) -> {
                        info.dirX = (info.mouseX - info.x);
                        info.dirY = (info.mouseY - info.y);
                    }),
            }, new double[]{12, 24, 2});

    public static SimulationSceneInfo multiShootMouseScene = new SimulationSceneInfo(40, 40,
            new InflowInfo[]{
                    new InflowInfo(40 / 5, 40 / 2, 0, 1, new double[]{0, 0, 255}, true, (info) -> {
                        info.dirX = (info.mouseX - info.x) / (40 / 2.0) * 10;
                        info.dirY = (info.mouseY - info.y) / (40 / 2.0) * 10;
                    }),
                    new InflowInfo(40 / 2, 40 / 2, 0, 1, new double[]{0, 255, 0}, true, (info) -> {
                        info.dirX = (info.mouseX - info.x) / (40 / 2.0) * 10;
                        info.dirY = (info.mouseY - info.y) / (40 / 2.0) * 10;
                    }),
                    new InflowInfo(40 / 5 * 4, 40 / 2, 0, 1, new double[]{255, 0, 0}, true, (info) -> {
                        info.dirX = (info.mouseX - info.x) / (40 / 2.0) * 10;
                        info.dirY = (info.mouseY - info.y) / (40 / 2.0) * 10;
                    })
            });
    public static SimulationSceneInfo clashScene = new SimulationSceneInfo(40, 20,
            new InflowInfo[]{
                    new InflowInfo(10, 10, 10, 0, new double[]{255, 127, 0}, true, (info) -> {
                    }),
                    new InflowInfo(30, 10, -10, 0, new double[]{0, 127, 255}, true, (info) -> {
                    }),
            });

    public static SimulationSceneInfo vortexScene = new SimulationSceneInfo(60, 60,
            new InflowInfo[]{
                    new InflowInfo(30, 10, 10, 0, new double[]{255, 255, 255}, true, (info) -> {
                    }),
                    new InflowInfo(10, 30, 0, -10, new double[]{0, 0, 0}, false, (info) -> {
                    }),
                    new InflowInfo(30, 50, -10, 0, new double[]{255, 255, 255}, true, (info) -> {
                    }),
                    new InflowInfo(50, 30, 0, 10, new double[]{0, 0, 0}, false, (info) -> {
                    }),
            });

    public static SimulationSceneInfo movingInflowScene = new SimulationSceneInfo(60, 30,
            new InflowInfo[]{
                    new InflowInfo(10, 10, 0, 10, new double[]{3, 252, 248}, true, (info) -> {
                        info.x = (int) (Math.sin(info.tick / 40f) * 20 + 30);
                    }),
                    new InflowInfo(10, 20, 0, -10, new double[]{252, 3, 115}, true, (info) -> {
                        info.x = (int) ((-Math.sin((info.tick) / 40f)) * 20 + 30);
                    }),
            });

    public static SimulationSceneInfo colorMixingScene = new SimulationSceneInfo(20, 30,
            new InflowInfo[]{
                    new InflowInfo(10, 3, 0, 10, new double[]{255, 0, 0}, false, (info) -> {
                        if (info.tick % 70 > 35) {
                            info.color = new double[]{255, 0, 0};
                        } else {
                            info.color = new double[]{0, 255, 0};
                        }
                    }),
            });

    public static SimulationSceneInfo colorSplashesScene = new SimulationSceneInfo(60, 60,
            new InflowInfo[]{
                    new InflowInfo(1, 1, 0, 0, new double[]{255, 255, 255}, true, (info) -> {
                        Random r = new Random();
                        if (info.tick % 50 == 0 || info.tick == 1) {
                            info.x = (int) (r.nextDouble() * 55 + 2);
                            info.y = (int) (r.nextDouble() * 55 + 2);
                            info.color = new double[]{r.nextDouble() * 100 + 155, r.nextDouble() * 100 + 155, r.nextDouble() * 100 + 155};
                            info.dirX = r.nextDouble() * 20 - 10;
                            info.dirY = r.nextDouble() * 20 - 10;
                        }
                    }),
                    new InflowInfo(1, 1, 0, 0, new double[]{255, 255, 255}, true, (info) -> {
                        Random r = new Random();
                        if (info.tick % 50 == 35 || info.tick == 1) {
                            info.x = (int) (r.nextDouble() * 55 + 2);
                            info.y = (int) (r.nextDouble() * 55 + 2);
                            info.color = new double[]{r.nextDouble() * 100 + 155, r.nextDouble() * 100 + 155, r.nextDouble() * 100 + 155};
                            info.dirX = r.nextDouble() * 20 - 10;
                            info.dirY = r.nextDouble() * 20 - 10;
                        }
                    }),
                    new InflowInfo(1, 1, 0, 0, new double[]{255, 255, 255}, true, (info) -> {
                        Random r = new Random();
                        if (info.tick % 50 == 20 || info.tick == 1) {
                            info.x = (int) (r.nextDouble() * 55 + 2);
                            info.y = (int) (r.nextDouble() * 55 + 2);
                            info.color = new double[]{r.nextDouble() * 100 + 155, r.nextDouble() * 100 + 155, r.nextDouble() * 100 + 155};
                            info.dirX = r.nextDouble() * 20 - 10;
                            info.dirY = r.nextDouble() * 20 - 10;
                        }
                    }),
                    new InflowInfo(1, 1, 0, 0, new double[]{255, 255, 255}, true, (info) -> {
                        Random r = new Random();
                        if (info.tick % 50 == 15 || info.tick == 1) {
                            info.x = (int) (r.nextDouble() * 55 + 2);
                            info.y = (int) (r.nextDouble() * 55 + 2);
                            info.color = new double[]{r.nextDouble() * 100 + 155, r.nextDouble() * 100 + 155, r.nextDouble() * 100 + 155};
                            info.dirX = r.nextDouble() * 20 - 10;
                            info.dirY = r.nextDouble() * 20 - 10;
                        }
                    }),
                    new InflowInfo(1, 1, 0, 0, new double[]{255, 255, 255}, true, (info) -> {
                        Random r = new Random();
                        if (info.tick % 50 == 40 || info.tick == 1) {
                            info.x = (int) (r.nextDouble() * 55 + 2);
                            info.y = (int) (r.nextDouble() * 55 + 2);
                            info.color = new double[]{r.nextDouble() * 100 + 155, r.nextDouble() * 100 + 155, r.nextDouble() * 100 + 155};
                            info.dirX = r.nextDouble() * 20 - 10;
                            info.dirY = r.nextDouble() * 20 - 10;
                        }
                    }),
            });

    public static SimulationSceneInfo waterFallScene = new SimulationSceneInfo(70, 70,
            new InflowInfo[]{
                    new InflowInfo(30, 30, 0, 10, new double[]{28, 163, 235}, true, (info) -> {
                    }),
                    new InflowInfo(31, 30, 0, 10, new double[]{28, 163, 235}, true, (info) -> {
                    }),
                    new InflowInfo(32, 30, 0, 10, new double[]{28, 163, 235}, true, (info) -> {
                    }),
                    new InflowInfo(33, 30, 0, 10, new double[]{28, 163, 235}, true, (info) -> {
                    }),
                    new InflowInfo(34, 30, 0, 10, new double[]{28, 163, 235}, true, (info) -> {
                    }),
                    new InflowInfo(35, 30, 0, 10, new double[]{28, 163, 235}, true, (info) -> {
                    }),
                    new InflowInfo(36, 30, 0, 10, new double[]{28, 163, 235}, true, (info) -> {
                    }),
                    new InflowInfo(37, 30, 0, 10, new double[]{28, 163, 235}, true, (info) -> {
                    }),
                    new InflowInfo(38, 30, 0, 10, new double[]{28, 163, 235}, true, (info) -> {
                    }),
                    new InflowInfo(39, 30, 0, 10, new double[]{28, 163, 235}, true, (info) -> {
                    }),
                    new InflowInfo(40, 30, 0, 10, new double[]{28, 163, 235}, true, (info) -> {
                    }),
            });

    public static SimulationSceneInfo shootCircleScene = new SimulationSceneInfo(60, 60,
            new InflowInfo[]{
                    new InflowInfo(10, 3, 0, 10, new double[]{252, 88, 0}, false, (info) -> {
                        info.x = (int)((Math.sin(info.tick / 40.0) * 25) + 30);
                        info.y = (int)((Math.cos(info.tick / 40.0) * 25) + 30);
                        info.dirX = (30 - ((Math.sin(info.tick / 40.0) * 25) + 30));
                        info.dirY = (30 - ((Math.cos(info.tick / 40.0) * 25) + 30));
                    }),
                    new InflowInfo(10, 3, 0, 10, new double[]{0, 109, 252}, false, (info) -> {
                        info.x = (int)((-Math.sin(info.tick / 40.0) * 25) + 30);
                        info.y = (int)((-Math.cos(info.tick / 40.0) * 25) + 30);
                        info.dirX = (30 - ((-Math.sin(info.tick / 40.0) * 25) + 30));
                        info.dirY = (30 - ((-Math.cos(info.tick / 40.0) * 25) + 30));
                    }),
            });
}

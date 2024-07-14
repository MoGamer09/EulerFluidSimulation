import java.awt.*;
import java.util.function.Consumer;

public class InflowInfo {
    public int x;
    public int y;
    public double dirX;
    public double dirY;
    public double[] color;
    public boolean noise;
    public double mouseX, mouseY;
    private final Consumer<InflowInfo> action;
    public int tick;

    public InflowInfo(int x, int y, double dirX, double dirY, double[] color, boolean noise, Consumer<InflowInfo> action) {
        this.x = x;
        this.y = y;
        this.dirX = dirX;
        this.dirY = dirY;
        this.color = color;
        this.noise = noise;
        this.action = action;
    }

    public void tick(double mouseX, double mouseY)
    {
        tick++;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        action.accept(this);
    }
}

public class FluidParticle {
    public double x;
    public double y;
    public double velX;
    public double velY;
    public double lifetime;

    public FluidParticle(int x, int y, double lifetime) {
        this.x = x;
        this.y = y;
        this.lifetime = lifetime;
    }

    public void move (double dx, double dy) {
        x += dx;
        y += dy;
        velX = dx;
        velY = dy;
    }

    public int getSize(int max){
        return (int)Math.floor((((1000-Math.abs(500-lifetime))/1000)*max));
        //Double d = (((-1*((lifetime-500)*(lifetime-500)+1000))/1000)*max);
        //return d.intValue();
    }

}

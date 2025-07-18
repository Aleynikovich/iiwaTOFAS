package hartu.robot.commands.positions;

public class CartesianPosition {
    private final double x, y, z;
    private final double a, b, c;

    public CartesianPosition(double x, double y, double z, double a, double b, double c) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public double getA() { return a; }
    public double getB() { return b; }
    public double getC() { return c; }
}

package hartu.robot.commands.positions;

public class AxisPosition {
    private final double j1, j2, j3, j4, j5, j6, j7;

    public AxisPosition(double j1, double j2, double j3, double j4, double j5, double j6, double j7) {
        this.j1 = j1;
        this.j2 = j2;
        this.j3 = j3;
        this.j4 = j4;
        this.j5 = j5;
        this.j6 = j6;
        this.j7 = j7;
    }

    public double getJ1() { return j1; }
    public double getJ2() { return j2; }
    public double getJ3() { return j3; }
    public double getJ4() { return j4; }
    public double getJ5() { return j5; }
    public double getJ6() { return j6; }
    public double getJ7() { return j7; }
}

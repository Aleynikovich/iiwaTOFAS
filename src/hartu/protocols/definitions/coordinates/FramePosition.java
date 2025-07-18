package hartu.protocols.definitions.coordinates;

import hartu.protocols.constants.LinearUnit;
import hartu.protocols.constants.AngularUnit;

public class FramePosition extends TargetPosition {
    private final double x;
    private final double y;
    private final double z;
    private final double a;
    private final double b;
    private final double c;

    private final LinearUnit linearUnit;
    private final AngularUnit angularUnit;

    // Constructor with explicit units
    public FramePosition(double x, double y, double z, double a, double b, double c,
                         LinearUnit linearUnit, AngularUnit angularUnit) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.a = a;
        this.b = b;
        this.c = c;
        this.linearUnit = linearUnit;
        this.angularUnit = angularUnit;
    }

    // Constructor with default units (MM for linear, DEGREE for angular)
    public FramePosition(double x, double y, double z, double a, double b, double c) {
        this(x, y, z, a, b, c, LinearUnit.MM, AngularUnit.DEGREE);
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public double getA() { return a; }
    public double getB() { return b; }
    public double getC() { return c; }

    public LinearUnit getLinearUnit() { return linearUnit; }
    public AngularUnit getAngularUnit() { return angularUnit; }

    public double getX(LinearUnit targetUnit) { return linearUnit.convertTo(x, targetUnit); }
    public double getY(LinearUnit targetUnit) { return linearUnit.convertTo(y, targetUnit); }
    public double getZ(LinearUnit targetUnit) { return linearUnit.convertTo(z, targetUnit); }
    public double getA(AngularUnit targetUnit) { return angularUnit.convertTo(a, targetUnit); }
    public double getB(AngularUnit targetUnit) { return angularUnit.convertTo(b, targetUnit); }
    public double getC(AngularUnit targetUnit) { return angularUnit.convertTo(c, targetUnit); }

    @Override
    public String toString() {
        return "FramePosition [x=" + x + " " + linearUnit.name() +
                ", y=" + y + " " + linearUnit.name() +
                ", z=" + z + " " + linearUnit.name() +
                ", a=" + a + " " + angularUnit.name() +
                ", b=" + b + " " + angularUnit.name() +
                ", c=" + c + " " + angularUnit.name() + "]";
    }
}
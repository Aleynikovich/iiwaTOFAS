package hartu.robot.commands.positions;

import com.kuka.roboticsAPI.geometricModel.AbstractFrame;
import com.kuka.roboticsAPI.geometricModel.Frame;
import hartu.robot.communication.server.Logger; // Assuming Logger is accessible

/**
 * Represents a robot's Cartesian position with X, Y, Z, A, B, C (Euler angles).
 * Stores A, B, C in both degrees (as input) and radians (for KUKA API).
 */
public class CartesianPosition {
    private final double x, y, z;
    private final double aDeg, bDeg, cDeg; // Store input in degrees
    private final double aRad, bRad, cRad; // Store converted radians for KUKA API

    public CartesianPosition(double x, double y, double z, double a, double b, double c) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.aDeg = a;
        this.bDeg = b;
        this.cDeg = c;

        // Convert degrees to radians in the constructor
        this.aRad = Math.toRadians(a);
        this.bRad = Math.toRadians(b);
        this.cRad = Math.toRadians(c);

        Logger.getInstance().log("CART_POS", String.format(
                "Created CartesianPosition: X=%.3f, Y=%.3f, Z=%.3f, A=%.3f (deg), B=%.3f (deg), C=%.3f (deg) " +
                        "| A=%.3f (rad), B=%.3f (rad), C=%.3f (rad)",
                x, y, z, a, b, c, aRad, bRad, cRad
                                                          ));
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public double getADeg() { return aDeg; } // Get Alpha in Degrees
    public double getBDeg() { return bDeg; } // Get Beta in Degrees
    public double getCDeg() { return cDeg; } // Get Gamma in Degrees

    // New getters for radian values, to be used by toFrame()
    public double getARad() { return aRad; }
    public double getBRad() { return bRad; }
    public double getCRad() { return cRad; }

    /**
     * Converts this CartesianPosition object into a KUKA RoboticsAPI Frame object.
     * The Frame will be defined relative to the provided parentFrame, using radian values for orientation.
     * @param parentFrame The parent frame to which this Cartesian position is relative.
     * @return A new Frame object.
     */
    public Frame toFrame(AbstractFrame parentFrame) {
        if (parentFrame == null) {
            Logger.getInstance().error("CART_POS", "Parent frame cannot be null when converting CartesianPosition to Frame.");
            throw new IllegalArgumentException("Parent frame cannot be null.");
        }
        // Use the radian values (aRad, bRad, cRad) for Frame construction
        return new Frame(parentFrame, x, y, z, aRad, bRad, cRad);
    }

    @Override
    public String toString() {
        return String.format("X:%.3f, Y:%.3f, Z:%.3f, A:%.3f(deg), B:%.3f(deg), C:%.3f(deg)", x, y, z, aDeg, bDeg, cDeg);
    }
}

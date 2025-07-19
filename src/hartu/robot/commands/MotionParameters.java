// --- MotionParameters.java ---
package hartu.robot.commands;

import hartu.robot.communication.server.Logger; // Import the Logger

public class MotionParameters {
    private final double speedOverride;
    private final String tool;
    private final String base;
    private final boolean continuous;
    private final int numPoints;

    public MotionParameters(double speedOverride, String tool, String base, boolean continuous, int numPoints) {
        // Log a warning if speedOverride is outside the expected 0.0-1.0 range initially
        // This check is before potential scaling, assuming client might send 0-100%
        if (speedOverride < 0.0 || speedOverride > 1.0) {
            Logger.getInstance().log("MotionParameters Warning: Initial speedOverride (" + speedOverride + ") is outside 0.0-1.0 range. Attempting to scale/clamp.");
            // Assume it's a percentage (0-100) and scale it down
            speedOverride = speedOverride / 100.0;
        }

        // Clamp the speedOverride to ensure it's strictly between 0.0 and 1.0 after any scaling
        this.speedOverride = Math.max(0.0, Math.min(1.0, speedOverride));
        Logger.getInstance().log("MotionParameters: Final speedOverride set to " + this.speedOverride);


        if (numPoints < 0) {
            // Keep throwing exception for invalid numPoints as it's a logical error
            throw new IllegalArgumentException("Number of points cannot be negative.");
        }
        this.tool = tool;
        this.base = base;
        this.continuous = continuous;
        this.numPoints = numPoints;
    }

    public double getSpeedOverride() {
        return speedOverride;
    }

    public String getTool() {
        return tool;
    }

    public String getBase() {
        return base;
    }

    public boolean isContinuous() {
        return continuous;
    }

    public int getNumPoints() {
        return numPoints;
    }
}

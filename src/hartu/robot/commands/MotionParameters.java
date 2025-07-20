package hartu.robot.commands;

import hartu.robot.communication.server.Logger;

public class MotionParameters
{
    private final double speedOverride;
    private final String tool;
    private final String base;
    private final boolean continuous;
    private final int numPoints;

    public MotionParameters(double speedOverride, String tool, String base, boolean continuous, int numPoints)
    {

        if (speedOverride <= 0)
        {
            Logger.getInstance().log("SpeedOverride is negative or zero");
        }
        if (speedOverride > 1.0)
        {
            Logger.getInstance().log("MotionParameters Warning: Initial speedOverride (" + speedOverride + ") is outside 0.0-1.0 range. Attempting to scale/clamp.");
            speedOverride = speedOverride / 100.0;
        }

        this.speedOverride = Math.max(0.0, Math.min(1.0, speedOverride));

        if (numPoints < 0)
        {
            throw new IllegalArgumentException("Number of points cannot be negative.");
        }
        this.tool = tool;
        this.base = base;
        this.continuous = continuous;
        this.numPoints = numPoints;
    }

    public double getSpeedOverride()
    {
        return speedOverride;
    }

    public String getTool()
    {
        return tool;
    }

    public String getBase()
    {
        return base;
    }

    public boolean isContinuous()
    {
        return continuous;
    }

    public int getNumPoints()
    {
        return numPoints;
    }
}

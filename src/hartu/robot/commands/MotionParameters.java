package hartu.robot.commands;

public class MotionParameters {
    private final double speedOverride;
    private final String tool;
    private final String base;

    public MotionParameters(double speedOverride, String tool, String base) {
        if (speedOverride < 0.0 || speedOverride > 1.0) {
            throw new IllegalArgumentException("Speed override must be between 0.0 and 1.0");
        }
        this.speedOverride = speedOverride;
        this.tool = tool;
        this.base = base;
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
}

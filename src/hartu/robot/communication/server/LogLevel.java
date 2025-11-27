package hartu.robot.communication.server;

/**
 * Enumeration for log severity levels with numeric ordering.
 * Lower ordinal values represent higher verbosity.
 * <p>
 * Usage:
 * - INFO: Detailed operational messages for debugging and development
 * - WARN: Warning messages for recoverable issues
 * - ERROR: Error messages for failures and critical issues
 */
public enum LogLevel
{
    INFO(0),    // Most verbose - shows all logs
    WARN(1),    // Shows warnings and errors only
    ERROR(2);   // Least verbose - shows only critical errors

    private final int value;

    LogLevel(int value)
    {
        this.value = value;
    }

    /**
     * Gets the numeric value of this log level.
     * Lower values indicate higher verbosity.
     *
     * @return The numeric value
     */
    public int getValue()
    {
        return value;
    }

    /**
     * Checks if this log level should be logged given the minimum configured level.
     *
     * @param minimumLevel The minimum log level configured
     * @return true if this level should be logged, false otherwise
     */
    public boolean shouldLog(LogLevel minimumLevel)
    {
        return this.value >= minimumLevel.value;
    }
}

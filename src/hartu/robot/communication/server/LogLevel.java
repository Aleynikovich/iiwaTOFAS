package hartu.robot.communication.server;

/**
 * Enumeration for log priority levels with numeric ordering.
 * Lower values represent higher priority (more important logs).
 * <p>
 * Priority Levels:
 * - CRITICAL (0): Must-have logs that should ALWAYS be shown (parsed commands, critical operations)
 * - HIGH (1): High-priority logs including errors and important status messages
 * - MEDIUM (2): Medium-priority logs including warnings and important state changes
 * - LOW (3): Low-priority logs like execution progress and motion operations
 * - DEBUG (4): Debug/trace logs like queue operations and connection events
 * <p>
 * Default minimum level is MEDIUM (2), showing priorities 0, 1, and 2.
 */
public enum LogLevel
{
    CRITICAL(0),  // Must-have logs (always shown)
    HIGH(1),      // High priority (errors, important status)
    MEDIUM(2),    // Medium priority (warnings, state changes) - DEFAULT
    LOW(3),       // Low priority (execution progress)
    DEBUG(4);     // Debug/trace (verbose operations)

    private final int value;

    LogLevel(int value)
    {
        this.value = value;
    }

    /**
     * Gets the numeric priority value of this log level.
     * Lower values indicate higher priority.
     *
     * @return The numeric priority value (0-4)
     */
    public int getValue()
    {
        return value;
    }

    /**
     * Checks if this log level should be logged given the minimum configured level.
     * A log is shown if its priority value is less than or equal to the minimum level.
     *
     * @param minimumLevel The minimum log level configured
     * @return true if this level should be logged, false otherwise
     */
    public boolean shouldLog(LogLevel minimumLevel)
    {
        return this.value <= minimumLevel.value;
    }
}

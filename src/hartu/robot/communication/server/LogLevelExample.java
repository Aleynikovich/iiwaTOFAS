package hartu.robot.communication.server;

/**
 * Example usage of the Logger with configurable log levels.
 * <p>
 * This demonstrates how to control log verbosity by setting the minimum log level.
 * <p>
 * Usage examples:
 * <p>
 * 1. Show all logs (default behavior):
 * Logger.getInstance().setMinimumLogLevel(LogLevel.INFO);
 * // Output: INFO, WARN, and ERROR messages
 * <p>
 * 2. Show only warnings and errors (reduce noise):
 * Logger.getInstance().setMinimumLogLevel(LogLevel.WARN);
 * // Output: WARN and ERROR messages only (INFO filtered out)
 * <p>
 * 3. Show only critical errors:
 * Logger.getInstance().setMinimumLogLevel(LogLevel.ERROR);
 * // Output: ERROR messages only (INFO and WARN filtered out)
 * <p>
 * The log level can be changed at any time during runtime, allowing dynamic
 * adjustment of log verbosity based on operational needs.
 */
public class LogLevelExample
{
    public static void demonstrateLogLevels()
    {
        Logger logger = Logger.getInstance();

        // Example 1: Default behavior - show all logs
        System.out.println("=== Example 1: INFO level (show all logs) ===");
        logger.setMinimumLogLevel(LogLevel.INFO);
        logger.log("EXAMPLE", "This is an INFO message");
        logger.warn("EXAMPLE", "This is a WARN message");
        logger.error("EXAMPLE", "This is an ERROR message");

        // Example 2: Show only WARN and ERROR
        System.out.println("\n=== Example 2: WARN level (filter out INFO) ===");
        logger.setMinimumLogLevel(LogLevel.WARN);
        logger.log("EXAMPLE", "This INFO message will be filtered out");
        logger.warn("EXAMPLE", "This WARN message will be shown");
        logger.error("EXAMPLE", "This ERROR message will be shown");

        // Example 3: Show only ERROR
        System.out.println("\n=== Example 3: ERROR level (show only critical errors) ===");
        logger.setMinimumLogLevel(LogLevel.ERROR);
        logger.log("EXAMPLE", "This INFO message will be filtered out");
        logger.warn("EXAMPLE", "This WARN message will be filtered out");
        logger.error("EXAMPLE", "This ERROR message will be shown");

        // Reset to default
        logger.setMinimumLogLevel(LogLevel.INFO);
    }
}

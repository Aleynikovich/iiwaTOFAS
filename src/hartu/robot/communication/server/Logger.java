package hartu.robot.communication.server;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton logger that broadcasts messages to multiple handlers.
 * Supports simultaneous logging to robot console and network clients.
 * Thread-safe implementation for use from multiple tasks.
 * <p>
 * Log Priority System:
 * - The logger supports priority-based filtering (0-4, lower = higher priority)
 * - Default level is MEDIUM (priority 2), showing priorities 0, 1, and 2
 * - Can be changed at runtime via setMinimumLogLevel()
 * - Priority Levels:
 * - CRITICAL (0): Must-have logs (parsed commands, critical operations)
 * - HIGH (1): Errors and important status messages
 * - MEDIUM (2): Warnings and important state changes [DEFAULT]
 * - LOW (3): Execution progress and motion operations
 * - DEBUG (4): Verbose debug/trace information
 * - Example: setMinimumLogLevel(LogLevel.HIGH) to show only priorities 0 and 1
 */
public class Logger
{
    private static Logger instance;
    private final SimpleDateFormat timeFormat;
    private final List<LogHandler> handlers;
    private volatile LogLevel minimumLogLevel;

    private Logger()
    {
        this.timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
        this.handlers = new CopyOnWriteArrayList<>();
        this.minimumLogLevel = LogLevel.MEDIUM; // Default: show priorities 0, 1, 2
    }

    public static synchronized Logger getInstance()
    {
        if (instance == null)
        {
            instance = new Logger();
        }
        return instance;
    }

    /**
     * Adds a log handler to receive log messages.
     *
     * @param handler The handler to add
     */
    public void addHandler(LogHandler handler)
    {
        if (handler != null && !handlers.contains(handler))
        {
            handlers.add(handler);
        }
    }

    /**
     * Removes a log handler.
     *
     * @param handler The handler to remove
     */
    public void removeHandler(LogHandler handler)
    {
        if (handler != null)
        {
            handlers.remove(handler);
        }
    }

    /**
     * Removes all log handlers.
     */
    public void clearHandlers()
    {
        for (LogHandler handler : handlers)
        {
            try
            {
                handler.close();
            } catch (Exception e)
            {
                // Ignore errors during cleanup
            }
        }
        handlers.clear();
    }

    /**
     * Gets the current minimum log level.
     *
     * @return The minimum log level
     */
    public LogLevel getMinimumLogLevel()
    {
        return minimumLogLevel;
    }

    /**
     * Sets the minimum log level that will be broadcast to handlers.
     * Messages with priority higher than this level will be filtered out.
     *
     * @param level The minimum log level (CRITICAL=0, HIGH=1, MEDIUM=2, LOW=3, DEBUG=4)
     */
    public void setMinimumLogLevel(LogLevel level)
    {
        if (level != null)
        {
            this.minimumLogLevel = level;
        }
    }

    /**
     * @deprecated Use addHandler with NetworkLogHandler instead.
     * This method is kept for backward compatibility during migration.
     */
    @Deprecated
    public void setLogClientHandler(ClientHandler handler)
    {
        // For backward compatibility, wrap the ClientHandler
        if (handler != null)
        {
            addHandler(new LegacyClientHandlerAdapter(handler));
            log("LOGGER", "Log client handler set (legacy mode).");
        }
    }

    /**
     * Logs a message with the specified priority level.
     *
     * @param level   The priority level (CRITICAL, HIGH, MEDIUM, LOW, DEBUG)
     * @param tag     Component identifier
     * @param message The log message
     */
    public void log(LogLevel level, String tag, String message)
    {
        logWithLevel(level, tag, message);
    }

    /**
     * Logs a message with CRITICAL priority (priority 0).
     * Use for must-have logs that should always be shown.
     *
     * @param tag     Component identifier
     * @param message The log message
     */
    public void critical(String tag, String message)
    {
        logWithLevel(LogLevel.CRITICAL, tag, message);
    }

    /**
     * Logs a message with HIGH priority (priority 1).
     * Use for errors and important status messages.
     *
     * @param tag     Component identifier
     * @param message The log message
     */
    public void high(String tag, String message)
    {
        logWithLevel(LogLevel.HIGH, tag, message);
    }

    /**
     * Logs a message with MEDIUM priority (priority 2).
     * Use for warnings and important state changes.
     *
     * @param tag     Component identifier
     * @param message The log message
     */
    public void medium(String tag, String message)
    {
        logWithLevel(LogLevel.MEDIUM, tag, message);
    }

    /**
     * Logs a message with LOW priority (priority 3).
     * Use for execution progress and motion operations.
     *
     * @param tag     Component identifier
     * @param message The log message
     */
    public void low(String tag, String message)
    {
        logWithLevel(LogLevel.LOW, tag, message);
    }

    /**
     * Logs a message with DEBUG priority (priority 4).
     * Use for verbose debug/trace information.
     *
     * @param tag     Component identifier
     * @param message The log message
     */
    public void debug(String tag, String message)
    {
        logWithLevel(LogLevel.DEBUG, tag, message);
    }

    /**
     * Logs an error with HIGH priority (priority 1).
     *
     * @param tag     Component identifier
     * @param message The log message
     */
    public void error(String tag, String message)
    {
        logWithLevel(LogLevel.HIGH, tag, message);
    }

    /**
     * Logs an error with HIGH priority (priority 1) and exception details.
     *
     * @param tag     Component identifier
     * @param message The log message
     * @param t       The throwable to log
     */
    public void error(String tag, String message, Throwable t)
    {
        logWithLevel(LogLevel.HIGH, tag, message + " - Exception: " + t.toString());
    }

    /**
     * Logs a warning with MEDIUM priority (priority 2).
     *
     * @param tag     Component identifier
     * @param message The log message
     */
    public void warn(String tag, String message)
    {
        logWithLevel(LogLevel.MEDIUM, tag, message);
    }

    /**
     * @deprecated Use log(LogLevel, String, String) with explicit priority level instead.
     * This method defaults to DEBUG priority for backward compatibility.
     */
    @Deprecated
    public void log(String tag, String message)
    {
        logWithLevel(LogLevel.DEBUG, tag, message);
    }

    /**
     * Internal method to log a message with level filtering.
     *
     * @param level   The log level of this message
     * @param tag     Component identifier
     * @param message The log message
     */
    private void logWithLevel(LogLevel level, String tag, String message)
    {
        // Check if this message should be logged based on configured minimum level
        if (!level.shouldLog(minimumLogLevel))
        {
            return; // Filter out this message
        }

        String formattedMessage = formatMessage(tag, message, level.name());
        broadcastToHandlers(formattedMessage);
    }

    private String formatMessage(String tag, String message, String level)
    {
        String timestamp = timeFormat.format(new Date());
        return "[" + timestamp + "] [" + level + "] [" + tag + "] " + message + "\n";
    }

    private void broadcastToHandlers(String formattedMessage)
    {
        for (LogHandler handler : handlers)
        {
            try
            {
                if (handler.isActive())
                {
                    handler.sendMessage(formattedMessage);
                }
            } catch (Exception e)
            {
                // Don't let one handler's failure affect others
                // Can't log this error as it would cause recursion
            }
        }
    }

    /**
     * Adapter class for backward compatibility with old ClientHandler-based logging.
     */
    private static class LegacyClientHandlerAdapter implements LogHandler
    {
        private final ClientHandler clientHandler;
        private boolean active = true;

        public LegacyClientHandlerAdapter(ClientHandler clientHandler)
        {
            this.clientHandler = clientHandler;
        }

        @Override
        public void sendMessage(String formattedMessage)
        {
            if (active && clientHandler != null)
            {
                clientHandler.sendMessage(formattedMessage);
            }
        }

        @Override
        public boolean isActive()
        {
            return active && clientHandler != null;
        }

        @Override
        public void close()
        {
            active = false;
        }
    }
}

package hartu.robot.communication.server;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton logger that broadcasts messages to multiple handlers.
 * Supports simultaneous logging to robot console and network clients.
 * Thread-safe implementation for use from multiple tasks.
 * 
 * Log Verbosity:
 * - The logger supports configurable minimum log level filtering
 * - Default level is INFO (shows all logs)
 * - Can be changed at runtime via setMinimumLogLevel()
 * - Example: setMinimumLogLevel(LogLevel.WARN) to show only WARN and ERROR
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
        this.minimumLogLevel = LogLevel.INFO; // Default: show all logs
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
            }
            catch (Exception e)
            {
                // Ignore errors during cleanup
            }
        }
        handlers.clear();
    }
    
    /**
     * Sets the minimum log level that will be broadcast to handlers.
     * Messages below this level will be filtered out.
     * 
     * @param level The minimum log level (INFO, WARN, or ERROR)
     */
    public void setMinimumLogLevel(LogLevel level)
    {
        if (level != null)
        {
            this.minimumLogLevel = level;
        }
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

    public void log(String tag, String message)
    {
        logWithLevel(LogLevel.INFO, tag, message);
    }

    public void warn(String tag, String message)
    {
        logWithLevel(LogLevel.WARN, tag, message);
    }

    public void error(String tag, String message)
    {
        logWithLevel(LogLevel.ERROR, tag, message);
    }

    public void error(String tag, String message, Throwable t)
    {
        logWithLevel(LogLevel.ERROR, tag, message + " - Exception: " + t.toString());
    }
    
    /**
     * Internal method to log a message with level filtering.
     * 
     * @param level The log level of this message
     * @param tag Component identifier
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
            }
            catch (Exception e)
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

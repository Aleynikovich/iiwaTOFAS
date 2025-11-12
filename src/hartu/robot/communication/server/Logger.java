package hartu.robot.communication.server;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton logger that broadcasts messages to multiple handlers.
 * Supports simultaneous logging to robot console and network clients.
 * Thread-safe implementation for use from multiple tasks.
 */
public class Logger
{
    private static Logger instance;
    private final SimpleDateFormat timeFormat;
    private final List<LogHandler> handlers;

    private Logger()
    {
        this.timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
        this.handlers = new CopyOnWriteArrayList<>();
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
        String formattedMessage = formatMessage(tag, message, "INFO");
        broadcastToHandlers(formattedMessage);
    }

    public void warn(String tag, String message)
    {
        String formattedMessage = formatMessage(tag, message, "WARN");
        broadcastToHandlers(formattedMessage);
    }

    public void error(String tag, String message)
    {
        String formattedMessage = formatMessage(tag, message, "ERROR");
        broadcastToHandlers(formattedMessage);
    }

    public void error(String tag, String message, Throwable t)
    {
        String formattedMessage = formatMessage(tag, message + " - Exception: " + t.toString(), "ERROR");
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

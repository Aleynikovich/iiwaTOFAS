// --- Logger.java ---
package hartu.robot.communication.server;

public class Logger
{
    private static Logger instance;
    private ClientHandler logClientHandler;

    private Logger()
    {
        // Private constructor to enforce singleton pattern
    }

    public static synchronized Logger getInstance()
    {
        if (instance == null)
        {
            instance = new Logger();
        }
        return instance;
    }

    // Method to set the active log client handler
    public void setLogClientHandler(ClientHandler handler)
    {
        this.logClientHandler = handler;
    }

    // Method to send a log message
    public void log(String message)
    {
        if (logClientHandler != null)
        {
            logClientHandler.sendMessage(message);
        }
    }
}

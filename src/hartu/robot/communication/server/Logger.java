// --- Logger.java ---
package hartu.robot.communication.server;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger
{
    private static Logger instance;
    private ClientHandler logClientHandler;
    private SimpleDateFormat dateFormat; // New field for date formatting

    private Logger()
    {
        // Private constructor to enforce singleton pattern
        // Initialize the date format
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
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
        // Log when the log client handler is set
        log("Logger: Log client handler set.");
    }

    // Method to send a log message
    public void log(String message)
    {
        // Prepend current date and time to the message
        String timestamp = dateFormat.format(new Date());
        String formattedMessage = "[" + timestamp + "] " + message;

        if (logClientHandler != null)
        {
            logClientHandler.sendMessage(formattedMessage);
        }
        // Optionally, you could also print to System.out/err as a fallback
        // if the log client is not connected, but you've expressed a preference
        // against local prints.
    }
}

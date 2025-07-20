package hartu.robot.communication.server;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger
{
    private static Logger instance;
    private ClientHandler logClientHandler;
    private final SimpleDateFormat timeFormat; // For HH:mm:ss.SSS

    private Logger()
    {
        this.timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    }

    public static synchronized Logger getInstance()
    {
        if (instance == null)
        {
            instance = new Logger();
        }
        return instance;
    }

    public void setLogClientHandler(ClientHandler handler)
    {
        this.logClientHandler = handler;
        // Log when the log client handler is set, using a tag
        log("LOGGER", "Log client handler set.");
    }

    // New method to send a log message with a tag
    public void log(String tag, String message)
    {
        String timestamp = timeFormat.format(new Date());
        // Format: [HH:mm:ss.SSS] [TAG] message\n
        String formattedMessage = "[" + timestamp + "] [" + tag + "] " + message + "\n";

        if (logClientHandler != null)
        {
            logClientHandler.sendMessage(formattedMessage);
        }
    }

    // Existing log method, now delegates to the tagged version with a default tag
    public void log(String message)
    {
        log("DEFAULT", message); // Use a default tag for existing calls
    }
}

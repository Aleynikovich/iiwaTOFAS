package hartu.robot.communication.server;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger
{
    private static Logger instance;
    private final SimpleDateFormat timeFormat;
    private ClientHandler logClientHandler;

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
        log("LOGGER", "Log client handler set.");
    }

    public void log(String tag, String message)
    {
        String timestamp = timeFormat.format(new Date());
        String formattedMessage = "[" + timestamp + "] [" + tag + "] " + message + "\n";

        if (logClientHandler != null)
        {
            logClientHandler.sendMessage(formattedMessage);
        }
    }

    public void warn(String tag, String message)
    {
        String timestamp = timeFormat.format(new Date());
        String formattedMessage = "[" + timestamp + "] [" + tag + "] " + message + "\n";

        if (logClientHandler != null)
        {
            logClientHandler.sendMessage(formattedMessage);
        }
    }

    public void error(String tag, String message)
    {
        String timestamp = timeFormat.format(new Date());
        String formattedMessage = "[" + timestamp + "] [" + tag + "] " + message + "\n";

        if (logClientHandler != null)
        {
            logClientHandler.sendMessage(formattedMessage);
        }
    }
}

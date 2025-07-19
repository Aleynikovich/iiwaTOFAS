// --- ServerClass.java ---
package hartu.robot.communication.server;
import java.io.*;
import java.net.*;

public class ServerClass implements IClientHandlerCallback
{
    private ServerPortListener taskPortListener;
    private ServerPortListener logPortListener;

    private ClientHandler taskClientHandler;
    private ClientHandler logClientHandler;

    private volatile boolean isLogClientConnected = false;

    public ServerClass(int taskPort, int logPort) throws IOException
    {
        ServerSocket taskServerSocket = new ServerSocket(taskPort);
        // Pass 'this' as the ServerClass instance to the taskPortListener constructor
        this.taskPortListener = new ServerPortListener(taskServerSocket, "Task Listener", this, this);

        ServerSocket logServerSocket = new ServerSocket(logPort);
        // Pass 'this' as the ServerClass instance to the logPortListener constructor
        this.logPortListener = new ServerPortListener(logServerSocket, "Log Listener", this, this);
    }

    public void start()
    {
        Thread taskListenerThread = new Thread(taskPortListener);
        Thread logListenerThread = new Thread(logPortListener);

        taskListenerThread.setDaemon(true);
        logListenerThread.setDaemon(true);

        taskListenerThread.start();
        logListenerThread.start();
    }

    public void stop() throws IOException
    {
        if (taskPortListener != null && taskPortListener.getServerSocket() != null && !taskPortListener.getServerSocket().isClosed())
        {
            taskPortListener.getServerSocket().close();
        }
        if (logPortListener != null && logPortListener.getServerSocket() != null && !logPortListener.getServerSocket().isClosed())
        {
            logPortListener.getServerSocket().close();
        }

        if (taskClientHandler != null) {
            taskClientHandler.close();
        }
        if (logClientHandler != null) {
            logClientHandler.close();
        }
        this.isLogClientConnected = false; // Reset flag on stop
    }

    @Override
    public void onClientConnected(ClientHandler handler, String listenerName)
    {
        if ("Task Listener".equals(listenerName)) {
            this.taskClientHandler = handler;
        } else if ("Log Listener".equals(listenerName)) {
            this.logClientHandler = handler;
            Logger.getInstance().setLogClientHandler(this.logClientHandler);
            this.isLogClientConnected = true; // Set flag when log client connects
        }
    }

    public boolean isLogClientConnected() {
        return isLogClientConnected;
    }

    public void sendHeartbeatToTaskClient(String message) {
        if (taskClientHandler != null) {
            taskClientHandler.sendMessage(message);
        }
    }

    public void sendHeartbeatToLogClient(String message) {
        if (logClientHandler != null) {
            logClientHandler.sendMessage(message);
        }
    }
}

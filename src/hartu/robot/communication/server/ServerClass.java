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

    public ServerClass(int taskPort, int logPort) throws IOException
    {
        ServerSocket taskServerSocket = new ServerSocket(taskPort);
        this.taskPortListener = new ServerPortListener(taskServerSocket, "Task Listener", this);

        ServerSocket logServerSocket = new ServerSocket(logPort);
        this.logPortListener = new ServerPortListener(logServerSocket, "Log Listener", this);
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
    }

    @Override
    public void onClientConnected(ClientHandler handler, String listenerName)
    {
        if ("Task Listener".equals(listenerName)) {
            this.taskClientHandler = handler;
        } else if ("Log Listener".equals(listenerName)) {
            this.logClientHandler = handler;
            Logger.getInstance().setLogClientHandler(this.logClientHandler);
        }
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

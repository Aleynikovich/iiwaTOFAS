package hartu.robot.communication.server;

import hartu.protocols.constants.ProtocolConstants.ListenerType;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerClass implements IClientHandlerCallback
{
    final Map<String, String> clientIpToNameMap;
    final AtomicInteger clientNameCounter;
    private final ServerPortListener taskPortListener;
    private final ServerPortListener logPortListener;
    private ClientHandler taskClientHandler;
    private ClientHandler logClientHandler;
    private volatile boolean isLogClientConnected = false;

    // Added to hold references to the listener threads
    private Thread taskListenerThread;
    private Thread logListenerThread;

    public ServerClass(int taskPort, int logPort) throws IOException
    {
        ServerSocket taskServerSocket = new ServerSocket(taskPort);
        this.taskPortListener = new ServerPortListener(taskServerSocket, ListenerType.TASK_LISTENER, this, this);

        ServerSocket logServerSocket = new ServerSocket(logPort);
        this.logPortListener = new ServerPortListener(logServerSocket, ListenerType.LOG_LISTENER, this, this);

        this.clientIpToNameMap = new ConcurrentHashMap<>();
        this.clientNameCounter = new AtomicInteger(0);
        Logger.getInstance().log("SERVER", "Server initialized on Task Port: " + taskPort + ", Log Port: " + logPort);
    }

    public void start()
    {
        taskListenerThread = new Thread(taskPortListener); // Assign to class member
        logListenerThread = new Thread(logPortListener);   // Assign to class member

        taskListenerThread.setDaemon(true);
        logListenerThread.setDaemon(true);

        taskListenerThread.start();
        logListenerThread.start();
        Logger.getInstance().log("SERVER", "Server listeners started.");
    }

    public void stop() throws IOException
    {
        Logger.getInstance().log("SERVER", "Stopping server listeners and client handlers...");
        // Signal listeners to stop and close their sockets
        if (taskPortListener != null) {
            taskPortListener.stopListening();
        }
        if (logPortListener != null) {
            logPortListener.stopListening();
        }

        // Wait for listener threads to actually terminate
        try {
            if (taskListenerThread != null && taskListenerThread.isAlive()) {
                taskListenerThread.join(2000); // Wait up to 2 seconds for task listener
                if (taskListenerThread.isAlive()) {
                    Logger.getInstance().log("SERVER", "Warning: Task Listener thread did not terminate within timeout.");
                }
            }
            if (logListenerThread != null && logListenerThread.isAlive()) {
                logListenerThread.join(2000); // Wait up to 2 seconds for log listener
                if (logListenerThread.isAlive()) {
                    Logger.getInstance().log("SERVER", "Warning: Log Listener thread did not terminate within timeout.");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore the interrupted status
            Logger.getInstance().log("SERVER", "Interrupted while waiting for listener threads to stop: " + e.getMessage());
        }


        if (taskClientHandler != null)
        {
            taskClientHandler.close();
        }
        if (logClientHandler != null)
        {
            logClientHandler.close();
        }
        this.isLogClientConnected = false;
        Logger.getInstance().log("SERVER", "Server stopped.");
    }

    @Override
    public void onClientConnected(ClientHandler handler, ListenerType listenerType)
    {
        String clientIp = handler.getClientSession().getRemoteAddress();
        String clientName = handler.getClientSession().getClientName();

        if (listenerType == ListenerType.TASK_LISTENER)
        {
            this.taskClientHandler = handler;
        }
        else if (listenerType == ListenerType.LOG_LISTENER)
        {
            this.logClientHandler = handler;
            Logger.getInstance().setLogClientHandler(this.logClientHandler);
            this.isLogClientConnected = true;
        }
        Logger.getInstance().log("SERVER", "Client " + clientName + " (" + clientIp + ") connected to " + listenerType.getName());
    }

    public boolean isLogClientConnected()
    {
        return isLogClientConnected;
    }

    public String getClientName(String ipAddress)
    {
        return clientIpToNameMap.get(ipAddress);
    }
}

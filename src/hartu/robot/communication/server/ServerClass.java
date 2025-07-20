package hartu.robot.communication.server;

import hartu.protocols.constants.ProtocolConstants.ListenerType;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerClass implements IClientHandlerCallback
{
    public final Map<String, String> clientIpToNameMap;
    public final AtomicInteger clientNameCounter;
    private final ServerPortListener taskPortListener;
    private final ServerPortListener logPortListener;
    private ClientHandler taskClientHandler;
    private ClientHandler logClientHandler;
    private volatile boolean isLogClientConnected = false;

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
        Thread taskListenerThread = new Thread(taskPortListener);
        Thread logListenerThread = new Thread(logPortListener);

        taskListenerThread.setDaemon(true);
        logListenerThread.setDaemon(true);

        taskListenerThread.start();
        logListenerThread.start();
        Logger.getInstance().log("SERVER", "Server listeners started.");
    }

    public void stop() throws IOException
    {
        Logger.getInstance().log("SERVER", "Stopping server listeners and client handlers...");
        if (taskPortListener != null && taskPortListener.getServerSocket() != null && !taskPortListener.getServerSocket().isClosed())
        {
            taskPortListener.getServerSocket().close();
        }
        if (logPortListener != null && logPortListener.getServerSocket() != null && !logPortListener.getServerSocket().isClosed())
        {
            logPortListener.getServerSocket().close();
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
        String clientName = handler.getClientSession().getClientName(); // Get the name already assigned by ServerPortListener

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

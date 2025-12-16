package hartu.robot.communication.server;

import hartu.protocols.constants.ProtocolConstants.ListenerType;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simplified server class that handles a single port with a single purpose.
 * This follows the single responsibility principle - one server, one port, one function.
 * <p>
 * For the complete system:
 * - Ros2ServerManager uses this for task commands on port 30001
 * - LoggingServerManager handles log broadcasting on port 30002
 * - JointStateServerManager handles joint state broadcasting on port 30003
 */
public class ServerClass implements IClientHandlerCallback
{
    final Map<String, String> clientIpToNameMap;
    final AtomicInteger clientNameCounter;
    private final ServerPortListener portListener;
    private final ListenerType listenerType;
    private ClientHandler clientHandler;
    private Thread listenerThread;

    /**
     * Creates a server that listens on a single port.
     *
     * @param port         The port number to listen on
     * @param listenerType The type of listener (TASK_LISTENER for command processing)
     * @throws IOException if the server socket cannot be created
     */
    public ServerClass(int port, ListenerType listenerType) throws IOException
    {
        ServerSocket serverSocket = new ServerSocket(port);
        this.listenerType = listenerType;
        this.portListener = new ServerPortListener(serverSocket, listenerType, this, this);

        this.clientIpToNameMap = new ConcurrentHashMap<>();
        this.clientNameCounter = new AtomicInteger(0);

        Logger.getInstance().debug("SERVER", "Server initialized on port " + port + " for " + listenerType.getName());
    }

    public void start()
    {
        listenerThread = new Thread(portListener);
        listenerThread.setDaemon(true);
        listenerThread.start();
        Logger.getInstance().debug("SERVER", "Server listener started for " + listenerType.getName());
    }

    public void stop() throws IOException
    {
        Logger.getInstance().debug("SERVER", "Stopping server listener...");

        if (portListener != null)
        {
            portListener.stopListening();
        }

        // Wait for listener thread to terminate
        try
        {
            if (listenerThread != null && listenerThread.isAlive())
            {
                listenerThread.join(2000);
                if (listenerThread.isAlive())
                {
                    Logger.getInstance().warn("SERVER", "Listener thread did not terminate within timeout");
                }
            }
        } catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            Logger.getInstance().error("SERVER", "Interrupted while waiting for listener thread to stop: " + e.getMessage());
        }

        if (clientHandler != null)
        {
            clientHandler.close();
        }

        Logger.getInstance().debug("SERVER", "Server stopped");
    }

    @Override
    public void onClientConnected(ClientHandler handler, ListenerType listenerType)
    {
        String clientIp = handler.getClientSession().getRemoteAddress();
        String clientName = handler.getClientSession().getClientName();

        this.clientHandler = handler;

        Logger.getInstance().debug("SERVER", "Client " + clientName + " (" + clientIp + ") connected to " + listenerType.getName());
    }

    public String getClientName(String ipAddress)
    {
        return clientIpToNameMap.get(ipAddress);
    }

    /**
     * Gets the currently connected client handler.
     * Used for sending position data to task clients via HMI buttons.
     *
     * @return The client handler, or null if no client is connected
     */
    public ClientHandler getClientHandler()
    {
        return clientHandler;
    }
}

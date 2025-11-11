package hartu.robot.communication.server;

import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import hartu.robot.utils.JointDataFormatter;

import javax.inject.Inject;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Server manager that broadcasts joint state data to connected clients.
 * The robot acts as a server, allowing multiple clients to connect and receive
 * real-time joint position updates.
 */
public class JointStateServerManager extends RoboticsAPICyclicBackgroundTask
{
    private static final int JOINT_STATE_PORT = 30003;
    
    @Inject
    private LBR lbr;
    
    private ServerSocket serverSocket;
    private Thread listenerThread;
    private volatile boolean isRunning = false;
    private final Map<String, ClientConnection> connectedClients = new ConcurrentHashMap<>();

    @Override
    public void initialize()
    {
        initializeCyclic(0, 10, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);
        lbr = getContext().getDeviceFromType(LBR.class);
        
        // Start server listener in a separate thread
        listenerThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    serverSocket = new ServerSocket(JOINT_STATE_PORT);
                    isRunning = true;
                    Logger.getInstance().log("JOINT_STATE_SRV", "Joint State Server started on port " + JOINT_STATE_PORT);
                    
                    while (isRunning)
                    {
                        try
                        {
                            Socket clientSocket = serverSocket.accept();
                            String clientIp = clientSocket.getInetAddress().getHostAddress();
                            Logger.getInstance().log("JOINT_STATE_SRV", "New client connected: " + clientIp);
                            
                            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                            ClientConnection connection = new ClientConnection(clientSocket, writer);
                            connectedClients.put(clientIp, connection);
                        }
                        catch (IOException e)
                        {
                            if (isRunning)
                            {
                                Logger.getInstance().warn("JOINT_STATE_SRV", "Error accepting client connection: " + e.getMessage());
                            }
                        }
                    }
                }
                catch (IOException e)
                {
                    Logger.getInstance().error("JOINT_STATE_SRV", "Error starting Joint State Server: " + e.getMessage());
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
        
        Logger.getInstance().log("JOINT_STATE_SRV", "Joint State Server Manager initialized.");
    }

    @Override
    public void runCyclic()
    {
        if (!isRunning || connectedClients.isEmpty())
        {
            return;
        }
        
        // Get current joint position
        JointPosition currentPosition = lbr.getCurrentJointPosition();
        String message = JointDataFormatter.formatJointPosition(currentPosition);
        
        // Broadcast to all connected clients
        for (Map.Entry<String, ClientConnection> entry : connectedClients.entrySet())
        {
            String clientIp = entry.getKey();
            ClientConnection connection = entry.getValue();
            
            if (connection.isConnected())
            {
                try
                {
                    connection.getWriter().print(message);
                    connection.getWriter().flush();
                }
                catch (Exception e)
                {
                    Logger.getInstance().warn("JOINT_STATE_SRV", "Error sending to client " + clientIp + ": " + e.getMessage());
                    closeClientConnection(clientIp, connection);
                }
            }
            else
            {
                closeClientConnection(clientIp, connection);
            }
        }
    }
    
    private void closeClientConnection(String clientIp, ClientConnection connection)
    {
        try
        {
            connection.close();
            connectedClients.remove(clientIp);
            Logger.getInstance().log("JOINT_STATE_SRV", "Client disconnected: " + clientIp);
        }
        catch (IOException e)
        {
            Logger.getInstance().warn("JOINT_STATE_SRV", "Error closing connection for " + clientIp + ": " + e.getMessage());
        }
    }

    @Override
    public void dispose()
    {
        super.dispose();
        
        isRunning = false;
        
        // Close all client connections
        for (Map.Entry<String, ClientConnection> entry : connectedClients.entrySet())
        {
            String clientIp = entry.getKey();
            ClientConnection connection = entry.getValue();
            try
            {
                connection.close();
            }
            catch (IOException e)
            {
                Logger.getInstance().warn("JOINT_STATE_SRV", "Error closing client " + clientIp + ": " + e.getMessage());
            }
        }
        connectedClients.clear();
        
        // Close server socket
        if (serverSocket != null && !serverSocket.isClosed())
        {
            try
            {
                serverSocket.close();
                Logger.getInstance().log("JOINT_STATE_SRV", "Joint State Server socket closed.");
            }
            catch (IOException e)
            {
                Logger.getInstance().error("JOINT_STATE_SRV", "Error closing server socket: " + e.getMessage());
            }
        }
        
        // Wait for listener thread to finish
        if (listenerThread != null && listenerThread.isAlive())
        {
            try
            {
                listenerThread.join(2000);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                Logger.getInstance().warn("JOINT_STATE_SRV", "Interrupted while waiting for listener thread.");
            }
        }
        
        Logger.getInstance().log("JOINT_STATE_SRV", "Joint State Server Manager disposed.");
    }
    
    /**
     * Inner class to represent a client connection
     */
    private static class ClientConnection
    {
        private final Socket socket;
        private final PrintWriter writer;
        
        public ClientConnection(Socket socket, PrintWriter writer)
        {
            this.socket = socket;
            this.writer = writer;
        }
        
        public PrintWriter getWriter()
        {
            return writer;
        }
        
        public boolean isConnected()
        {
            return socket != null && socket.isConnected() && !socket.isClosed();
        }
        
        public void close() throws IOException
        {
            if (writer != null)
            {
                writer.close();
            }
            if (socket != null && !socket.isClosed())
            {
                socket.close();
            }
        }
    }
}

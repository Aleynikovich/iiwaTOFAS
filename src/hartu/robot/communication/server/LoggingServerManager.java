package hartu.robot.communication.server;

import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Centralized logging server that broadcasts log messages to all connected clients.
 * Runs as a background task and listens on port 30002.
 * 
 * Architecture:
 * - Logger sends messages to this server via a queue
 * - Server broadcasts to all connected network clients (Python log clients)
 * - CommandExecutor connects as a client to receive logs for robot console output
 * 
 * This design allows:
 * - All tasks (background and foreground) to log centrally
 * - Robot console to display all logs (via CommandExecutor's println)
 * - Multiple network clients to receive logs simultaneously
 */
public class LoggingServerManager extends RoboticsAPICyclicBackgroundTask implements LogHandler
{
    private static final int LOG_PORT = 30002;
    private static final int QUEUE_CAPACITY = 10000; // Large queue to prevent message loss
    
    private ServerSocket serverSocket;
    private Thread listenerThread;
    private volatile boolean isRunning = false;
    private final Map<String, LogClientConnection> connectedClients = new ConcurrentHashMap<>();
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private int clientCounter = 0;

    @Override
    public void initialize()
    {
        initializeCyclic(0, 10, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);
        
        // Register this server as a log handler so Logger sends messages here
        Logger.getInstance().addHandler(this);
        
        // Start server listener in a separate thread
        listenerThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    serverSocket = new ServerSocket(LOG_PORT);
                    isRunning = true;
                    System.out.println("[LoggingServerManager] Started on port " + LOG_PORT);
                    
                    while (isRunning)
                    {
                        try
                        {
                            Socket clientSocket = serverSocket.accept();
                            String clientIp = clientSocket.getInetAddress().getHostAddress();
                            String clientId = "LogClient-" + (++clientCounter);
                            
                            System.out.println("[LoggingServerManager] New client connected: " + clientId + " (" + clientIp + ")");
                            
                            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                            LogClientConnection connection = new LogClientConnection(clientSocket, writer, clientId);
                            connectedClients.put(clientId, connection);
                        }
                        catch (IOException e)
                        {
                            if (isRunning)
                            {
                                System.out.println("[LoggingServerManager] Error accepting client: " + e.getMessage());
                            }
                        }
                    }
                }
                catch (IOException e)
                {
                    System.out.println("[LoggingServerManager] Error starting server: " + e.getMessage());
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
        
        System.out.println("[LoggingServerManager] Initialized successfully.");
    }

    @Override
    public void runCyclic()
    {
        try
        {
            // Process messages from queue and broadcast to all clients
            String message = messageQueue.poll(10, TimeUnit.MILLISECONDS);
            if (message != null && !connectedClients.isEmpty())
            {
                broadcastToClients(message);
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        catch (Exception e)
        {
            // Catch any unexpected errors to prevent the cyclic task from stopping
            System.out.println("[LoggingServerManager] Error in runCyclic: " + e.getMessage());
        }
    }
    
    /**
     * Broadcasts a message to all connected clients.
     * Removes disconnected clients automatically.
     */
    private void broadcastToClients(String message)
    {
        for (Map.Entry<String, LogClientConnection> entry : connectedClients.entrySet())
        {
            String clientId = entry.getKey();
            LogClientConnection connection = entry.getValue();
            
            if (connection.isConnected())
            {
                try
                {
                    connection.getWriter().print(message);
                    connection.getWriter().flush();
                    
                    // Check if write failed
                    if (connection.getWriter().checkError())
                    {
                        closeClientConnection(clientId, connection);
                    }
                }
                catch (Exception e)
                {
                    closeClientConnection(clientId, connection);
                }
            }
            else
            {
                closeClientConnection(clientId, connection);
            }
        }
    }
    
    private void closeClientConnection(String clientId, LogClientConnection connection)
    {
        try
        {
            connection.close();
            connectedClients.remove(clientId);
            System.out.println("[LoggingServerManager] Client disconnected: " + clientId);
        }
        catch (IOException e)
        {
            System.out.println("[LoggingServerManager] Error closing connection for " + clientId + ": " + e.getMessage());
        }
    }

    @Override
    public void dispose()
    {
        super.dispose();
        
        isRunning = false;
        
        // Remove this handler from Logger
        Logger.getInstance().removeHandler(this);
        
        // Close all client connections
        for (Map.Entry<String, LogClientConnection> entry : connectedClients.entrySet())
        {
            String clientId = entry.getKey();
            LogClientConnection connection = entry.getValue();
            try
            {
                connection.close();
            }
            catch (IOException e)
            {
                System.out.println("[LoggingServerManager] Error closing client " + clientId + ": " + e.getMessage());
            }
        }
        connectedClients.clear();
        
        // Close server socket
        if (serverSocket != null && !serverSocket.isClosed())
        {
            try
            {
                serverSocket.close();
                System.out.println("[LoggingServerManager] Server socket closed.");
            }
            catch (IOException e)
            {
                System.out.println("[LoggingServerManager] Error closing server socket: " + e.getMessage());
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
            }
        }
        
        System.out.println("[LoggingServerManager] Disposed.");
    }
    
    // LogHandler implementation
    
    @Override
    public void sendMessage(String formattedMessage)
    {
        // Add message to queue for broadcasting
        // Non-blocking - if queue is full, drop oldest messages
        if (!messageQueue.offer(formattedMessage))
        {
            // Queue full - remove oldest and try again
            messageQueue.poll();
            messageQueue.offer(formattedMessage);
        }
    }
    
    @Override
    public boolean isActive()
    {
        return isRunning;
    }
    
    @Override
    public void close()
    {
        // Called when removed from Logger
        isRunning = false;
    }
    
    /**
     * Inner class to represent a log client connection
     */
    private static class LogClientConnection
    {
        private final Socket socket;
        private final PrintWriter writer;
        private final String clientId;
        
        public LogClientConnection(Socket socket, PrintWriter writer, String clientId)
        {
            this.socket = socket;
            this.writer = writer;
            this.clientId = clientId;
        }
        
        public PrintWriter getWriter()
        {
            return writer;
        }
        
        public String getClientId()
        {
            return clientId;
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

package hartu.communication.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit; // Import TimeUnit

public class LogServer extends AbstractServer
{

    private final List<LogClientHandler> connectedHandlers;
    private final BlockingQueue<String> logMessageQueue;
    private Thread dispatcherThread;

    public LogServer(int port)
    {
        super(port);
        this.connectedHandlers = new CopyOnWriteArrayList<>();
        this.logMessageQueue = new LinkedBlockingQueue<>();
    }

    @Override
    protected String getServerName()
    {
        return "LogServer";
    }

    @Override
    public void start()
    {
        if (isRunning)
        {
            System.out.println(getServerName() + " is already running on port " + port);
            return;
        }

        try
        {
            serverSocket = new ServerSocket(port);
            isRunning = true;
            System.out.println(getServerName() + " started on port " + port + " (" + getInetAddress().getHostAddress() + ")");

            dispatcherThread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    dispatchLogMessages();
                }
            }, "LogDispatcherThread");
            dispatcherThread.start();

            while (isRunning)
            {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Log Client connected to " + getServerName() + " from: " + clientSocket.getInetAddress().getHostAddress());
                handleClient(clientSocket);
            }

        } catch (IOException e)
        {
            if (isRunning)
            {
                System.err.println(getServerName() + " error: " + e.getMessage());
            } else
            {
                System.out.println(getServerName() + " shut down normally.");
            }
        } finally
        {
            stop();
        }
    }

    @Override
    public void stop()
    {
        if (!isRunning)
        {
            System.out.println(getServerName() + " is not running.");
            return;
        }
        isRunning = false;
        try
        {
            if (dispatcherThread != null)
            {
                dispatcherThread.interrupt();
                dispatcherThread.join(1000); // Wait for dispatcher to finish
            }
            if (serverSocket != null && !serverSocket.isClosed())
            {
                serverSocket.close();
                System.out.println(getServerName() + " stopped.");
            }
            for (LogClientHandler handler : connectedHandlers)
            {
                handler.disconnect();
            }
            connectedHandlers.clear();
        } catch (IOException e)
        {
            System.err.println("Error closing " + getServerName() + " socket: " + e.getMessage());
        } catch (InterruptedException e) { // Catch InterruptedException for join
            Thread.currentThread().interrupt();
            System.err.println("LogServer: Interruption during dispatcher thread join: " + e.getMessage());
        }
    }

    @Override
    protected void handleClient(Socket clientSocket)
    {
        LogClientHandler handler = new LogClientHandler(clientSocket, this);
        connectedHandlers.add(handler);
        handler.start();
    }

    /**
     * Publishes a message to all connected clients, prepending a timestamp.
     * Messages received from clients via LogClientHandler are also published here.
     * @param message The raw message to publish.
     */
    public void publish(String message)
    {
        if (isRunning)
        {
            try
            {
                // Use the formatLogMessage method from AbstractServer
                String timestampedMessage = formatLogMessage(message);
                logMessageQueue.put(timestampedMessage);
            } catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                System.err.println("LogServer: Interrupted while queuing log message: " + e.getMessage());
            }
        } else
        {
            System.err.println("LogServer: Cannot publish, server is not running: " + message);
        }
    }

    private void dispatchLogMessages()
    {
        while (!Thread.currentThread().isInterrupted())
        {
            try
            {
                String message = logMessageQueue.take(); // Blocks until a message is available
                for (LogClientHandler handler : connectedHandlers)
                {
                    // Check if handler is still active and connected before sending
                    if (handler.isClientConnected())
                    {
                        handler.sendMessage(message);
                    } else
                    {
                        // Remove disconnected handlers to avoid trying to send to them
                        connectedHandlers.remove(handler);
                        System.out.println("LogServer: Cleaned up disconnected handler for client: " + handler.getClientAddress());
                    }
                }
            } catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                System.out.println("LogServer dispatcher interrupted.");
                break;
            } catch (Exception e)
            {
                System.err.println("LogServer dispatcher error: " + e.getMessage());
            }
        }
    }

    protected void removeHandler(LogClientHandler handler)
    {
        connectedHandlers.remove(handler);
        System.out.println("LogServer: Handler removed for client: " + handler.getClientAddress());
    }
}
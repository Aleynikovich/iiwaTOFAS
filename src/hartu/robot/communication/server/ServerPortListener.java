package hartu.robot.communication.server;

import hartu.protocols.constants.ProtocolConstants;
import hartu.protocols.constants.ProtocolConstants.ListenerType;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class ServerPortListener implements Runnable
{
    private final ServerSocket serverSocket;
    private final ListenerType listenerType;
    private final IClientHandlerCallback clientHandlerCallback;
    private final ServerClass serverInstance;
    private volatile boolean isRunning = true;

    public ServerPortListener(ServerSocket serverSocket, ListenerType listenerType, IClientHandlerCallback callback, ServerClass serverInstance)
    {
        this.serverSocket = serverSocket;
        this.listenerType = listenerType;
        this.clientHandlerCallback = callback;
        this.serverInstance = serverInstance;
    }

    public ServerSocket getServerSocket()
    {
        return serverSocket;
    }

    public void stopListening()
    {
        isRunning = false;
        try
        {
            if (!serverSocket.isClosed())
            {
                serverSocket.close();
            }
        }
        catch (IOException e)
        {
            Logger.getInstance().log("COMM", listenerType.getName() + ": Error closing server socket during shutdown: " + e.getMessage());
        }
    }

    @Override
    public void run()
    {
        Logger.getInstance().log("COMM", listenerType.getName() + " started listening on port " + serverSocket.getLocalPort());

        try (ServerSocket ss = this.serverSocket)
        {
            while (isRunning)
            {
                Logger.getInstance().log("COMM", listenerType.getName() + ": Waiting for a new client to connect...");
                Socket clientSocket;
                try
                {
                    clientSocket = ss.accept();
                }
                catch (IOException e)
                {
                    if (!isRunning)
                    {
                        Logger.getInstance().log("COMM", listenerType.getName() + ": Server socket closed, listener shutting down gracefully.");
                        break;
                    }
                    else
                    {
                        Logger.getInstance().log("COMM", listenerType.getName() + ": Listener I/O error (unexpected): " + e.getMessage());
                        continue;
                    }
                }

                if (listenerType == ListenerType.TASK_LISTENER)
                {
                    if (!serverInstance.isLogClientConnected())
                    {
                        Logger.getInstance().log("COMM", listenerType.getName() + ": Task client connection from " + clientSocket.getInetAddress().getHostAddress() + " rejected. Log client not connected.");
                        try
                        {
                            clientSocket.close();
                        }
                        catch (IOException e)
                        {
                            Logger.getInstance().log("COMM", listenerType.getName() + ": Error closing rejected task client socket: " + e.getMessage());
                        }
                        try
                        {
                            TimeUnit.MILLISECONDS.sleep(1000);
                        }
                        catch (InterruptedException ie)
                        {
                            Thread.currentThread().interrupt();
                            Logger.getInstance().log("COMM", listenerType.getName() + ": Listener interrupted during sleep.");
                        }
                        continue;
                    }
                }

                String clientIp = clientSocket.getInetAddress().getHostAddress();
                String clientName = serverInstance.clientIpToNameMap.get(clientIp);

                if (clientName == null)
                {
                    clientName = "Client-" + serverInstance.clientNameCounter.incrementAndGet();
                    serverInstance.clientIpToNameMap.put(clientIp, clientName);
                }

                Logger.getInstance().log("COMM", listenerType.getName() + ": Client " + clientName + " (" + clientIp + ") connected.");

                ClientSession session = new ClientSession(clientSocket, listenerType, clientName);
                ClientHandler handler = new ClientHandler(session);
                Thread handlerThread = new Thread(handler);
                handlerThread.setDaemon(true);
                handlerThread.start();

                if (clientHandlerCallback != null)
                {
                    clientHandlerCallback.onClientConnected(handler, listenerType);
                }

                if (listenerType == ListenerType.TASK_LISTENER)
                {
                    handler.sendMessage(ProtocolConstants.INITIAL_TASK_CLIENT_RESPONSE);
                    Logger.getInstance().log("COMM", "Sent '" + ProtocolConstants.INITIAL_TASK_CLIENT_RESPONSE + "' to new task client " + clientName + " (" + clientIp + ")");
                }
            }
        }
        catch (IOException e)
        {
            Logger.getInstance().log("COMM", listenerType.getName() + ": Listener I/O error (initialization or unexpected): " + e.getMessage());
        }
        finally
        {
            if (!serverSocket.isClosed())
            {
                try
                {
                    serverSocket.close();
                }
                catch (IOException e)
                {
                    Logger.getInstance().log("COMM", listenerType.getName() + ": Error closing server socket in finally block: " + e.getMessage());
                }
            }
        }
    }
}

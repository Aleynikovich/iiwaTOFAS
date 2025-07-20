package hartu.robot.communication.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class ServerPortListener implements Runnable
{
    private final ServerSocket serverSocket;
    private final String listenerName;
    private final IClientHandlerCallback clientHandlerCallback;
    private final ServerClass serverInstance;

    public ServerPortListener(ServerSocket serverSocket, String listenerName, IClientHandlerCallback callback, ServerClass serverInstance)
    {
        this.serverSocket = serverSocket;
        this.listenerName = listenerName;
        this.clientHandlerCallback = callback;
        this.serverInstance = serverInstance;
    }

    public ServerSocket getServerSocket()
    {
        return serverSocket;
    }

    @Override
    public void run()
    {
        Logger.getInstance().log(listenerName + " started listening on port " + serverSocket.getLocalPort());

        try (ServerSocket ss = this.serverSocket)
        {
            do
            {
                Logger.getInstance().log(listenerName + ": Waiting for a new client to connect...");
                Socket clientSocket = ss.accept();

                if ("Task Listener".equals(listenerName))
                {
                    if (!serverInstance.isLogClientConnected())
                    {
                        Logger.getInstance().log(listenerName + ": Task client connection from " + clientSocket.getInetAddress().getHostAddress() + " rejected. Log client not connected.");
                        try
                        {
                            clientSocket.close();
                        } catch (IOException e)
                        {
                            Logger.getInstance().log(listenerName + ": Error closing rejected task client socket: " + e.getMessage());
                        }
                        try
                        {
                            TimeUnit.MILLISECONDS.sleep(1000);
                        } catch (InterruptedException ie)
                        {
                            Thread.currentThread().interrupt();
                            Logger.getInstance().log(listenerName + ": Listener interrupted during sleep.");
                        }
                        continue;
                    }
                }

                Logger.getInstance().log(listenerName + ": Client connected from: " + clientSocket.getInetAddress().getHostAddress());

                ClientHandler handler = new ClientHandler(clientSocket, listenerName);
                Thread handlerThread = new Thread(handler);
                handlerThread.setDaemon(true);
                handlerThread.start();

                if (clientHandlerCallback != null)
                {
                    clientHandlerCallback.onClientConnected(handler, listenerName);
                }

                if ("Task Listener".equals(listenerName))
                {
                    handler.sendMessage("FREE|0#");
                    Logger.getInstance().log("Sent 'FREE|0#' to new task client " + clientSocket.getInetAddress().getHostAddress());
                }
            } while (true);
        } catch (IOException e)
        {
            Logger.getInstance().log(listenerName + ": Listener I/O error (server shutting down or port issue): " + e.getMessage());
        }
    }
}

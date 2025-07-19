// --- ServerPortListener.java ---
package hartu.robot.communication.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerPortListener implements Runnable
{
    private ServerSocket serverSocket;
    private String listenerName;
    private IClientHandlerCallback clientHandlerCallback; // New field for callback

    // Constructor now takes a callback interface
    public ServerPortListener(ServerSocket serverSocket, String listenerName, IClientHandlerCallback callback)
    {
        this.serverSocket = serverSocket;
        this.listenerName = listenerName;
        this.clientHandlerCallback = callback; // Store the callback
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    @Override
    public void run()
    {
        Logger.getInstance().log(listenerName + " started listening on port " + serverSocket.getLocalPort());

        try (ServerSocket ss = this.serverSocket)
        {
            while (true)
            {
                Logger.getInstance().log(listenerName + ": Waiting for a client to connect...");
                Socket clientSocket = ss.accept();
                Logger.getInstance().log(listenerName + ": Client connected from: " + clientSocket.getInetAddress().getHostAddress());

                ClientHandler handler = new ClientHandler(clientSocket);
                Thread handlerThread = new Thread(handler);
                handlerThread.setDaemon(true);
                handlerThread.start();


                if (clientHandlerCallback != null) {
                    clientHandlerCallback.onClientConnected(handler, listenerName);
                }
            }
        }
        catch (IOException e)
        {
            Logger.getInstance().log(listenerName + ": Listener error on port " + serverSocket.getLocalPort() + ": " + e.getMessage());
            throw new RuntimeException("Listener error on port " + serverSocket.getLocalPort() + ": " + e.getMessage(), e);
        }
    }

}

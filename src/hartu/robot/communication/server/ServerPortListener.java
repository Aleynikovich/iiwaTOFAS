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
        try (ServerSocket ss = this.serverSocket)
        {
            while (true)
            {
                Socket clientSocket = ss.accept();
                ClientHandler handler = new ClientHandler(clientSocket); // Create ClientHandler
                Thread handlerThread = new Thread(handler); // Create a new thread for the handler
                handlerThread.setDaemon(true); // Mark as daemon
                handlerThread.start(); // Start the handler thread

                // Use the callback to notify ServerClass about the new handler
                if (clientHandlerCallback != null) {
                    clientHandlerCallback.onClientConnected(handler, listenerName);
                }
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException("Listener error on port " + serverSocket.getLocalPort() + ": " + e.getMessage(), e);
        }
    }
}

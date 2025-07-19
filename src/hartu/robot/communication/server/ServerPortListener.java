// --- ServerPortListener.java ---
package hartu.robot.communication.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit; // Import for sleep

public class ServerPortListener implements Runnable
{
    private ServerSocket serverSocket;
    private String listenerName;
    private IClientHandlerCallback clientHandlerCallback;
    private ServerClass serverInstance; // New field to hold reference to ServerClass

    // Constructor now takes ServerClass instance
    public ServerPortListener(ServerSocket serverSocket, String listenerName, IClientHandlerCallback callback, ServerClass serverInstance)
    {
        this.serverSocket = serverSocket;
        this.listenerName = listenerName;
        this.clientHandlerCallback = callback;
        this.serverInstance = serverInstance; // Store the ServerClass instance
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
                Logger.getInstance().log(listenerName + ": Waiting for a new client to connect...");
                Socket clientSocket = ss.accept(); // Accept the connection first

                // Check if this is the Task Listener and if the Log Client is connected
                if ("Task Listener".equals(listenerName)) {
                    if (!serverInstance.isLogClientConnected()) {
                        Logger.getInstance().log(listenerName + ": Task client connection from " + clientSocket.getInetAddress().getHostAddress() + " rejected. Log client not connected.");
                        try {
                            clientSocket.close(); // Close the rejected socket
                        } catch (IOException e) {
                            Logger.getInstance().log(listenerName + ": Error closing rejected task client socket: " + e.getMessage());
                        }
                        // Optionally, add a small delay before the next accept to prevent busy-looping on rapid rejections
                        try {
                            TimeUnit.MILLISECONDS.sleep(100);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt(); // Restore interrupt status
                            Logger.getInstance().log(listenerName + ": Listener interrupted during sleep.");
                        }
                        continue; // Go back to waiting for the next client
                    }
                }

                // If we reach here, either it's a Log Listener, or it's a Task Listener and log client is connected
                Logger.getInstance().log(listenerName + ": Client connected from: " + clientSocket.getInetAddress().getHostAddress());

                ClientHandler handler = new ClientHandler(clientSocket, listenerName);
                Thread handlerThread = new Thread(handler);
                handlerThread.setDaemon(true);
                handlerThread.start();

                if (clientHandlerCallback != null) {
                    clientHandlerCallback.onClientConnected(handler, listenerName);
                }

                // Send FREE|0# if this is the Task Listener
                if ("Task Listener".equals(listenerName)) {
                    handler.sendMessage("FREE|0#");
                    Logger.getInstance().log("Sent 'FREE|0#' to new task client " + clientSocket.getInetAddress().getHostAddress()+"\n");
                }
            }
        }
        catch (IOException e)
        {
            Logger.getInstance().log(listenerName + ": Listener I/O error (server shutting down or port issue): " + e.getMessage());
        }
    }
}

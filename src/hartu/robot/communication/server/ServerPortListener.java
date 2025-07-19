// --- ServerPortListener.java ---
package hartu.robot.communication.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerPortListener implements Runnable
{
    private ServerSocket serverSocket;
    private String listenerName;
    private IClientHandlerCallback clientHandlerCallback;

    public ServerPortListener(ServerSocket serverSocket, String listenerName, IClientHandlerCallback callback)
    {
        this.serverSocket = serverSocket;
        this.listenerName = listenerName;
        this.clientHandlerCallback = callback;
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

                if ("Task Listener".equals(listenerName)) {
                    handler.sendMessage("#FREE");
                    Logger.getInstance().log("ServerPortListener (" + listenerName + "): Sent '#FREE' to new task client " + clientSocket.getInetAddress().getHostAddress());
                }
            }
        }
        catch (IOException e)
        {
            // Log the error through the Logger.
            // This IOException is expected when serverSocket.close() is called
            // (e.g., from ServerClass.stop()), causing accept() to throw.
            // We log it and allow the thread to terminate gracefully.
            Logger.getInstance().log(listenerName + ": Listener I/O error (server shutting down or port issue): " + e.getMessage());
            // *** IMPORTANT CHANGE: Removed 'throw new RuntimeException(e);' ***
            // Allow the thread to terminate gracefully after logging.
        }
        // No finally block needed here for ServerSocket as try-with-resources handles closing
        // and any RuntimeException would still allow the thread to terminate.
    }
}

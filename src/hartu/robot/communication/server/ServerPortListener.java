// --- ServerPortListener.java ---
package hartu.robot.communication.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerPortListener implements Runnable
{
    private ServerSocket serverSocket;
    private String listenerName;

    public ServerPortListener(ServerSocket serverSocket, String listenerName)
    {
        this.serverSocket = serverSocket;
        this.listenerName = listenerName;
    }

    @Override
    public void run()
    {
        try (ServerSocket ss = this.serverSocket)
        {
            while (true)
            {
                Socket clientSocket = ss.accept();
                // This is where we will eventually hand off clientSocket
                // to a ClientHandler in its own thread.
                // For now, we'll just close it immediately to allow the listener
                // to accept the next connection without blocking indefinitely.
                clientSocket.close();
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException("Listener error on port " + serverSocket.getLocalPort() + ": " + e.getMessage(), e);
        }
    }

    public ServerSocket getServerSocket() { return serverSocket; }
}

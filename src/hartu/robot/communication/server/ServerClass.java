// --- ServerClass.java ---
package hartu.robot.communication.server;
import java.io.*;
import java.net.*;

public class ServerClass
{
    private ServerPortListener taskPortListener;
    private ServerPortListener logPortListener;

    public ServerClass(int taskPort, int logPort) throws IOException
    {
        ServerSocket taskServerSocket = new ServerSocket(taskPort);
        this.taskPortListener = new ServerPortListener(taskServerSocket, "Task Listener");

        ServerSocket logServerSocket = new ServerSocket(logPort);
        this.logPortListener = new ServerPortListener(logServerSocket, "Log Listener");
    }

    public void start()
    {
        Thread taskListenerThread = new Thread(taskPortListener);
        Thread logListenerThread = new Thread(logPortListener);

        taskListenerThread.setDaemon(true);
        logListenerThread.setDaemon(true);

        taskListenerThread.start();
        logListenerThread.start();
    }

    // New stop method for graceful shutdown
    public void stop() throws IOException
    {
        // Close the ServerSockets via their listeners
        // This will cause the accept() calls in ServerPortListener.run() to throw an IOException
        // and the listener threads should then terminate.
        if (taskPortListener != null && taskPortListener.getServerSocket() != null && !taskPortListener.getServerSocket().isClosed())
        {
            taskPortListener.getServerSocket().close();
        }
        if (logPortListener != null && logPortListener.getServerSocket() != null && !logPortListener.getServerSocket().isClosed())
        {
            logPortListener.getServerSocket().close();
        }
    }

}

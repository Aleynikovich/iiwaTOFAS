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

}

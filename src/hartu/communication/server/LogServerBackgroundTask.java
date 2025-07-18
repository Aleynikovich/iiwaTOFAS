// File: hartu/communication/server/LogServerBackgroundTask.java
package hartu.communication.server;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;
import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.controllerModel.Controller;

public class LogServerBackgroundTask extends RoboticsAPICyclicBackgroundTask {

    @Inject
    private Controller controller;

    private LogServer logServer;
    private Thread serverListenThread;

    private final int LOG_SERVER_PORT = 30003;

    @Override
    public void initialize() {
        initializeCyclic(0, 500, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);

        logServer = new LogServer(LOG_SERVER_PORT);
        System.out.println("LogServerBackgroundTask: LogServer instance created.");
    }

    @Override
    public void runCyclic() {
        if (serverListenThread == null || !serverListenThread.isAlive() || !logServer.isRunning()) {
            System.out.println("LogServerBackgroundTask: LogServer thread not running or server stopped. Attempting restart...");

            if (serverListenThread != null && serverListenThread.isAlive()) {
                serverListenThread.interrupt(); // Try to interrupt if it's stuck
                try {
                    serverListenThread.join(1000); // Give it a moment to die
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Create a new thread to run the blocking server start method
            serverListenThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        logServer.start();
                    } catch (Exception e) {
                        System.err.println("LogServerBackgroundTask: Error during LogServer.start(): " + e.getMessage());
                    }
                }
            }, "LogServerListenThread");

            serverListenThread.start();
            System.out.println("LogServerBackgroundTask: LogServer listening thread (re)started on port " + LOG_SERVER_PORT);
        }
    }

    @Override
    public void dispose() {
        System.out.println("LogServerBackgroundTask: dispose() called. Stopping LogServer...");
        if (logServer != null && logServer.isRunning()) {
            logServer.stop();

            try {
                if (serverListenThread != null) {
                    serverListenThread.join(5000);
                    if (serverListenThread.isAlive()) {
                        System.err.println("LogServerBackgroundTask: LogServer thread did not terminate within timeout.");
                        serverListenThread.interrupt();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("LogServerBackgroundTask: Interrupted while waiting for LogServer thread to stop.");
            }
        }
        System.out.println("LogServerBackgroundTask: LogServer stopped.");
        super.dispose();
    }

    public LogServer getLogServer() {
        return logServer;
    }
}
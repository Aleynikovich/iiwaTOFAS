// File: hartu/communication/server/LogServerBackgroundTask.java
package hartu.communication.server.background;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;
import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.controllerModel.Controller;
import hartu.communication.server.LogServer;

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

        serverListenThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    logServer.start();
                } catch (Exception e) {
                    System.err.println("LogServerBackgroundTask: Error starting LogServer: " + e.getMessage());
                }
            }
        }, "LogServerListenThread");

        if (!serverListenThread.isAlive()) {
            serverListenThread.start();
            System.out.println("LogServerBackgroundTask: LogServer listening thread started on port " + LOG_SERVER_PORT);
        } else {
            System.out.println("LogServerBackgroundTask: LogServer listening thread already alive.");
        }
    }

    @Override
    public void runCyclic() {
    }

    @Override
    public void dispose() {
        System.out.println("LogServerBackgroundTask: dispose() called. Stopping LogServer...");
        if (logServer != null && logServer.isRunning()) {
            logServer.stop();

            try {
                serverListenThread.join(5000);
                if (serverListenThread.isAlive()) {
                    System.err.println("LogServerBackgroundTask: LogServer thread did not terminate within timeout.");
                    serverListenThread.interrupt();
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
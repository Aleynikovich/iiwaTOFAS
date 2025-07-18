package hartu.communication.server;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;
import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.controllerModel.Controller;

public class ExecutionLogServerBackgroundTask extends RoboticsAPICyclicBackgroundTask {

    @Inject
    private Controller controller;

    private LogServer executionLogServer;
    private Thread serverListenThread;

    private final int EXECUTION_LOG_SERVER_PORT = 30004; // This must match the port used by HartuServerBackgroundTask

    @Override
    public void initialize() {
        initializeCyclic(0, 500, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);

        executionLogServer = new LogServer(EXECUTION_LOG_SERVER_PORT);
        getLogger().info("ExecutionLogServerBackgroundTask: ExecutionLogServer instance created on port " + EXECUTION_LOG_SERVER_PORT + ".");
    }

    @Override
    public void runCyclic() {
        if (serverListenThread == null || !serverListenThread.isAlive()) {
            getLogger().info("ExecutionLogServerBackgroundTask: ExecutionLogServer listening thread is not running. Attempting to (re)start...");

            if (serverListenThread != null) {
                serverListenThread.interrupt();
                try {
                    serverListenThread.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    getLogger().error("ExecutionLogServerBackgroundTask: Interrupted while waiting for old ExecutionLogServer thread to stop: " + e.getMessage());
                }
            }

            serverListenThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        executionLogServer.start();
                    } catch (Exception e) {
                        getLogger().error("ExecutionLogServerBackgroundTask: Error during ExecutionLogServer.start(): " + e.getMessage());
                    }
                }
            }, "ExecutionLogServerListenThread");

            serverListenThread.start();
            getLogger().info("ExecutionLogServerBackgroundTask: ExecutionLogServer listening thread (re)started.");
        }
    }

    @Override
    public void dispose() {
        getLogger().info("ExecutionLogServerBackgroundTask: dispose() called. Stopping ExecutionLogServer...");

        if (executionLogServer != null) {
            executionLogServer.stop();

            try {
                if (serverListenThread != null) {
                    serverListenThread.join(5000);
                    if (serverListenThread.isAlive()) {
                        getLogger().error("ExecutionLogServerBackgroundTask: ExecutionLogServer thread did not terminate within timeout. Forcing interrupt.");
                        serverListenThread.interrupt();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                getLogger().error("ExecutionLogServerBackgroundTask: Interrupted while waiting for ExecutionLogServer thread to stop.");
            }
        }
        getLogger().info("ExecutionLogServerBackgroundTask: ExecutionLogServer stopped.");
        super.dispose();
    }

    /**
     * Provides access to the ExecutionLogServer instance.
     * @return The LogServer instance managed by this background task.
     */
    public LogServer getExecutionLogServer() {
        return executionLogServer;
    }
}
package hartu.communication.server;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.controllerModel.Controller;

import hartu.communication.client.LoggerClient;

public class HartuServerBackgroundTask extends RoboticsAPICyclicBackgroundTask {

    @Inject
    private Controller controller;

    @Inject
    private LogServerBackgroundTask logServerBackgroundTask;

    private HartuServer hartuServer;
    private Thread serverListenThread;

    private LoggerClient protocolLoggerClient;
    private LoggerClient executionLoggerClient;
    private BlockingQueue<String> rawMessageQueue;

    private final int HARTU_SERVER_PORT = 30001;
    private final int PROTOCOL_LOG_PORT = 30003;
    // private final int EXECUTION_LOG_PORT = 30004; // REMOVED: This constant is not needed here.

    private final String LOG_SERVER_ADDRESS = "127.0.0.1"; // Assuming localhost on the controller

    @Override
    public void initialize() {
        getLogger().info("HartuServerBackgroundTask: Entering initialize() method.");
        initializeCyclic(0, 500, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);

        try {
            Thread.sleep(2000);

            getLogger().info("HartuServerBackgroundTask: Attempting to get LogServer instance from LogServerBackgroundTask.");
            LogServer protocolLogServerInstance = logServerBackgroundTask.getLogServer();
            if (protocolLogServerInstance == null) {
                getLogger().error("HartuServerBackgroundTask: CRITICAL ERROR: LogServer instance is null from LogServerBackgroundTask. Cannot establish protocol logger. Check LogServerBackgroundTask initialization.");
                throw new IllegalStateException("LogServer not initialized by LogServerBackgroundTask.");
            }
            getLogger().info("HartuServerBackgroundTask: LogServer instance obtained. Proceeding to connect protocol logger client.");

            getLogger().info("HartuServerBackgroundTask: Sleeping for 2 seconds to allow LogServer to fully start.");
 
            getLogger().info("HartuServerBackgroundTask: Creating Protocol Logger Client for port " + PROTOCOL_LOG_PORT + ".");
            protocolLoggerClient = new LoggerClient(LOG_SERVER_ADDRESS, PROTOCOL_LOG_PORT);
            getLogger().info("HartuServerBackgroundTask: Attempting to connect Protocol Logger Client.");
            
            protocolLoggerClient.connect(); 
            if (!protocolLoggerClient.isConnected()) {
                getLogger().error("HartuServerBackgroundTask: Failed to connect Protocol Logger Client to LogServer on port " + PROTOCOL_LOG_PORT + ". Check LogServer status and network configuration.");
                throw new RuntimeException("Failed to connect Protocol Logger Client for HartuServer.");
            }
            protocolLoggerClient.sendMessage("HartuServerBackgroundTask: Protocol Logger Client connected successfully to LogServer on port " + PROTOCOL_LOG_PORT + ".");
            getLogger().info("HartuServerBackgroundTask: Protocol Logger Client reports connected.");


            // The EXECUTION_LOG_PORT is now defined directly where the client is instantiated.
            // This is cleaner as HartuServerBackgroundTask's only role for this client is to create it.
            getLogger().info("HartuServerBackgroundTask: Creating Execution Logger Client for port 30004."); // Explicitly using 30004 here
            executionLoggerClient = new LoggerClient(LOG_SERVER_ADDRESS, 30004); // Using literal 30004
            getLogger().info("HartuServerBackgroundTask: Attempting to connect Execution Logger Client.");
            
            executionLoggerClient.connect();
            if (!executionLoggerClient.isConnected()) {
                protocolLoggerClient.sendMessage("HartuServerBackgroundTask: ERROR: Failed to connect Execution Logger Client to LogServer on port 30004.");
                getLogger().error("HartuServerBackgroundTask: Failed to connect Execution Logger Client to LogServer on port 30004. Check LogServer status and network configuration.");
                throw new RuntimeException("Failed to connect Execution Logger Client for HartuServer.");
            }
            protocolLoggerClient.sendMessage("HartuServerBackgroundTask: Execution Logger Client connected successfully to LogServer on port 30004.");
            getLogger().info("HartuServerBackgroundTask: Execution Logger Client reports connected.");

            getLogger().info("HartuServerBackgroundTask: Initializing raw message queue.");
            rawMessageQueue = new LinkedBlockingQueue<>();

            getLogger().info("HartuServerBackgroundTask: Creating HartuServer instance.");
            hartuServer = new HartuServer(HARTU_SERVER_PORT, protocolLoggerClient, rawMessageQueue);
            protocolLoggerClient.sendMessage("HartuServerBackgroundTask: HartuServer instance created, linked to loggers and raw message queue.");
            getLogger().info("HartuServerBackgroundTask: Exiting initialize() method successfully.");

        } catch (Exception e) {
            getLogger().error("HartuServerBackgroundTask: FATAL ERROR during HartuServerBackgroundTask initialization: " + e.getMessage(), e);
            throw new IllegalStateException("Failed to initialize HartuServerBackgroundTask due to an unexpected error.", e);
        }
    }

    @Override
    public void runCyclic() {
        if (serverListenThread == null || !serverListenThread.isAlive()) {
            if (protocolLoggerClient == null) {
                getLogger().error("HartuServerBackgroundTask: protocolLoggerClient is null in runCyclic(). Initialization might have failed. Cannot log further.");
                return;
            }
            protocolLoggerClient.sendMessage("HartuServerBackgroundTask: HartuServer thread not running or has died. Attempting restart...");

            if (serverListenThread != null) {
                serverListenThread.interrupt();
                try {
                    serverListenThread.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    protocolLoggerClient.sendMessage("HartuServerBackgroundTask: Interrupted while waiting for old HartuServer thread to stop: " + e.getMessage());
                }
            }

            try {
                protocolLoggerClient.sendMessage("HartuServerBackgroundTask: Waiting 5 seconds before attempting server restart...");
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                protocolLoggerClient.sendMessage("HartuServerBackgroundTask: Interrupted during pre-restart delay. Aborting restart attempt.");
                return;
            }

            serverListenThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        hartuServer.start();
                    } catch (Exception e) {
                        protocolLoggerClient.sendMessage("HartuServerBackgroundTask: ERROR: HartuServer.start() failed: " + e.getMessage());
                    }
                }
            }, "HartuServerListenThread");

            serverListenThread.start();
            protocolLoggerClient.sendMessage("HartuServerBackgroundTask: HartuServer listening thread (re)started on port " + HARTU_SERVER_PORT);
        }
    }

    @Override
    public void dispose() {
        if (protocolLoggerClient != null) {
            protocolLoggerClient.sendMessage("HartuServerBackgroundTask: dispose() called. Stopping HartuServer...");
        } else {
            getLogger().warn("HartuServerBackgroundTask: protocolLoggerClient is null during dispose. Cannot log shutdown messages fully.");
        }

        if (hartuServer != null) {
            hartuServer.stop();

            try {
                if (serverListenThread != null) {
                    serverListenThread.join(5000);
                    if (serverListenThread.isAlive()) {
                        if (protocolLoggerClient != null) protocolLoggerClient.sendMessage("HartuServerBackgroundTask: HartuServer thread did not terminate within timeout. Forcing interrupt.");
                        serverListenThread.interrupt();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (protocolLoggerClient != null) protocolLoggerClient.sendMessage("HartuServerBackgroundTask: Interrupted while waiting for HartuServer thread to stop.");
            }
        }
        if (protocolLoggerClient != null) {
            protocolLoggerClient.disconnect();
        }
        if (executionLoggerClient != null) {
            executionLoggerClient.disconnect();
        }
        if (protocolLoggerClient != null) {
            protocolLoggerClient.sendMessage("HartuServerBackgroundTask: HartuServer stopped.");
        }
        super.dispose();
    }

    public BlockingQueue<String> getRawMessageQueue() {
        return rawMessageQueue;
    }

    public LoggerClient getExecutionLoggerClient() {
        return executionLoggerClient;
    }
}
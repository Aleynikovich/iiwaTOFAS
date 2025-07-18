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
    private final int EXECUTION_LOG_PORT = 30004; // Still a separate port for execution logs

    private final String LOG_SERVER_ADDRESS = "127.0.0.1";

    @Override
    public void initialize() {
        initializeCyclic(0, 500, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);

        // --- FIRST THING: Establish connection to the existing LogServer for protocol logging ---
        LogServer protocolLogServerInstance = logServerBackgroundTask.getLogServer();
        if (protocolLogServerInstance == null) {
            // This is a critical failure. If the LogServer isn't ready, we can't log.
            // Fallback to KUKA's internal logger, but this indicates a setup issue.
            getLogger().error("HartuServerBackgroundTask: CRITICAL ERROR: LogServer instance is null from LogServerBackgroundTask. Cannot establish protocol logger. Check LogServerBackgroundTask initialization.");
            throw new IllegalStateException("LogServer not initialized by LogServerBackgroundTask.");
        }

        try {
            // Give LogServerBackgroundTask a moment to fully start the LogServer
            // This is crucial to prevent connection refused errors
            Thread.sleep(2000);

            protocolLoggerClient = new LoggerClient(LOG_SERVER_ADDRESS, PROTOCOL_LOG_PORT);
            protocolLoggerClient.connect();
            if (!protocolLoggerClient.isConnected()) {
                getLogger().error("HartuServerBackgroundTask: Failed to connect Protocol Logger Client to LogServer on port " + PROTOCOL_LOG_PORT + ".");
                throw new RuntimeException("Failed to connect Protocol Logger Client for HartuServer.");
            }
            // Now that protocolLoggerClient is connected, use it for all subsequent logging
            protocolLoggerClient.sendMessage("HartuServerBackgroundTask: Protocol Logger Client connected successfully to LogServer on port " + PROTOCOL_LOG_PORT + ".");

            // --- Continue with other initializations, logging through protocolLoggerClient ---
            executionLoggerClient = new LoggerClient(LOG_SERVER_ADDRESS, EXECUTION_LOG_PORT);
            executionLoggerClient.connect();
            if (!executionLoggerClient.isConnected()) {
                protocolLoggerClient.sendMessage("HartuServerBackgroundTask: ERROR: Failed to connect Execution Logger Client to LogServer on port " + EXECUTION_LOG_PORT + ".");
                throw new RuntimeException("Failed to connect Execution Logger Client for HartuServer.");
            }
            protocolLoggerClient.sendMessage("HartuServerBackgroundTask: Execution Logger Client connected successfully to LogServer on port " + EXECUTION_LOG_PORT + ".");

        } catch (Exception e) {
            // If protocolLoggerClient failed, this will still go to KUKA's internal logger
            getLogger().error("HartuServerBackgroundTask: FATAL ERROR during LoggerClients initialization: " + e.getMessage(), e);
            throw new IllegalStateException("Failed to initialize LoggerClients for HartuServer.", e);
        }

        rawMessageQueue = new LinkedBlockingQueue<>();

        hartuServer = new HartuServer(HARTU_SERVER_PORT, protocolLoggerClient, rawMessageQueue);
        protocolLoggerClient.sendMessage("HartuServerBackgroundTask: HartuServer instance created, linked to loggers and raw message queue.");
    }

    @Override
    public void runCyclic() {
        if (serverListenThread == null || !serverListenThread.isAlive()) {
            protocolLoggerClient.sendMessage("HartuServerBackgroundTask: HartuServer thread not running. Attempting restart...");

            if (serverListenThread != null) {
                serverListenThread.interrupt();
                try {
                    serverListenThread.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    protocolLoggerClient.sendMessage("HartuServerBackgroundTask: Interrupted while waiting for old HartuServer thread to stop: " + e.getMessage());
                }
            }

            serverListenThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        hartuServer.start();
                    } catch (Exception e) {
                        protocolLoggerClient.sendMessage("HartuServerBackgroundTask: ERROR during HartuServer.start(): " + e.getMessage());
                        // For detailed stack trace, you'd need to convert it to string and send.
                        // Example: StringWriter sw = new StringWriter(); e.printStackTrace(new PrintWriter(sw)); protocolLoggerClient.sendMessage(sw.toString());
                    }
                }
            }, "HartuServerListenThread");

            serverListenThread.start();
            protocolLoggerClient.sendMessage("HartuServerBackgroundTask: HartuServer listening thread (re)started on port " + HARTU_SERVER_PORT);
        }
    }

    @Override
    public void dispose() {
        if (protocolLoggerClient != null) { // Ensure logger is available before logging dispose
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
        if (protocolLoggerClient != null) { // Final message before potential disconnect
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
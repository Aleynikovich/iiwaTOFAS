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
    private final int EXECUTION_LOG_PORT = 30004;

    private final String LOG_SERVER_ADDRESS = "127.0.0.1";

    @Override
    public void initialize() {
        initializeCyclic(0, 500, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);

        LogServer protocolLogServer = logServerBackgroundTask.getLogServer();
        if (protocolLogServer == null) {
            System.err.println("HartuServerBackgroundTask: LogServer instance is null. Cannot proceed."); // Fallback to System.err
            throw new IllegalStateException("LogServer not initialized by LogServerBackgroundTask.");
        }

        try {
            protocolLoggerClient = new LoggerClient(LOG_SERVER_ADDRESS, PROTOCOL_LOG_PORT);
            protocolLoggerClient.connect();
            if (!protocolLoggerClient.isConnected()) {
                throw new RuntimeException("Failed to connect Protocol Logger Client for HartuServer.");
            }
            protocolLoggerClient.sendMessage("HartuServerBackgroundTask: Protocol Logger Client connected.");

            executionLoggerClient = new LoggerClient(LOG_SERVER_ADDRESS, EXECUTION_LOG_PORT);
            executionLoggerClient.connect();
            if (!executionLoggerClient.isConnected()) {
                throw new RuntimeException("Failed to connect Execution Logger Client for HartuServer.");
            }
            protocolLoggerClient.sendMessage("HartuServerBackgroundTask: Execution Logger Client connected.");

        } catch (Exception e) {
            System.err.println("HartuServerBackgroundTask: Error initializing LoggerClients: " + e.getMessage()); // Fallback to System.err
            e.printStackTrace();
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
                        e.printStackTrace();
                    }
                }
            }, "HartuServerListenThread");

            serverListenThread.start();
            protocolLoggerClient.sendMessage("HartuServerBackgroundTask: HartuServer listening thread (re)started on port " + HARTU_SERVER_PORT);
        }
    }

    @Override
    public void dispose() {
        protocolLoggerClient.sendMessage("HartuServerBackgroundTask: dispose() called. Stopping HartuServer...");
        if (hartuServer != null) {
            hartuServer.stop();

            try {
                if (serverListenThread != null) {
                    serverListenThread.join(5000);
                    if (serverListenThread.isAlive()) {
                        protocolLoggerClient.sendMessage("HartuServerBackgroundTask: HartuServer thread did not terminate within timeout. Forcing interrupt.");
                        serverListenThread.interrupt();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                protocolLoggerClient.sendMessage("HartuServerBackgroundTask: Interrupted while waiting for HartuServer thread to stop.");
            }
        }
        if (protocolLoggerClient != null) {
            protocolLoggerClient.disconnect();
        }
        if (executionLoggerClient != null) {
            executionLoggerClient.disconnect();
        }
        protocolLoggerClient.sendMessage("HartuServerBackgroundTask: HartuServer stopped.");
        super.dispose();
    }

    public BlockingQueue<String> getRawMessageQueue() {
        return rawMessageQueue;
    }

    public LoggerClient getExecutionLoggerClient() {
        return executionLoggerClient;
    }
}
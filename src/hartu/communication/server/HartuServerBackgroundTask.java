package hartu.communication.server;

import javax.inject.Inject;
import java.io.IOException;
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
    private LogServerBackgroundTask logServerBackgroundTask; // Still needed to ensure it runs and for dependency order

    private HartuServer hartuServer;
    private Thread serverListenThread;

    private LoggerClient protocolLoggerClient;
    // Removed: private LoggerClient executionLoggerClient; // THIS WAS THE ERROR, IT'S GONE
    private BlockingQueue<String> rawMessageQueue;

    private final int HARTU_SERVER_PORT = 30001;
    private final int PROTOCOL_LOG_PORT = 30003;
    // Removed: private final int EXECUTION_LOG_PORT = 30004; // GONE

    private final String LOG_SERVER_ADDRESS = "127.0.0.1"; // Assuming localhost on the controller

    private int currentRestartAttempt = 0;
    private static final int MAX_RESTART_ATTEMPTS = 10; // Max times to try restarting HartuServer
    private static final long INITIAL_RESTART_DELAY_MS = 1000; // Initial delay for restart (1 second)

    @Override
    public void initialize() {
        getLogger().info("HartuServerBackgroundTask: Entering initialize() method.");
        initializeCyclic(0, 500, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);

        try {
            // Step 1: Initialize Protocol Logger Client first, as it's essential for logging subsequent steps.
            getLogger().info("HartuServerBackgroundTask: Sleeping for 2 seconds to allow LogServer (protocol) to fully start.");
            Thread.sleep(2000); // Give LogServer time to bind

            getLogger().info("HartuServerBackgroundTask: Creating Protocol Logger Client for port " + PROTOCOL_LOG_PORT + ".");
            protocolLoggerClient = new LoggerClient(LOG_SERVER_ADDRESS, PROTOCOL_LOG_PORT);
            getLogger().info("HartuServerBackgroundTask: Attempting to connect Protocol Logger Client.");

            // Call connect() (which is void) and then check isConnected()
            protocolLoggerClient.connect();
            if (!protocolLoggerClient.isConnected()) { // Now checking the boolean state
                getLogger().error("HartuServerBackgroundTask: CRITICAL ERROR: Failed to connect Protocol Logger Client to LogServer on port " + PROTOCOL_LOG_PORT + ".");
                throw new RuntimeException("Failed to connect Protocol Logger Client for HartuServer.");
            }
            protocolLoggerClient.sendMessage("HartuServerBackgroundTask: Protocol Logger Client connected successfully to LogServer on port " + PROTOCOL_LOG_PORT + ".");
            getLogger().info("HartuServerBackgroundTask: Protocol Logger Client reports connected.");

            // Removed all code related to executionLoggerClient from here.
            // It will be initialized and managed by CommandProcessorBackgroundTask.

            // Step 2: Initialize the raw message queue
            getLogger().info("HartuServerBackgroundTask: Initializing raw message queue.");
            rawMessageQueue = new LinkedBlockingQueue<>();

            // Step 3: Instantiate HartuServer (it does not start here, just created)
            getLogger().info("HartuServerBackgroundTask: Creating HartuServer instance for port " + HARTU_SERVER_PORT + ".");
            hartuServer = new HartuServer(HARTU_SERVER_PORT, protocolLoggerClient,rawMessageQueue);
            protocolLoggerClient.sendMessage("HartuServerBackgroundTask: HartuServer instance created, linked to protocol logger.");
            getLogger().info("HartuServerBackgroundTask: Exiting initialize() method successfully.");

        } catch (Exception e) {
            getLogger().error("HartuServerBackgroundTask: FATAL ERROR during HartuServerBackgroundTask initialization: " + e.getMessage(), e);
            throw new IllegalStateException("Failed to initialize HartuServerBackgroundTask due to an unexpected error.", e);
        }
    }

    @Override
    public void runCyclic() {
        // This method monitors the HartuServer's dedicated thread and restarts it if it dies.
        if (serverListenThread == null || !serverListenThread.isAlive()) {
            if (currentRestartAttempt >= MAX_RESTART_ATTEMPTS) {
                protocolLoggerClient.sendMessage("HartuServerBackgroundTask: CRITICAL: Max restart attempts reached for HartuServer. Aborting further restarts.");
                getLogger().error("HartuServerBackgroundTask: Max restart attempts reached for HartuServer. Aborting.");
                return; // Stop trying to restart
            }

            protocolLoggerClient.sendMessage("HartuServerBackgroundTask: HartuServer thread not running or has died. Attempting restart (Attempt " + (currentRestartAttempt + 1) + "/" + MAX_RESTART_ATTEMPTS + ")...");

            // Ensure any previous thread is properly shut down before starting a new one
            if (serverListenThread != null) {
                serverListenThread.interrupt();
                try {
                    serverListenThread.join(INITIAL_RESTART_DELAY_MS); // Give it a moment to die
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    protocolLoggerClient.sendMessage("HartuServerBackgroundTask: Interrupted while waiting for old HartuServer thread to stop: " + e.getMessage());
                }
            }

            // Calculate exponential backoff delay
            long delay = INITIAL_RESTART_DELAY_MS * (long) Math.pow(2, currentRestartAttempt);
            delay = Math.min(delay, 30000); // Cap delay at 30 seconds to avoid excessively long waits

            try {
                protocolLoggerClient.sendMessage("HartuServerBackgroundTask: Waiting " + delay + "ms before attempting server restart...");
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                protocolLoggerClient.sendMessage("HartuServerBackgroundTask: Interrupted during pre-restart delay. Aborting restart attempt.");
                return;
            }

            serverListenThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        hartuServer.start(); // This call blocks until an IOException or stop()
                        protocolLoggerClient.sendMessage("HartuServerBackgroundTask: HartuServer thread exited normally.");
                    } catch (IOException e) {
                        // This catches BindException or other IOExceptions from HartuServer.start()
                        protocolLoggerClient.sendMessage("HartuServerBackgroundTask: ERROR: HartuServer.start() failed with IOException: " + e.getMessage());
                        getLogger().error("HartuServerBackgroundTask: HartuServer.start() failed: " + e.getMessage(), e); // Log full stack trace to KUKA logger
                    } catch (Exception e) {
                        // Catch any other unexpected exceptions
                        protocolLoggerClient.sendMessage("HartuServerBackgroundTask: ERROR: HartuServer.start() failed with unexpected exception: " + e.getMessage());
                        getLogger().error("HartuServerBackgroundTask: HartuServer.start() failed unexpectedly: " + e.getMessage(), e);
                    } finally {
                        // Ensure isRunning is false if the thread dies due to an error
                        if (hartuServer.isRunning()) {
                            // This might happen if start() throws an exception before isRunning is set to false internally
                            hartuServer.stop(); // Force stop to reset state
                        }
                    }
                }
            }, "HartuServerListenThread");

            serverListenThread.start();
            protocolLoggerClient.sendMessage("HartuServerBackgroundTask: HartuServer listening thread (re)started on port " + HARTU_SERVER_PORT);
            currentRestartAttempt++; // Increment attempt counter after starting (or attempting to start)
        } else {
            // If the thread is alive, reset restart attempt counter.
            // This means the server is currently running successfully.
            currentRestartAttempt = 0;
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
            hartuServer.stop(); // Request the HartuServer to stop its internal loop

            try {
                if (serverListenThread != null) {
                    serverListenThread.join(5000); // Wait for the server thread to terminate
                    if (serverListenThread.isAlive()) {
                        if (protocolLoggerClient != null) protocolLoggerClient.sendMessage("HartuServerBackgroundTask: HartuServer thread did not terminate within timeout. Forcing interrupt.");
                        serverListenThread.interrupt(); // Force interrupt if it's still alive
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (protocolLoggerClient != null) protocolLoggerClient.sendMessage("HartuServerBackgroundTask: Interrupted while waiting for HartuServer thread to stop.");
            }
        }
        // Disconnect protocol logger client
        if (protocolLoggerClient != null) {
            protocolLoggerClient.disconnect();
        }
        // Removed executionLoggerClient disconnection from here.
        // It will be disconnected by CommandProcessorBackgroundTask's dispose.
        if (protocolLoggerClient != null) {
            protocolLoggerClient.sendMessage("HartuServerBackgroundTask: HartuServer stopped.");
        }
        super.dispose();
    }

    public BlockingQueue<String> getRawMessageQueue() {
        return rawMessageQueue;
    }

    // Removed getExecutionLoggerClient() method from here.
}
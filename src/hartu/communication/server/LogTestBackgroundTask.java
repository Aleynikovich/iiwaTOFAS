package hartu.communication.server;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.controllerModel.Controller; // Injected but not used in this simple test

import hartu.communication.client.LoggerClient;
import hartu.communication.server.LogServerBackgroundTask; // To get the LogServer's port

/**
 * A barebones KUKA RoboticsAPICyclicBackgroundTask to test the LogServer and LoggerClient.
 * It attempts to connect to the LogServer and sends periodic heartbeat messages.
 * Any connection or send failures are logged via KUKA's internal logger.
 */
public class LogTestBackgroundTask extends RoboticsAPICyclicBackgroundTask {

    @Inject
    private Controller controller; // Injected by KUKA framework, but not used in this test logic

    @Inject
    private LogServerBackgroundTask logServerBackgroundTask; // To get access to the LogServer's port

    private LoggerClient testLoggerClient;
    private long messageCounter = 0;

    private final int LOG_SERVER_PORT = 30003; // Must match the port of your LogServerBackgroundTask
    private final String LOG_SERVER_ADDRESS = "127.0.0.1"; // Assuming LogServer is on the same controller

    @Override
    public void initialize() {
        // Run this task every 1 second
        initializeCyclic(0, 1000, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);

        getLogger().info("LogTestBackgroundTask: Initializing...");

        try {
            // Give the LogServerBackgroundTask a moment to ensure its server is fully started
            Thread.sleep(3000); // Wait 3 seconds

            testLoggerClient = new LoggerClient(LOG_SERVER_ADDRESS, LOG_SERVER_PORT);
            testLoggerClient.connect();

            if (testLoggerClient.isConnected()) {
                getLogger().info("LogTestBackgroundTask: Successfully connected LoggerClient to LogServer on port " + LOG_SERVER_PORT + ".");
                testLoggerClient.sendMessage("LogTestBackgroundTask: LoggerClient connected and sending first message.");
            } else {
                getLogger().error("LogTestBackgroundTask: Failed to connect LoggerClient to LogServer on port " + LOG_SERVER_PORT + ".");
                // If connection fails, subsequent sendMessage calls will also fail, but won't crash.
            }
        } catch (Exception e) {
            getLogger().error("LogTestBackgroundTask: FATAL ERROR during LoggerClient initialization: " + e.getMessage(), e);
            // Re-throw to make the KUKA application crash if initialization fails critically
            throw new IllegalStateException("Failed to initialize LoggerClient for LogTestBackgroundTask.", e);
        }
    }

    @Override
    public void runCyclic() {
        if (testLoggerClient != null && testLoggerClient.isConnected()) {
            messageCounter++;
            try {
                testLoggerClient.sendMessage("LogTestBackgroundTask: Heartbeat message #" + messageCounter);
            } catch (Exception e) {
                // Log send errors, but don't crash the cyclic task
                getLogger().error("LogTestBackgroundTask: Error sending heartbeat message: " + e.getMessage(), e);
            }
        } else {
            // If not connected, try to reconnect (optional, but good for resilience)
            if (testLoggerClient != null && !testLoggerClient.isConnected()) {
                getLogger().warn("LogTestBackgroundTask: LoggerClient not connected. Attempting to reconnect...");
                try {
                    testLoggerClient.connect();
                    if (testLoggerClient.isConnected()) {
                        getLogger().info("LogTestBackgroundTask: Reconnected LoggerClient successfully.");
                        testLoggerClient.sendMessage("LogTestBackgroundTask: Reconnected and sending message.");
                    }
                } catch (Exception e) {
                    getLogger().error("LogTestBackgroundTask: Error during LoggerClient reconnection attempt: " + e.getMessage(), e);
                }
            } else if (testLoggerClient == null) {
                 getLogger().error("LogTestBackgroundTask: LoggerClient is null. Initialization likely failed.");
            }
        }
    }

    @Override
    public void dispose() {
        getLogger().info("LogTestBackgroundTask: dispose() called. Disconnecting LoggerClient...");
        if (testLoggerClient != null) {
            // Send a final message before disconnecting
            try {
                testLoggerClient.sendMessage("LogTestBackgroundTask: Disposing. Sending final message.");
            } catch (Exception e) {
                getLogger().error("LogTestBackgroundTask: Error sending final message during dispose: " + e.getMessage(), e);
            }
            testLoggerClient.disconnect();
        }
        getLogger().info("LogTestBackgroundTask: LoggerClient disconnected.");
        super.dispose();
    }
}
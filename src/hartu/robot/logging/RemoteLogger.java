package hartu.robot.logging;

import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class RemoteLogger extends RoboticsAPICyclicBackgroundTask {

    private static final String UBUNTU_IP = "10.66.171.69";
    private static final int LOGGING_PORT = 30003;

    // Initialization-on-demand holder idiom for thread-safe lazy singleton
    private static class LoggerHolder {
        private static final RemoteLogger INSTANCE = new RemoteLogger();
    }

    private Socket socket;
    private volatile boolean connectionAttempted = false;

    // Private constructor to enforce singleton pattern
    private RemoteLogger() {
        // Constructor is now private and only called once by LoggerHolder
    }

    public static RemoteLogger getInstance() {
        return LoggerHolder.INSTANCE;
    }

    @Override
    public void initialize() {
        // Set cyclic behavior, but runCyclic will be empty for this test
        initializeCyclic(0, 200, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);

        // Attempt to connect immediately in initialize
        try {
            socket = new Socket(UBUNTU_IP, LOGGING_PORT);
            connectionAttempted = true; // Indicates success
        } catch (IOException e) {
            connectionAttempted = false;
        } catch (Exception e) {
            connectionAttempted = false;
        }
    }

    @Override
    public void runCyclic() {
        // For this barebones test, runCyclic does nothing.
        // We are only interested in the initial connection attempt in initialize().
    }

    @Override
    public void dispose() {
        // Close the socket if it was opened
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // No logging here
        } finally {
            socket = null;
            // No need to nullify 'instance' as it's managed by LoggerHolder
        }
    }

    // Dummy log method to prevent compilation errors if called by TestRobotApplication
    public void log(String message) {
        // This barebones version does not log anything
    }
}

package hartu.robot.logging;

import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class RemoteLogger extends RoboticsAPICyclicBackgroundTask {

    private static final String UBUNTU_IP = "10.66.171.69";
    private static final int LOGGING_PORT = 30003;

    private static RemoteLogger instance;
    private Socket socket;
    private volatile boolean connectionAttempted = false; // Flag to indicate an attempt has been made


    @Override
    public void initialize() {
        // Set cyclic behavior, but runCyclic will be empty for this test
        initializeCyclic(0, 200, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);

        // Attempt to connect immediately in initialize
        try {
            socket = new Socket(UBUNTU_IP, LOGGING_PORT);
            // If connection is successful, set a flag. We can't log remotely yet.
            connectionAttempted = true; // Indicates success
        } catch (IOException e) {
            // If connection fails, connectionAttempted remains false.
            // No System.out.println or getLogger() as per your instruction.
            connectionAttempted = false;
        } catch (Exception e) {
            // Catch any other unexpected errors during connection attempt
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
            instance = null; // Clear singleton instance
        }
    }

    // Dummy log method to prevent compilation errors if called by TestRobotApplication
    public void log(String message) {
        // This barebones version does not log anything
    }
}

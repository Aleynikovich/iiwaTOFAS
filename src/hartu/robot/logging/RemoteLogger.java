package hartu.robot.logging;

import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class RemoteLogger extends RoboticsAPICyclicBackgroundTask {

    private static final String UBUNTU_IP = "10.66.171.69";
    private static final int LOGGING_PORT = 30003;
    private static final long RECONNECT_DELAY_MS = 5000; // 5 seconds

    private static RemoteLogger instance;
    private Socket socket;
    private PrintWriter out;
    private BlockingQueue<String> logQueue;
    private volatile boolean running;

    // Private constructor to enforce singleton pattern
    private RemoteLogger() {
        logQueue = new LinkedBlockingQueue<String>();
        running = true;
    }

    public static synchronized RemoteLogger getInstance() {
        if (instance == null) {
            instance = new RemoteLogger();
        }
        return instance;
    }

    @Override
    public void initialize() {
        // Initialize the cyclic behavior for this background task
        initializeCyclic(0, 200, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);
        log("RemoteLogger: Initializing background task.");
    }

    public void log(String message) {
        try {
            logQueue.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // Fallback for logger's own internal critical errors if queueing fails
            System.err.println("RemoteLogger: CRITICAL - Interrupted while queuing log message: " + e.getMessage());
        }
    }

    @Override
    public void runCyclic() {
        if (socket == null || socket.isClosed() || !socket.isConnected()) {
            log("RemoteLogger: Attempting to connect to log server at " + UBUNTU_IP + ":" + LOGGING_PORT);
            try {
                socket = new Socket(UBUNTU_IP, LOGGING_PORT);
                out = new PrintWriter(socket.getOutputStream(), true);
                log("RemoteLogger: Successfully connected to log server.");
            } catch (IOException e) {
                log("RemoteLogger: Failed to connect to log server: " + e.getMessage() + ". Retrying in " + RECONNECT_DELAY_MS + "ms.");
                try {
                    TimeUnit.MILLISECONDS.sleep(RECONNECT_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log("RemoteLogger: Reconnect delay interrupted.");
                }
                return;
            }
        }

        try {
            String message = logQueue.poll(); // Poll without timeout to process all available messages in this cycle
            while (message != null) {
                out.println(message);
                if (out.checkError()) {
                    throw new IOException("PrintWriter error during send.");
                }
                message = logQueue.poll();
            }
        } catch (IOException e) {
            log("RemoteLogger: I/O error sending log: " + e.getMessage() + ". Closing socket and attempting reconnect.");
            closeConnection();
        }
    }

    @Override
    public void dispose() {
        running = false; // Signal to stop any potential internal loops if they were not cyclic
        log("RemoteLogger: Disposing logger. Attempting to send remaining logs.");
        // Try to send any remaining logs before closing
        try {
            // Give a short grace period for any last logs to be processed
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        closeConnection();
        instance = null;
        log("RemoteLogger: Logger disposed.");
    }

    private void closeConnection() {
        try {
            if (out != null) {
                out.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("RemoteLogger: CRITICAL - Error closing connection: " + e.getMessage()); // Fallback for critical closing errors
        } finally {
            out = null;
            socket = null;
        }
    }
}

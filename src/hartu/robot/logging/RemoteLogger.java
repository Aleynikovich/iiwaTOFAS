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
    private static final long RECONNECT_DELAY_MS = 5000; // 5 seconds between reconnection attempts

    private static RemoteLogger instance;
    private Socket socket;
    private PrintWriter out;
    private BlockingQueue<String> logQueue;
    private volatile boolean running;

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
        initializeCyclic(0, 200, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);
        log("RemoteLogger: Initializing background task.");

        // Attempt initial connection here
        try {
            log("RemoteLogger: Attempting initial connection to log server at " + UBUNTU_IP + ":" + LOGGING_PORT);
            socket = new Socket(UBUNTU_IP, LOGGING_PORT);
            out = new PrintWriter(socket.getOutputStream(), true); // Auto-flush
            log("RemoteLogger: Successfully connected to log server during initialization.");
        } catch (IOException e) {
            log("RemoteLogger: Initial connection failed: " + e.getMessage() + ". Will retry in runCyclic().");
            // Do not throw, allow runCyclic to handle reconnection
            closeConnection(); // Ensure socket/out are null if connection failed
        } catch (Exception e) {
            log("RemoteLogger: Unexpected error during initial connection: " + e.getMessage() + ". Disabling logger.");
            running = false; // Critical error, prevent further attempts
            closeConnection();
        }
    }

    public void log(String message) {
        if (!running) {
            return;
        }
        try {
            logQueue.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
        }
    }

    @Override
    public void runCyclic() {
        if (!running) { // If a critical error during init disabled it
            return;
        }

        // Attempt to establish connection if not connected
        if (socket == null || socket.isClosed() || !socket.isConnected()) {
            log("RemoteLogger: Reconnection attempt to log server at " + UBUNTU_IP + ":" + LOGGING_PORT);
            try {
                socket = new Socket(UBUNTU_IP, LOGGING_PORT);
                out = new PrintWriter(socket.getOutputStream(), true); // Auto-flush
                log("RemoteLogger: Successfully reconnected to log server.");
            } catch (IOException e) {
                log("RemoteLogger: Failed to reconnect: " + e.getMessage() + ". Retrying in " + RECONNECT_DELAY_MS + "ms.");
                try {
                    TimeUnit.MILLISECONDS.sleep(RECONNECT_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                return; // Exit this cycle if reconnection failed
            } catch (Exception e) {
                log("RemoteLogger: Unexpected error during reconnection attempt: " + e.getMessage() + ". Closing connection.");
                closeConnection();
                return;
            }
        }

        // Process and send logs from the queue
        try {
            String message = logQueue.poll();
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
        } catch (Exception e) {
            log("RemoteLogger: Unexpected error during log sending: " + e.getMessage() + ". Closing socket.");
            closeConnection();
        }
    }

    @Override
    public void dispose() {
        running = false;
        log("RemoteLogger: Disposing logger. Attempting to send remaining logs.");
        try {
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
            // No logging here as per user's instruction.
        } finally {
            out = null;
            socket = null;
        }
    }
}
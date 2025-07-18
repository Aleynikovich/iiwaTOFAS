package hartu.robot.logging;

import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class RemoteLogger extends RoboticsAPICyclicBackgroundTask {

    private static final String UBUNTU_IP = "10.66.171.69";
    private static final int LOGGING_PORT = 30003;
    private static final long RECONNECT_DELAY_MS = 5000; // Not strictly for reconnects, but for retry logic if socket fails

    private static RemoteLogger instance;
    private DatagramSocket socket;
    private InetAddress ubuntuAddress;
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
        try {
            ubuntuAddress = InetAddress.getByName(UBUNTU_IP);
            log("RemoteLogger: Initializing background task. Ubuntu IP resolved.");
        } catch (UnknownHostException e) {
            log("RemoteLogger: Failed to resolve Ubuntu IP: " + UBUNTU_IP + " - " + e.getMessage());
            // Cannot proceed without a valid IP, so the logger won't function.
            // Consider stopping the task or setting a flag to prevent further operations.
            running = false; // Prevent runCyclic from trying to send
        }
    }

    public void log(String message) {
        if (!running) {
            // If the logger is not running (e.g., failed to initialize),
            // we can't queue messages.
            return;
        }
        try {
            logQueue.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // Catch any other unexpected errors during queuing
        }
    }

    @Override
    public void runCyclic() {
        if (!running || ubuntuAddress == null) {
            return; // Don't proceed if not running or IP not resolved
        }

        // Initialize DatagramSocket if not already initialized or if it was closed
        if (socket == null || socket.isClosed()) {
            try {
                // For UDP, we just create the socket. No explicit "connect" needed.
                // It binds to any available local port.
                socket = new DatagramSocket();
                log("RemoteLogger: UDP socket created successfully.");
            } catch (SocketException e) {
                log("RemoteLogger: Failed to create UDP socket: " + e.getMessage() + ". Retrying in " + RECONNECT_DELAY_MS + "ms.");
                try {
                    TimeUnit.MILLISECONDS.sleep(RECONNECT_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                return; // Exit this cycle if socket creation failed
            } catch (Exception e) {
                log("RemoteLogger: Unexpected error during UDP socket creation: " + e.getMessage() + ". Disabling logger.");
                running = false; // Critical error, stop logging attempts
                return;
            }
        }

        // Process and send logs from the queue
        try {
            String message = logQueue.poll();
            while (message != null) {
                byte[] data = message.getBytes("UTF-8"); // Convert string to bytes
                DatagramPacket packet = new DatagramPacket(data, data.length, ubuntuAddress, LOGGING_PORT);
                socket.send(packet); // Send the UDP packet
                message = logQueue.poll();
            }
        } catch (IOException e) {
            log("RemoteLogger: I/O error sending UDP packet: " + e.getMessage() + ". Closing socket and attempting re-creation.");
            closeConnection(); // Close the socket, it will be re-created next cycle
        } catch (Exception e) {
            log("RemoteLogger: Unexpected error during UDP log sending: " + e.getMessage() + ". Closing socket.");
            closeConnection();
        }
    }

    @Override
    public void dispose() {
        running = false;
        log("RemoteLogger: Disposing logger. Attempting to send remaining logs.");
        try {
            TimeUnit.MILLISECONDS.sleep(500); // Give a short grace period
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        closeConnection();
        instance = null;
        log("RemoteLogger: Logger disposed.");
    }

    private void closeConnection() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        socket = null;
    }
}

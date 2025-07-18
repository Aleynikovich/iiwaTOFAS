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
    private static final long HEARTBEAT_INTERVAL_MS = 2000; // Send heartbeat every 2 seconds

    private static RemoteLogger instance;
    private DatagramSocket socket;
    private InetAddress ubuntuAddress;
    private BlockingQueue<String> logQueue;
    private volatile boolean running;
    private long lastHeartbeatTime;

    private RemoteLogger() {
        logQueue = new LinkedBlockingQueue<String>();
        running = true;
        lastHeartbeatTime = 0; // Initialize last heartbeat time
    }

    public static synchronized RemoteLogger getInstance() {
        if (instance == null) {
            instance = new RemoteLogger();
        }
        return instance;
    }

    @Override
    public void initialize() {
        initializeCyclic(0, 200, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort); // Cyclic runs every 200ms
        try {
            ubuntuAddress = InetAddress.getByName(UBUNTU_IP);
            log("RemoteLogger: Initializing background task. Ubuntu IP resolved.");
        } catch (UnknownHostException e) {
            log("RemoteLogger: Failed to resolve Ubuntu IP: " + UBUNTU_IP + " - " + e.getMessage());
            running = false;
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
        if (!running || ubuntuAddress == null) {
            return;
        }

        if (socket == null || socket.isClosed()) {
            try {
                socket = new DatagramSocket();
                log("RemoteLogger: UDP socket created successfully.");
            } catch (SocketException e) {
                log("RemoteLogger: Failed to create UDP socket: " + e.getMessage() + ". Retrying next cycle.");
                // No sleep here, runCyclic will be called again by KUKA runtime
                return;
            } catch (Exception e) {
                log("RemoteLogger: Unexpected error during UDP socket creation: " + e.getMessage() + ". Disabling logger.");
                running = false;
                return;
            }
        }

        // Send heartbeat periodically
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastHeartbeatTime >= HEARTBEAT_INTERVAL_MS) {
            sendHeartbeat();
            lastHeartbeatTime = currentTime;
        }

        // Process and send logs from the queue (existing log logic)
        try {
            String message = logQueue.poll();
            while (message != null) {
                byte[] data = message.getBytes("UTF-8");
                DatagramPacket packet = new DatagramPacket(data, data.length, ubuntuAddress, LOGGING_PORT);
                socket.send(packet);
                message = logQueue.poll();
            }
        } catch (IOException e) {
            log("RemoteLogger: I/O error sending UDP packet: " + e.getMessage() + ". Closing socket and attempting re-creation.");
            closeConnection();
        } catch (Exception e) {
            log("RemoteLogger: Unexpected error during UDP log sending: " + e.getMessage() + ". Closing socket.");
            closeConnection();
        }
    }

    private void sendHeartbeat() {
        if (socket != null && !socket.isClosed() && ubuntuAddress != null) {
            try {
                String heartbeatMessage = "HEARTBEAT_FROM_ROBOT";
                byte[] data = heartbeatMessage.getBytes("UTF-8");
                DatagramPacket packet = new DatagramPacket(data, data.length, ubuntuAddress, LOGGING_PORT);
                socket.send(packet);
                // We don't log the heartbeat itself via the queue to avoid recursion/circular logging
                // if the logger itself is having issues. This is a direct test.
            } catch (IOException e) {
                // Log this error using the main log method, which will queue it
                log("RemoteLogger: Failed to send heartbeat: " + e.getMessage());
                closeConnection(); // Close socket if sending heartbeat fails, force re-creation
            } catch (Exception e) {
                log("RemoteLogger: Unexpected error sending heartbeat: " + e.getMessage());
                closeConnection();
            }
        } else {
            log("RemoteLogger: Cannot send heartbeat, socket or address not ready.");
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
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        socket = null;
    }
}

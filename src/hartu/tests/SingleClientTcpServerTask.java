package hartu.tests;

import com.kuka.roboticsAPI.RoboticsAPIContext;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileWriter; // For file logging
import java.io.IOException;
import java.io.PrintWriter; // For file logging
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat; // For timestamp in logs
import java.util.Date; // For timestamp in logs
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SingleClientTcpServerTask extends RoboticsAPICyclicBackgroundTask {

    private final int serverPort;
    private ServerSocket serverSocket; // Initialized in constructor
    private volatile Socket clientSocket;
    private volatile Thread clientCommunicationThread;

    private final Lock socketLock = new ReentrantLock();

    private static final long HEARTBEAT_INTERVAL_MS = 2000;
    private volatile long lastHeartbeatSentTime = 0;

    // --- LOGGING FIELDS ---
    private static final String LOG_FILE_PATH = "C:\\KRC\\ROBOTER\\log\\server_task_log.log";
    private PrintWriter logWriter;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    // --- END LOGGING FIELDS ---

    /**
     * Helper method to write messages to the log file.
     * Includes a timestamp and ensures the message is flushed immediately.
     */
    private void log(String message) {
        try {
            // Lazy initialization of logWriter (first call to log)
            if (logWriter == null) {
                // true means append to file
                logWriter = new PrintWriter(new FileWriter(LOG_FILE_PATH, true));
            }
            logWriter.println(dateFormat.format(new Date()) + " [INFO] " + message);
            logWriter.flush(); // Ensure message is written to disk immediately
        } catch (IOException e) {
            // Fallback: If file logging fails, print to System.err
            // This error itself might not be visible easily, but it's a fallback
            System.err.println(dateFormat.format(new Date()) + " [ERROR] Failed to write to log file: " + e.getMessage());
            System.err.println(dateFormat.format(new Date()) + " [ERROR] Original message: " + message);
        }
    }

    /**
     * Helper method to log exceptions, including their stack trace.
     */
    private void logException(String message, Throwable t) {
        log(message); // Log the main message first
        if (logWriter != null) {
            logWriter.print(dateFormat.format(new Date()) + " [ERROR] Stack Trace: ");
            t.printStackTrace(logWriter); // Print stack trace to the file
            logWriter.flush();
        } else {
            // Fallback: Print to System.err if file logging is not initialized or failed
            System.err.println(dateFormat.format(new Date()) + " [ERROR] " + message);
            t.printStackTrace(System.err);
        }
    }


    public SingleClientTcpServerTask(RoboticsAPIContext context, int port) {
        super(context);
        this.serverPort = port;

        // Initialize cyclic behavior: how often runCyclic() is called
        // Period of 1000 milliseconds (1 second) for connection checks.
        // CycleBehavior.Lenient is preferred as accept() can block for a timeout period.
        initializeCyclic(0, 1000, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);

        // IMPORTANT: Initialize ServerSocket here in the constructor.
        // This runs when the task is instantiated, before run() is called by the framework.
        try {
            // Attempt to initialize the log writer early, so any issues here are logged.
            logWriter = new PrintWriter(new FileWriter(LOG_FILE_PATH, true));
            log("[ServerTask] Initializing ServerSocket on port " + serverPort + " in constructor...");

            socketLock.lock(); // Ensure thread-safe access to network resources
            serverSocket = new ServerSocket(serverPort);
            // CRITICAL: Set a timeout on accept() to prevent it from blocking indefinitely.
            // This allows runCyclic() to complete and cycle.
            serverSocket.setSoTimeout(500); // Will throw SocketTimeoutException after 500ms if no client connects
            log("[ServerTask] ServerSocket successfully initialized and listening on port " + serverPort);
        } catch (IOException e) {
            // Log the fatal error and re-throw, as the task cannot function without the ServerSocket.
            logException("[ServerTask] FATAL ERROR: Failed to initialize ServerSocket in constructor. Task will not start.", e);
            // This RuntimeException will likely cause the KUKA application to report a startup failure.
            throw new RuntimeException("Failed to initialize server socket for TCP task", e);
        } finally {
            socketLock.unlock(); // Always release the lock
        }
    }

    // The 'run()' method is final in RoboticsAPICyclicBackgroundTask and cannot be overridden.
    // The framework will call this 'runCyclic()' method periodically based on initializeCyclic() settings.
    @Override
    protected void runCyclic() {
        socketLock.lock(); // Protect network resources (clientSocket, serverSocket)
        try {
            // 1. Check if a client is currently connected and active
            if (clientSocket != null && clientSocket.isConnected() && !clientSocket.isClosed()) {
                // Client is connected. Perform heartbeat to ensure connection is still alive.
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastHeartbeatSentTime > HEARTBEAT_INTERVAL_MS) {
                    try {
                        // Send a dummy byte (0x01) as a heartbeat.
                        // Your client should ignore this or respond to it.
                        clientSocket.getOutputStream().write(0x01);
                        clientSocket.getOutputStream().flush();
                        lastHeartbeatSentTime = currentTime;
                        // log("[ServerTask] Heartbeat sent to client."); // Uncomment for detailed heartbeat logging
                    } catch (IOException e) {
                        // If heartbeat fails, assume connection is lost.
                        logException("[ServerTask] Heartbeat failed, connection likely lost.", e);
                        closeCurrentClientConnection(); // Trigger reconnection in the next cycle
                    }
                }
            } else {
                // No client connected or previous connection lost. Attempt to accept a new one.
                log("[ServerTask] No client connected. Attempting to accept a new connection...");
                closeCurrentClientConnection(); // Ensure any old, broken socket is fully closed before accepting a new one.

                try {
                    // This accept() call will block for max 500ms (due to setSoTimeout).
                    // If a client connects, it returns the socket. If not, it throws SocketTimeoutException.
                    clientSocket = serverSocket.accept();
                    log("[ServerTask] New client connected: " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());

                    // Reset heartbeat timer for the new connection
                    lastHeartbeatSentTime = System.currentTimeMillis();

                    // Start a dedicated thread for continuous communication with this newly connected client.
                    // This is crucial to prevent the runCyclic() from blocking on I/O.
                    startClientCommunication(clientSocket);

                } catch (SocketTimeoutException e) {
                    // This is expected if no client connects within the timeout. Not an error.
                    // log("[ServerTask] No client connected within timeout. Retrying in next cycle."); // Uncomment for detailed logs
                } catch (IOException e) {
                    // Other IOException during accept (e.g., serverSocket closed unexpectedly).
                    logException("[ServerTask] Error accepting client connection.", e);
                    closeCurrentClientConnection(); // Try to clean up for the next cycle
                }
            }
        } finally {
            socketLock.unlock(); // Always release the lock
        }
    }

    /**
     * Closes the current client socket and interrupts its communication thread.
     */
    private void closeCurrentClientConnection() {
        // First, try to gracefully stop the communication thread if it's running.
        if (clientCommunicationThread != null && clientCommunicationThread.isAlive()) {
            clientCommunicationThread.interrupt(); // Signal the thread to stop its loop
            try {
                // Wait for the thread to terminate gracefully (max 1 second).
                clientCommunicationThread.join(1000);
            } catch (InterruptedException e) {
                // Restore the interrupted status for the current thread.
                Thread.currentThread().interrupt();
                logException("[ServerTask] Interruption during client communication thread join.", e);
            }
        }
        // Then, close the client socket itself.
        if (clientSocket != null) {
            try {
                clientSocket.close();
                log("[ServerTask] Previous client connection closed.");
            } catch (IOException e) {
                logException("[ServerTask] Error closing client socket: " + e.getMessage(), e);
            } finally {
                // Set clientSocket to null to indicate no active client, prompting reconnection.
                clientSocket = null;
            }
        }
    }

    /**
     * Starts a dedicated new thread to handle continuous data communication with the client.
     * This separates the blocking I/O from the cyclic task.
     *
     * @param socket The client socket to communicate with.
     */
    private void startClientCommunication(final Socket socket) {
        // Use an anonymous Runnable class for Java 7 compatibility (no lambdas).
        clientCommunicationThread = new Thread(new Runnable() {
            @Override
            public void run() {
                log("[ServerTask] Starting communication thread for client: " + socket.getInetAddress().getHostAddress());
                DataInputStream in = null;
                DataOutputStream out = null;

                try {
                    in = new DataInputStream(socket.getInputStream());
                    out = new DataOutputStream(socket.getOutputStream());

                    // Main communication loop: runs as long as the thread is not interrupted
                    // and the socket is connected/open.
                    while (!Thread.currentThread().isInterrupted() && socket.isConnected() && !socket.isClosed()) {
                        try {
                            // --- YOUR ACTUAL CLIENT COMMUNICATION PROTOCOL GOES HERE ---
                            // Example: Read an integer and echo it back. Adapt this to your needs.
                            int receivedData = in.readInt();
                            log("[ServerTask] Received: " + receivedData + " from " + socket.getInetAddress().getHostAddress());
                            out.writeInt(receivedData); // Echo back
                            out.flush();

                        } catch (IOException e) {
                            // A communication error means the connection is likely broken.
                            logException("[ServerTask] Communication error with client: " + e.getMessage(), e);
                            break; // Exit loop, thread will terminate.
                        }
                        // Small sleep to yield CPU if your communication is not continuously blocking
                        // (e.g., if you have non-blocking reads or complex processing between I/O).
                        // If 'readInt()' is always blocking and you expect constant data, this sleep might be removed.
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            log("[ServerTask] Communication thread interrupted during sleep.");
                            Thread.currentThread().interrupt(); // Restore interrupt status
                            break; // Exit loop.
                        }
                    }
                } catch (IOException e) {
                    logException("[ServerTask] Error setting up communication streams for client.", e);
                } finally {
                    log("[ServerTask] Communication thread for client terminated for " + socket.getInetAddress().getHostAddress());
                    // Manually close streams in Java 7, in reverse order of opening.
                    if (out != null) {
                        try { out.close(); } catch (IOException ignore) {}
                    }
                    if (in != null) {
                        try { in.close(); } catch (IOException ignore) {}
                    }
                    // Close the socket specifically associated with this communication thread.
                    // This prevents issues if 'clientSocket' in the main task has already been reassigned.
                    try { socket.close(); } catch (IOException ignore) {}

                    // Nullify the global clientSocket reference if this was the one being handled.
                    // This signals runCyclic() to look for a new connection.
                    socketLock.lock();
                    try {
                        if (clientSocket == socket) { // Check if the global ref points to THIS socket
                            clientSocket = null;
                        }
                    } finally {
                        socketLock.unlock();
                    }
                }
            }
        });
        clientCommunicationThread.setDaemon(true); // Allow JVM to exit even if this thread is running.
        clientCommunicationThread.start();
    }

    /**
     * Called when the background task is being disposed (e.g., KUKA application stops).
     * Ensures all network resources and threads are cleanly shut down.
     */
    @Override
    public void dispose() {
        log("[ServerTask] Disposing SingleClientTcpServerTask...");
        socketLock.lock(); // Ensure thread-safe cleanup
        try {
            closeCurrentClientConnection(); // Close client socket and stop its communication thread.
            if (serverSocket != null) {
                try {
                    serverSocket.close(); // Close the main server socket.
                    log("[ServerTask] ServerSocket closed.");
                } catch (IOException e) {
                    logException("[ServerTask] Error closing ServerSocket during dispose.", e);
                } finally {
                    serverSocket = null;
                }
            }
        } finally {
            socketLock.unlock();
        }

        // Close the log writer last to capture all disposal messages.
        if (logWriter != null) {
            log("[ServerTask] Log writer closed.");
            try {
                logWriter.close(); // Release the file handle.
            } catch (Exception e) {
                System.err.println(dateFormat.format(new Date()) + " [ERROR] Error closing logWriter: " + e.getMessage());
            } finally {
                logWriter = null;
            }
        }
        super.dispose(); // Call the base class's dispose method for its cleanup.
    }
}
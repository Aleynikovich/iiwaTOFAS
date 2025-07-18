package hartuTofas;

import com.kuka.generated.ioAccess.Ethercat_x44IOGroup;
import com.kuka.generated.ioAccess.IOFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.deviceModel.LBR;

import javax.inject.Inject;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RoboticsAPICyclicBackgroundTask implementation for a TCP/IP server
 * that listens for incoming connections and manages communication with clients.
 * This task runs continuously in the background, accepting new client connections
 * and delegating message handling to individual ClientHandler threads.
 */
public class RobotTCPServerTask extends RoboticsAPICyclicBackgroundTask {

    // Injected robot and I/O components
    @Inject
    private LBR robot;
    @Inject
    private IOFlangeIOGroup gimatic;
    @Inject
    private Ethercat_x44IOGroup iOs;

    // The main server socket for listening to incoming connections
    private ServerSocket serverSocket = null;
    // The port number for the server to listen on
    private static final int SERVER_PORT = 30001;
    // Timeout for accepting new connections (in milliseconds)
    // This makes the accept() call non-blocking  the cyclic task, allowing for clean shutdown.
    private static final int ACCEPT_TIMEOUT = 1000; // 1 second

    // Logger for logging messages and errors
    private static final Logger LOGGER = Logger.getLogger(RobotTCPServerTask.class.getName());
a
    // Flag to control the server listening thread
    private volatile boolean isRunning = true;
sssaa
    // List to keep track of active client handler threads
    // Using Collections.synchronizedList for thread-safe access
    private List<ClientHandler> activeClientHandlers = Collections.synchronizedList(new ArrayList<>());

    // The thread responsible for continuously accepting new client connections
    private Thread acceptThread;

    /**
     * Initializes the background task.
     * This method is called once when the task starts.
     * It sets up the cyclic behavior, initializes the server socket,
     * and starts a dedicated thread to accept client connections.
     */
    @Override
    public void initialize() {
        super.initialize(); // Call superclass initialize method

        // Initialize the cyclic task behavior:
        // Period: 100 milliseconds (how often runCyclic() is called)
        // Initial Delay: 0 milliseconds
        // CycleBehavior.BestEffort: Task runs as frequently as possible, but might be delayed if system is busy.
        initializeCyclic(0, 100, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);
        //LOGGER.info("RobotTCPServerTask initialized.");

        // Start the server socket in a separate thread to avoid blocking initialize() or runCyclic()
        acceptThread = new Thread(() -> {
            try {
                // Create the server socket and bind it to the specified port
                serverSocket = new ServerSocket(SERVER_PORT);
                // Set a timeout for the accept operation. This allows the thread to periodically
                // check the 'isRunning' flag and terminate gracefully.
                serverSocket.setSoTimeout(ACCEPT_TIMEOUT);
                LOGGER.info("Server listening on port " + SERVER_PORT);

                // Main server loop: continuously accept new client connections
                while (isRunning) {
                    try {
                        // Accept a new client connection. This call blocks until a client connects
                        // or the socket timeout occurs.
                        Socket clientSocket = serverSocket.accept();
                        LOGGER.info("Client connected: " + clientSocket.getInetAddress().getHostAddress());

                        // Create a new MessageHandler instance for each client.
                        // Pass the RoboticsAPIApplication instance from the current task context.
                        // This allows MessageHandler to access application data like frames.
                        MessageHandler messageHandler = new MessageHandler(robot, gimatic, iOs, (RoboticsAPIApplication) getApplicationContext());

                        // Create and start a new ClientHandler thread for the connected client.
                        // Each client gets its own thread to handle communication independently.
                        ClientHandler clientHandler = new ClientHandler(clientSocket, messageHandler, LOGGER);
                        activeClientHandlers.add(clientHandler); // Add to the list of active handlers
                        clientHandler.start(); // Start the client communication thread

                    } catch (SocketTimeoutException e) {
                        // Timeout occurred, check if the server is still supposed to be running.
                        // This is normal behavior when using setSoTimeout.
                        // LOGGER.log(Level.FINE, "Server socket accept timed out, checking isRunning flag.");
                    } catch (IOException e) {
                        // Handle other I/O errors during accept operation
                        if (isRunning) { // Only log if server is expected to be running
                            LOGGER.log(Level.SEVERE, "Error accepting client connection: " + e.getMessage(), e);
                        }
                        // If serverSocket is closed externally, accept() will throw an IOException
                        // and isRunning will be false, so loop will terminate.
                    }
                }
            } catch (IOException e) {
                // Handle errors during server socket creation or binding
                LOGGER.log(Level.SEVERE, "Could not listen on port " + SERVER_PORT + ": " + e.getMessage(), e);
                isRunning = false; // Stop the server loop if initialization fails
            } finally {
                // Ensure the server socket is closed when the thread terminates
                if (serverSocket != null && !serverSocket.isClosed()) {
                    try {
                        serverSocket.close();
                        LOGGER.info("Server socket closed.");
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, "Error closing server socket: " + e.getMessage(), e);
                    }
                }
            }
        }, "AcceptThread"); // Name the thread for easier debugging
        acceptThread.start(); // Start the accept thread
    }

    /**
     * This method is called cyclically by the RoboticsAPI framework.
     * For a server task where the accept loop runs in a separate thread,
     * this method can be used for periodic checks, cleanup of dead client handlers,
     * or other background tasks that don't involve blocking I/O.
     */
    @Override
    public void runCyclic() {
        // Periodically clean up client handlers that have finished their work (e.g., client disconnected)
        // This prevents the activeClientHandlers list from growing indefinitely.
        activeClientHandlers.removeIf(handler -> !handler.isAlive());
        // LOGGER.log(Level.FINEST, "Running cyclic task. Active clients: " + activeClientHandlers.size());
    }

    /**
     * Disposes of resources when the task is stopped or the application terminates.
     * This method ensures that the server socket and all active client sockets are closed gracefully.
     */
    @Override
    public void dispose() {
        super.dispose(); // Call superclass dispose method
        LOGGER.info("RobotTCPServerTask disposing...");

        // Signal the accept thread to stop
        isRunning = false;
        // Interrupt the accept thread to break it out of a blocking accept() call
        if (acceptThread != null) {
            acceptThread.interrupt();
            try {
                // Wait for the accept thread to finish
                acceptThread.join(5000); // Wait up to 5 seconds for the thread to terminate
                if (acceptThread.isAlive()) {
                    LOGGER.warning("Accept thread did not terminate gracefully after join.");
                }
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "Interrupted while waiting for accept thread to terminate.", e);
                Thread.currentThread().interrupt(); // Restore interrupt status
            }
        }

        // Close the server socket if it's still open
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                LOGGER.info("Server socket closed during dispose.");
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error closing server socket during dispose: " + e.getMessage(), e);
            }
        }

        // Close all active client handler connections
        for (ClientHandler handler : activeClientHandlers) {
            handler.closeClientResources();
            try {
                handler.join(1000); // Wait a bit for client handler to finish
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "Interrupted while waiting for client handler to terminate.", e);
                Thread.currentThread().interrupt(); // Restore interrupt status
            }
        }
        activeClientHandlers.clear(); // Clear the list

        LOGGER.info("RobotTCPServerTask disposed.");
    }
}

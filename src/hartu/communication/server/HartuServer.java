package hartu.communication.server;

import hartu.communication.client.LoggerClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.BindException; // Specific exception for "Address already in use"
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The HartuServer is a network listener. Its sole responsibility is to:
 * 1. Open a ServerSocket on a specified port.
 * 2. Accept incoming client connections.
 * 3. Hand off each connected client to a dedicated HartuClientHandler thread.
 * 4. Receive raw string messages from these clients via their handlers.
 * 5. Enqueue these raw strings into a BlockingQueue for processing by another component.
 * 6. Log its own operational events (connections, disconnections, I/O errors) via a LoggerClient.
 * It does NOT parse messages, execute commands, or manage robot-specific logic.
 */
public class HartuServer extends AbstractServer {

    private final LoggerClient protocolLoggerClient; // Used for logging server's own operations
    private final BlockingQueue<String> rawMessageQueue; // Queue to put received raw messages into

    private final List<HartuClientHandler> connectedHandlers; // List to manage active client connections
    private ExecutorService clientHandlerExecutor; // Thread pool for handling individual client connections

    /**
     * Constructs the HartuServer.
     * @param port The network port this server will listen on.
     * @param protocolLoggerClient The LoggerClient instance for logging server events.
     * @param rawMessageQueue The BlockingQueue to enqueue received raw messages.
     */
    public HartuServer(int port, LoggerClient protocolLoggerClient, BlockingQueue<String> rawMessageQueue) {
        super(port);
        if (protocolLoggerClient == null || rawMessageQueue == null) {
            throw new IllegalArgumentException("Protocol LoggerClient and Raw Message Queue must be non-null.");
        }
        this.protocolLoggerClient = protocolLoggerClient;
        this.rawMessageQueue = rawMessageQueue;
        this.connectedHandlers = new CopyOnWriteArrayList<>(); // Thread-safe list for handlers

        this.clientHandlerExecutor = Executors.newCachedThreadPool(); // Flexible thread pool

        protocolLoggerClient.sendMessage(formatLogMessage(getServerName() + ": Initialized for port " + port + ". Raw message queue ready."));
    }

    @Override
    protected String getServerName() {
        return "HartuServer";
    }

    /**
     * Starts the server. This method will block indefinitely, listening for connections,
     * until an IOException occurs or stop() is called from another thread.
     * It will log status messages via the provided LoggerClient.
     * @throws IOException if binding fails or other critical I/O errors occur.
     */
    @Override
    public void start() throws IOException { // Declare IOException to be thrown
        if (isRunning) {
            protocolLoggerClient.sendMessage(formatLogMessage(getServerName() + ": is already running on port " + port));
            return;
        }

        // Set isRunning to true BEFORE attempting to bind the socket.
        // If binding fails, the exception will be caught by the caller (BackgroundTask).
        isRunning = true;

        try {
            protocolLoggerClient.sendMessage(formatLogMessage(getServerName() + ": Attempting to create ServerSocket on port " + port + "..."));
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true); // Crucial for rapid restarts

            protocolLoggerClient.sendMessage(formatLogMessage(getServerName() + ": Started successfully on port " + port + " (" + getInetAddress().getHostAddress() + ")"));

            while (isRunning) { // This loop keeps the server alive
                protocolLoggerClient.sendMessage(formatLogMessage(getServerName() + ": Waiting for client connection..."));
                Socket clientSocket = serverSocket.accept(); // This call blocks until a client connects
                protocolLoggerClient.sendMessage(formatLogMessage(getServerName() + ": Client connected from: " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort()));

                // Hand off client connection to a new handler thread
                HartuClientHandler handler = new HartuClientHandler(clientSocket, this);
                connectedHandlers.add(handler); // Add to list for management
                clientHandlerExecutor.submit(handler); // Submit to thread pool
            }

        } catch (BindException e) {
            protocolLoggerClient.sendMessage(formatLogMessage(getServerName() + " ERROR: Failed to bind to port " + port + ": Address already in use. " + e.getMessage()));
            isRunning = false; // Set to false because binding failed
            throw e; // Re-throw to signal failure to the caller (BackgroundTask)
        } catch (IOException e) {
            if (isRunning) { // If it was running and an unexpected error occurred
                protocolLoggerClient.sendMessage(formatLogMessage(getServerName() + " ERROR: Encountered I/O error: " + e.getMessage()));
                isRunning = false; // Explicitly set to false on error to signal shutdown
            } else {
                protocolLoggerClient.sendMessage(formatLogMessage(getServerName() + ": Shut down normally due to socket close."));
            }
            throw e; // Re-throw to signal failure to the caller (BackgroundTask)
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                    protocolLoggerClient.sendMessage(formatLogMessage(getServerName() + ": ServerSocket closed in finally block."));
                } catch (IOException e) {
                    protocolLoggerClient.sendMessage(formatLogMessage(getServerName() + " ERROR: Error closing ServerSocket in finally: " + e.getMessage()));
                }
            }
        }
    }

    /**
     * Stops the server by closing its ServerSocket and shutting down client handlers.
     */
    @Override
    public void stop() {
        if (!isRunning) {
            protocolLoggerClient.sendMessage(formatLogMessage(getServerName() + ": is not running."));
            return;
        }
        isRunning = false; // Signal server's main loop to terminate

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); // Close the server socket to unblock accept() and terminate the main loop
                protocolLoggerClient.sendMessage(formatLogMessage(getServerName() + ": Server socket explicitly closed."));
            }
        } catch (IOException e) {
            protocolLoggerClient.sendMessage(formatLogMessage(getServerName() + " ERROR: Error closing server socket: " + e.getMessage()));
        } finally {
            // Disconnect all connected client handlers
            for (HartuClientHandler handler : connectedHandlers) {
                handler.disconnect(); // Request handler to disconnect
            }
            connectedHandlers.clear(); // Clear the list

            // Shut down the executor service gracefully
            clientHandlerExecutor.shutdown();
            try {
                if (!clientHandlerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    clientHandlerExecutor.shutdownNow(); // Force shutdown if not terminated
                    protocolLoggerClient.sendMessage(formatLogMessage(getServerName() + " WARNING: Client handler executor did not terminate cleanly. Forced shutdown."));
                }
            } catch (InterruptedException e) {
                clientHandlerExecutor.shutdownNow();
                Thread.currentThread().interrupt();
                protocolLoggerClient.sendMessage(formatLogMessage(getServerName() + " WARNING: Client handler executor interrupted during shutdown. Forced shutdown."));
            }
            protocolLoggerClient.sendMessage(formatLogMessage(getServerName() + ": Stopped."));
        }
    }

    /**
     * Called by a HartuClientHandler when it finishes its lifecycle (e.g., client disconnects).
     * @param handler The handler to remove from the list.
     */
    protected void removeHandler(HartuClientHandler handler) {
        connectedHandlers.remove(handler);
        protocolLoggerClient.sendMessage(formatLogMessage("Handler removed for client: " + handler.getClientAddress()));
    }

    @Override
    protected void handleClient(Socket clientSocket) { /* Not used directly, handled by ExecutorService */ }

    /**
     * Enqueues a raw message received from a client into the raw message queue.
     * This method is called by the HartuClientHandler.
     * @param rawMessage The raw string message received.
     * @param clientAddress The address of the client that sent the message.
     */
    public void enqueueRawMessage(String rawMessage, String clientAddress) {
        try {
            rawMessageQueue.put(rawMessage);
            protocolLoggerClient.sendMessage(formatLogMessage("Enqueued raw message from " + clientAddress + ": " + rawMessage));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            protocolLoggerClient.sendMessage(formatLogMessage("ERROR: Interrupted while enqueuing raw message from " + clientAddress + ": " + e.getMessage()));
        }
    }

    /**
     * Inner class to handle individual client connections for HartuServer.
     * It reads raw messages from its client and enqueues them into the server's rawMessageQueue.
     */
    private static class HartuClientHandler implements Runnable {
        private Socket clientSocket;
        private HartuServer server; // Reference to the outer HartuServer
        private BufferedReader in;
        private PrintWriter out;
        private volatile boolean clientConnected;
        private String clientAddress;

        public HartuClientHandler(Socket socket, HartuServer server) {
            this.clientSocket = socket;
            this.server = server;
            this.clientConnected = true;
            this.clientAddress = clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                // Send a greeting to the client
                out.println(server.formatLogMessage("HartuServer: Welcome! Send your robot commands."));

                String inputLine;
                while (clientConnected && (inputLine = in.readLine()) != null) {
                    server.protocolLoggerClient.sendMessage(server.formatLogMessage("HartuServer: Received from client " + this.clientAddress + ": " + inputLine));
                    server.enqueueRawMessage(inputLine, this.clientAddress); // Enqueue the received raw message
                    out.println(server.formatLogMessage("HartuServer: Command received and enqueued.")); // Acknowledge receipt
                }
            } catch (IOException e) {
                if (clientConnected) { // Only log if not intentionally disconnected
                    server.protocolLoggerClient.sendMessage(server.formatLogMessage("HartuServer Client Handler I/O error for " + this.clientAddress + ": " + e.getMessage()));
                }
            } finally {
                disconnect(); // Ensure proper cleanup
                server.removeHandler(this); // Notify server to remove this handler
            }
        }

        /**
         * Disconnects the client handler, closing streams and socket.
         */
        public void disconnect() {
            if (!clientConnected) {
                return;
            }
            clientConnected = false;
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                    server.protocolLoggerClient.sendMessage(server.formatLogMessage("Client " + this.clientAddress + " disconnected from HartuServer."));
                }
            } catch (IOException e) {
                server.protocolLoggerClient.sendMessage(server.formatLogMessage("ERROR: Error closing resources for client " + this.clientAddress + ": " + e.getMessage()));
            }
        }

        public String getClientAddress() {
            return this.clientAddress;
        }
    }
}
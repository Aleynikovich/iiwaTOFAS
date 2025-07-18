package hartu.communication.server;

import hartu.communication.client.LoggerClient; // For HartuServer's own logging

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * The server responsible solely for receiving raw protocol commands from clients.
 * It handles client connections and places received raw messages into a BlockingQueue
 * for further processing by another component.
 * It uses a LoggerClient for its own operational logging (e.g., connection events).
 */
public class HartuServer extends AbstractServer {

    private final LoggerClient protocolLoggerClient; // For logging HartuServer's own operations
    private final BlockingQueue<String> rawMessageQueue; // Queue to pass raw messages to the processing component

    // Executor service for handling client connections
    private ExecutorService clientHandlerExecutor;

    /**
     * Constructs the HartuServer.
     * @param port The port to listen on.
     * @param protocolLoggerClient Logger for HartuServer's own operations (connects to protocol log server).
     * @param rawMessageQueue The BlockingQueue where received raw messages will be placed.
     */
    public HartuServer(int port, LoggerClient protocolLoggerClient, BlockingQueue<String> rawMessageQueue) {
        super(port);
        if (protocolLoggerClient == null || rawMessageQueue == null) {
            throw new IllegalArgumentException("Protocol LoggerClient and Raw Message Queue must be non-null.");
        }
        this.protocolLoggerClient = protocolLoggerClient;
        this.rawMessageQueue = rawMessageQueue;

        this.clientHandlerExecutor = Executors.newCachedThreadPool(); // Use a cached thread pool for client handlers

        protocolLoggerClient.sendMessage(formatLogMessage(getServerName() + " initialized. Raw message queue ready."));
    }

    @Override
    protected String getServerName() {
        return "HartuServer";
    }

    @Override
    public void start() {
        if (isRunning) {
            protocolLoggerClient.sendMessage(formatLogMessage(getServerName() + " is already running on port " + port));
            return;
        }

        try {
            serverSocket = new ServerSocket(port);
            isRunning = true;
            protocolLoggerClient.sendMessage(formatLogMessage(getServerName() + " started on port " + port + " (" + getInetAddress().getHostAddress() + ")"));

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                protocolLoggerClient.sendMessage(formatLogMessage("Client connected to " + getServerName() + " from: " + clientSocket.getInetAddress().getHostAddress()));
                // Submit client handling to the executor service
                clientHandlerExecutor.submit(new HartuClientHandler(clientSocket, this));
            }

        } catch (IOException e) {
            if (isRunning) {
                protocolLoggerClient.sendMessage(formatLogMessage("ERROR: " + getServerName() + " error: " + e.getMessage()));
                System.err.println(getServerName() + " error: " + e.getMessage()); // Also print to console for server process
            } else {
                protocolLoggerClient.sendMessage(formatLogMessage(getServerName() + " shut down normally."));
                System.out.println(getServerName() + " shut down normally.");
            }
        } finally {
            stop(); // Ensure server resources are cleaned up
        }
    }

    @Override
    public void stop() {
        if (!isRunning) {
            protocolLoggerClient.sendMessage(formatLogMessage(getServerName() + " is not running."));
            return;
        }
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                protocolLoggerClient.sendMessage(formatLogMessage(getServerName() + " stopped."));
                System.out.println(getServerName() + " stopped.");
            }
        } catch (IOException e) {
            protocolLoggerClient.sendMessage(formatLogMessage("ERROR: Error closing " + getServerName() + " socket: " + e.getMessage()));
            System.err.println("Error closing " + getServerName() + " socket: " + e.getMessage());
        } finally {
            // Shut down the client handler executor
            clientHandlerExecutor.shutdown();
            try {
                if (!clientHandlerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    clientHandlerExecutor.shutdownNow();
                    protocolLoggerClient.sendMessage(formatLogMessage("WARNING: HartuServer client handler executor did not terminate cleanly."));
                }
            } catch (InterruptedException e) {
                clientHandlerExecutor.shutdownNow();
                Thread.currentThread().interrupt();
                protocolLoggerClient.sendMessage(formatLogMessage("WARNING: HartuServer client handler executor interrupted during shutdown."));
            }
        }
    }

    @Override
    protected void handleClient(Socket clientSocket) {
        // This method is now implicitly handled by the clientHandlerExecutor.submit(new HartuClientHandler(...)) call in start()
        // No direct implementation needed here.
    }

    /**
     * Called by HartuClientHandler to enqueue a raw message for processing.
     * @param rawMessage The raw string message received from a client.
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
     * It reads raw messages and passes them to the outer HartuServer for enqueuing.
     */
    private static class HartuClientHandler implements Runnable {
        private Socket clientSocket;
        private HartuServer server; // Reference to the outer HartuServer instance
        private BufferedReader in;
        private PrintWriter out; // For sending responses back to client, if needed

        public HartuClientHandler(Socket socket, HartuServer server) {
            this.clientSocket = socket;
            this.server = server;
        }

        @Override
        public void run() {
            String clientAddress = clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true); // Auto-flush

                // Send a greeting to the client connecting to HartuServer
                out.println(server.formatLogMessage("HartuServer: Welcome! Send your robot commands."));

                String commandLine;
                while ((commandLine = in.readLine()) != null) {
                    server.enqueueRawMessage(commandLine, clientAddress); // Enqueue the raw message
                    // Optionally, send a response back to the client after enqueuing
                    // out.println(server.formatLogMessage("HartuServer: Command received and enqueued."));
                }
            } catch (IOException e) {
                // Log the exception using the server's protocol logger
                server.protocolLoggerClient.sendMessage(server.formatLogMessage("Client handler error for " + clientAddress + ": " + e.getMessage()));
            } finally {
                try {
                    if (out != null) out.close();
                    if (in != null) in.close();
                    if (clientSocket != null && !clientSocket.isClosed()) {
                        clientSocket.close();
                        server.protocolLoggerClient.sendMessage(server.formatLogMessage("Client " + clientAddress + " disconnected from HartuServer."));
                    }
                } catch (IOException e) {
                    server.protocolLoggerClient.sendMessage(server.formatLogMessage("Error closing resources for client " + clientAddress + ": " + e.getMessage()));
                }
            }
        }
    }
}
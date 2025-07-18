package hartu.communication.server;

import hartu.communication.client.LoggerClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class HartuServer extends AbstractServer {

    private final LoggerClient protocolLoggerClient;
    private final BlockingQueue<String> rawMessageQueue;

    private final List<HartuClientHandler> connectedHandlers;
    private ExecutorService clientHandlerExecutor;

    public HartuServer(int port, LoggerClient protocolLoggerClient, BlockingQueue<String> rawMessageQueue) {
        super(port);
        if (protocolLoggerClient == null || rawMessageQueue == null) {
            throw new IllegalArgumentException("Protocol LoggerClient and Raw Message Queue must be non-null.");
        }
        this.protocolLoggerClient = protocolLoggerClient;
        this.rawMessageQueue = rawMessageQueue;
        this.connectedHandlers = new CopyOnWriteArrayList<>();

        this.clientHandlerExecutor = Executors.newCachedThreadPool();

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
                HartuClientHandler handler = new HartuClientHandler(clientSocket, this);
                connectedHandlers.add(handler);
                clientHandlerExecutor.submit(handler);
            }

        } catch (IOException e) {
            if (isRunning) {
                protocolLoggerClient.sendMessage(formatLogMessage("ERROR: " + getServerName() + " error: " + e.getMessage()));
                System.err.println(getServerName() + " error: " + e.getMessage());
            } else {
                protocolLoggerClient.sendMessage(formatLogMessage(getServerName() + " stopped due to IOException: " + e.getMessage()));
                System.out.println(getServerName() + " stopped due to IOException.");
            }
        } finally {
            // No explicit stop() call here.
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
                protocolLoggerClient.sendMessage(formatLogMessage(getServerName() + " server socket closed."));
                System.out.println(getServerName() + " server socket closed.");
            }
        } catch (IOException e) {
            protocolLoggerClient.sendMessage(formatLogMessage("ERROR: Error closing " + getServerName() + " server socket: " + e.getMessage()));
            System.err.println("Error closing " + getServerName() + " server socket: " + e.getMessage());
        } finally {
            for (HartuClientHandler handler : connectedHandlers) {
                handler.disconnect();
            }
            connectedHandlers.clear();

            clientHandlerExecutor.shutdown();
            try {
                if (!clientHandlerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    clientHandlerExecutor.shutdownNow();
                    protocolLoggerClient.sendMessage(formatLogMessage("WARNING: HartuServer client handler executor did not terminate cleanly. Forced shutdown."));
                }
            } catch (InterruptedException e) {
                clientHandlerExecutor.shutdownNow();
                Thread.currentThread().interrupt();
                protocolLoggerClient.sendMessage(formatLogMessage("WARNING: HartuServer client handler executor interrupted during shutdown. Forced shutdown."));
            }
            protocolLoggerClient.sendMessage(formatLogMessage(getServerName() + " stopped."));
            System.out.println(getServerName() + " stopped.");
        }
    }

    @Override
    protected void handleClient(Socket clientSocket) {
        // This method is conceptually handled by the clientHandlerExecutor.submit(handler) in start()
    }

    public void enqueueRawMessage(String rawMessage, String clientAddress) {
        try {
            rawMessageQueue.put(rawMessage);
            protocolLoggerClient.sendMessage(formatLogMessage("Enqueued raw message from " + clientAddress + ": " + rawMessage));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            protocolLoggerClient.sendMessage(formatLogMessage("ERROR: Interrupted while enqueuing raw message from " + clientAddress + ": " + e.getMessage()));
        }
    }

    protected void removeHandler(HartuClientHandler handler) {
        connectedHandlers.remove(handler);
        protocolLoggerClient.sendMessage(formatLogMessage("Handler removed for client: " + handler.getClientAddress()));
    }

    private static class HartuClientHandler implements Runnable {
        private Socket clientSocket;
        private HartuServer server;
        private BufferedReader in;
        private PrintWriter out;
        private volatile boolean clientConnected;
        private String clientAddress; // Made into a field

        public HartuClientHandler(Socket socket, HartuServer server) {
            this.clientSocket = socket;
            this.server = server;
            this.clientConnected = true;
            this.clientAddress = clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort(); // Initialize here
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                out.println(server.formatLogMessage("HartuServer: Welcome! Send your robot commands."));

                String commandLine;
                while (clientConnected && (commandLine = in.readLine()) != null) {
                    server.enqueueRawMessage(commandLine, this.clientAddress); // Use this.clientAddress
                }
            } catch (IOException e) {
                if (clientConnected) {
                    server.protocolLoggerClient.sendMessage(server.formatLogMessage("Client handler I/O error for " + this.clientAddress + ": " + e.getMessage())); // Use this.clientAddress
                }
            } finally {
                disconnect();
                server.removeHandler(this);
            }
        }

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
                    server.protocolLoggerClient.sendMessage(server.formatLogMessage("Client " + this.clientAddress + " disconnected from HartuServer.")); // Use this.clientAddress
                }
            } catch (IOException e) {
                server.protocolLoggerClient.sendMessage(server.formatLogMessage("ERROR: Error closing resources for client " + this.clientAddress + ": " + e.getMessage())); // Use this.clientAddress
            }
        }

        public String getClientAddress() {
            return clientSocket != null ? clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort() : "Unknown";
        }
    }
}
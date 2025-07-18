package hartu.robot.communication.server.hartu;

import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream; // For the logging client
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException; // For specific error handling
import java.util.concurrent.TimeUnit;

import hartu.robot.utils.CommandParser;
import hartu.robot.commands.ParsedCommand;

public class hartuReceiverServer extends RoboticsAPICyclicBackgroundTask {

    // Command Server (Robot as Server)
    private static final int COMMAND_SERVER_PORT = 30001;
    private ServerSocket commandServerSocket;
    private Socket clientCommandSocket; // Socket for the connected command client

    // Logging Client (Robot as Client)
    private static final String UBUNTU_LOG_SERVER_IP = "10.66.171.69";
    private static final int LOG_SERVER_PORT = 30004; // New port for logs
    private Socket logClientSocket;
    private OutputStream logOutputStream;
    private boolean logConnectionEstablished = false;
    private static final long LOG_RECONNECT_DELAY_MS = 5000; // 5 seconds for log client reconnection

    @Override
    public void initialize() {
        initializeCyclic(0, 500, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);

        // --- Initialize Command Server ---
        try {
            commandServerSocket = new ServerSocket(COMMAND_SERVER_PORT);
            logToClient("hartuReceiverServer: Command server initialized on port " + COMMAND_SERVER_PORT);
        } catch (IOException e) {
            logToClient("hartuReceiverServer: Could not initialize command server socket: " + e.getMessage());
        }

        // --- Initialize Logging Client ---
        connectToLogServer(); // Attempt initial connection to log server
    }

    // Method to connect to the dedicated log server on Ubuntu
    private void connectToLogServer() {
        try {
            closeLogClientConnection(); // Ensure a clean slate before connecting
            logClientSocket = new Socket(UBUNTU_LOG_SERVER_IP, LOG_SERVER_PORT);
            logOutputStream = logClientSocket.getOutputStream();
            logConnectionEstablished = true;
            logToClient("hartuReceiverServer: Successfully connected to log server at " + UBUNTU_LOG_SERVER_IP + ":" + LOG_SERVER_PORT);
        } catch (IOException e) {
            logToClient("hartuReceiverServer: Error connecting to log server: " + e.getMessage());
            logConnectionEstablished = false;
        } catch (Exception e) {
            logToClient("hartuReceiverServer: Unexpected error during log client connection: " + e.getMessage());
            logConnectionEstablished = false;
        }
    }

    // Centralized logging method that sends messages to the log server
    private void logToClient(String message) {
        if (!logConnectionEstablished) {
            // If connection is not established, try to queue or just return.
            // For simplicity in this direct integration, we'll just skip if not connected.
            // In a more robust system, you might queue messages for later sending.
            return;
        }
        try {
            byte[] data = (message + "\n").getBytes("UTF-8");
            logOutputStream.write(data);
            logOutputStream.flush();
        } catch (IOException e) {
            // Log this error via internal KUKA logger if log client fails,
            // or simply set flag to false to trigger reconnection.
            logConnectionEstablished = false;
            closeLogClientConnection(); // Clean up on send error
        } catch (Exception e) {
            logConnectionEstablished = false;
            closeLogClientConnection();
        }
    }


    @Override
    public void runCyclic() {
        // --- Manage Command Server Client Acceptance ---
        if (commandServerSocket == null || commandServerSocket.isClosed()) {
            logToClient("hartuReceiverServer: Command server socket is not active. Attempting to re-initialize.");
            try {
                commandServerSocket = new ServerSocket(COMMAND_SERVER_PORT);
                logToClient("hartuReceiverServer: Command server re-initialized on port " + COMMAND_SERVER_PORT);
            } catch (IOException e) {
                logToClient("hartuReceiverServer: Failed to re-initialize command server socket: " + e.getMessage());
                return;
            }
        }

        if (clientCommandSocket == null || clientCommandSocket.isClosed()) {
            logToClient("hartuReceiverServer: Waiting for new command client connection...");
            try {
                clientCommandSocket = commandServerSocket.accept();
                logToClient("hartuReceiverServer: Command client connected from " + clientCommandSocket.getInetAddress());
                new Thread(new CommandClientHandler(clientCommandSocket)).start();

            } catch (IOException e) {
                logToClient("hartuReceiverServer: Error accepting command client connection: " + e.getMessage());
                clientCommandSocket = null;
            }
        }

        // --- Manage Logging Client Reconnection ---
        if (!logConnectionEstablished) {
            logToClient("hartuReceiverServer: Log client not connected. Attempting to reconnect...");
            connectToLogServer(); // Attempt to reconnect
            if (!logConnectionEstablished) {
                // If reconnection failed, pause before next attempt to avoid busy-loop
                try {
                    TimeUnit.MILLISECONDS.sleep(LOG_RECONNECT_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Override
    public void dispose() {
        // --- Dispose Command Server ---
        try {
            if (clientCommandSocket != null && !clientCommandSocket.isClosed()) {
                clientCommandSocket.close();
            }
            if (commandServerSocket != null && !commandServerSocket.isClosed()) {
                commandServerSocket.close();
            }
            logToClient("hartuReceiverServer: Command server socket disposed.");
        } catch (IOException e) {
            logToClient("hartuReceiverServer: Error while disposing command server socket: " + e.getMessage());
        }

        // --- Dispose Logging Client ---
        logToClient("hartuReceiverServer: Disposing log client connection.");
        closeLogClientConnection();
    }

    // Helper method to close the log client connection cleanly
    private void closeLogClientConnection() {
        try {
            if (logOutputStream != null) {
                logOutputStream.close();
            }
            if (logClientSocket != null && !logClientSocket.isClosed()) {
                logClientSocket.close();
            }
        } catch (IOException e) {
            // No logging here as per user's instruction.
        } finally {
            logOutputStream = null;
            logClientSocket = null;
            logConnectionEstablished = false;
        }
    }

    // Inner class to handle communication with a single command client
    private class CommandClientHandler implements Runnable {
        private Socket clientSocket;

        public CommandClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
            ) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    logToClient("hartuReceiverServer: Received: " + inputLine);
                    try {
                        ParsedCommand command = CommandParser.parse(inputLine);
                        logToClient("hartuReceiverServer: Parsed command ID: " + command.getId() + ", ActionType: " + command.getActionType().name());

                        // Placeholder for command execution - this is where CommandExecutor will go
                        // For now, it just simulates success.
                        // Thread.sleep(100);

                        out.println("#FREE");
                        logToClient("hartuReceiverServer: Sent #FREE for ID: " + command.getId());

                    } catch (IllegalArgumentException e) {
                        logToClient("hartuReceiverServer: Parsing error: " + e.getMessage());
                        out.println("#ERROR");
                    } catch (Exception e) {
                        logToClient("hartuReceiverServer: Unexpected error during command processing: " + e.getMessage());
                        out.println("#ERROR");
                    }
                }
            } catch (IOException e) {
                logToClient("hartuReceiverServer: CommandClientHandler I/O error for " + clientSocket.getInetAddress() + ": " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                    logToClient("hartuReceiverServer: Command client disconnected: " + clientSocket.getInetAddress());
                } catch (IOException e) {
                    logToClient("hartuReceiverServer: Error closing command client socket: " + e.getMessage());
                }
            }
        }
    }
}

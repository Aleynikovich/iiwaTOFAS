package hartu.robot.communication.server.hartu;

import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import hartu.robot.utils.CommandParser;
import hartu.robot.commands.ParsedCommand;

public class hartuReceiverServer extends RoboticsAPICyclicBackgroundTask {

    private static final int PORT = 30001;
    private ServerSocket serverSocket;
    private Socket clientSocket; // To hold the currently accepted client socket

    @Override
    public void initialize() {
        initializeCyclic(0, 500, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("CommandReceiverServer: Server socket initialized on port " + PORT);
        } catch (IOException e) {
            System.err.println("CommandReceiverServer: Could not initialize server socket: " + e.getMessage());
            // In a real application, you might want to log this more robustly
            // or even stop the task if initialization fails.
        }
    }

    @Override
    public void runCyclic() {
        if (serverSocket == null || serverSocket.isClosed()) {
            System.err.println("CommandReceiverServer: Server socket is not active. Attempting to re-initialize.");
            try {
                serverSocket = new ServerSocket(PORT);
                System.out.println("CommandReceiverServer: Server socket re-initialized on port " + PORT);
            } catch (IOException e) {
                System.err.println("CommandReceiverServer: Failed to re-initialize server socket: " + e.getMessage());
                return; // Exit this cycle if re-initialization fails
            }
        }

        if (clientSocket == null || clientSocket.isClosed()) {
            System.out.println("CommandReceiverServer: Waiting for new client connection...");
            try {
                // This call is blocking. It waits until a client connects.
                // The runCyclic() method will pause here until a connection is established.
                clientSocket = serverSocket.accept();
                System.out.println("CommandReceiverServer: Client connected from " + clientSocket.getInetAddress());

                // Hand off the client communication to a new thread
                // This prevents runCyclic from blocking while reading commands
                // and allows it to accept new clients if the current one disconnects
                // or if you want to support multiple concurrent clients.
                new Thread(new ClientHandler(clientSocket)).start();

            } catch (IOException e) {
                System.err.println("CommandReceiverServer: Error accepting client connection: " + e.getMessage());
                clientSocket = null; // Reset clientSocket to null to try accepting again next cycle
            }
        }
        // If clientSocket is not null and not closed, it means a client is connected
        // and its communication is being handled by ClientHandler in a separate thread.
        // runCyclic will quickly return and be ready for the next cycle.
    }

    @Override
    public void dispose() {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            System.out.println("CommandReceiverServer: Server socket disposed.");
        } catch (IOException e) {
            System.err.println("CommandReceiverServer: Error while disposing server socket: " + e.getMessage());
        }
    }

    // Inner class to handle communication with a single client in a separate thread
    private static class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
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
                    System.out.println("CommandReceiverServer: Received: " + inputLine);
                    try {
                        ParsedCommand command = CommandParser.parse(inputLine);
                        System.out.println("CommandReceiverServer: Parsed command ID: " + command.getId() + ", ActionType: " + command.getActionType().name());

                        // Placeholder for command execution
                        // In a real scenario, you'd pass 'command' to your CommandExecutor
                        // and wait for its completion to send #FREE or #ERROR
                        // For now, we'll just simulate the response.
                        // Thread.sleep(100); // Simulate work

                        out.println("#FREE"); // Send response back to client
                        System.out.println("CommandReceiverServer: Sent #FREE for ID: " + command.getId());

                    } catch (IllegalArgumentException e) {
                        System.err.println("CommandReceiverServer: Parsing error: " + e.getMessage());
                        out.println("#ERROR"); // Send error response
                    } catch (Exception e) {
                        System.err.println("CommandReceiverServer: Unexpected error during command processing: " + e.getMessage());
                        out.println("#ERROR"); // Send error response for other exceptions
                    }
                }
            } catch (IOException e) {
                System.err.println("CommandReceiverServer: ClientHandler I/O error for " + clientSocket.getInetAddress() + ": " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                    System.out.println("CommandReceiverServer: Client disconnected: " + clientSocket.getInetAddress());
                } catch (IOException e) {
                    System.err.println("CommandReceiverServer: Error closing client socket: " + e.getMessage());
                }
            }
        }
    }
}

package hartu.communication.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Handles communication with a single log client connected to the LogServer.
 * Each client connection gets its own handler thread.
 */
public class LogClientHandler extends Thread {
    private Socket clientSocket;
    private LogServer server; // Reference to the LogServer to access formatLogMessage
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean clientConnected;

    public LogClientHandler(Socket clientSocket, LogServer server) {
        this.clientSocket = clientSocket;
        this.server = server;
        this.clientConnected = true;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // --- Send Greeting Message on Connection, now formatted by the server's method ---
            // Use the server's formatLogMessage to ensure consistency
            String greeting = server.formatLogMessage("Welcome! You are connected to the " + server.getServerName() + ".");
            sendMessage(greeting);

            String inputLine;
            while (clientConnected && (inputLine = in.readLine()) != null) {
                System.out.println("LogClientHandler received from " + getClientAddress() + ": " + inputLine);
                server.publish(inputLine);
            }
        } catch (IOException e) {
            if (clientConnected) {
                System.err.println("LogClientHandler error for " + getClientAddress() + ": " + e.getMessage());
            }
        } finally {
            disconnect();
            server.removeHandler(this);
        }
    }

    /**
     * Sends a raw message to this specific client.
     * The message is assumed to be already formatted (e.g., timestamped).
     * @param message The message string to send.
     */
    public void sendMessage(String message) {
        if (out != null && clientConnected) {
            out.println(message);
        } else {
            System.err.println("LogClientHandler: Cannot send message, client not connected or output stream not ready.");
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
            }
            System.out.println("LogClientHandler: Client " + getClientAddress() + " disconnected.");
        } catch (IOException e) {
            System.err.println("LogClientHandler: Error closing client socket for " + getClientAddress() + ": " + e.getMessage());
        }
    }

    /**
     * Returns the client's address for logging purposes.
     */
    public String getClientAddress() {
        return clientSocket != null ? clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort() : "Unknown";
    }

    /**
     * Checks if the client is still connected.
     * @return true if connected, false otherwise.
     */
    public boolean isClientConnected() {
        return clientConnected && clientSocket != null && !clientSocket.isClosed();
    }
}
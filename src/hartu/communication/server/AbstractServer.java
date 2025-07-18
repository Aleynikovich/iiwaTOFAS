package hartu.communication.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat; // Import SimpleDateFormat
import java.util.Date; // Import Date

/**
 * Abstract base class for server implementations.
 * Provides common server functionalities like port management, running status,
 * and a template for starting, stopping, and handling client connections.
 * Also includes common logging utilities like timestamping.
 */
public abstract class AbstractServer
{
    protected int port;
    protected ServerSocket serverSocket;
    protected volatile boolean isRunning; // Use volatile for thread visibility
    protected final SimpleDateFormat dateFormat; // Date format for logging

    public AbstractServer(int port)
    {
        this.port = port;
        this.isRunning = false;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"); // Initialize date format
    }

    /**
     * Gets the name of the server implementation.
     * @return The server's name.
     */
    protected abstract String getServerName();

    /**
     * Starts the server, making it listen for incoming client connections.
     */
    public abstract void start();

    /**
     * Stops the server, closing the server socket and any active connections.
     */
    public abstract void stop();

    /**
     * Handles a new client connection.
     * @param clientSocket The socket representing the new client connection.
     */
    protected abstract void handleClient(Socket clientSocket);

    /**
     * Retrieves the InetAddress of the server socket.
     * @return The InetAddress of the server.
     * @throws UnknownHostException If the local host name could not be resolved into an address.
     */
    protected InetAddress getInetAddress() throws UnknownHostException
    {
        // Get local host address if serverSocket is not yet bound
        return (serverSocket != null && serverSocket.isBound()) ? serverSocket.getInetAddress() : InetAddress.getLocalHost();
    }

    /**
     * Formats a raw log message by prepending a timestamp and the server's name.
     * This method is intended to be called by subclasses before publishing a message.
     * @param rawMessage The original message string.
     * @return The timestamped and formatted message string.
     */
    protected String formatLogMessage(String rawMessage) {
        return dateFormat.format(new Date()) + " [" + getServerName() + "] " + rawMessage;
    }
}
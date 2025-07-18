package hartu.communication.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Abstract base class for server implementations, implementing the IServer interface.
 * Provides common server functionalities like port management, running status,
 * and a template for starting, stopping, and handling client connections.
 * Also includes common logging utilities like timestamping.
 */
public abstract class AbstractServer implements IServer // <--- ADDED: Implements IServer
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
    public abstract void start() throws IOException; // Added throws IOException as concrete implementations might throw it

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
     * Implements isRunning() from IServer.
     * @return true if the server is currently running and accepting connections, false otherwise.
     */
    @Override // <--- ADDED: @Override for IServer method
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Implements getPort() from IServer.
     * @return The port number on which the server is listening.
     */
    @Override // <--- ADDED: @Override for IServer method
    public int getPort() {
        return port;
    }

    /**
     * Implements getInetAddress() from IServer.
     * Retrieves the InetAddress of the server socket.
     * @return The InetAddress of the server.
     * @throws UnknownHostException If the local host name could not be resolved into an address.
     */
    @Override // <--- ADDED: @Override for IServer method
    public InetAddress getInetAddress() throws UnknownHostException
    {
        // Get local host address if serverSocket is not yet bound or is closed
        if (serverSocket != null && serverSocket.isBound() && !serverSocket.isClosed()) {
            return serverSocket.getInetAddress();
        } else {
            // Fallback to local host if server socket is not available or not bound
            return InetAddress.getLocalHost();
        }
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
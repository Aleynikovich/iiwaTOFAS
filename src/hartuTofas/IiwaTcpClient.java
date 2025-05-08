package hartuTofas;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

/**
 * IiwaTcpClient class to manage TCP communication with a server.
 */
public class IiwaTcpClient {

    private final String serverIp;
    private final int serverPort;
    private Socket socket;
    private OutputStream outputStream;
    private static final int CONNECT_TIMEOUT = 5000; // 5 seconds timeout for connection

    /**
     * Constructor for IiwaTcpClient.
     *
     * @param serverIp The IP address of the server.
     * @param serverPort The port number of the server.
     */
    public IiwaTcpClient(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    /**
     * Establishes a connection to the server.
     *
     * @throws IOException If an I/O error occurs during the connection attempt.
     * @throws TimeoutException If the connection attempt times out.
     */
    public void connect() throws IOException, TimeoutException {
        try {
            socket = new Socket(serverIp, serverPort);
            socket.setSoTimeout(CONNECT_TIMEOUT); // Set timeout for the connection
            outputStream = socket.getOutputStream();
        } catch (ConnectException e) {
            throw new ConnectException("Failed to connect to " + serverIp + ":" + serverPort + ".  " + e.getMessage()); //wrap
        } catch (SocketException e) {
            if (e.getMessage().contains("connect timed out")) {
                throw new TimeoutException("Connection to " + serverIp + ":" + serverPort + " timed out.");
            } else {
                throw e; // Re-throw other SocketExceptions
            }
        }
    }

    /**
     * Sends a string message to the server.
     *
     * @param message The string message to send.
     * @throws IOException If an I/O error occurs while sending the message.
     */
    public void send(String message) throws IOException {
        if (outputStream != null) {
            outputStream.write(message.getBytes());
            outputStream.flush();
        } else {
            throw new IOException("Output stream is not initialized.  Call connect() first.");
        }
    }

     /**
     * Sends a string message to the server without waiting for a reply.  This is
     * generally more efficient for high-frequency data like joint states.
     *
     * @param message The string message to send.
     * @throws IOException If an I/O error occurs while sending the message.
     */
    public void sendOnly(String message) throws IOException {
        if (outputStream != null) {
            outputStream.write(message.getBytes());
            outputStream.flush();
        } else {
            throw new IOException("Output stream is not initialized. Call connect() first.");
        }
    }

    /**
     * Closes the connection to the server.
     */
    public void closeConnection() {
        try {
            if (outputStream != null) {
                outputStream.close();
            }
            if (socket != null) {
                socket.close();
            }
            outputStream = null;
            socket = null;
        } catch (IOException e) {
            // Log the error.  It's usually safe to ignore it, as we're closing anyway.
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    /**
     * Checks if the socket is connected.
     * @return true if the socket is connected, false otherwise
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}


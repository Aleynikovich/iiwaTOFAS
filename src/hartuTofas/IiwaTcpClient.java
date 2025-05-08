package hartuTofas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A TCP client for communicating with a KUKA IIWA robot.
 * This class provides functionality to connect to the robot, send commands,
 * receive responses, and handle communication timeouts.
 * It is designed to be used within the KUKA Sunrise Workbench environment.
 */
public class IiwaTcpClient {

    private String serverAddress;
    private int serverPort;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private static final int DEFAULT_TIMEOUT_MS = 5000; // Default timeout
    private int timeoutMs; // Configurable timeout
    private boolean isConnected = false;

    /**
     * Constructor for the IiwaTcpClient class. Uses the default timeout.
     *
     * @param serverAddress The IP address of the KUKA robot controller.
     * @param serverPort    The port number for the TCP connection on the robot controller.
     */
    public IiwaTcpClient(String serverAddress, int serverPort) {
        this(serverAddress, serverPort, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Constructor for the IiwaTcpClient class with a configurable timeout.
     *
     * @param serverAddress The IP address of the KUKA robot controller.
     * @param serverPort    The port number for the TCP connection on the robot controller.
     * @param timeoutMs     The timeout in milliseconds for socket operations.
     */
    public IiwaTcpClient(String serverAddress, int serverPort, int timeoutMs) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.timeoutMs = timeoutMs;
    }

    /**
     * Establishes a TCP connection with the KUKA robot controller.
     *
     * @throws IOException           If an error occurs during the connection process.
     * @throws TimeoutException      If the connection attempt times out.
     */
    public void connect() throws IOException, TimeoutException {
        final CountDownLatch connectionLatch = new CountDownLatch(1);
        final AtomicReference<IOException> connectionError = new AtomicReference<>();

        // Use a separate thread for the connection to handle the timeout
        Thread connectionThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(serverAddress, serverPort);
                    socket.setSoTimeout(timeoutMs); // Apply the timeout to the socket
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    isConnected = true;
                    connectionLatch.countDown(); // Signal successful connection
                } catch (IOException e) {
                    connectionError.set(e);
                    connectionLatch.countDown(); // Signal connection failure
                }
            }
        });
        connectionThread.start();

        try {
            // Wait for the connection to be established or the timeout to occur
            if (!connectionLatch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                // If the latch didn't count down, it means the timeout occurred
                connectionThread.interrupt(); // Attempt to interrupt the connection thread
                closeConnection(); // Clean up resources
                throw new TimeoutException("Connection to " + serverAddress + ":" + serverPort + " timed out after " + timeoutMs + "ms");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore the interrupted status
            closeConnection();
            throw new IOException("Connection attempt was interrupted", e); // Wrap in IOException
        }

        // Check for any exceptions that occurred in the connection thread
        if (connectionError.get() != null) {
            closeConnection(); // Clean up resources
            throw connectionError.get(); // Throw the exception that occurred in the thread
        }
    }

    /**
     * Sends a command to the KUKA robot controller and waits for a response.
     *
     * @param command The command string to send.
     * @return The response string from the robot controller.
     * @throws IOException           If an error occurs during sending or receiving data.
     * @throws TimeoutException      If no response is received within the timeout period.
     * @throws IllegalStateException If the client is not connected.
     */
    public String sendCommand(final String command) throws IOException, TimeoutException, IllegalStateException {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected to the robot. Call connect() first.");
        }

        final CountDownLatch responseLatch = new CountDownLatch(1);
        final AtomicReference<String> receivedResponse = new AtomicReference<>();
        final AtomicReference<IOException> sendError = new AtomicReference<>();

        // Use a separate thread for sending and receiving
        Thread sendReceiveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    out.println(command);
                    out.flush();
                    String response = in.readLine(); // This will block until a line is received
                    if (response != null) {
                        receivedResponse.set(response);
                    } else {
                        sendError.set(new IOException("Received null response from server"));
                    }
                    responseLatch.countDown();
                } catch (IOException e) {
                    sendError.set(e);
                    responseLatch.countDown();
                }
            }
        });
        sendReceiveThread.start();

        try {
            // Wait for the response or timeout
            if (!responseLatch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                sendReceiveThread.interrupt();
                throw new TimeoutException("Timeout waiting for response after sending command: " + command + " after " + timeoutMs + "ms");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for response", e);
        }

        if (sendError.get() != null) {
            throw sendError.get(); // Throw any errors from the send/receive thread
        }
        return receivedResponse.get();
    }

    /**
     * Closes the TCP connection to the KUKA robot controller.
     * This method should be called when communication is no longer needed
     * to release resources.
     */
    public void closeConnection() {
        if (isConnected()) {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                // Log the error (important!) -  DON'T throw from close(), may mask original error
                System.err.println("Error closing connection: " + e.getMessage());
            } finally {
                // Ensure that the connection status is updated.
                isConnected = false;
                in = null;
                out = null;
                socket = null;
            }
        }
    }

    /**
     * Checks if the client is currently connected to the server.
     *
     * @return true if the client is connected, false otherwise.
     */
    public boolean isConnected() {
        return isConnected;
    }

     /**
     * Gets the connection timeout value.
     * @return The timeout in milliseconds.
     */
    public int getTimeoutMs() {
        return timeoutMs;
    }

    /**
     * Sets the connection timeout value.
     * @param timeoutMs The timeout in milliseconds.
     */
    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
        if (socket != null) {
            try {
                socket.setSoTimeout(timeoutMs); // Update the timeout on the existing socket
            } catch (IOException e) {
                System.err.println("Error setting timeout: " + e.getMessage());
                // Consider if you want to throw an exception here.  It might be better to
                // just log it and allow the program to continue, as a timeout change
                // isn't usually a critical error.
            }
        }
    }
}

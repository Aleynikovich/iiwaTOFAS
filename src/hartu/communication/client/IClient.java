package hartu.communication.client;

public interface IClient {
    /**
     * Establishes a connection to the server.
     */
    void connect();

    /**
     * Disconnects from the server.
     */
    void disconnect();

    /**
     * Sends a message to the server.
     * @param message The string message to send.
     */
    void sendMessage(String message);

    /**
     * Checks if the client is currently connected to the server.
     * @return true if connected, false otherwise.
     */
    boolean isConnected();
}
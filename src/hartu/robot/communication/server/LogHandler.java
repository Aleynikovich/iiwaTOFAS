package hartu.robot.communication.server;

/**
 * Interface for handling log messages with different output destinations.
 * Implementations can send logs to console, network clients, files, etc.
 */
public interface LogHandler
{
    /**
     * Sends a formatted log message to the handler's destination.
     * 
     * @param formattedMessage The fully formatted message including timestamp and tag
     */
    void sendMessage(String formattedMessage);
    
    /**
     * Checks if this handler is currently active and able to send messages.
     * 
     * @return True if the handler can send messages, false otherwise
     */
    boolean isActive();
    
    /**
     * Closes this handler and releases any resources.
     */
    void close();
}

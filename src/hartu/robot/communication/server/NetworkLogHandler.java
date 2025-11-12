package hartu.robot.communication.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Log handler that broadcasts messages to multiple connected network clients.
 * Supports simultaneous connections from multiple log clients (e.g., Python log client).
 * Works with existing ClientHandler infrastructure instead of managing sockets directly.
 */
public class NetworkLogHandler implements LogHandler
{
    private final Map<String, ClientHandler> connectedClients = new ConcurrentHashMap<>();
    private boolean active = true;
    
    /**
     * Adds a new network client to receive log messages.
     * 
     * @param clientId Unique identifier for the client
     * @param clientHandler The ClientHandler managing this client's connection
     */
    public void addClient(String clientId, ClientHandler clientHandler)
    {
        if (clientHandler != null)
        {
            connectedClients.put(clientId, clientHandler);
        }
    }
    
    /**
     * Removes a client from receiving log messages.
     * 
     * @param clientId The identifier of the client to remove
     */
    public void removeClient(String clientId)
    {
        connectedClients.remove(clientId);
    }
    
    @Override
    public void sendMessage(String formattedMessage)
    {
        if (!active || connectedClients.isEmpty())
        {
            return;
        }
        
        // Broadcast to all connected clients
        for (Map.Entry<String, ClientHandler> entry : connectedClients.entrySet())
        {
            String clientId = entry.getKey();
            ClientHandler handler = entry.getValue();
            
            try
            {
                if (handler != null)
                {
                    handler.sendMessage(formattedMessage);
                }
            }
            catch (Exception e)
            {
                // Client disconnected or error, remove it
                removeClient(clientId);
            }
        }
    }
    
    @Override
    public boolean isActive()
    {
        return active;
    }
    
    @Override
    public void close()
    {
        active = false;
        connectedClients.clear();
    }
    
    /**
     * Returns the number of currently connected clients.
     * 
     * @return The number of connected clients
     */
    public int getClientCount()
    {
        return connectedClients.size();
    }
}

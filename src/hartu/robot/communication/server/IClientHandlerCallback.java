// --- IClientHandlerCallback.java ---
package hartu.robot.communication.server;

public interface IClientHandlerCallback
{
    void onClientConnected(ClientHandler handler, String listenerName);
}

package hartu.robot.communication.server;

import hartu.protocols.constants.ProtocolConstants.ListenerType;

public interface IClientHandlerCallback
{
    void onClientConnected(ClientHandler handler, ListenerType listenerType);
}

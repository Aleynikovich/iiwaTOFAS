package hartu.robot.communication.client;

import hartu.protocols.constants.ProtocolConstants;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class ClientClass
{
    private final String serverIp;
    private final int serverPort;
    private Socket clientSocket;
    private OutputStream out;

    public ClientClass(String serverIp, int serverPort)
    {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    public void connect() throws IOException
    {
        clientSocket = new Socket(serverIp, serverPort);
        out = clientSocket.getOutputStream();
    }

    public void sendMessage(String message) throws IOException
    {
        if (out != null)
        {
            out.write(message.getBytes());
            out.flush();
        }
    }

    public void close() throws IOException
    {
        if (out != null)
        {
            out.close();
        }
        if (clientSocket != null && !clientSocket.isClosed())
        {
            clientSocket.close();
        }
    }

    public boolean isConnected()
    {
        return clientSocket != null && clientSocket.isConnected() && !clientSocket.isClosed();
    }

    public String getServerIp()
    {
        return serverIp;
    }

    public int getServerPort()
    {
        return serverPort;
    }
}

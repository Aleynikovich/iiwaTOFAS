package hartu.robot.communication.client;

import hartu.protocols.constants.ProtocolConstants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * @deprecated This class is deprecated as it was used by the now-deprecated Ros2ClientManager.
 * The robot now acts as a server for joint state data instead of a client.
 * See {@link hartu.robot.communication.server.JointStateServerManager}.
 */
@Deprecated
public class ClientClass
{
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private final String serverIp;
    private final int serverPort;

    public ClientClass(String serverIp, int serverPort)
    {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    public void connect() throws IOException
    {
        clientSocket = new Socket(serverIp, serverPort);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    public void sendMessage(String message)
    {
        if (out != null)
        {
            out.print(message);
            out.flush();
        }
    }

    public String readMessage() throws IOException
    {
        StringBuilder messageBuilder = new StringBuilder();
        int charCode;
        while ((charCode = in.read()) != -1)
        {
            char c = (char) charCode;
            if (c == ProtocolConstants.MESSAGE_TERMINATOR.charAt(0))
            {
                break;
            }
            messageBuilder.append(c);
        }
        return messageBuilder.toString();
    }

    public void close() throws IOException
    {
        if (in != null)
        {
            in.close();
        }
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

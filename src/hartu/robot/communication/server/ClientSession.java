package hartu.robot.communication.server;

import hartu.protocols.constants.ProtocolConstants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;

public class ClientSession
{
    private final Socket socket;
    private final PrintWriter writer;
    private final BufferedReader reader;
    private final ProtocolConstants.ListenerType clientType;
    private final String sessionId;
    private final String clientName;

    public ClientSession(Socket socket, ProtocolConstants.ListenerType clientType, String clientName) throws IOException
    {
        this.socket = socket;
        this.writer = new PrintWriter(socket.getOutputStream(), true);
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.clientType = clientType;
        this.sessionId = UUID.randomUUID().toString();
        this.clientName = clientName;
    }

    public Socket getSocket()
    {
        return socket;
    }

    public PrintWriter getWriter()
    {
        return writer;
    }

    public BufferedReader getReader()
    {
        return reader;
    }

    public ProtocolConstants.ListenerType getClientType()
    {
        return clientType;
    }

    public String getSessionId()
    {
        return sessionId;
    }

    public String getClientName()
    {
        return clientName;
    }

    public void close() throws IOException
    {
        if (reader != null)
        {
            reader.close();
        }
        if (writer != null)
        {
            writer.close();
        }
        if (socket != null && !socket.isClosed())
        {
            socket.close();
        }
    }

    public String getRemoteAddress()
    {
        if (socket != null && socket.getInetAddress() != null)
        {
            return socket.getInetAddress().getHostAddress();
        }
        return "Unknown";
    }
}

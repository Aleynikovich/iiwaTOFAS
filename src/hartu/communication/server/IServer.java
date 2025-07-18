package hartu.communication.server;

import java.net.InetAddress;
import java.net.UnknownHostException; // Import UnknownHostException
import java.io.IOException; // Import IOException

public interface IServer
{
    void start() throws IOException; // <--- ADDED: throws IOException
    void stop();
    boolean isRunning();
    int getPort();
    InetAddress getInetAddress() throws UnknownHostException;
}
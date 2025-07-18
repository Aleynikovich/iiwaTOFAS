package hartu.communication.server;
import java.net.InetAddress;

public interface IServer
{
    void start();
    void stop();
    boolean isRunning();
    int getPort();
    InetAddress getInetAddress();
}
package hartu.communication.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public abstract class AbstractServer implements IServer
{

    protected int port;
    protected ServerSocket serverSocket;
    protected volatile boolean isRunning;

    public AbstractServer(int port) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Port number must be between 0 and 65535.");
        }
        this.port = port;
        this.isRunning = false;
    }

    @Override
    public void start() {
        if (isRunning) {
            System.out.println(getServerName() + " is already running on port " + port);
            return;
        }

        try {
            serverSocket = new ServerSocket(port);
            isRunning = true;
            System.out.println(getServerName() + " started on port " + port + " (" + getInetAddress().getHostAddress() + ")");

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected to " + getServerName() + " from: " + clientSocket.getInetAddress().getHostAddress());
                handleClient(clientSocket);
            }

        } catch (IOException e) {
            if (isRunning) {
                System.err.println(getServerName() + " error: " + e.getMessage());
            } else {
                System.out.println(getServerName() + " shut down normally.");
            }
        } finally {
            stop();
        }
    }

    @Override
    public void stop() {
        if (!isRunning) {
            System.out.println(getServerName() + " is not running.");
            return;
        }
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println(getServerName() + " stopped.");
            }
        } catch (IOException e) {
            System.err.println("Error closing " + getServerName() + " socket: " + e.getMessage());
        }
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public InetAddress getInetAddress() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            return serverSocket.getInetAddress();
        }
        try {
            return InetAddress.getLocalHost();
        } catch (java.net.UnknownHostException e) {
            System.err.println("Could not determine local host address: " + e.getMessage());
            return null;
        }
    }

    protected abstract void handleClient(Socket clientSocket);

    protected abstract String getServerName();
}
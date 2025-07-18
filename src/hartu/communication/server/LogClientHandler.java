// File: hartu/communication/server/LogClientHandler.java
package hartu.communication.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class LogClientHandler extends Thread {
    private Socket clientSocket;
    private PrintWriter out;
    private BlockingQueue<String> messagesToSend;
    private LogServer parentServer;

    public LogClientHandler(Socket socket, LogServer parentServer) {
        this.clientSocket = socket;
        this.parentServer = parentServer;
        this.messagesToSend = new LinkedBlockingQueue<>();
    }

    public void sendMessage(String message) {
        try {
            messagesToSend.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("LogClientHandler for " + getClientAddress() + ": Interrupted while queuing message.");
        }
    }

    public boolean isClientConnected() {
        return clientSocket != null && clientSocket.isConnected() && !clientSocket.isClosed();
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            while (!Thread.currentThread().isInterrupted() && isClientConnected()) {
                String message = messagesToSend.take();
                out.println(message);
            }
        } catch (IOException e) {
            System.err.println("LogClientHandler I/O error with " + getClientAddress() + ": " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("LogClientHandler for " + getClientAddress() + " interrupted.");
        } finally {
            disconnect();
        }
    }

    public void disconnect() {
        try {
            if (out != null) out.close();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
                System.out.println("Log Client " + getClientAddress() + " disconnected.");
            }
        } catch (IOException e) {
            System.err.println("Error closing resources for Log Client " + getClientAddress() + ": " + e.getMessage());
        } finally {
            parentServer.removeHandler(this);
            this.interrupt();
        }
    }

    public String getClientAddress() {
        return clientSocket.getInetAddress().getHostAddress();
    }
}
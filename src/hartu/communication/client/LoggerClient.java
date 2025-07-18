package hartu.communication.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class LoggerClient implements IClient {
    private String serverAddress;
    private int serverPort;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean isConnected;
    private final BlockingQueue<String> messageQueue;
    private Thread senderThread;
    private Thread receiverThread;

    public LoggerClient(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.isConnected = false;
        this.messageQueue = new LinkedBlockingQueue<String>();
    }

    @Override
    public void connect() {
        if (isConnected) {
            System.out.println("LoggerClient: Already connected.");
            return;
        }
        try {
            clientSocket = new Socket(serverAddress, serverPort);
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            isConnected = true;
            System.out.println("LoggerClient: Connected to LogServer at " + serverAddress + ":" + serverPort);

            senderThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (!Thread.currentThread().isInterrupted() && isConnected) {
                            String message = messageQueue.poll(100, TimeUnit.MILLISECONDS);
                            if (message != null) {
                                out.println(message);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.out.println("LoggerClient sender interrupted.");
                    } catch (Exception e) {
                        System.err.println("LoggerClient sender error: " + e.getMessage());
                        disconnect();
                    }
                }
            }, "LoggerClientSender");
            senderThread.start();

            receiverThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String line;
                        while (!Thread.currentThread().isInterrupted() && isConnected && (line = in.readLine()) != null) {
                            System.out.println("LoggerClient received: " + line);
                        }
                    } catch (IOException e) {
                        if (isConnected) {
                            System.err.println("LoggerClient receiver error: " + e.getMessage());
                        }
                    } finally {
                        disconnect();
                    }
                }
            }, "LoggerClientReceiver");
            receiverThread.start();

        } catch (IOException e) {
            System.err.println("LoggerClient: Failed to connect to LogServer: " + e.getMessage());
            isConnected = false;
        }
    }

    @Override
    public void sendMessage(String message) {
        if (isConnected) {
            try {
                messageQueue.put(message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("LoggerClient: Interrupted while queuing message.");
            }
        } else {
            System.err.println("LoggerClient: Not connected. Message not sent: " + message);
        }
    }

    @Override
    public void disconnect() {
        if (!isConnected) {
            System.out.println("LoggerClient: Already disconnected.");
            return;
        }
        isConnected = false;
        try {
            if (senderThread != null) {
                senderThread.interrupt();
                senderThread.join(1000);
            }
            if (receiverThread != null) {
                receiverThread.interrupt();
                receiverThread.join(1000);
            }
            if (out != null) out.close();
            if (in != null) in.close();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
            System.out.println("LoggerClient: Disconnected from LogServer.");
        } catch (IOException e) {
            System.err.println("LoggerClient: Error during disconnection: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("LoggerClient: Interruption during thread join: " + e.getMessage());
        }
    }

    @Override
    public boolean isConnected() {
        return isConnected;
    }
}
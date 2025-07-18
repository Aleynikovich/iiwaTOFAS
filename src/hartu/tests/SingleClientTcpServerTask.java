package hartu.tests; // Adjust package as needed

import com.kuka.roboticsAPI.RoboticsAPIContext;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.common.ThreadInterruptedException; // Assuming this is part of KUKA API
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SingleClientTcpServerTask extends RoboticsAPICyclicBackgroundTask {

    private ServerSocket serverSocket; // Initialized in constructor
    private volatile Socket clientSocket;
    private volatile Thread clientCommunicationThread;

    private final Lock socketLock = new ReentrantLock();

    private static final long HEARTBEAT_INTERVAL_MS = 2000;
    private volatile long lastHeartbeatSentTime = 0;

    public SingleClientTcpServerTask(RoboticsAPIContext context, int port) {
        super(context);

        initializeCyclic(0, 1000, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort); // Check every 1 second

        // IMPORTANT: Initialize ServerSocket here in the constructor
        try {
            socketLock.lock();
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(500); // CRITICAL: Make accept() non-blocking with a timeout
            System.out.println("[ServerTask] ServerSocket initialized on port " + port);
        } catch (IOException e) {
            System.err.println("[ServerTask] FATAL: Failed to initialize ServerSocket in constructor: " + e.getMessage());
            throw new RuntimeException("Failed to initialize server socket for TCP task", e);
        } finally {
            socketLock.unlock();
        }
    }

    @Override
    protected void runCyclic() {
        socketLock.lock();
        try {
            // 1. Check if a client is currently connected and active
            if (clientSocket != null && clientSocket.isConnected() && !clientSocket.isClosed()) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastHeartbeatSentTime > HEARTBEAT_INTERVAL_MS) {
                    try {
                        clientSocket.getOutputStream().write(0x01); // Example heartbeat byte
                        clientSocket.getOutputStream().flush();
                        lastHeartbeatSentTime = currentTime;
                    } catch (IOException e) {
                        System.err.println("[ServerTask] Heartbeat failed, connection likely lost: " + e.getMessage());
                        closeCurrentClientConnection();
                    }
                }
            } else {
                // No client connected or previous connection lost. Attempt to accept a new one.
                System.out.println("[ServerTask] No client connected. Attempting to accept a new connection...");
                closeCurrentClientConnection(); // Ensure any old, broken socket is fully closed

                try {
                    clientSocket = serverSocket.accept();
                    System.out.println("[ServerTask] Client connected: " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());

                    lastHeartbeatSentTime = System.currentTimeMillis();
                    startClientCommunication(clientSocket);

                } catch (SocketTimeoutException e) {
                    // Expected if no client connects within the timeout.
                } catch (IOException e) {
                    System.err.println("[ServerTask] Error accepting client connection: " + e.getMessage());
                    closeCurrentClientConnection();
                }
            }
        } finally {
            socketLock.unlock();
        }
    }

    private void closeCurrentClientConnection() {
        if (clientCommunicationThread != null && clientCommunicationThread.isAlive()) {
            clientCommunicationThread.interrupt();
            try {
                clientCommunicationThread.join(1000); // Wait a bit for it to terminate
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (clientSocket != null) {
            try {
                clientSocket.close();
                System.out.println("[ServerTask] Previous client connection closed.");
            } catch (IOException e) {
                System.err.println("[ServerTask] Error closing client socket: " + e.getMessage());
            } finally {
                clientSocket = null;
            }
        }
    }

    // IMPORTANT: MANUAL RESOURCE CLOSING FOR JAVA 7
    private void startClientCommunication(final Socket socket) {
        clientCommunicationThread = new Thread(new Runnable() { // Anonymous class for Runnable
            @Override
            public void run() {
                System.out.println("[ServerTask] Starting communication thread for client: " + socket.getInetAddress());
                DataInputStream in = null;
                DataOutputStream out = null;

                try {
                    in = new DataInputStream(socket.getInputStream());
                    out = new DataOutputStream(socket.getOutputStream());

                    while (!Thread.currentThread().isInterrupted() && socket.isConnected() && !socket.isClosed()) {
                        try {
                            int receivedData = in.readInt();
                            System.out.println("[ServerTask] Received: " + receivedData);
                            out.writeInt(receivedData);
                            out.flush();

                        } catch (IOException e) {
                            System.err.println("[ServerTask] Communication error with client: " + e.getMessage());
                            break;
                        }
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            System.out.println("[ServerTask] Communication thread interrupted.");
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } catch (IOException e) {
                    System.err.println("[ServerTask] Error setting up communication streams: " + e.getMessage());
                } finally {
                    System.out.println("[ServerTask] Communication thread for client terminated.");
                    // Manually close resources in reverse order of opening
                    if (out != null) {
                        try { out.close(); } catch (IOException ignore) {}
                    }
                    if (in != null) {
                        try { in.close(); } catch (IOException ignore) {}
                    }
                    // Socket will be closed by closeCurrentClientConnection() or when new client connects.
                    // This specific 'socket' variable might be different from 'clientSocket' if a new connection was made
                    // in the main cyclic task while this thread was still active.
                    // It's safer to just close 'socket' directly.
                    try { socket.close(); } catch (IOException ignore) {}

                    socketLock.lock();
                    try {
                        if (clientSocket == socket) {
                            clientSocket = null;
                        }
                    } finally {
                        socketLock.unlock();
                    }
                }
            }
        });
        clientCommunicationThread.setDaemon(true);
        clientCommunicationThread.start();
    }

    @Override
    public void dispose() {
        System.out.println("[ServerTask] Disposing SingleClientTcpServerTask...");
        socketLock.lock();
        try {
            closeCurrentClientConnection();
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                    System.out.println("[ServerTask] ServerSocket closed.");
                } catch (IOException e) {
                    System.err.println("[ServerTask] Error closing ServerSocket during dispose: " + e.getMessage());
                } finally {
                    serverSocket = null;
                }
            }
        } finally {
            socketLock.unlock();
        }
        super.dispose();
        System.out.println("[ServerTask] SingleClientTcpServerTask disposed.");
    }
}
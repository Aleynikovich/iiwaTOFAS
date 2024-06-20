package hartuTofas;

import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.deviceModel.LBR;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

public class TCPServer extends RoboticsAPIApplication {
    private LBR robot;
    private boolean isBusy = false;
    private final Object lock = new Object();
    private MessageHandler messageHandler;

    @Override
    public void initialize() {
        robot = getContext().getDeviceFromType(LBR.class);
        messageHandler = new MessageHandler(robot);
    }

    @Override
    public void run() {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(30001);
            getLogger().info("Server started. Listening on port 30001...");

            while (true) {
                final Socket clientSocket = serverSocket.accept();
                getLogger().info("Connected to client: " + clientSocket.getRemoteSocketAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            getLogger().error("Exception in server: " + e.getMessage());
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    getLogger().error("Exception closing server socket: " + e.getMessage());
                }
            }
        }
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    getLogger().info("Received message: " + inputLine);
                    String response = handleClientMessage(inputLine);
                    out.println(response);
                }
            } catch (IOException e) {
                getLogger().error("Exception in client connection: " + e.getMessage());
            } finally {
                if (clientSocket != null) {
                    try {
                        clientSocket.close();
                    } catch (IOException e) {
                        getLogger().error("Exception closing client socket: " + e.getMessage());
                    }
                }
            }
        }

        private String handleClientMessage(String message) {
            synchronized (lock) {
                if (isBusy) {
                    return "Robot is busy";
                }
                isBusy = true;
            }

            getLogger().info("Robot state: busy");
            String response;

            try {
                response = messageHandler.handleMessage(message);
            } finally {
                synchronized (lock) {
                    isBusy = false;
                }
                getLogger().info("Robot state: free");
            }

            return response;
        }
    }

    public static void main(String[] args) {
        TCPServer app = new TCPServer();
        app.runApplication();
    }
}

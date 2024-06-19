package stdio;

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

    @Override
    public void initialize() {
        robot = getContext().getDeviceFromType(LBR.class);
    }

    @Override
    public void run() {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(30001);
            getLogger().info("Server started. Listening on port 6400...");

            while (true) {
                Socket clientSocket = null;
                try {
                    clientSocket = serverSocket.accept();
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                    getLogger().info("Connected to client: " + clientSocket.getRemoteSocketAddress());

                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        getLogger().info("Received message: " + inputLine);
                        handleMessage(inputLine);
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

    private void handleMessage(String message) {
        if (message.equals("asd")) {
            getLogger().info("Executing example command");
        } else {
            getLogger().warn("Unknown command: " + message);
        }
    }

    public static void main(String[] args) {
        TCPServer app = new TCPServer();
        app.runApplication();
    }
}
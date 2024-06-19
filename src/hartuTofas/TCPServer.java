package hartuTofas;

import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.deviceModel.LBR;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import com.kuka.common.ThreadUtil;

public class TCPServer extends RoboticsAPIApplication {
    private LBR robot;
    private boolean isBusy = false;

    @Override
    public void initialize() {
        robot = getContext().getDeviceFromType(LBR.class);
    }

    @Override
    public void run() {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(30001);
            getLogger().info("Server started. Listening on port 30001...");

            while (true) {
                Socket clientSocket = null;
                try {
                    clientSocket = serverSocket.accept();
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                    getLogger().info("Connected to client: " + clientSocket.getRemoteSocketAddress());

                    String inputLine;
                    inputLine = in.readLine();
                    getLogger().info("Received message: " + inputLine);
                    ThreadUtil.milliSleep(500);
                    getLogger().info("tras espera 500ms");
                    String response = handleMessage(inputLine);
                    out.println(response);
                    
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

    private String handleMessage(String message) {
        if (isBusy) {
            return "Robot is busy";
        }

        isBusy = true;
        getLogger().info("Robot state: busy");

        String response;
        switch (1) {
            case 1:
                getLogger().info("Executing example command");
                // Perform the action for the command, e.g., move the robot
                response = "Command executed: example_command";
                break;
            default:
                getLogger().warn("Unknown command: " + message);
                response = "Unknown command: " + message;
        }

        isBusy = false;
        getLogger().info("Robot state: free");

        return response;
    }

    public static void main(String[] args) {
        TCPServer app = new TCPServer();
        app.runApplication();
    }
}
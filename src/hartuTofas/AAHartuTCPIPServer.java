package hartuTofas;

import javax.inject.Inject;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import com.kuka.roboticsAPI.deviceModel.LBR;

import java.io.*;
import java.net.*;

/**
 * Implementation of a robot application.
 */
public class AAHartuTCPIPServer extends RoboticsAPIApplication {
    @Inject
    private LBR lBR_iiwa_14_R820_1;

    @Override
    public void initialize() {
        // Initialization logic here (if needed)
    }

    @Override
    public void run() {
        // Start the robot in the home position
        lBR_iiwa_14_R820_1.move(ptpHome());
        System.out.println("Robot moved to home position.");

        // Start the TCP/IP server
        int port = 30001; // Define the port number for the server
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on 10.66.171.147:" + port);

            while (true) {
                System.out.println("Waiting for a client...");
                Socket clientSocket = serverSocket.accept(); // Accept a client connection
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                // Handle client communication
                handleClient(clientSocket);
            }
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close(); // Ensure server socket is closed
                } catch (IOException e) {
                    System.err.println("Error closing server socket: " + e.getMessage());
                }
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        BufferedReader in = null;
        PrintWriter out = null;

        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            String message;
            while ((message = in.readLine()) != null) { // Read client messages
                System.out.println("Received: " + message);

                // Simple acknowledgment response
                if (message.equals("FREE")) {
                    out.println("Robot is FREE");
                } else {
                    out.println("Unknown command");
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client resources: " + e.getMessage());
            }
        }
    }
}

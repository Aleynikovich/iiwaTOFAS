package hartuTofas;

import javax.inject.Inject;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import com.kuka.roboticsAPI.deviceModel.LBR;

import java.io.*;
import java.net.*;

/**
 * Robot TCP/IP Server Implementation.
 */
public class AAHartuTCPIPServer extends RoboticsAPIApplication {
    @Inject
    private LBR lBR_iiwa_14_R820_1;

    @Override
    public void initialize() {
        // Initialization logic, if needed
    }

    @Override
    public void run() {
        // Move robot to home position at startup
        lBR_iiwa_14_R820_1.move(ptpHome());
        System.out.println("Robot moved to home position.");

        // Start the TCP server
        int port = 30001; // Listening port
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on 10.66.171.147:" + port);

            while (true) {
                System.out.println("Waiting for a client...");
                Socket clientSocket = serverSocket.accept(); // Accept client connection
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                // Handle client communication
                handleClient(clientSocket);
            }
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
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

                // Parse the incoming message
                String[] parts = message.split("\\|");
                if (parts.length != 9) {
                    System.err.println("Invalid message format");
                    out.println("ERROR|0#"); // Respond with error if format is incorrect
                    continue;
                }

                // Extract fields
                String moveType = parts[0].trim();
                String numPoints = parts[1].trim();
                String targetPoints = parts[2].trim();
                String ioPoint = parts[3].trim();
                String ioPin = parts[4].trim();
                String ioState = parts[5].trim();
                String tool = parts[6].trim();
                String base = parts[7].trim();
                String requestId = parts[8].replace("#", "").trim(); // Remove '#' from the ID

                System.out.println("Parsed Request ID: " + requestId);

                // Perform robot actions here based on moveType, numPoints, etc.
                // (For now, we'll skip motion handling and just respond with FREE.)

                // Respond with the state and request ID
                String response = "FREE|" + requestId + "#";
                System.out.println("Sending response: " + response);
                out.println(response);
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

package hartuTofas;

import javax.inject.Inject;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import com.kuka.roboticsAPI.deviceModel.LBR;

import java.io.*;
import java.net.*;

public class AAHartuTCPIPServer extends RoboticsAPIApplication {
    @Inject
    private LBR lBR_iiwa_14_R820_1;

    private MessageHandler messageHandler;

    @Override
    public void initialize() {
        // Initialize the MessageHandler with the robot instance
        messageHandler = new MessageHandler(lBR_iiwa_14_R820_1);
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
            // Prepare input and output streams
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            // Send the initial FREE message
            String initialResponse = "FREE|0#";
            System.out.println("Sending initial response: " + initialResponse);
            out.println(initialResponse);

            System.out.println("Waiting for a message from the client...");

            // Read and process messages
            StringBuilder messageBuilder = new StringBuilder();
            int charInt;
            while ((charInt = in.read()) != -1) {
                char currentChar = (char) charInt;

                // Append character to the message buffer
                messageBuilder.append(currentChar);

                // Check if we've received the complete message (terminated by #)
                if (currentChar == '#') {
                    String message = messageBuilder.toString();
                    System.out.println("Received: " + message);

                    // Process the message using MessageHandler
                    String response = messageHandler.handleMessage(message);

                    // Extract the request ID from the message
                    String requestId = extractRequestId(message);
                    
                    // Send the appropriate response
                    if (response.startsWith("Invalid") || response.startsWith("Unknown")) {
                        String errorResponse = "ERROR|" + requestId + "#";
                        System.out.println("Sending error response: " + errorResponse);
                        out.println(errorResponse);
                    } else {
                        String successResponse = "FREE|" + requestId + "#";
                        out.println(successResponse);
                        System.out.println("Sending success response: " + successResponse);
                    }

                    // Clear the message buffer for the next message
                    messageBuilder.setLength(0);

                    System.out.println("Waiting for the next message...");
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            // Clean up resources
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client resources: " + e.getMessage());
            }
        }
    }


    private String extractRequestId(String message) {
        // Ensure message ends with # and remove it
        if (message.endsWith("#")) {
            message = message.substring(0, message.length() - 1);
        }

        // Split by | and return the last part (request ID)
        String[] parts = message.split("\\|");
        return parts.length >= 9 ? parts[8] : "0";
    }
}

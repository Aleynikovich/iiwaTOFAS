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
            // Prepare streams for communication
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            // Send initial FREE message
            String initialResponse = "FREE|0#";
            System.out.println("Sending initial response: " + initialResponse);
            out.println(initialResponse);

            // Log that the robot is waiting for a message
            System.out.println("Waiting for a message from the client...");
            	
            // Wait for and process incoming messages
            String message;
            message = in.readLine();
            
            while (message == null){
            	try {
					Thread.sleep(3000);
					message = in.readLine();
					System.out.println("Waiting for a message from the client...");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
            
            System.out.println("Current message: " + message);
            while (message  != null) {
                System.out.println("Received: " + message);

                // Process the message using MessageHandler
                String response = messageHandler.handleMessage(message);

                // Extract the request ID (last field in the message)
                String requestId = extractRequestId(message);

                // Send response to client
                if (response.startsWith("Invalid") || response.startsWith("Unknown")) {
                    String errorResponse = "ERROR|" + requestId + "#";
                    System.out.println("Sending error response: " + errorResponse);
                    out.println(errorResponse);
                } else {
                    String successResponse = "FREE|" + requestId + "#";
                    System.out.println("Sending success response: " + successResponse);
                    out.println(successResponse);
                }

                // Log that the robot is ready for the next message
                System.out.println("Waiting for the next message from the client...");
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

// File: AAHartuTCPIPServer.java
package hartuTofas;

import javax.inject.Inject; // Required for @Inject annotations

import com.kuka.generated.ioAccess.Ethercat_x44IOGroup;
import com.kuka.generated.ioAccess.IOFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import com.kuka.roboticsAPI.deviceModel.LBR;

import java.io.*;
import java.net.*;

public class AAHartuTCPIPServer extends RoboticsAPIApplication {
    // Injected robot instance
    @Inject
    private LBR lBR_iiwa_14_R820_1;
    
    // Server and client sockets for TCP communication
    private ServerSocket serverSocket = null;
    private Socket clientSocket = null;
    
    // Injected I/O groups
    @Inject
    public IOFlangeIOGroup gimatic;
    @Inject
    public Ethercat_x44IOGroup IOs;

    // The MessageHandler instance will be automatically injected by the KUKA framework.
    // This removes the need for manual instantiation in initialize().
    @Inject
    private MessageHandler messageHandler; 

    /**
     * Called once when the application starts.
     * Used to initialize injected components and other application-wide settings.
     */
    @Override
    public void initialize() {
        // No manual instantiation of messageHandler is needed here due to @Inject.
        // It's automatically created and its own @Inject fields are filled.
        System.out.println("AAHartuTCPIPServer initialized. MessageHandler injected.");
    }

    /**
     * The main execution loop of the robot application.
     * Sets the robot to home position and starts the TCP server to listen for client connections.
     */
    @Override
    public void run() {
        // Move robot to home position at startup
        lBR_iiwa_14_R820_1.move(ptpHome().setJointVelocityRel(0.25));
        System.out.println("Robot moved to home position.");

        int port = 30001; // The port on which the server will listen

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on 10.66.171.147:" + port);

            while (true) {
                System.out.println("Waiting for a client...");
                clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                handleClient(clientSocket);
            }
        } catch (IOException e) {
            System.err.println("Error starting or running server: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                    System.out.println("Server socket closed in finally block.");
                } catch (IOException e) {
                    System.err.println("Error closing server socket in finally block: " + e.getMessage());
                }
            }
        }
    }
    

    /**
     * Handles communication with a single connected client.
     * Reads messages, processes them using MessageHandler, and sends responses.
     * @param clientSocket The socket connected to the client.
     */
    private void handleClient(Socket clientSocket) {
        BufferedReader in = null;
        PrintWriter out = null;

        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            String initialResponse = "FREE|0#";
            System.out.println("Sending initial response: " + initialResponse);
            out.print(initialResponse);
            out.flush();

            System.out.println("Waiting for a message from the client...");

            StringBuilder messageBuilder = new StringBuilder();
            int charInt;
            while ((charInt = in.read()) != -1) {
                char currentChar = (char) charInt;
                messageBuilder.append(currentChar);

                if (currentChar == '#') {
                    String receivedMessage = messageBuilder.toString();
                    System.out.println("Received: " + receivedMessage);

                    String handlerResponse = messageHandler.handleMessage(receivedMessage);

                    String requestId = extractRequestId(receivedMessage);
                    
                    String responseToSend;
                    if (handlerResponse.startsWith("Invalid") || handlerResponse.startsWith("Unknown") || handlerResponse.startsWith("Error") || handlerResponse.startsWith("Failed")) {
                        System.out.println("Handler reported error: " + handlerResponse);
                        responseToSend = "ERROR|" + requestId + "#";
                    } else {
                        System.out.println("Handler reported success: " + handlerResponse);
                        responseToSend = "FREE|" + requestId + "#";
                    }

                    out.print(responseToSend);
                    out.flush();
                    System.out.println("Sent response: " + responseToSend);

                    messageBuilder.setLength(0);
                    System.out.println("Waiting for the next message...");
                }
            }
            System.out.println("Client disconnected or end of stream reached.");
        } catch (IOException e) {
            System.err.println("Error handling client communication: " + e.getMessage());
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null) clientSocket.close();
                System.out.println("Client resources closed.");
            } catch (IOException e) {
                System.err.println("Error closing client resources: " + e.getMessage());
            }
        }
    }

    private String extractRequestId(String message) {
        if (message.endsWith("#")) {
            message = message.substring(0, message.length() - 1);
        }

        String[] parts = message.split("\\|");
        // The ID is expected to be the 10th part (index 9) based on your format.
        return parts.length >= 10 ? parts[9] : "0";
    }
    
    @Override
    public void dispose() {
        System.out.println("Program was cancelled. Disposing resources.");
        
        if (clientSocket != null && !clientSocket.isClosed()) {
            try {
                clientSocket.close();
                System.out.println("Client socket closed during dispose.");
            } catch (IOException e) {
                System.err.println("Error closing client socket during dispose: " + e.getMessage());
            }
        }

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                System.out.println("Server socket closed during dispose.");
            } catch (IOException e) {
                System.err.println("Error closing server socket during dispose: " + e.getMessage());
            }
        }
    }
}
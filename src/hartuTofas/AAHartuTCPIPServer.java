package hartuTofas;

import javax.inject.Inject;

import com.kuka.generated.ioAccess.Ethercat_x44IOGroup;
import com.kuka.generated.ioAccess.IOFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import com.kuka.roboticsAPI.deviceModel.LBR;

import java.io.*;
import java.net.*;

public class AAHartuTCPIPServer extends RoboticsAPIApplication {
    @Inject
    private LBR lBR_iiwa_14_R820_1;
    
    private ServerSocket serverSocket = null;
    
    private Socket clientSocket = null;
    
	@Inject
	public IOFlangeIOGroup gimatic; // Out 7 True=Unlock False = Lock
	@Inject
	public Ethercat_x44IOGroup IOs; // Out 1 = Pick Out 2 = Place [raise]

    private MessageHandler messageHandler;

    @Override
    public void initialize() {
    	// Pass robot and I/O groups to the handler explicitly
    	messageHandler = new MessageHandler(lBR_iiwa_14_R820_1, gimatic, IOs, this); 
    }


    @Override
    public void run() {
        // Move robot to home position at startup
        lBR_iiwa_14_R820_1.move(ptpHome().setJointVelocityRel(0.25));
        System.out.println("Robot moved to home position.");

        // Start the TCP server
        int port = 30001; // Listening port

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on 10.66.171.147:" + port);

            while (true) {
                System.out.println("Waiting for a client...");
                clientSocket = serverSocket.accept(); // Accept client connection
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
            out.print(initialResponse);
            out.flush();

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
                    	System.out.println(response);
                        String errorResponse = "FREE|" + requestId + "#";
                        System.out.println("Sending error response: " + errorResponse);
                        out.print(errorResponse);
                        out.flush();
                    } else {
                    	System.out.println(response);
                        String successResponse = "FREE|" + requestId + "#";
                        out.print(successResponse);
                        out.flush();
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
        return parts.length >= 9 ? parts[9] : "0";
    }
    
    @Override
    public void dispose() {
        System.out.println("Program was cancelled.");
        
        // Cierra el socket del cliente si está activo
        if (clientSocket != null && !clientSocket.isClosed()) {
            try {
                clientSocket.close();
                System.out.println("Client socket closed.");
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }

        // Cierra el socket del servidor
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                System.out.println("Server socket closed.");
            } catch (IOException e) {
                System.err.println("Error closing server socket: " + e.getMessage());
            }
        }
        
        // Código para cerrar recursos, detener movimientos, etc.
    }
}

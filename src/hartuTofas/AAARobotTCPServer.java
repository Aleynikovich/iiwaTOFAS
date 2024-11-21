package hartuTofas;

import java.io.*;
import java.net.*;

public class AAARobotTCPServer {
    public static void main(String[] args) {
        int port = 30001; // Listening port
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(port); // Create server socket
            System.out.println("Server started on 10.66.171.147:" + port);

            while (true) { // Keep the server running
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

    private static void handleClient(Socket clientSocket) {
        BufferedReader in = null;
        PrintWriter out = null;

        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            String message;
            while ((message = in.readLine()) != null) { // Read client messages
                System.out.println("Received: " + message);

                // Send acknowledgment back to client
                out.println("ACK");
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

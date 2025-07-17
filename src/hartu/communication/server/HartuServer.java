package hartu.communication.server;

import com.kuka.roboticsAPI.controllerModel.Controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.InetAddress;
import java.net.ServerSocket;

public class HartuServer extends AbstractServer {

    private Controller robotController;

    public HartuServer(int port, Controller controller) {
        super(port);
        this.robotController = controller;
    }

    @Override
    protected String getServerName() {
        return "HartuServer";
    }

    @Override
    protected void handleClient(Socket clientSocket) {
        new HartuClientHandler(clientSocket, robotController).start();
    }

    private static class HartuClientHandler extends Thread {
        private Socket clientSocket;
        private BufferedReader in;
        private PrintWriter out;
        private Controller controller;

        public HartuClientHandler(Socket socket, Controller controller) {
            this.clientSocket = socket;
            this.controller = controller;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                String commandLine;
                while ((commandLine = in.readLine()) != null) {
                    System.out.println("Received from client " + clientSocket.getInetAddress().getHostAddress() + ": " + commandLine);
                    //TODO:STUFF
                    String responseToClient = "Received: " + commandLine;
                    out.println(responseToClient);
                }
            } catch (IOException e) {
                System.err.println("I/O error with client " + clientSocket.getInetAddress().getHostAddress() + ": " + e.getMessage());
            } finally {
                try {
                    if (out != null) out.close();
                    if (in != null) in.close();
                    if (clientSocket != null && !clientSocket.isClosed()) {
                        clientSocket.close();
                        System.out.println("Client " + clientSocket.getInetAddress().getHostAddress() + " disconnected.");
                    }
                } catch (IOException e) {
                    System.err.println("Error closing resources for client " + clientSocket.getInetAddress().getHostAddress() + ": " + e.getMessage());
                }
            }
        }
    }
}
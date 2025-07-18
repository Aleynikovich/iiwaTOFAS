// File: hartu/communication/server/HartuServer.java
package hartu.communication.server;

import com.kuka.roboticsAPI.controllerModel.Controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class HartuServer extends AbstractServer {

    private Controller robotController;
    private LogServer logServer;

    public HartuServer(int port, Controller controller, LogServer logServer) {
        super(port);
        this.robotController = controller;
        this.logServer = logServer;
    }

    @Override
    protected String getServerName() {
        return "HartuServer";
    }

    @Override
    protected void handleClient(Socket clientSocket) {
        new HartuClientHandler(clientSocket, robotController, logServer).start();
    }

    private static class HartuClientHandler extends Thread {
        private Socket clientSocket;
        private BufferedReader in;
        private PrintWriter out;
        private Controller controller;
        private LogServer handlerLogServer;

        public HartuClientHandler(Socket socket, Controller controller, LogServer logServer) {
            this.clientSocket = socket;
            this.controller = controller;
            this.handlerLogServer = logServer;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                String clientAddress = clientSocket.getInetAddress().getHostAddress();
                handlerLogServer.publish("HartuServer: Client " + clientAddress + " connected.");

                String commandLine;
                while ((commandLine = in.readLine()) != null) {
                    handlerLogServer.publish("HartuServer: Received from client " + clientAddress + ": " + commandLine);

                    String responseToClient = "Received: " + commandLine;
                    out.println(responseToClient);
                }
            } catch (IOException e) {
                handlerLogServer.publish("HartuServer: I/O error with client " + clientSocket.getInetAddress().getHostAddress() + ": " + e.getMessage());
            } finally {
                try {
                    if (out != null) out.close();
                    if (in != null) in.close();
                    if (clientSocket != null && !clientSocket.isClosed()) {
                        clientSocket.close();
                        handlerLogServer.publish("HartuServer: Client " + clientSocket.getInetAddress().getHostAddress() + " disconnected.");
                    }
                } catch (IOException e) {
                    handlerLogServer.publish("HartuServer: Error closing resources for client " + clientSocket.getInetAddress().getHostAddress() + ": " + e.getMessage());
                }
            }
        }
    }
}
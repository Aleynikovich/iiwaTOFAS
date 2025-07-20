package hartu.robot.communication.client;

import hartu.protocols.constants.ProtocolConstants;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientClass {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private String serverIp;
    private int serverPort;

    public ClientClass(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    public void connect() throws IOException {
        clientSocket = new Socket(serverIp, serverPort);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.print(message + ProtocolConstants.MESSAGE_TERMINATOR);
            out.flush();
        }
    }

    public String readMessage() throws IOException {
        StringBuilder messageBuilder = new StringBuilder();
        int charCode;
        while ((charCode = in.read()) != -1) {
            char c = (char) charCode;
            if (c == ProtocolConstants.MESSAGE_TERMINATOR.charAt(0)) {
                break;
            }
            messageBuilder.append(c);
        }
        return messageBuilder.toString();
    }

    public void close() throws IOException {
        if (in != null) {
            in.close();
        }
        if (out != null) {
            out.close();
        }
        if (clientSocket != null && !clientSocket.isClosed()) {
            clientSocket.close();
        }
    }

    public boolean isConnected() {
        return clientSocket != null && clientSocket.isConnected() && !clientSocket.isClosed();
    }
}

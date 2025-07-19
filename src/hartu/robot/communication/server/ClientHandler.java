// --- ClientHandler.java ---
package hartu.robot.communication.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable
{
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private String clientType; // New field to store client type (e.g., "Task Listener", "Log Listener")

    // Constructor now takes clientType
    public ClientHandler(Socket socket, String clientType) throws IOException
    {
        this.clientSocket = socket;
        this.out = new PrintWriter(clientSocket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        this.clientType = clientType; // Store the client type
    }

    public void sendMessage(String message)
    {
        if (out != null) {
            out.println(message);
        } else {
            Logger.getInstance().log("ClientHandler (" + clientType + "): Attempted to send message before PrintWriter was initialized.");
        }
    }

    public String readMessage() throws IOException
    {
        // This method might need to be adapted later if external calls need to read specific delimiters
        return in.readLine(); // Still uses readLine for now, but run() will handle delimiter
    }

    public void close() throws IOException
    {
        if (clientSocket != null && !clientSocket.isClosed()) {
            clientSocket.close();
            Logger.getInstance().log("ClientHandler (" + clientType + "): Client socket closed for " + clientSocket.getInetAddress().getHostAddress());
        }
    }

    @Override
    public void run()
    {
        String clientAddress = clientSocket.getInetAddress().getHostAddress();
        Logger.getInstance().log("ClientHandler (" + clientType + "): Started for client " + clientAddress);

        try
        {
            if ("Task Listener".equals(clientType)) {
                // Logic for reading messages delimited by '#'
                StringBuilder messageBuilder = new StringBuilder();
                int charCode;
                while ((charCode = in.read()) != -1) { // Read character by character
                    char c = (char) charCode;
                    if (c == '#') {
                        String receivedMessage = messageBuilder.toString();
                        Logger.getInstance().log("ClientHandler (" + clientType + " - " + clientAddress + "): Received: " + receivedMessage);
                        // TODO: Implement actual task command processing here later
                        messageBuilder.setLength(0); // Clear the builder for the next message
                    } else {
                        messageBuilder.append(c);
                    }
                }
            } else {
                // Original logic for reading messages delimited by newline (for Log Listener)
                String inputLine;
                while ((inputLine = in.readLine()) != null)
                {
                    Logger.getInstance().log("ClientHandler (" + clientType + " - " + clientAddress + "): Received: " + inputLine);
                    if ("bye".equalsIgnoreCase(inputLine.trim()))
                    {
                        Logger.getInstance().log("ClientHandler (" + clientType + " - " + clientAddress + "): Client sent 'bye'.");
                        break;
                    }
                }
            }
        }
        catch (IOException e)
        {
            Logger.getInstance().log("ClientHandler (" + clientType + " - " + clientAddress + "): I/O error (client disconnected): " + e.getMessage());
        }
        finally
        {
            try
            {
                close();
            }
            catch (IOException e)
            {
                Logger.getInstance().log("ClientHandler (" + clientType + " - " + clientAddress + "): Error closing client socket: " + e.getMessage());
            }
        }
        Logger.getInstance().log("ClientHandler (" + clientType + "): Terminated for client " + clientAddress);
    }
}

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

    public ClientHandler(Socket socket) throws IOException
    {
        this.clientSocket = socket;
        this.out = new PrintWriter(clientSocket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    public void sendMessage(String message)
    {
        if (out != null) { // Defensive check
            out.println(message);
        } else {
            Logger.getInstance().log("ClientHandler: Attempted to send message before PrintWriter was initialized.");
        }
    }

    public String readMessage() throws IOException
    {
        return in.readLine();
    }

    public void close() throws IOException
    {
        if (clientSocket != null && !clientSocket.isClosed()) {
            clientSocket.close();
            Logger.getInstance().log("ClientHandler: Client socket closed for " + clientSocket.getInetAddress().getHostAddress());
        }
    }

    @Override
    public void run()
    {
        String clientAddress = clientSocket.getInetAddress().getHostAddress();
        Logger.getInstance().log("ClientHandler: Started for client " + clientAddress);

        String inputLine;
        try
        {
            while ((inputLine = in.readLine()) != null)
            {
                Logger.getInstance().log("ClientHandler (" + clientAddress + "): Received: " + inputLine);
                if ("bye".equalsIgnoreCase(inputLine.trim()))
                {
                    Logger.getInstance().log("ClientHandler (" + clientAddress + "): Client sent 'bye'.");
                    break;
                }
            }
        }
        catch (IOException e)
        {
            // Log the error through the Logger
            Logger.getInstance().log("ClientHandler (" + clientAddress + "): I/O error: " + e.getMessage());
            throw new RuntimeException("ClientHandler I/O error: " + e.getMessage(), e);
        }
        finally
        {
            try
            {
                close();
            }
            catch (IOException e)
            {
                Logger.getInstance().log("ClientHandler (" + clientAddress + "): Error closing client socket: " + e.getMessage());
                throw new RuntimeException("Error closing client socket in handler: " + e.getMessage(), e);
            }
        }
        Logger.getInstance().log("ClientHandler: Terminated for client " + clientAddress);
    }
}

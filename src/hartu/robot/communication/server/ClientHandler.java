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

    // Method to send a message to this specific client
    public void sendMessage(String message)
    {
        out.println(message);
    }

    // Method to read a message from this specific client (blocking)
    public String readMessage() throws IOException
    {
        return in.readLine();
    }

    // Method to close the client socket
    public void close() throws IOException
    {
        clientSocket.close();
    }

    @Override
    public void run()
    {
        // The run method will contain the primary communication loop for this client.
        // For now, it will simply read messages until the client disconnects or sends "bye".
        // In later steps, we'll add logic to process task requests or manage log streaming.
        String inputLine;
        try
        {
            while ((inputLine = in.readLine()) != null)
            {
                // For now, we'll just read and discard, or you can add simple echo logic if needed for testing.
                // The main purpose of this ClientHandler's run method is to keep the connection alive
                // and be ready to receive. Sending will be done via sendMessage().
                if ("bye".equalsIgnoreCase(inputLine.trim()))
                {
                    break;
                }
            }
        }
        catch (IOException e)
        {
            // Re-throw as RuntimeException as per preference
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
                throw new RuntimeException("Error closing client socket in handler: " + e.getMessage(), e);
            }
        }
    }
}

// --- ServerClass.java ---
package hartu.robot.communication.server;
import java.io.*;
import java.net.*;

public class ServerClass
{
    private ServerSocket serverSocket;

    public ServerClass(int port) throws IOException
    {
        this.serverSocket = new ServerSocket(port);
    }

    public void start() throws IOException
    {
        while (true)
        {
            Socket clientSocket = serverSocket.accept();
            handleClient(clientSocket);
        }
    }

    private void handleClient(Socket clientSocket) throws IOException
    {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String inputLine;
            while ((inputLine = in.readLine()) != null)
            {
                out.println("Echo: " + inputLine);
                if ("bye".equalsIgnoreCase(inputLine.trim()))
                {
                    break;
                }
            }
        }
        clientSocket.close();
    }
}

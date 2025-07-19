// --- ClientHandler.java ---
package hartu.robot.communication.server;

import hartu.robot.utils.CommandParser;
import hartu.robot.commands.ParsedCommand;

import java.io.BufferedReader;
import java.io.File; // Import File class
import java.io.FileWriter; // Import FileWriter
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable
{
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private String clientType;

    // Define the path for the JSON log file
    private static final String PARSED_DATA_DIR = "parsedData";
    private static final String PARSED_COMMAND_FILE = PARSED_DATA_DIR + File.separator + "parsedCommand.json";

    public ClientHandler(Socket socket, String clientType) throws IOException
    {
        this.clientSocket = socket;
        this.out = new PrintWriter(clientSocket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        this.clientType = clientType;
    }

    public void sendMessage(String message)
    {
        if (out != null) {
            out.print(message);
            out.flush();
        } else {
            Logger.getInstance().log("ClientHandler (" + clientType + "): Attempted to send message before PrintWriter was initialized.");
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
                StringBuilder messageBuilder = new StringBuilder();
                int charCode;
                while ((charCode = in.read()) != -1) {
                    char c = (char) charCode;
                    if (c == '#') {
                        String receivedMessage = messageBuilder.toString();
                        Logger.getInstance().log("ClientHandler (" + clientType + " - " + clientAddress + "): Received: " + receivedMessage);

                        try {
                            ParsedCommand parsedCommand = CommandParser.parseCommand(receivedMessage + MESSAGE_TERMINATOR);
                            String parsedCommandJson = parsedCommand.toJson(); // Get the JSON string

                            Logger.getInstance().log("ClientHandler (" + clientType + " - " + clientAddress + "): Successfully parsed command:\n" + parsedCommandJson);

                            // --- NEW: Save JSON to file ---
                            saveJsonToFile(parsedCommandJson, PARSED_COMMAND_FILE);
                            // --- END NEW ---

                            String commandId = parsedCommand.getId();
                            String responseToClient = "FREE|" + commandId + MESSAGE_TERMINATOR;
                            sendMessage(responseToClient);
                            Logger.getInstance().log("ClientHandler (" + clientType + " - " + clientAddress + "): Sent response: " + responseToClient);

                        } catch (IllegalArgumentException e) {
                            Logger.getInstance().log("ClientHandler (" + clientType + " - " + clientAddress + "): Parsing Error: " + e.getMessage());
                        }

                        messageBuilder.setLength(0);
                    } else {
                        messageBuilder.append(c);
                    }
                }
            } else {
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

    /**
     * Saves the given JSON string to a specified file, overwriting it if it exists.
     * Creates the parent directory if it does not exist.
     * @param jsonString The JSON content to save.
     * @param filePath The path to the file where the JSON should be saved.
     */
    private void saveJsonToFile(String jsonString, String filePath) {
        File outputFile = new File(filePath);
        File parentDir = outputFile.getParentFile();

        // Create parent directories if they don't exist
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                Logger.getInstance().log("ClientHandler: Failed to create directory: " + parentDir.getAbsolutePath());
                return; // Exit if directory creation fails
            }
        }

        try (FileWriter writer = new FileWriter(outputFile, false)) { // false for overwrite
            writer.write(jsonString);
            Logger.getInstance().log("ClientHandler: Parsed command JSON saved to: " + filePath);
        } catch (IOException e) {
            Logger.getInstance().log("ClientHandler Error: Could not save parsed command JSON to file " + filePath + ": " + e.getMessage());
        }
    }

    private static final String MESSAGE_TERMINATOR = "#";
}

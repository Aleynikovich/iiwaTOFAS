// --- ClientHandler.java ---
package hartu.robot.communication.server;

import hartu.robot.utils.CommandParser;
import hartu.robot.commands.ParsedCommand;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.TimeUnit; // For await timeout

public class ClientHandler implements Runnable
{
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private String clientType;

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

                        String commandId = "N/A"; // Default ID for error responses
                        boolean executionSuccess = false;

                        try {
                            ParsedCommand parsedCommand = CommandParser.parseCommand(receivedMessage + MESSAGE_TERMINATOR);
                            commandId = parsedCommand.getId(); // Get ID for response
                            Logger.getInstance().log("ClientHandler (" + clientType + " - " + clientAddress + "): Successfully parsed command:\n" + parsedCommand.toString());

                            // Create a CommandResultHolder to pass the command and wait for its execution
                            CommandResultHolder resultHolder = new CommandResultHolder(parsedCommand);
                            CommandQueue.putCommand(resultHolder); // Put the command into the shared queue

                            Logger.getInstance().log("ClientHandler (" + clientType + " - " + clientAddress + "): Waiting for command ID " + commandId + " to execute...");
                            // Wait for the executor to signal completion (with a timeout)
                            boolean awaited = resultHolder.getLatch().await(60, TimeUnit.SECONDS); // Wait up to 60 seconds

                            if (awaited) {
                                executionSuccess = resultHolder.isSuccess();
                                Logger.getInstance().log("ClientHandler (" + clientType + " - " + clientAddress + "): Command ID " + commandId + " execution finished. Success: " + executionSuccess);
                            } else {
                                Logger.getInstance().log("ClientHandler (" + clientType + " - " + clientAddress + "): Command ID " + commandId + " execution TIMED OUT.");
                                executionSuccess = false; // Treat timeout as failure
                            }

                        } catch (IllegalArgumentException e) {
                            Logger.getInstance().log("ClientHandler (" + clientType + " - " + clientAddress + "): Parsing Error: " + e.getMessage());
                            executionSuccess = false;
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt(); // Restore interrupted status
                            Logger.getInstance().log("ClientHandler (" + clientType + " - " + clientAddress + "): Interrupted while waiting for command execution: " + e.getMessage());
                            executionSuccess = false;
                        } catch (Exception e) { // Catch any other unexpected errors during processing
                            Logger.getInstance().log("ClientHandler (" + clientType + " - " + clientAddress + "): Unexpected error during command processing: " + e.getMessage());
                            e.printStackTrace();
                            executionSuccess = false;
                        }

                        // Send response back to task client based on execution result
                        String responseToClient;
                        if (executionSuccess) {
                            responseToClient = "FREE|" + commandId + MESSAGE_TERMINATOR;
                        } else {
                            responseToClient = "ERROR|" + commandId + MESSAGE_TERMINATOR;
                        }
                        sendMessage(responseToClient);
                        Logger.getInstance().log("ClientHandler (" + clientType + " - " + clientAddress + "): Sent response: " + responseToClient);

                        messageBuilder.setLength(0); // Clear buffer for next command
                    } else {
                        messageBuilder.append(c);
                    }
                }
            } else { // Log Listener
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

    private static final String MESSAGE_TERMINATOR = "#";
}

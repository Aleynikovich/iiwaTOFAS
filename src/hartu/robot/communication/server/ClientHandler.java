package hartu.robot.communication.server;

import hartu.protocols.constants.ProtocolConstants;
import hartu.protocols.constants.ProtocolConstants.ListenerType;
import hartu.robot.commands.ParsedCommand;
import hartu.robot.utils.CommandParser;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ClientHandler implements Runnable
{
    private final ClientSession clientSession;

    public ClientHandler(ClientSession clientSession)
    {
        this.clientSession = clientSession;
    }

    public ClientSession getClientSession()
    {
        return clientSession;
    }

    public void sendMessage(String message)
    {
        if (clientSession.getWriter() != null)
        {
            clientSession.getWriter().print(message);
            clientSession.getWriter().flush();
        }
        else
        {
            Logger.getInstance().log(
                    "COMM",
                    "ClientHandler (" + clientSession.getClientType().getName() + "): Attempted to send message before PrintWriter was initialized."
                                    );
        }
    }

    public String readMessage() throws IOException
    {
        StringBuilder messageBuilder = new StringBuilder();
        int charCode;
        while ((charCode = clientSession.getReader().read()) != -1)
        {
            char c = (char) charCode;
            if (c == ProtocolConstants.MESSAGE_TERMINATOR.charAt(0))
            {
                break;
            }
            messageBuilder.append(c);
        }
        return messageBuilder.toString();
    }

    public void close() throws IOException
    {
        clientSession.close();

        Logger.getInstance().log(
                "COMM",
                "ClientHandler (" + clientSession.getClientType().getName() + "): Client session closed for " + clientSession.getRemoteAddress()
                                );
    }

    @Override
    public void run()
    {
        String clientAddress = clientSession.getRemoteAddress();
        String listenerName = clientSession.getClientType().getName();

        Logger.getInstance().log(
                "COMM",
                "ClientHandler (" + listenerName + "): Started for client " + clientAddress + " (Session ID: " + clientSession.getSessionId() + ")"
                                );

        try
        {
            if (clientSession.getClientType() == ListenerType.TASK_LISTENER)
            {
                StringBuilder messageBuilder = new StringBuilder();
                int charCode;
                while ((charCode = clientSession.getReader().read()) != -1)
                {
                    char c = (char) charCode;
                    if (c == ProtocolConstants.MESSAGE_TERMINATOR.charAt(0))
                    {
                        String receivedMessage = messageBuilder.toString();

                        Logger.getInstance().log(
                                "COMM",
                                "ClientHandler (" + listenerName + " - " + clientAddress + "): Received: " + receivedMessage
                                                );

                        String commandId = "N/A";
                        boolean executionSuccess = false;

                        try
                        {
                            ParsedCommand parsedCommand = CommandParser.parseCommand(receivedMessage + ProtocolConstants.MESSAGE_TERMINATOR);
                            commandId = parsedCommand.getId();

                            Logger.getInstance().log(
                                    "COMM",
                                    "ClientHandler (" + listenerName + " - " + clientAddress + "): Successfully parsed command: " + parsedCommand
                                                    );

                            CommandResultHolder resultHolder = new CommandResultHolder(parsedCommand);
                            CommandQueue.putCommand(resultHolder);

                            Logger.getInstance().log(
                                    "COMM",
                                    "ClientHandler (" + listenerName + " - " + clientAddress + "): Waiting for command ID " + commandId + " to execute..."
                                                    );

                            boolean awaited = resultHolder.getLatch().await(60, TimeUnit.SECONDS);

                            if (awaited)
                            {
                                executionSuccess = resultHolder.isSuccess();

                                Logger.getInstance().log(
                                        "COMM",
                                        "ClientHandler (" + listenerName + " - " + clientAddress + "): Command ID " + commandId + " execution finished. Success: " + executionSuccess
                                                        );
                            }
                            else
                            {

                                Logger.getInstance().log(
                                        "COMM",
                                        "ClientHandler (" + listenerName + " - " + clientAddress + "): Command ID " + commandId + " execution TIMED OUT."
                                                        );
                                executionSuccess = false;
                            }

                        }
                        catch (IllegalArgumentException e)
                        {

                            Logger.getInstance().log(
                                    "COMM",
                                    "ClientHandler (" + listenerName + " - " + clientAddress + "): Parsing Error: " + e.getMessage()
                                                    );
                            executionSuccess = false;
                        }
                        catch (InterruptedException e)
                        {
                            Thread.currentThread().interrupt();

                            Logger.getInstance().log(
                                    "COMM",
                                    "ClientHandler (" + listenerName + " - " + clientAddress + "): Interrupted while waiting for command execution: " + e.getMessage()
                                                    );
                            executionSuccess = false;
                        }
                        catch (Exception e)
                        {

                            Logger.getInstance().log(
                                    "COMM",
                                    "ClientHandler (" + listenerName + " - " + clientAddress + "): Unexpected error during command processing: " + e.getMessage()
                                                    );
                            executionSuccess = false;
                        }

                        String responseToClient;
                        if (executionSuccess)
                        {
                            responseToClient = "FREE|" + commandId + ProtocolConstants.MESSAGE_TERMINATOR;
                        }
                        else
                        {
                            responseToClient = "FREE|" + commandId + ProtocolConstants.MESSAGE_TERMINATOR;
                        }
                        sendMessage(responseToClient);

                        Logger.getInstance().log(
                                "COMM",
                                "ClientHandler (" + listenerName + " - " + clientAddress + "): Sent response: " + responseToClient
                                                );

                        messageBuilder.setLength(0);
                    }
                    else
                    {
                        messageBuilder.append(c);
                    }
                }
            }
            else if (clientSession.getClientType() == ListenerType.LOG_LISTENER)
            {
                String inputLine;
                while ((inputLine = clientSession.getReader().readLine()) != null)
                {

                    Logger.getInstance().log(
                            "COMM",
                            "ClientHandler (" + listenerName + " - " + clientAddress + "): Received: " + inputLine
                                            );
                    if ("bye".equalsIgnoreCase(inputLine.trim()))
                    {

                        Logger.getInstance().log(
                                "COMM",
                                "ClientHandler (" + listenerName + " - " + clientAddress + "): Client sent 'bye'."
                                                );
                        break;
                    }
                }
            }
        }
        catch (IOException e)
        {

            Logger.getInstance().log(
                    "COMM",
                    "ClientHandler (" + listenerName + " - " + clientAddress + "): I/O error (client disconnected): " + e.getMessage()
                                    );
        }
        finally
        {
            try
            {
                close();
            }
            catch (IOException e)
            {

                Logger.getInstance().log(
                        "COMM",
                        "ClientHandler (" + listenerName + " - " + clientAddress + "): Error closing client session: " + e.getMessage()
                                        );
            }
        }

        Logger.getInstance().log(
                "COMM",
                "ClientHandler (" + listenerName + "): Terminated for client " + clientAddress
                                );
    }
}

package hartu.execution;

import hartu.protocols.definitions.MessageProtocol;
import hartu.protocols.definitions.io.IOAction;
import hartu.communication.client.LoggerClient;

/**
 * Executes commands related to Input/Output (IO) actions on the robot.
 * This class simulates the interaction with robot's IO system and logs
 * execution details to a dedicated LoggerClient.
 */
public class IOCommandExecutor implements ICommandExecutor {

    private final LoggerClient executionLoggerClient;

    /**
     * Constructs an IOCommandExecutor with a specific LoggerClient for execution logging.
     * @param executionLoggerClient The LoggerClient instance to send execution logs to.
     */
    public IOCommandExecutor(LoggerClient executionLoggerClient) {
        if (executionLoggerClient == null) {
            throw new IllegalArgumentException("Execution LoggerClient cannot be null.");
        }
        this.executionLoggerClient = executionLoggerClient;
    }

    /**
     * Executes an IO command.
     * It expects a MessageProtocol of type IOAction and simulates
     * setting an IO point and pin to a specified state.
     * Log messages about execution status are sent via the executionLoggerClient.
     *
     * @param protocol The MessageProtocol object, expected to be an IOAction.
     * @throws IllegalArgumentException if the protocol is not an instance of IOAction.
     * @throws Exception if a simulated error occurs during IO activation.
     */
    @Override
    public void execute(MessageProtocol protocol) throws Exception {
        if (!(protocol instanceof IOAction)) {
            String errorMsg = "IOCommandExecutor can only execute IOAction protocols. Received: " + protocol.getClass().getName();
            executionLoggerClient.sendMessage("ERROR: " + errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        IOAction ioAction = (IOAction) protocol;

        String startMsg = "Executing IO Command: " + ioAction.actionType.name() +
                " | IO Point: " + ioAction.getIoPoint() +
                " | IO Pin: " + ioAction.getIoPin() +
                " | IO State: " + ioAction.getIoState();
        executionLoggerClient.sendMessage(startMsg);
        System.out.println(startMsg);

        String successMsg = "IO Command executed successfully (simulated) for ID: " + ioAction.id;
        executionLoggerClient.sendMessage(successMsg);
        System.out.println(successMsg);
    }
}
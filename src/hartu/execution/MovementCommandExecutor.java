package hartu.execution;

import hartu.protocols.definitions.MessageProtocol;
import hartu.protocols.definitions.movement.MovementMessage;
import hartu.communication.client.LoggerClient;

/**
 * Executes commands related to robot movement.
 * This class simulates the interaction with robot's motion system and logs
 * execution details to a dedicated LoggerClient.
 */
public class MovementCommandExecutor implements ICommandExecutor {

    private final LoggerClient executionLoggerClient; // Dedicated logger for execution events

    /**
     * Constructs a MovementCommandExecutor with a specific LoggerClient for execution logging.
     * @param executionLoggerClient The LoggerClient instance to send execution logs to.
     */
    public MovementCommandExecutor(LoggerClient executionLoggerClient) {
        if (executionLoggerClient == null) {
            throw new IllegalArgumentException("Execution LoggerClient cannot be null.");
        }
        this.executionLoggerClient = executionLoggerClient;
    }

    /**
     * Executes a movement command.
     * It expects a MessageProtocol of type MovementMessage and simulates
     * the robot moving to a specified target position with a given speed override.
     * Log messages about execution status are sent via the executionLoggerClient.
     *
     * @param protocol The MessageProtocol object, expected to be a MovementMessage.
     * @throws IllegalArgumentException if the protocol is not an instance of MovementMessage.
     * @throws Exception if a simulated error occurs during movement.
     */
    @Override
    public void execute(MessageProtocol protocol) throws Exception {
        if (!(protocol instanceof MovementMessage)) {
            String errorMsg = "MovementCommandExecutor can only execute MovementMessage protocols. Received: " + protocol.getClass().getName();
            executionLoggerClient.sendMessage("ERROR: " + errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        MovementMessage movementMessage = (MovementMessage) protocol;

        String startMsg = "Executing Movement Command: " + movementMessage.movementType.name() +
                " | Target: " + (movementMessage.getTargetPosition() != null ? movementMessage.getTargetPosition().toString() : "N/A") +
                " | Speed Override: " + (movementMessage.getSpeedOverride() * 100) + "%";
        executionLoggerClient.sendMessage(startMsg);
        System.out.println(startMsg); // Also print to console for immediate feedback

        // Simulate a potential error (e.g., if target position is null)
        if (movementMessage.getTargetPosition() == null) {
            String errorMsg = "Simulated movement error: Target position is null.";
            executionLoggerClient.sendMessage("ERROR: " + errorMsg);
            throw new Exception(errorMsg);
        }

        // Simulate the movement time based on speed override
        try {
            // A simple delay to simulate work
            long delayMs = (long) (1000 / movementMessage.getSpeedOverride());
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String errorMsg = "Movement interrupted: " + e.getMessage();
            executionLoggerClient.sendMessage("ERROR: " + errorMsg);
            throw new Exception(errorMsg);
        }
        String successMsg = "Movement Command executed successfully (simulated) for ID: " + movementMessage.id;
        executionLoggerClient.sendMessage(successMsg);
        System.out.println(successMsg); // Also print to console
    }
}
package hartu.execution;

import hartu.protocols.definitions.MessageProtocol;

/**
 * Defines the contract for classes that execute specific robot commands
 * based on a parsed MessageProtocol.
 */
public interface ICommandExecutor {
    /**
     * Executes the command represented by the given MessageProtocol.
     * Implementations should cast the MessageProtocol to the expected
     * concrete type (e.g., MovementMessage, IOAction) and perform
     * the corresponding robot action.
     *
     * @param protocol The MessageProtocol object to be executed.
     * @throws IllegalArgumentException if the protocol type is not supported by this executor.
     * @throws Exception if an error occurs during command execution.
     */
    void execute(MessageProtocol protocol) throws Exception;
}
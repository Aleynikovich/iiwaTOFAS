// --- CommandResultHolder.java ---
package hartu.robot.communication.server;

import hartu.robot.commands.ParsedCommand;

import java.util.concurrent.CountDownLatch;

/**
 * A holder class to pass a ParsedCommand along with a mechanism
 * to signal its execution completion and result back to the sender.
 * This is used for inter-task communication in Java 1.7.
 */
public class CommandResultHolder
{
    private final ParsedCommand command;
    private final CountDownLatch latch;
    private volatile boolean success; // volatile to ensure visibility across threads
    private volatile String customResponseData; // Optional custom response data (e.g., for input reading commands)
    private volatile boolean timedOut; // Flag to signal that command execution timed out

    /**
     * Creates a new CommandResultHolder.
     *
     * @param command The ParsedCommand to be executed.
     */
    public CommandResultHolder(ParsedCommand command)
    {
        this.command = command;
        this.latch = new CountDownLatch(1); // Latch will count down once when command is executed
        this.success = false; // Default to false
        this.customResponseData = null;
        this.timedOut = false;
    }

    public ParsedCommand getCommand()
    {
        return command;
    }

    public CountDownLatch getLatch()
    {
        return latch;
    }

    public boolean isSuccess()
    {
        return success;
    }

    public void setSuccess(boolean success)
    {
        this.success = success;
    }

    public String getCustomResponseData()
    {
        return customResponseData;
    }

    public void setCustomResponseData(String customResponseData)
    {
        this.customResponseData = customResponseData;
    }

    public boolean isTimedOut()
    {
        return timedOut;
    }

    public void setTimedOut(boolean timedOut)
    {
        this.timedOut = timedOut;
    }
}

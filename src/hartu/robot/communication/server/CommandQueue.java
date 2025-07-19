// --- CommandQueue.java ---
package hartu.robot.communication.server;

// No longer importing ParsedCommand directly, as it's wrapped in CommandResultHolder
// import hartu.robot.commands.ParsedCommand;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A static, thread-safe queue for CommandResultHolder objects.
 * This allows different KUKA background tasks (e.g., communication server and executor)
 * to pass commands and receive execution results.
 */
public class CommandQueue {

    // Now storing CommandResultHolder objects
    private static final BlockingQueue<CommandResultHolder> queue = new LinkedBlockingQueue<>();

    // Private constructor to prevent instantiation
    private CommandQueue() {}

    /**
     * Adds a CommandResultHolder to the queue.
     * This method will block if the queue is full (if a capacity was set).
     * @param resultHolder The CommandResultHolder to add.
     */
    public static void putCommand(CommandResultHolder resultHolder) {
        try {
            queue.put(resultHolder);
            Logger.getInstance().log("CommandQueue: Added command ID " + resultHolder.getCommand().getId() + " to queue. Queue size: " + queue.size());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore the interrupted status
            Logger.getInstance().log("CommandQueue Error: Interrupted while trying to put command: " + e.getMessage());
        }
    }

    /**
     * Retrieves and removes the head of the queue, waiting if necessary until an element becomes available.
     * @return The CommandResultHolder at the head of the queue.
     */
    public static CommandResultHolder takeCommand() {
        CommandResultHolder resultHolder = null;
        try {
            resultHolder = queue.take();
            Logger.getInstance().log("CommandQueue: Took command ID " + resultHolder.getCommand().getId() + " from queue. Queue size: " + queue.size());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore the interrupted status
            Logger.getInstance().log("CommandQueue Error: Interrupted while trying to take command: " + e.getMessage());
        }
        return resultHolder;
    }

    /**
     * Retrieves and removes the head of the queue, waiting up to the specified wait time
     * if necessary for an element to become available.
     * @param timeout The maximum time to wait.
     * @param unit The time unit of the timeout argument.
     * @return The CommandResultHolder at the head of the queue, or null if the specified waiting time elapses before an element is available.
     */
    public static CommandResultHolder pollCommand(long timeout, TimeUnit unit) {
        CommandResultHolder resultHolder = null;
        try {
            resultHolder = queue.poll(timeout, unit);
            if (resultHolder != null) {
                Logger.getInstance().log("CommandQueue: Polled command ID " + resultHolder.getCommand().getId() + " from queue. Queue size: " + queue.size());
            } else {
                // Logger.getInstance().log("CommandQueue: No command available after " + timeout + " " + unit.name().toLowerCase() + ".");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.getInstance().log("CommandQueue Error: Interrupted while trying to poll command: " + e.getMessage());
        }
        return resultHolder;
    }

    /**
     * Checks if the queue is empty.
     * @return true if the queue is empty, false otherwise.
     */
    public static boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Returns the number of elements in this queue.
     * @return The number of elements in this queue.
     */
    public static int size() {
        return queue.size();
    }
}

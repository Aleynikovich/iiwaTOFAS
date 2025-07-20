package hartu.robot.communication.server;

import hartu.robot.commands.ParsedCommand;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class CommandQueue {

    private static final BlockingQueue<CommandResultHolder> queue = new LinkedBlockingQueue<>();

    private CommandQueue() {}

    public static void putCommand(CommandResultHolder resultHolder) {
        try {
            queue.put(resultHolder);
            Logger.getInstance().log("QUEUE", "Added command ID " + resultHolder.getCommand().getId() + " to queue. Queue size: " + queue.size());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.getInstance().log("QUEUE", "Error: Interrupted while trying to put command: " + e.getMessage());
        }
    }

    public static CommandResultHolder takeCommand() {
        CommandResultHolder resultHolder = null;
        try {
            resultHolder = queue.take();
            Logger.getInstance().log("QUEUE", "Took command ID " + resultHolder.getCommand().getId() + " from queue. Queue size: " + queue.size());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.getInstance().log("QUEUE", "Error: Interrupted while trying to take command: " + e.getMessage());
        }
        return resultHolder;
    }

    public static CommandResultHolder pollCommand(long timeout, TimeUnit unit) {
        CommandResultHolder resultHolder = null;
        try {
            resultHolder = queue.poll(timeout, unit);
            if (resultHolder != null) {
                Logger.getInstance().log("QUEUE", "Polled command ID " + resultHolder.getCommand().getId() + " from queue. Queue size: " + queue.size());
            } else {
                // Logger.getInstance().log("QUEUE", "No command available after " + timeout + " " + unit.name().toLowerCase() + "."); // Keep this commented if you want to avoid frequent logs
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.getInstance().log("QUEUE", "Error: Interrupted while trying to poll command: " + e.getMessage());
        }
        return resultHolder;
    }

    public static boolean isEmpty() {
        return queue.isEmpty();
    }

    public static int size() {
        return queue.size();
    }
}

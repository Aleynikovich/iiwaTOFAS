// --- TestExecutingServer.java ---
package hartu.tests;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.LBR;

// KUKA Generated IO Access Imports
import com.kuka.generated.ioAccess.Ethercat_x44IOGroup;
import com.kuka.generated.ioAccess.IOFlangeIOGroup;
// import com.kuka.generated.ioAccess.MediaFlangeIOGroup; // Uncomment if needed

// Imports for ParsedCommand, IoCommandData, and CommandQueue
import hartu.robot.commands.ParsedCommand;
import hartu.robot.commands.io.IoCommandData;
import hartu.robot.communication.server.CommandQueue;
import hartu.robot.communication.server.CommandResultHolder;
import hartu.robot.communication.server.Logger;

/**
 * A temporary background KUKA application that acts as the command executor.
 * It continuously polls the shared CommandQueue for ParsedCommand objects,
 * executes them (currently only IO commands), and signals the result back
 * via the CommandResultHolder.
 */
public class TestExecutingServer extends RoboticsAPICyclicBackgroundTask {

    @Inject
    private Controller robotController;

    @Inject
    private LBR robot;

    // Inject the specific IO Groups directly into this class
    @Inject
    private IOFlangeIOGroup ioFlangeIOGroup;
    @Inject
    private Ethercat_x44IOGroup ethercatX44IOGroup;
    // @Inject // Uncomment if MediaFlangeIOGroup is also directly used for IO
    // private MediaFlangeIOGroup mediaFlangeIOGroup;

    @Override
    public void initialize() {
        // This task will run cyclically, checking the queue in runCyclic
        // Set a short cycle period for responsiveness
        initializeCyclic(0, 100, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort); // Check queue every 100ms

        Logger.getInstance().log("TestExecutingServer: Initializing. Ready to take commands from queue.");
    }

    @Override
    public void runCyclic() {
        // Continuously try to take a command from the queue without blocking indefinitely
        CommandResultHolder resultHolder = CommandQueue.pollCommand(0, TimeUnit.MILLISECONDS); // Non-blocking poll

        if (resultHolder != null) {
            ParsedCommand command = resultHolder.getCommand();
            Logger.getInstance().log("TestExecutingServer: Received command ID " + command.getId() + " from queue for execution.");
            boolean executionSuccess = false;

            try {
                if (command.isIoCommand()) {
                    IoCommandData ioData = command.getIoCommandData();
                    int ioPin = ioData.getIoPin();
                    boolean ioState = ioData.getIoState();

                    Logger.getInstance().log("TestExecutingServer: Executing IO command. Pin: " + ioPin + ", State: " + ioState);

                    switch (ioPin) {
                        case 1:
                            ioFlangeIOGroup.setDO_Flange7(ioState);
                            Logger.getInstance().log("TestExecutingServer: Set DO_Flange7 to " + ioState);
                            executionSuccess = true;
                            break;
                        case 2:
                            ethercatX44IOGroup.setOutput2(ioState);
                            Logger.getInstance().log("TestExecutingServer: Set Ethercat_x44 Output2 to " + ioState);
                            executionSuccess = true;
                            break;
                        case 3:
                            ethercatX44IOGroup.setOutput1(ioState);
                            Logger.getInstance().log("TestExecutingServer: Set Ethercat_x44 Output1 to " + ioState);
                            executionSuccess = true;
                            break;
                        default:
                            Logger.getInstance().log("TestExecutingServer Error: Invalid IO pin in parsed command for direct mapping: " + ioPin);
                            executionSuccess = false;
                    }
                } else {
                    Logger.getInstance().log("TestExecutingServer Warning: Received non-IO command. Only IO commands are supported in this test: " + command.getActionType().name());
                    executionSuccess = false; // Mark as failure for unsupported command types
                }
            } catch (Exception e) {
                Logger.getInstance().log("TestExecutingServer Error: Exception during command execution for ID " + command.getId() + ": " + e.getMessage());
                e.printStackTrace();
                executionSuccess = false;
            } finally {
                // Signal completion back to the ClientHandler
                resultHolder.setSuccess(executionSuccess);
                resultHolder.getLatch().countDown(); // Decrement the latch, releasing the waiting ClientHandler
                Logger.getInstance().log("TestExecutingServer: Signaled completion for command ID " + command.getId() + ". Success: " + executionSuccess);
            }
        }
        // If resultHolder is null, no command was available, just continue to next cycle.
    }

    @Override
    public void dispose() {
        Logger.getInstance().log("TestExecutingServer: Disposing...");
        super.dispose();
    }
}

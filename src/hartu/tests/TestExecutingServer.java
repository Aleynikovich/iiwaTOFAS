// --- TestExecutingServer.java ---
package hartu.tests; // Assuming this is in your 'tests' package

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.LBR; // Inject LBR if needed for future motion tests

// Imports for CommandExecutor and ParsedCommand
import hartu.robot.executor.CommandExecutor;
import hartu.robot.commands.ParsedCommand;
import hartu.robot.commands.MotionParameters; // Needed for ParsedCommand constructor
import hartu.robot.commands.io.IoCommandData; // Needed for ParsedCommand constructor
import hartu.protocols.constants.ActionTypes; // Needed for ParsedCommand constructor

import hartu.robot.communication.server.Logger; // For logging

/**
 * A temporary background KUKA application to test the CommandExecutor.
 * It will execute a simple IO command on initialization to verify functionality.
 */
public class TestExecutingServer extends RoboticsAPICyclicBackgroundTask {

    @Inject
    private Controller robotController; // Injected KUKA Controller

    @Inject
    private LBR robot; // Injected LBR, even if not used for motion in initial test

    @Inject
    private CommandExecutor commandExecutor; // Inject the CommandExecutor

    @Override
    public void initialize() {
        // Initialize the cyclic background task behavior
        // We'll set a long period or just let it run once if the test is in initialize
        initializeCyclic(0, 5000, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort); // 5-second cycle for background

        Logger.getInstance().log("TestExecutingServer: Initializing...");

        try {
            // --- Create a simple ParsedCommand for an IO action ---
            // This mimics a command that would come from your Task Client
            String testId = "test_io_command_123";
            int testIoPoint = 0; // ioPoint is not used in CommandExecutor's IO switch, but part of ParsedCommand
            int testIoPin = 1;   // Corresponds to DO_Flange7 in IOFlangeIOGroup
            boolean testIoState = true; // Set to true to activate

            IoCommandData testIoData = new IoCommandData(testIoPoint, testIoPin, testIoState);

            // MotionParameters can be null or default for IO commands if not relevant
            MotionParameters defaultMotionParams = new MotionParameters(0.0, "", "", false, 0);

            ParsedCommand testCommand = ParsedCommand.forIo(ActionTypes.ACTIVATE_IO, testId, testIoData);

            Logger.getInstance().log("TestExecutingServer: Attempting to execute test IO command...");
            boolean success = commandExecutor.executeCommand(testCommand);

            if (success) {
                Logger.getInstance().log("TestExecutingServer: Test IO command executed SUCCESSFULLY.");
            } else {
                Logger.getInstance().log("TestExecutingServer: Test IO command execution FAILED.");
            }

        } catch (Exception e) {
            Logger.getInstance().log("TestExecutingServer Error: Exception during initialization and test command execution: " + e.getMessage());
            e.printStackTrace(); // Print stack trace to KUKA console for detailed error
        }
    }

    @Override
    public void runCyclic() {
        // This method will be called cyclically.
        // For this simple test, it can just log a heartbeat or remain empty.
        Logger.getInstance().log("TestExecutingServer: Cyclic heartbeat. Still running...");
    }

    @Override
    public void dispose() {
        // Cleanup resources if necessary
        Logger.getInstance().log("TestExecutingServer: Disposing...");
        super.dispose();
    }
}

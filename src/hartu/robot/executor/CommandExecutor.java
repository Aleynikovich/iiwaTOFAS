package hartu.robot.executor;

import com.kuka.generated.ioAccess.Ethercat_x44IOGroup;
import com.kuka.generated.ioAccess.IOFlangeIOGroup;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;

import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.deviceModel.Device;
import com.kuka.roboticsAPI.executionModel.CancelledException;
import com.kuka.roboticsAPI.executionModel.CommandInvalidException;
import com.kuka.roboticsAPI.executionModel.ExecutionException;
import com.kuka.roboticsAPI.executionModel.ExternalStopException;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.motionModel.*;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.ptpHome;
import com.kuka.roboticsAPI.motionModel.ErrorHandlingAction;
import com.kuka.roboticsAPI.motionModel.IErrorHandler;
import hartu.protocols.constants.ActionTypes;
import hartu.protocols.constants.MovementType;
import hartu.robot.commands.MotionParameters;
import hartu.robot.commands.ParsedCommand;
import hartu.robot.commands.io.IoCommandData;
import hartu.robot.communication.server.CommandQueue;
import hartu.robot.communication.server.CommandResultHolder;
import hartu.robot.communication.server.Logger;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CommandExecutor extends RoboticsAPIApplication {

    @Inject
    private LBR iiwa;
    @Inject
    private IOFlangeIOGroup gimaticIO;
    @Inject
    private Ethercat_x44IOGroup toolControlIO;
    @Inject
    private MediaFlangeIOGroup mediaFlangeIO;
    
    private RobotConsoleClient consoleClient;
    private Thread consoleClientThread;
    
    // Track the current command being executed for error handling
    private volatile ParsedCommand currentCommand = null;
    private volatile boolean currentCommandFailed = false;
    
    private IErrorHandler moveAsyncErrorHandler;

    @Override
    public void initialize() {
        // Start robot console client to receive logs from LoggingServerManager
        // and broadcast them via println (only foreground tasks can println to robot console)
        startRobotConsoleClient();
        
        Logger.getInstance().log("ROBOT_EXEC", "Initializing CommandExecutor.");
        
        // Register error handler for asynchronous motion failures
        // This prevents the application from terminating when moveAsync fails
        registerMoveAsyncErrorHandler();
        
        // Flush any stale commands from previous runs
        int flushedCount = CommandQueue.flushQueue();
        if (flushedCount > 0) {
            Logger.getInstance().log("ROBOT_EXEC", "Cleared " + flushedCount + " stale command(s) from queue on initialization.");
        }
        
        // Move robot to home position after flushing queue
        try {
            Logger.getInstance().log("ROBOT_EXEC", "Moving robot to home position...");
            iiwa.move(ptpHome().setJointVelocityRel(0.2));
            Logger.getInstance().log("ROBOT_EXEC", "Robot successfully moved to home position.");
        } catch (Exception e) {
            Logger.getInstance().error("ROBOT_EXEC", "Failed to move robot to home position: " + e.getMessage());
            Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", e);
        }
        
        Logger.getInstance().log("ROBOT_EXEC", "Ready to take commands from queue.");
    }
    
    /**
     * Registers the error handler for asynchronous motion commands.
     * According to KUKA Sunrise.OS manual section 15.29.3, this is the correct way
     * to handle failed moveAsync commands without terminating the application.
     * 
     * The error handler:
     * - Logs the failed motion command details
     * - Logs any canceled motion commands
     * - Flushes the command queue to prevent cascading failures
     * - Returns ErrorHandlingAction.Ignore to continue the application
     */
    private void registerMoveAsyncErrorHandler() {
        moveAsyncErrorHandler = new IErrorHandler() {
            @Override
            public ErrorHandlingAction handleError(Device device, 
                                                   IMotionContainer failedContainer,
                                                   List<IMotionContainer> canceledContainers) {
                // Log the failed motion command
                Logger.getInstance().error("ROBOT_EXEC", "Asynchronous motion failed: " + failedContainer.getCommand().toString());
                
                if (currentCommand != null) {
                    Logger.getInstance().error("ROBOT_EXEC", "Failed command ID: " + currentCommand.getId());
                    Logger.getInstance().error("ROBOT_EXEC", "This usually means unreachable pose, singularity, joint limits exceeded, or timeout.");
                    currentCommandFailed = true;
                }
                
                // Log canceled motion commands
                if (canceledContainers != null && !canceledContainers.isEmpty()) {
                    Logger.getInstance().warn("ROBOT_EXEC", "The following " + canceledContainers.size() + " motion(s) were canceled:");
                    for (int i = 0; i < canceledContainers.size(); i++) {
                        Logger.getInstance().warn("ROBOT_EXEC", "  [" + (i+1) + "] " + canceledContainers.get(i).getCommand().toString());
                    }
                }
                
                // Flush the command queue to prevent cascading failures
                Logger.getInstance().warn("ROBOT_EXEC", "Flushing command queue due to motion failure...");
                int flushedCount = CommandQueue.flushQueue();
                if (flushedCount > 0) {
                    Logger.getInstance().log("ROBOT_EXEC", "Flushed " + flushedCount + " pending command(s) from queue after motion failure.");
                }
                
                // Return Ignore to prevent application termination and allow continued operation
                // This is the recommended approach per KUKA Sunrise.OS manual section 15.29.3
                return ErrorHandlingAction.Ignore;
            }
        };
        
        // Register the error handler with the application controller
        getApplicationControl().registerMoveAsyncErrorHandler(moveAsyncErrorHandler);
        Logger.getInstance().log("ROBOT_EXEC", "Registered moveAsync error handler for graceful failure handling.");
    }
    
    /**
     * Starts the robot console client that connects to LoggingServerManager
     * and broadcasts all logs to robot console via println.
     * Only foreground tasks (like CommandExecutor) can use println to write to robot console.
     */
    private void startRobotConsoleClient() {
        consoleClient = new RobotConsoleClient();
        consoleClientThread = new Thread(consoleClient);
        consoleClientThread.setDaemon(true);
        consoleClientThread.start();
    }

    @Override
    public void run() {
        // Main execution loop - runs indefinitely until application is stopped
        // All exceptions are caught and logged without terminating the loop
        Logger.getInstance().log("ROBOT_EXEC", "Starting main execution loop.");
        while (true) {
            // Check for intentional shutdown signal at the start of each iteration
            if (Thread.currentThread().isInterrupted()) {
                Logger.getInstance().log("ROBOT_EXEC", "Shutdown signal received, exiting execution loop gracefully.");
                break;
            }
            
            try {
                CommandResultHolder resultHolder = CommandQueue.pollCommand(100, TimeUnit.MILLISECONDS);

                if (resultHolder != null) {
                    ParsedCommand command = resultHolder.getCommand();
                    Logger.getInstance().log("ROBOT_EXEC", "Received command ID " + command.getId() + " from queue for execution.");
                    boolean executionSuccess = false;

                    try {
                        switch (command.getCommandCategory()) {
                            case MOVEMENT:
                                executionSuccess = executeMovementCommand(command);
                                break;
                            case IO:
                                executionSuccess = executeIO(command);
                                break;
                            case PROGRAM_CALL:
                                executionSuccess = executeProgramCallCommand(command);
                                break;
                            case UNKNOWN:
                            default:
                                Logger.getInstance().warn("ROBOT_EXEC", "Unknown or unsupported primary command category for ID " + command.getId() + ": " + command.getCommandCategory().name());
                                executionSuccess = false;
                                break;
                        }
                    } catch (Throwable t) {
                        Logger.getInstance().error("ROBOT_EXEC", "Exception during command execution for ID " + command.getId() + ": " + t.getClass().getName() + " - " + t.getMessage());
                        Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", t instanceof Exception ? (Exception)t : new Exception("Throwable wrapper", t));
                        executionSuccess = false;
                        // Continue processing - don't let one command failure stop the system
                    } finally {
                        // If command failed, flush the command queue to prevent cascading failures
                        // Note: Motion command failures are already handled by the IErrorHandler which flushes the queue
                        // This catches IO, program call, and other non-motion command failures
                        if (!executionSuccess) {
                            Logger.getInstance().warn("ROBOT_EXEC", "Command ID " + command.getId() + " failed. Flushing command queue to prevent cascading failures.");
                            int flushedCount = CommandQueue.flushQueue();
                            if (flushedCount > 0) {
                                Logger.getInstance().log("ROBOT_EXEC", "Flushed " + flushedCount + " pending command(s) from queue after failure.");
                            }
                        }
                        
                        resultHolder.setSuccess(executionSuccess);
                        resultHolder.getLatch().countDown();
                        Logger.getInstance().log("ROBOT_EXEC", "Signaled completion for command ID " + command.getId() + ". Success: " + executionSuccess);
                    }
                }
            } catch (Throwable t) {
                // Catch ANY exception or error in the main loop itself
                // This is the ultimate safety net - prevents the entire run loop from exiting
                Logger.getInstance().error("ROBOT_EXEC", "Unexpected error in main execution loop: " + t.getClass().getName() + " - " + t.getMessage());
                Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", t instanceof Exception ? (Exception)t : new Exception("Throwable wrapper", t));
                Logger.getInstance().log("ROBOT_EXEC", "Recovering from error and continuing execution...");
                // Brief pause to prevent tight error loops
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    Logger.getInstance().log("ROBOT_EXEC", "Sleep interrupted during error recovery - shutdown signal detected, exiting...");
                    break;
                }
            }
        }
        Logger.getInstance().log("ROBOT_EXEC", "CommandExecutor run method is exiting.");
    }

    /**
     * Executes a movement command by delegating to specific motion type handlers.
     * Uses the registered IErrorHandler for handling asynchronous motion failures.
     * Per KUKA Sunrise.OS manual section 15.29.3, moveAsync failures are handled
     * by the error handler which returns ErrorHandlingAction.Ignore to continue operation.
     *
     * @param command The ParsedCommand to execute.
     * @return True if the motion was successful, false otherwise.
     */
    private boolean executeMovementCommand(ParsedCommand command) {
        ActionTypes actionType = command.getActionType();
        Logger.getInstance().log("ROBOT_EXEC", "Executing " + actionType.name() + " command ID " + command.getId());

        List<IMotion> motions;

        MovementType movementType = actionType.getMovementType();
        if (movementType == MovementType.PTP) {
            if (actionType.isJointMotion()) {
                motions = createPtpJointMotions(command);
            } else {
                motions = createPtpCartesianMotions(command);
            }
        } else if (movementType == MovementType.LIN) {
            motions = createLinMotions(command);
        } else if (movementType == MovementType.CIRC) {
            motions = createCircMotions(command);
        } else {
            Logger.getInstance().error("ROBOT_EXEC", "Unsupported ActionType for movement command: " + actionType.name());
            return false;
        }

        if (motions.isEmpty()) {
            Logger.getInstance().error("ROBOT_EXEC", "Failed to create any motion for command ID " + command.getId());
            return false;
        }

        boolean motionSuccess = true;
        
        try {
            IMotion motionToExecute;
            if (motions.size() > 1) {
                // Use MotionBatch to execute multiple motions as a single sequence
                motionToExecute = new MotionBatch(motions.toArray(new RobotMotion[0]));
            } else {
                // For a single motion, execute it directly
                motionToExecute = motions.get(0);
            }

            // Set the current command so the error handler can access it
            currentCommand = command;
            currentCommandFailed = false;
            
            // Execute asynchronous motion
            // Failures are handled by the registered IErrorHandler (see registerMoveAsyncErrorHandler)
            // The error handler will flush the queue and return ErrorHandlingAction.Ignore
            // This approach is per KUKA Sunrise.OS manual section 15.29.3
            IMotionContainer container = iiwa.moveAsync(motionToExecute);
            container.await();
            
            // Check if the error handler was triggered
            if (currentCommandFailed) {
                Logger.getInstance().error("ROBOT_EXEC", "Motion for command ID " + command.getId() + " failed (handled by error handler).");
                motionSuccess = false;
            } else {
                Logger.getInstance().log("ROBOT_EXEC", "Motion for command ID " + command.getId() + " completed successfully.");
            }
            
        } catch (Throwable t) {
            // This catch block handles exceptions during motion setup or unexpected errors
            // The IErrorHandler handles failures during moveAsync execution
            Logger.getInstance().error("ROBOT_EXEC", "Error preparing or executing motion for command ID " + command.getId() + ": " + t.getClass().getName() + " - " + t.getMessage());
            Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", t instanceof Exception ? (Exception)t : new Exception("Throwable wrapper", t));
            motionSuccess = false;
        } finally {
            // Clear current command reference
            currentCommand = null;
            currentCommandFailed = false;
        }

        return motionSuccess;
    }

    /**
     * Creates a list of PTP motions for a sequence of JointPositions.
     */
    private List<IMotion> createPtpJointMotions(ParsedCommand command) {
        List<IMotion> motions = new ArrayList<>();
        MotionParameters params = command.getMotionParameters();

        for (JointPosition axPos : command.getAxisTargetPoints()) {
            motions.add(params.createPTPJointMotion(axPos));
        }
        return motions;
    }

    /**
     * Creates a list of PTP motions for a sequence of Cartesian Frames.
     */
    private List<IMotion> createPtpCartesianMotions(ParsedCommand command) {
        List<IMotion> motions = new ArrayList<>();
        MotionParameters params = command.getMotionParameters();

        for (Frame cartPos : command.getCartesianTargetPoints()) {
            motions.add(params.createPTPMotion(cartPos));
        }
        return motions;
    }

    /**
     * Creates a list of LIN motions for a sequence of Cartesian Frames.
     */
    private List<IMotion> createLinMotions(ParsedCommand command) {
        List<IMotion> motions = new ArrayList<>();
        MotionParameters params = command.getMotionParameters();

        for (Frame cartPos : command.getCartesianTargetPoints()) {
            motions.add(params.createLINMotion(cartPos));
        }
        return motions;
    }

    /**
     * Creates a list of circular motions for a sequence of Cartesian Frames.
     */
    private List<IMotion> createCircMotions(ParsedCommand command) {
        List<IMotion> motions = new ArrayList<>();
        MotionParameters params = command.getMotionParameters();
        List<Frame> cartesianPoints = command.getCartesianTargetPoints();

        if (cartesianPoints == null || cartesianPoints.size() < 2) {
            Logger.getInstance().error("ROBOT_EXEC", "Circular motion requires at least two Cartesian points. Command ID: " + command.getId());
            return Collections.emptyList();
        }

        for (int i = 0; i < cartesianPoints.size() - 1; i++) {
            Frame auxiliaryFrame = cartesianPoints.get(i);
            Frame destinationFrame = cartesianPoints.get(i + 1);
            motions.add(params.createCircularMotion(auxiliaryFrame, destinationFrame));
        }
        return motions;
    }

    private boolean executeIO(ParsedCommand command) {
        IoCommandData ioData = command.getIoCommandData();
        if (ioData == null) {
            Logger.getInstance().error("ROBOT_EXEC", "IO command ID " + command.getId() + " has no IO data.");
            return false;
        }

        int ioPin = ioData.getIoPin();
        boolean ioState = ioData.getIoState();

        Logger.getInstance().log("ROBOT_EXEC", "Executing IO command ID " + command.getId() + ". Pin: " + ioPin + ", State: " + ioState);

        try {
            switch (ioPin) {
                case 1:
            		return closeTool(0);
                case 2:
                	return openTool(0);
                case 3:
 
                    return true;
                case 10:
                    // Lock Gimatic tool changer
                    return lockGimatic();
                case 11:
                    // Unlock Gimatic tool changer
                    return unlockGimatic();
                case 12:
                    // Open tool (activate vacuum) - default to tool 1
                    return openTool(1);
                case 13:
                    // Close tool (blow air) - default to tool 1
                    return closeTool(1);
                default:
                    Logger.getInstance().error("ROBOT_EXEC", "Invalid IO pin in parsed command for direct mapping: " + ioPin + " for command ID " + command.getId());
                    return false;
            }
        } catch (Throwable t) {
            // Catch ALL exceptions to ensure IO failures don't crash the robot
            Logger.getInstance().error("ROBOT_EXEC", "IO command ID " + command.getId() + " failed with exception: " + t.getClass().getName() + " - " + t.getMessage());
            Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", t instanceof Exception ? (Exception)t : new Exception("Throwable wrapper", t));
            return false;
        }
    }

    /**
     * Executes external program call commands by calling appropriate subprograms.
     * 
     * Program ID mapping:
     * - 0: Place current tool (detects tool from digital inputs, places and detaches)
     * - 1-3: Pick tool 1-3 (moves to tool position and attaches with lock gimatic)
     * - 101: Open tool (global, activates vacuum/suction)
     * - 102: Close tool (global, blows air to release)
     * 
     * Examples:
     * - Command 100 → Program 0: Place current tool
     * - Command 101 → Program 1: Pick tool 1
     * - Command 102 → Program 2: Pick tool 2
     * - Command 103 → Program 3: Pick tool 3
     * - Command 201 → Program 101: Open tool
     * - Command 202 → Program 102: Close tool
     *
     * @param command The ParsedCommand representing an external program call.
     * @return True if the program call executed successfully, false otherwise.
     */
    private boolean executeProgramCallCommand(ParsedCommand command) {
        Integer programId = command.getProgramId();
        if (programId == null) {
            Logger.getInstance().error("ROBOT_EXEC", "Program call command ID " + command.getId() + " has no program ID.");
            return false;
        }

        Logger.getInstance().log("ROBOT_EXEC", "Executing program call command ID " + command.getId() + " with program ID: " + programId);

        try {
            // Route based on program ID
            if (programId == 0) {
                // Program 0: Place current tool (detect from inputs)
                return placeCurrentTool();
            } else if (programId >= 1 && programId <= 3) {
                // Programs 1-3: Pick tools 1-3
                return pickTool(programId);
            } else if (programId == 101) {
                // Program 101: Open tool (global)
                return openTool(0); // Pass 0 as toolId since it's global
            } else if (programId == 102) {
                // Program 102: Close tool (global)
                return closeTool(0); // Pass 0 as toolId since it's global
            } else {
                Logger.getInstance().error("ROBOT_EXEC", "Invalid program ID: " + programId + " (supported: 0-3, 101-102) for command ID " + command.getId());
                return false;
            }
        } catch (Throwable t) {
            // Catch ALL exceptions to ensure program call failures don't crash the robot
            Logger.getInstance().error("ROBOT_EXEC", "Program call command ID " + command.getId() + " failed with exception: " + t.getClass().getName() + " - " + t.getMessage());
            Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", t instanceof Exception ? (Exception)t : new Exception("Throwable wrapper", t));
            return false;
        }
    }

    /**
     * Detects the currently attached tool from MediaFlange digital inputs.
     * Digital inputs indicate the tool ID in binary format:
     * - InputX3Pin3 (bit 0): value 1
     * - InputX3Pin4 (bit 1): value 2
     * - InputX3Pin10 (bit 2): value 4
     * - InputX3Pin13 (bit 3): value 8
     * - InputX3Pin16 (bit 4): value 16
     * 
     * Examples:
     * - Tool 1: InputX3Pin3 = true (binary: 00001)
     * - Tool 3: InputX3Pin3 = true, InputX3Pin4 = true (binary: 00011, decimal: 1+2=3)
     * - No tool: All inputs = false (binary: 00000)
     * 
     * @return The ID of the currently attached tool (0 if no tool attached)
     */
    private int getCurrentToolId() {
        int toolId = 0;
        
        if (mediaFlangeIO.getInputX3Pin3()) {
            toolId += 1;
        }
        if (mediaFlangeIO.getInputX3Pin4()) {
            toolId += 2;
        }
        if (mediaFlangeIO.getInputX3Pin10()) {
            toolId += 4;
        }
        if (mediaFlangeIO.getInputX3Pin13()) {
            toolId += 8;
        }
        if (mediaFlangeIO.getInputX3Pin16()) {
            toolId += 16;
        }
        
        Logger.getInstance().log("ROBOT_EXEC", "Current tool ID detected from digital inputs: " + toolId);
        return toolId;
    }

    /**
     * Places the currently attached tool back to its storage position.
     * First detects which tool is attached by reading digital inputs,
     * then moves to the tool's storage position and unlocks the Gimatic tool changer.
     * 
     * @return True if the operation executed successfully, false otherwise.
     */
    private boolean placeCurrentTool() {
        try {
            // Detect which tool is currently attached
            int currentToolId = getCurrentToolId();
            
            if (currentToolId == 0) {
                Logger.getInstance().warn("ROBOT_EXEC", "No tool detected to place. Tool ID is 0.");
                return false;
            }
            
            Logger.getInstance().log("ROBOT_EXEC", "Placing current tool " + currentToolId);
            
            // Get tool-specific position
            Frame toolPosition = getToolPosition(currentToolId);
            if (toolPosition == null) {
                Logger.getInstance().error("ROBOT_EXEC", "Tool " + currentToolId + " position not configured");
                return false;
            }
            
            // Move to tool position
            Logger.getInstance().log("ROBOT_EXEC", "Moving to tool " + currentToolId + " storage position");
            CartesianPTP ptpMotion = new CartesianPTP(toolPosition);
            ptpMotion.setJointVelocityRel(0.2); // Conservative speed for tool placement
            
            try {
                IMotionContainer container = iiwa.moveAsync(ptpMotion);
                container.await();
                Logger.getInstance().log("ROBOT_EXEC", "Reached tool " + currentToolId + " storage position");
            } catch (Throwable t) {
                Logger.getInstance().error("ROBOT_EXEC", "Failed to move to tool " + currentToolId + " storage position: " + t.getClass().getName() + " - " + t.getMessage());
                Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", t instanceof Exception ? (Exception)t : new Exception("Throwable wrapper", t));
                return false;
            }
            
            // Unlock the Gimatic tool changer to release the tool
            if (!unlockGimatic()) {
                Logger.getInstance().error("ROBOT_EXEC", "Failed to unlock Gimatic for tool " + currentToolId);
                return false;
            }
            
            Logger.getInstance().log("ROBOT_EXEC", "Successfully placed tool " + currentToolId);
            return true;
        } catch (Throwable t) {
            Logger.getInstance().error("ROBOT_EXEC", "Place current tool operation failed: " + t.getClass().getName() + " - " + t.getMessage());
            Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", t instanceof Exception ? (Exception)t : new Exception("Throwable wrapper", t));
            return false;
        }
    }

    /**
     * Opens the tool by activating vacuum/suction (same for all tools).
     * Controls the IO outputs to activate suction.
     * This operation is global and works for all pneumatic tools.
     * 
     * @param toolId The ID of the tool to open (0 for global operation)
     * @return True if the operation executed successfully, false otherwise.
     */
    private boolean openTool(int toolId) {
        try {
            String toolDesc = (toolId == 0) ? "(global)" : String.valueOf(toolId);
            Logger.getInstance().log("ROBOT_EXEC", "Opening tool " + toolDesc + " (blowing air)");
            
            // Same IO sequence for all tools - activates vacuum/suction
            toolControlIO.setOutput3(true);
            toolControlIO.setOutput2(true);
            toolControlIO.setOutput1(false);
            gimaticIO.setDO_Flange2(true);
            gimaticIO.setDO_Flange1(false);
            Thread.sleep(300);
            toolControlIO.setOutput2(false);
            toolControlIO.setOutput3(false);
            Logger.getInstance().log("ROBOT_EXEC", "Tool " + toolDesc + " opened (blowing air)");
            return true;
        } catch (InterruptedException e) {
            Logger.getInstance().error("ROBOT_EXEC", "Open tool operation interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Closes the tool by blowing air (same for all tools).
     * Controls the IO outputs to blow air and release vacuum.
     * This operation is global and works for all pneumatic tools.
     * 
     * @param toolId The ID of the tool to close (0 for global operation)
     * @return True if the operation executed successfully, false otherwise.
     */
    private boolean closeTool(int toolId) {
        try {
            String toolDesc = (toolId == 0) ? "(global)" : String.valueOf(toolId);
            Logger.getInstance().log("ROBOT_EXEC", "Closing tool " + toolDesc + " (vacuum on)");
            
            // Same IO sequence for all tools - blows air to release
            toolControlIO.setOutput3(true);
            toolControlIO.setOutput2(false);
            toolControlIO.setOutput1(true);
            gimaticIO.setDO_Flange2(false);
            gimaticIO.setDO_Flange1(true);
            Thread.sleep(300);
            toolControlIO.setOutput1(false);
            Logger.getInstance().log("ROBOT_EXEC", "Tool " + toolDesc + " closed (vacuum on)");
            return true;
        } catch (InterruptedException e) {
            Logger.getInstance().error("ROBOT_EXEC", "Close tool operation interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Locks the Gimatic tool changer.
     * IO operations to command the locking of the tool changer.
     * 
     * @return True if the operation executed successfully, false otherwise.
     */
    private boolean lockGimatic() {
        try {
            Logger.getInstance().log("ROBOT_EXEC", "Locking Gimatic tool changer");
            gimaticIO.setDO_Flange7(false);
            Thread.sleep(300);
            Logger.getInstance().log("ROBOT_EXEC", "Gimatic tool changer locked");
            return true;
        } catch (InterruptedException e) {
            Logger.getInstance().error("ROBOT_EXEC", "Lock Gimatic operation interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Unlocks the Gimatic tool changer.
     * IO operations to command the unlocking of the tool changer.
     * 
     * @return True if the operation executed successfully, false otherwise.
     */
    private boolean unlockGimatic() {
        try {
            Logger.getInstance().log("ROBOT_EXEC", "Unlocking Gimatic tool changer");
            toolControlIO.setOutput3(false);
            gimaticIO.setDO_Flange7(true);
            Thread.sleep(300);
            Logger.getInstance().log("ROBOT_EXEC", "Gimatic tool changer unlocked");
            return true;
        } catch (InterruptedException e) {
            Logger.getInstance().error("ROBOT_EXEC", "Unlock Gimatic operation interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Picks up tool from its storage position.
     * Moves to tool position and locks the Gimatic tool changer.
     * Uses tool-specific base coordinate system but same motion pattern.
     * 
     * @param toolId The ID of the tool to pick (1-99)
     * @return True if the operation executed successfully, false otherwise.
     */
    private boolean pickTool(int toolId) {
        try {
            Logger.getInstance().log("ROBOT_EXEC", "Picking tool " + toolId);
            
            // Get tool-specific position (different base frame per tool)
            Frame toolPosition = getToolPosition(toolId);
            if (toolPosition == null) {
                Logger.getInstance().error("ROBOT_EXEC", "Tool " + toolId + " position not configured");
                return false;
            }
            
            // Move to tool position
            Logger.getInstance().log("ROBOT_EXEC", "Moving to tool " + toolId + " position");
            CartesianPTP ptpMotion = new CartesianPTP(toolPosition);
            ptpMotion.setJointVelocityRel(0.2); // Conservative speed for tool pickup
            
            try {
                IMotionContainer container = iiwa.moveAsync(ptpMotion);
                container.await();
                Logger.getInstance().log("ROBOT_EXEC", "Reached tool " + toolId + " position");
            } catch (Throwable t) {
                Logger.getInstance().error("ROBOT_EXEC", "Failed to move to tool " + toolId + " position: " + t.getClass().getName() + " - " + t.getMessage());
                Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", t instanceof Exception ? (Exception)t : new Exception("Throwable wrapper", t));
                return false;
            }
            
            // Lock the Gimatic tool changer
            if (!lockGimatic()) {
                Logger.getInstance().error("ROBOT_EXEC", "Failed to lock Gimatic for tool " + toolId);
                return false;
            }
            
            Logger.getInstance().log("ROBOT_EXEC", "Successfully picked tool " + toolId);
            return true;
        } catch (Throwable t) {
            Logger.getInstance().error("ROBOT_EXEC", "Pick tool " + toolId + " operation failed: " + t.getClass().getName() + " - " + t.getMessage());
            Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", t instanceof Exception ? (Exception)t : new Exception("Throwable wrapper", t));
            return false;
        }
    }



    /**
     * Gets the position/frame for a specific tool.
     * Each tool has its own base coordinate system.
     * 
     * @param toolId The ID of the tool (1-99)
     * @return The Frame representing the tool position, or null if not configured
     */
    private Frame getToolPosition(int toolId) {
        // TODO: Implement tool position configuration
        // This should return tool-specific positions with different base frames
        // For now, returning null to indicate positions need to be configured
        
        // Example implementation could be:
        // - Load from configuration file
        // - Use predefined positions based on toolId
        // - Query from a tool management system
        
        Logger.getInstance().warn("ROBOT_EXEC", "Tool " + toolId + " position not yet configured. Please implement getToolPosition().");
        return null;
    }

    @Override
    public void dispose() {
        Logger.getInstance().log("ROBOT_EXEC", "Disposing CommandExecutor.");
        
        // Stop robot console client
        if (consoleClient != null) {
            consoleClient.stop();
        }
        if (consoleClientThread != null && consoleClientThread.isAlive()) {
            try {
                consoleClientThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        super.dispose();
    }
    
    /**
     * Inner class that connects to LoggingServerManager as a client
     * and broadcasts received log messages to robot console via println.
     * 
     * This is necessary because only foreground tasks (like CommandExecutor)
     * can use println to write to the robot's SmartPad console.
     * Background tasks cannot directly write to console.
     */
    private static class RobotConsoleClient implements Runnable {
        private static final String LOG_SERVER_HOST = "localhost";
        private static final int LOG_SERVER_PORT = 30002;
        private static final int RECONNECT_DELAY_MS = 5000;
        
        private volatile boolean running = true;
        private Socket socket;
        private BufferedReader reader;
        
        @Override
        public void run() {
            System.out.println("[RobotConsoleClient] Starting console client to receive logs from LoggingServerManager...");
            
            while (running) {
                try {
                    // Connect to logging server
                    socket = new Socket(LOG_SERVER_HOST, LOG_SERVER_PORT);
                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    
                    System.out.println("[RobotConsoleClient] Connected to LoggingServerManager on port " + LOG_SERVER_PORT);
                    
                    // Read and broadcast log messages
                    String line;
                    while (running && (line = reader.readLine()) != null) {
                        // Broadcast to robot console using println
                        // Only foreground tasks can do this
                        System.out.println(line);
                    }
                    
                } catch (IOException e) {
                    if (running) {
                        System.out.println("[RobotConsoleClient] Connection error: " + e.getMessage());
                        System.out.println("[RobotConsoleClient] Will retry in " + RECONNECT_DELAY_MS + "ms...");
                        
                        // Wait before reconnecting
                        try {
                            Thread.sleep(RECONNECT_DELAY_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } finally {
                    // Clean up connection
                    closeConnection();
                }
            }
            
            System.out.println("[RobotConsoleClient] Console client stopped.");
        }
        
        public void stop() {
            running = false;
            closeConnection();
        }
        
        private void closeConnection() {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                // Ignore
            }
            
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}
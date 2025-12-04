package hartu.robot.executor.motion;

import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.motionModel.*;
import hartu.protocols.constants.ActionTypes;
import hartu.protocols.constants.MovementType;
import hartu.robot.commands.MotionParameters;
import hartu.robot.commands.ParsedCommand;
import hartu.robot.communication.server.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles execution of motion commands for the robot.
 * Responsible for creating and executing PTP, LIN, and CIRC motions.
 * Supports dynamic tool attachment based on command tool ID.
 */
public class MotionExecutor
{

    private final LBR robot;
    private final hartu.robot.executor.CommandExecutor commandExecutor;
    private final IErrorHandler moveAsyncErrorHandler;

    // Track the current command being executed for error handling
    private volatile ParsedCommand currentCommand = null;
    private volatile boolean currentCommandFailed = false;

    /**
     * Creates a new MotionExecutor.
     *
     * @param robot           The robot device to execute motions on
     * @param commandExecutor Reference to CommandExecutor for tool attachment
     * @param errorHandler    The error handler for asynchronous motion failures
     */
    public MotionExecutor(LBR robot, hartu.robot.executor.CommandExecutor commandExecutor, IErrorHandler errorHandler)
    {
        this.robot = robot;
        this.commandExecutor = commandExecutor;
        this.moveAsyncErrorHandler = errorHandler;
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
    public boolean executeMotion(ParsedCommand command)
    {
        ActionTypes actionType = command.getActionType();
        Logger.getInstance().low("ROBOT_EXEC", "Executing " + actionType.name() + " command ID " + command.getId());

        List<IMotion> motions;

        MovementType movementType = actionType.getMovementType();
        if (movementType == MovementType.PTP)
        {
            if (actionType.isJointMotion())
            {
                motions = createPtpJointMotions(command);
            } else
            {
                motions = createPtpCartesianMotions(command);
            }
        } else if (movementType == MovementType.LIN)
        {
            motions = createLinMotions(command);
        } else if (movementType == MovementType.CIRC)
        {
            motions = createCircMotions(command);
        } else
        {
            Logger.getInstance().error("ROBOT_EXEC", "Unsupported ActionType for movement command: " + actionType.name());
            return false;
        }

        if (motions.isEmpty())
        {
            Logger.getInstance().error("ROBOT_EXEC", "Failed to create any motion for command ID " + command.getId());
            return false;
        }

        boolean motionSuccess = true;

        try
        {
            IMotion motionToExecute;
            if (motions.size() > 1)
            {
                // Use MotionBatch to execute multiple motions as a single sequence
                motionToExecute = new MotionBatch(motions.toArray(new RobotMotion[0]));
            } else
            {
                // For a single motion, execute it directly
                motionToExecute = motions.get(0);
            }

            // Set the current command so the error handler can access it
            currentCommand = command;
            currentCommandFailed = false;

            // Get tool ID from motion parameters (stored as string, parse to int)
            String toolString = command.getMotionParameters() != null ? command.getMotionParameters().getTool() : null;
            int toolId = 0; // Default to tool 0 (GimaticCamera)

            if (toolString != null && !toolString.isEmpty())
            {
                try
                {
                    toolId = Integer.parseInt(toolString);
                } catch (NumberFormatException e)
                {
                    Logger.getInstance().warn("ROBOT_EXEC", "Invalid tool ID '" + toolString + "' in command. Using tool ID 0 (GimaticCamera).");
                    toolId = 0;
                }
            }

            // Get and attach the appropriate tool based on tool ID
            // This will handle tool switching if needed
            Tool selectedTool = commandExecutor.getAndAttachToolForId(toolId);

            // Execute asynchronous motion
            // Failures are handled by the registered IErrorHandler (see registerMoveAsyncErrorHandler)
            // The error handler will flush the queue and return ErrorHandlingAction.Ignore
            // This approach is per KUKA Sunrise.OS manual section 15.29.3
            IMotionContainer container;
            if (selectedTool != null)
            {
                // Use tool's default motion frame for execution
                // This ensures proper TCP (Tool Center Point) control
                container = selectedTool.moveAsync(motionToExecute);
                Logger.getInstance().debug("ROBOT_EXEC", "Executing motion with tool '" + selectedTool.getName() + "' (ID " + toolId + ") TCP.");
            } else
            {
                // Use robot directly when tool not available
                container = robot.moveAsync(motionToExecute);
                Logger.getInstance().debug("ROBOT_EXEC", "Executing motion with robot flange (tool ID " + toolId + " not available).");
            }
            container.await();

            // Check if the error handler was triggered
            if (currentCommandFailed)
            {
                Logger.getInstance().error("ROBOT_EXEC", "Motion for command ID " + command.getId() + " failed (handled by error handler).");
                motionSuccess = false;
            } else
            {
                Logger.getInstance().debug("ROBOT_EXEC", "Motion for command ID " + command.getId() + " completed successfully.");
            }

        } catch (Throwable t)
        {
            // This catch block handles exceptions during motion setup or unexpected errors
            // The IErrorHandler handles failures during moveAsync execution
            Logger.getInstance().error("ROBOT_EXEC", "Error preparing or executing motion for command ID " + command.getId() + ": " + t.getClass().getName() + " - " + t.getMessage());
            Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", t instanceof Exception ? (Exception) t : new Exception("Throwable wrapper", t));
            motionSuccess = false;
        } finally
        {
            // Clear current command reference
            currentCommand = null;
            currentCommandFailed = false;
        }

        return motionSuccess;
    }

    /**
     * Signals that the current command has failed (called by error handler).
     */
    public void signalCommandFailure()
    {
        currentCommandFailed = true;
    }

    /**
     * Gets the current command being executed.
     *
     * @return The current command, or null if no command is being executed
     */
    public ParsedCommand getCurrentCommand()
    {
        return currentCommand;
    }

    /**
     * Creates a list of PTP motions for a sequence of JointPositions.
     */
    private List<IMotion> createPtpJointMotions(ParsedCommand command)
    {
        List<IMotion> motions = new ArrayList<IMotion>();
        MotionParameters params = command.getMotionParameters();

        for (JointPosition axPos : command.getAxisTargetPoints())
        {
            motions.add(params.createPTPJointMotion(axPos));
        }
        return motions;
    }

    /**
     * Creates a list of PTP motions for a sequence of Cartesian Frames.
     */
    private List<IMotion> createPtpCartesianMotions(ParsedCommand command)
    {
        List<IMotion> motions = new ArrayList<IMotion>();
        MotionParameters params = command.getMotionParameters();

        for (Frame cartPos : command.getCartesianTargetPoints())
        {
            motions.add(params.createPTPMotion(cartPos));
        }
        return motions;
    }

    /**
     * Creates a list of LIN motions for a sequence of Cartesian Frames.
     */
    private List<IMotion> createLinMotions(ParsedCommand command)
    {
        List<IMotion> motions = new ArrayList<IMotion>();
        MotionParameters params = command.getMotionParameters();

        for (Frame cartPos : command.getCartesianTargetPoints())
        {
            motions.add(params.createLINMotion(cartPos));
        }
        return motions;
    }

    /**
     * Creates a list of circular motions for a sequence of Cartesian Frames.
     */
    private List<IMotion> createCircMotions(ParsedCommand command)
    {
        List<IMotion> motions = new ArrayList<IMotion>();
        MotionParameters params = command.getMotionParameters();
        List<Frame> cartesianPoints = command.getCartesianTargetPoints();

        if (cartesianPoints == null || cartesianPoints.size() < 2)
        {
            Logger.getInstance().error("ROBOT_EXEC", "Circular motion requires at least two Cartesian points. Command ID: " + command.getId());
            return Collections.emptyList();
        }

        for (int i = 0; i < cartesianPoints.size() - 1; i++)
        {
            Frame auxiliaryFrame = cartesianPoints.get(i);
            Frame destinationFrame = cartesianPoints.get(i + 1);
            motions.add(params.createCircularMotion(auxiliaryFrame, destinationFrame));
        }
        return motions;
    }
}

package hartu.robot.executor;

import com.kuka.generated.ioAccess.Ethercat_x44IOGroup;
import com.kuka.generated.ioAccess.IOFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.executionModel.CancelledException;
import com.kuka.roboticsAPI.executionModel.CommandInvalidException;
import com.kuka.roboticsAPI.executionModel.ExecutionException;
import com.kuka.roboticsAPI.executionModel.ExternalStopException;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.motionModel.IMotion;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.motionModel.MotionBatch;
import com.kuka.roboticsAPI.motionModel.RobotMotion;
import hartu.protocols.constants.ActionTypes;
import hartu.protocols.constants.MovementType;
import hartu.robot.commands.MotionParameters;
import hartu.robot.commands.ParsedCommand;
import hartu.robot.commands.io.IoCommandData;
import hartu.robot.communication.server.CommandQueue;
import hartu.robot.communication.server.CommandResultHolder;
import hartu.robot.communication.server.Logger;

import javax.inject.Inject;
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

    @Override
    public void initialize() {
        Logger.getInstance().log("ROBOT_EXEC", "Initializing. Ready to take commands from queue.");
    }

    @Override
    public void run() {
        try {
            while (true) {
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
                                break;
                        }
                    } catch (Exception e) {
                        Logger.getInstance().error("ROBOT_EXEC", "Error: Exception during command execution for ID " + command.getId() + ": " + e.getMessage());
                        Logger.getInstance().error("ROBOT_EXEC", "Stack trace:" + e);
                        executionSuccess = false;
                    } finally {
                        resultHolder.setSuccess(executionSuccess);
                        resultHolder.getLatch().countDown();
                        Logger.getInstance().log("ROBOT_EXEC", "Signaled completion for command ID " + command.getId() + ". Success: " + executionSuccess);
                    }
                }
            }
        } catch (Exception e) {
            // Catch any other unexpected exceptions that cause the entire run loop to exit
            Logger.getInstance().error("ROBOT_EXEC", "Critical, unhandled error in CommandExecutor run loop: " + e.getMessage());
            // Log full stack trace for debugging critical unhandled exceptions
            Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", e);
        } finally {
            // This block will be executed if the run() method exits for any reason.
            // The dispose() method is called by the RoboticsAPIApplication framework as part of its lifecycle.
            Logger.getInstance().log("ROBOT_EXEC", "CommandExecutor run method is exiting.");
        }
    }

    /**
     * Executes a movement command by delegating to specific motion type handlers.
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

        try {
            IMotion motionToExecute;
            if (motions.size() > 1) {
                // Use MotionBatch to execute multiple motions as a single sequence
                motionToExecute = new MotionBatch(motions.toArray(new RobotMotion[0]));
            } else {
                // For a single motion, execute it directly
                motionToExecute = motions.get(0);
            }

            // Log the specific motion or batch details
            Logger.getInstance().log("ROBOT_EXEC", "Executing " + actionType.name() + " command ID " + command.getId() + " with motion: " + motionToExecute.toString());
            //TODO: Catch Software axis limit violations in order to not stop task execution continuity
             try {
                IMotionContainer container = iiwa.moveAsync(motionToExecute);
                container.await();
                Logger.getInstance().log("ROBOT_EXEC", "All motions for command ID " + command.getId() + " completed successfully.");
            } catch (CommandInvalidException e) {
                Logger.getInstance().error("ROBOT_EXEC", "CommandInvalidException: " + e.getMessage());
            } catch (CancelledException e) {
                Logger.getInstance().error("ROBOT_EXEC", "CancelledException: " + e.getMessage());
            } catch (ExternalStopException e) {
                Logger.getInstance().error("ROBOT_EXEC", "ExternalStopException: " + e.getMessage());
            }

        } catch (Exception e) {
            Logger.getInstance().error("ROBOT_EXEC", "Error during movement execution for command ID " + command.getId() + ": " + e.getMessage());
            return false;
        }

        return true;
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
                    gimaticIO.setDO_Flange7(ioState);
                    Logger.getInstance().log("ROBOT_EXEC", "Set DO_Flange7 to " + ioState);
                    return true;
                case 2:
                    toolControlIO.setOutput2(ioState);
                    Logger.getInstance().log("ROBOT_EXEC", "Set Ethercat_x44 Output2 to " + ioState);
                    return true;
                case 3:
                    toolControlIO.setOutput1(ioState);
                    Logger.getInstance().log("ROBOT_EXEC", "Set Ethercat_x44 Output1 to " + ioState);
                    return true;
                default:
                    Logger.getInstance().error("ROBOT_EXEC", "Invalid IO pin in parsed command for direct mapping: " + ioPin + " for command ID " + command.getId());
                    return false;
            }
        } catch (Exception e) {
            Logger.getInstance().error("ROBOT_EXEC", "IO command ID " + command.getId() + " failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Placeholder for executing external program call commands.
     *
     * @param command The ParsedCommand representing an external program call.
     * @return True if the program call executed successfully, false otherwise.
     */
    private boolean executeProgramCallCommand(ParsedCommand command) {
        Logger.getInstance().warn("ROBOT_EXEC", "External Program Call command ID " + command.getId() + " received. This functionality is not yet implemented.");
        // TODO: Implement logic for executing external programs based on command.getProgramId() or other data.
        return false; // Currently always returns false as it's not implemented
    }

    @Override
    public void dispose() {
        Logger.getInstance().log("ROBOT_EXEC", "Disposing CommandExecutor.");
        super.dispose();
    }
}
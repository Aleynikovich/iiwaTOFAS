package hartu.robot.executor;

import com.kuka.generated.ioAccess.Ethercat_x44IOGroup;
import com.kuka.generated.ioAccess.IOFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.motionModel.IMotion;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
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

import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;

public class CommandExecutor extends RoboticsAPIApplication {

    @Inject
    private Controller robotController;
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
        while (true) {
            CommandResultHolder resultHolder = CommandQueue.pollCommand(100, TimeUnit.MILLISECONDS);

            if (resultHolder != null) {
                ParsedCommand command = resultHolder.getCommand();
                Logger.getInstance().log(
                        "ROBOT_EXEC",
                        "Received command ID " + command.getId() + " from queue for execution."
                );
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
                } catch (Exception e) {
                    Logger.getInstance().error(
                            "ROBOT_EXEC",
                            "Error: Exception during command execution for ID " + command.getId() + ": " + e.getMessage()
                    );
                    executionSuccess = false;
                } finally {
                    resultHolder.setSuccess(executionSuccess);
                    resultHolder.getLatch().countDown();
                    Logger.getInstance().log(
                            "ROBOT_EXEC",
                            "Signaled completion for command ID " + command.getId() + ". Success: " + executionSuccess
                    );
                }
            }
        }
    }

    /**
     * Consolidates the execution logic for all movement commands (PTP, LIN, CIRC).
     * This method now handles both single-point and continuous motions, applying blending for the latter.
     *
     * @param command The ParsedCommand representing a movement.
     * @return True if the movement executed successfully, false otherwise.
     */
    private boolean executeMovementCommand(ParsedCommand command) {
        MotionParameters motionParams = command.getMotionParameters();
        double speed = motionParams.getSpeedOverride();
        String commandId = command.getId();
        ActionTypes actionType = command.getActionType();
        MovementType movementType = MovementType.fromActionType(actionType);

        Logger.getInstance().log("ROBOT_EXEC", "Executing " + actionType.name() + " command ID " + commandId + " with speed override: " + speed);

        // Handle CIRC motions separately as they require two points at a time
        if (actionType == ActionTypes.CIRC_AXIS || actionType == ActionTypes.CIRC_FRAME) {
            //TODO: Implement circ
            return false;//executeCIRC(command); // Keep executeCIRC as a dedicated method
        }

        List<IMotionContainer> motionContainers = new ArrayList<>(); // Store motion containers
        boolean isJointMotion = movementType.isAxisMotion();
        String motionDescription = isJointMotion ? "Axis" : "Cartesian";

        Logger.getInstance().warn("ROBOT_EXEC", actionType.name() + " with multiple points: Attempting continuous motion via chained individual moves with blending.");

        try {
            if (isJointMotion) {
                for (JointPosition axPos : command.getAxisTargetPoints()) {
                    RobotMotion<?> currentMotion = null;
                    if (actionType == ActionTypes.PTP_AXIS || actionType == ActionTypes.PTP_AXIS_C) {
                        currentMotion = ptp(axPos);
                    } else if (actionType == ActionTypes.LIN_AXIS) {
                        currentMotion = lin(iiwa.getForwardKinematic(axPos));
                    }
                    if (currentMotion != null) {
                        motionContainers.add(iiwa.moveAsync(currentMotion.setBlendingRel(0.5)));
                    } else {
                        Logger.getInstance().error("ROBOT_EXEC", "Failed to create joint motion for " + actionType.name() + " command ID " + commandId);
                        return false;
                    }
                }
            } else { // Cartesian motion
                List<Frame> cartesianPoints = command.getCartesianTargetPoints();
                for (Frame cartPos : cartesianPoints) {
                    RobotMotion<?> currentMotion = null;
                    if (actionType == ActionTypes.PTP_FRAME || actionType == ActionTypes.PTP_FRAME_C) {
                        currentMotion = ptp(cartPos);
                    } else if (actionType == ActionTypes.LIN_FRAME || actionType == ActionTypes.LIN_FRAME_C) {
                        currentMotion = lin(cartPos);
                    } else if (actionType == ActionTypes.LIN_REL_TOOL) {
                        currentMotion = linRel(cartPos.getX(), cartPos.getY(), cartPos.getZ(), iiwa.getFlange());
                    } else if (actionType == ActionTypes.LIN_REL_BASE) {
                        currentMotion = linRel(cartPos.getX(), cartPos.getY(), cartPos.getZ(), iiwa.getRootFrame());
                    }
                    if (currentMotion != null) {
                        motionContainers.add(iiwa.moveAsync(currentMotion.setBlendingRel(0.5)));
                    } else {
                        Logger.getInstance().error("ROBOT_EXEC", "Failed to create cartesian motion for " + actionType.name() + " command ID " + commandId);
                        return false;
                    }
                }
            }

            if (motionContainers.isEmpty()) {
                Logger.getInstance().error("ROBOT_EXEC", "Failed to create any motion for command ID " + commandId + " with ActionType " + actionType.name());
                return false;
            }

            // Wait for all motions to complete
            for (IMotionContainer container : motionContainers) {
                container.await();
            }
            Logger.getInstance().log("ROBOT_EXEC", "All motions for command ID " + commandId + " completed.");
            return true;

        } catch (Exception e) {
            Logger.getInstance().error(
                    "ROBOT_EXEC",
                    "Error during movement execution for command ID " + commandId + ": " + e.getMessage()
            );
            return false;
        }
    }


//    private boolean executeCIRC(ParsedCommand command) {
//        MotionParameters motionParams = command.getMotionParameters();
//        double speed = motionParams.getSpeedOverride();
//        Logger.getInstance().log("ROBOT_EXEC", "Executing CIRC command ID " + command.getId() + " with speed override: " + speed);
//
//        IMotion circMotion = null;
//        boolean isJointMotion = false; // CIRC motions are always Cartesian in terms of velocity setting
//
//        // Use the targetClass from MovementTargets to cast the list correctly
//        if (movementTargets.getTargetClass() == AxisPosition.class) {
//            List<AxisPosition> axisPoints = (List<AxisPosition>) rawPoints;
//            AxisPosition intermediate = axisPoints.get(0);
//            AxisPosition end = axisPoints.get(1);
//            circMotion = circ(iiwa.getForwardKinematic(intermediate.toJointPosition()), iiwa.getForwardKinematic(end.toJointPosition()));
//            isJointMotion = false;
//        } else if (movementTargets.getTargetClass() == CartesianPosition.class) {
//            List<CartesianPosition> cartesianPoints = (List<CartesianPosition>) rawPoints;
//            CartesianPosition intermediate = cartesianPoints.get(0);
//            CartesianPosition end = cartesianPoints.get(1);
//            circMotion = circ(intermediate.toFrame(iiwa.getFlange()), end.toFrame(iiwa.getFlange()));
//            isJointMotion = false;
//        } else {
//            Logger.getInstance().error("ROBOT_EXEC", "CIRC command ID " + command.getId() + " has an unsupported target class for CIRC motion: " + movementTargets.getTargetClass().getSimpleName());
//            return false;
//        }
//        return executeMotionInternal(circMotion, speed, isJointMotion, command.getId(), command.getActionType());
//    }

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
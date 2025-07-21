//package hartu.robot.executor;
//
//import com.kuka.generated.ioAccess.Ethercat_x44IOGroup;
//import com.kuka.generated.ioAccess.IOFlangeIOGroup;
//import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
//import com.kuka.roboticsAPI.controllerModel.Controller;
//import com.kuka.roboticsAPI.deviceModel.LBR;
//import com.kuka.roboticsAPI.motionModel.IMotion;
//import com.kuka.roboticsAPI.motionModel.IMotionContainer;
//import com.kuka.roboticsAPI.motionModel.RobotMotion;
//import hartu.protocols.constants.ActionTypes;
//import hartu.protocols.constants.MovementType;
//import hartu.robot.commands.MotionParameters;
//import hartu.robot.commands.MovementTargets;
//import hartu.robot.commands.ParsedCommand;
//import hartu.robot.commands.io.IoCommandData;
//import hartu.robot.communication.server.CommandQueue;
//import hartu.robot.communication.server.CommandResultHolder;
//import hartu.robot.communication.server.Logger;
//
//import javax.inject.Inject;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//
//import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
//
//public class CommandExecutor extends RoboticsAPIApplication
//{
//
//    @Inject
//    private Controller robotController;
//    @Inject
//    private LBR iiwa;
//    @Inject
//    private IOFlangeIOGroup gimaticIO;
//    @Inject
//    private Ethercat_x44IOGroup toolControlIO;
//
//    @Override
//    public void initialize()
//    {
//        Logger.getInstance().log("ROBOT_EXEC", "Initializing. Ready to take commands from queue.");
//    }
//
//    @Override
//    public void run()
//    {
//        while (true)
//        {
//            CommandResultHolder resultHolder = CommandQueue.pollCommand(100, TimeUnit.MILLISECONDS);
//
//            if (resultHolder != null)
//            {
//                ParsedCommand command = resultHolder.getCommand();
//                Logger.getInstance().log(
//                        "ROBOT_EXEC",
//                        "Received command ID " + command.getId() + " from queue for execution."
//                                        );
//                boolean executionSuccess = false;
//
//                try
//                {
//                    switch (command.getCommandCategory()) {
//                        case MOVEMENT:
//                            executionSuccess = executeMovementCommand(command);
//                            break;
//                        case IO:
//                            executionSuccess = executeIO(command);
//                            break;
//                        case PROGRAM_CALL:
//                            executionSuccess = executeProgramCallCommand(command);
//                            break;
//                        case UNKNOWN:
//                        default:
//                            Logger.getInstance().warn("ROBOT_EXEC", "Unknown or unsupported primary command category for ID " + command.getId() + ": " + command.getCommandCategory().name());
//                            executionSuccess = false;
//                            break;
//                    }
//                }
//                catch (Exception e)
//                {
//                    Logger.getInstance().error(
//                            "ROBOT_EXEC",
//                            "Error: Exception during command execution for ID " + command.getId() + ": " + e.getMessage()
//                                              );
//                    executionSuccess = false;
//                }
//                finally
//                {
//                    resultHolder.setSuccess(executionSuccess);
//                    resultHolder.getLatch().countDown();
//                    Logger.getInstance().log(
//                            "ROBOT_EXEC",
//                            "Signaled completion for command ID " + command.getId() + ". Success: " + executionSuccess
//                                            );
//                }
//            }
//        }
//    }
//
//    /**
//     * Consolidates the execution logic for all movement commands (PTP, LIN, CIRC).
//     * This method now handles both single-point and continuous motions, applying blending for the latter.
//     * @param command The ParsedCommand representing a movement.
//     * @return True if the movement executed successfully, false otherwise.
//     */
//    private boolean executeMovementCommand(ParsedCommand command) {
//        MotionParameters motionParams = command.getMotionParameters();
//        double speed = motionParams.getSpeedOverride();
//        String commandId = command.getId();
//        ActionTypes actionType = command.getActionType();
//        MovementType movementType = MovementType.fromActionType(actionType);
//
//        Logger.getInstance().log("ROBOT_EXEC", "Executing " + actionType.name() + " command ID " + commandId + " with speed override: " + speed);
//
//        // Handle CIRC motions separately as they require two points at a time
//        if (actionType == ActionTypes.CIRC_AXIS || actionType == ActionTypes.CIRC_FRAME) {
//            return executeCIRC(command); // Keep executeCIRC as a dedicated method
//        }
//
//        List<IMotion> motionsToExecute = new ArrayList<>();
//        boolean isJointMotion = movementType.isAxisMotion();
//        String motionDescription = isJointMotion ? "Axis" : "Cartesian";
//
//        // Use the new consolidated getter
//        MovementTargets<? extends PositionClass> movementTargets = command.getMovementTargetPoints();
//        if (movementTargets == null || movementTargets.getTargets() == null || movementTargets.getTargets().isEmpty()) {
//            Logger.getInstance().error("ROBOT_EXEC", actionType.name() + " command ID " + commandId + " has no target points for " + motionDescription + ".");
//            return false;
//        }
//
//        // Get the raw list of points
//        List<? extends PositionClass> rawPoints = movementTargets.getTargets();
//
//        if (motionParams.isContinuous() && rawPoints.size() > 1) {
//            Logger.getInstance().warn("ROBOT_EXEC", actionType.name() + " with multiple points: Attempting continuous motion via chained individual moves with blending.");
//
//            if (isJointMotion) {
//                // Cast the list to the specific type for iteration
//                List<AxisPosition> axisPoints = (List<AxisPosition>) rawPoints;
//                for (AxisPosition axPos : axisPoints) { // Iterate directly with AxisPosition
//                    RobotMotion<?> currentMotion = null;
//                    if (actionType == ActionTypes.PTP_AXIS || actionType == ActionTypes.PTP_AXIS_C) {
//                        currentMotion = ptp(axPos.toJointPosition());
//                    } else if (actionType == ActionTypes.LIN_AXIS) {
//                        currentMotion = lin(iiwa.getForwardKinematic(axPos.toJointPosition()));
//                    }
//                    if (currentMotion != null) {
//                        motionsToExecute.add(currentMotion.setBlendingRel(0.5)); // Apply blending
//                    }
//                }
//            } else { // Cartesian motion
//                // Cast the list to the specific type for iteration
//                List<CartesianPosition> cartesianPoints = (List<CartesianPosition>) rawPoints;
//                for (CartesianPosition cartPos : cartesianPoints) { // Iterate directly with CartesianPosition
//                    RobotMotion<?> currentMotion = null;
//                    if (actionType == ActionTypes.PTP_FRAME || actionType == ActionTypes.PTP_FRAME_C) {
//                        currentMotion = ptp(cartPos.toFrame(iiwa.getFlange()));
//                    } else if (actionType == ActionTypes.LIN_FRAME || actionType == ActionTypes.LIN_FRAME_C) {
//                        currentMotion = lin(cartPos.toFrame(iiwa.getFlange()));
//                    } else if (actionType == ActionTypes.LIN_REL_TOOL) {
//                        currentMotion = linRel(cartPos.getX(), cartPos.getY(), cartPos.getZ(), iiwa.getFlange());
//                    } else if (actionType == ActionTypes.LIN_REL_BASE) {
//                        currentMotion = linRel(cartPos.getX(), cartPos.getY(), cartPos.getZ(), iiwa.getRootFrame());
//                    }
//                    if (currentMotion != null) {
//                        motionsToExecute.add(currentMotion.setBlendingRel(0.5)); // Apply blending
//                    }
//                }
//            }
//        } else {
//            // Single point motion
//            PositionClass singlePoint = rawPoints.get(0); // Still get as PositionClass for initial access
//            IMotion singleMotion = null;
//
//            if (isJointMotion) { // Axis motion
//                AxisPosition axPos = (AxisPosition) singlePoint; // Cast to AxisPosition
//                if (actionType == ActionTypes.PTP_AXIS) {
//                    singleMotion = ptp(axPos.toJointPosition());
//                } else if (actionType == ActionTypes.LIN_AXIS) {
//                    singleMotion = lin(iiwa.getForwardKinematic(axPos.toJointPosition()));
//                }
//            } else { // Cartesian motion
//                CartesianPosition cartPos = (CartesianPosition) singlePoint; // Cast to CartesianPosition
//                if (actionType == ActionTypes.PTP_FRAME) {
//                    singleMotion = ptp(cartPos.toFrame(iiwa.getFlange()));
//                } else if (actionType == ActionTypes.LIN_FRAME) {
//                    singleMotion = lin(cartPos.toFrame(iiwa.getFlange()));
//                } else if (actionType == ActionTypes.LIN_REL_TOOL) {
//                    singleMotion = linRel(cartPos.getX(), cartPos.getY(), cartPos.getZ(), iiwa.getFlange());
//                } else if (actionType == ActionTypes.LIN_REL_BASE) {
//                    singleMotion = linRel(cartPos.getX(), cartPos.getY(), cartPos.getZ(), iiwa.getRootFrame());
//                }
//            }
//            if (singleMotion != null) {
//                motionsToExecute.add(singleMotion);
//            }
//        }
//
//        if (motionsToExecute.isEmpty()) {
//            Logger.getInstance().error("ROBOT_EXEC", "Failed to create any motion for command ID " + commandId + " with ActionType " + actionType.name());
//            return false;
//        }
//
//        boolean overallSuccess = true;
//        for (IMotion motion : motionsToExecute) {
//            if (!executeMotionInternal(motion, speed, isJointMotion, commandId, actionType)) {
//                overallSuccess = false;
//                break;
//            }
//        }
//        return overallSuccess;
//    }
//
//    /**
//     * Helper method to execute a motion and handle its completion/failure.
//     * Sets velocity based on whether it's a joint-space motion.
//     *
//     * @param motion The IMotion to execute.
//     * @param speed The relative speed override (0.0 - 1.0).
//     * @param isJointMotion True if this is a joint-space motion (PTP_AXIS, PTP_AXIS_C), false for Cartesian.
//     * @param commandId The ID of the command for logging purposes.
//     * @param actionType The ActionType for logging purposes.
//     * @return True if the motion executed successfully, false otherwise.
//     */
//    private boolean executeMotionInternal(IMotion motion, double speed, boolean isJointMotion, String commandId, ActionTypes actionType) {
//        IMotionContainer motionContainer = null;
//        try {
//            RobotMotion<?> robotMotion = (RobotMotion<?>) motion;
//
//            motionContainer = iiwa.move(robotMotion.setJointVelocityRel(speed));
//            motionContainer.await();
//            Logger.getInstance().log("ROBOT_EXEC", actionType.name() + " command ID " + commandId + " executed successfully.");
//            return true;
//        } catch (Exception e) {
//            Logger.getInstance().error("ROBOT_EXEC", actionType.name() + " command ID " + commandId + " failed: " + e.getMessage());
//            return false;
//        }
//    }
//
//    private boolean executeCIRC(ParsedCommand command) {
//        MotionParameters motionParams = command.getMotionParameters();
//        double speed = motionParams.getSpeedOverride();
//        Logger.getInstance().log("ROBOT_EXEC", "Executing CIRC command ID " + command.getId() + " with speed override: " + speed);
//
//        IMotion circMotion = null;
//        boolean isJointMotion = false; // CIRC motions are always Cartesian in terms of velocity setting
//
//        // Use the new consolidated getter
//        MovementTargets<? extends PositionClass> movementTargets = command.getMovementTargetPoints();
//        if (movementTargets == null || movementTargets.getTargets() == null || movementTargets.getTargets().size() < 2) {
//            Logger.getInstance().error("ROBOT_EXEC", "CIRC command ID " + command.getId() + " requires at least two target points (intermediate and end).");
//            return false;
//        }
//
//        List<? extends PositionClass> rawPoints = movementTargets.getTargets();
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
//
//    private boolean executeIO(ParsedCommand command) {
//        IoCommandData ioData = command.getIoCommandData();
//        if (ioData == null) {
//            Logger.getInstance().error("ROBOT_EXEC", "IO command ID " + command.getId() + " has no IO data.");
//            return false;
//        }
//
//        int ioPin = ioData.getIoPin();
//        boolean ioState = ioData.getIoState();
//
//        Logger.getInstance().log("ROBOT_EXEC", "Executing IO command ID " + command.getId() + ". Pin: " + ioPin + ", State: " + ioState);
//
//        try {
//            switch (ioPin) {
//                case 1:
//                    gimaticIO.setDO_Flange7(ioState);
//                    Logger.getInstance().log("ROBOT_EXEC", "Set DO_Flange7 to " + ioState);
//                    return true;
//                case 2:
//                    toolControlIO.setOutput2(ioState);
//                    Logger.getInstance().log("ROBOT_EXEC", "Set Ethercat_x44 Output2 to " + ioState);
//                    return true;
//                case 3:
//                    toolControlIO.setOutput1(ioState);
//                    Logger.getInstance().log("ROBOT_EXEC", "Set Ethercat_x44 Output1 to " + ioState);
//                    return true;
//                default:
//                    Logger.getInstance().error("ROBOT_EXEC", "Invalid IO pin in parsed command for direct mapping: " + ioPin + " for command ID " + command.getId());
//                    return false;
//            }
//        } catch (Exception e) {
//            Logger.getInstance().error("ROBOT_EXEC", "IO command ID " + command.getId() + " failed: " + e.getMessage());
//            return false;
//        }
//    }
//
//    /**
//     * Placeholder for executing external program call commands.
//     * @param command The ParsedCommand representing an external program call.
//     * @return True if the program call executed successfully, false otherwise.
//     */
//    private boolean executeProgramCallCommand(ParsedCommand command) {
//        Logger.getInstance().warn("ROBOT_EXEC", "External Program Call command ID " + command.getId() + " received. This functionality is not yet implemented.");
//        // TODO: Implement logic for executing external programs based on command.getProgramId() or other data.
//        return false; // Currently always returns false as it's not implemented
//    }
//
//    @Override
//    public void dispose()
//    {
//        Logger.getInstance().log("ROBOT_EXEC", "Disposing CommandExecutor.");
//        super.dispose();
//    }
//}

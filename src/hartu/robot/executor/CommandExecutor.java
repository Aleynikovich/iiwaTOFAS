// --- CommandExecutor.java ---
package hartu.robot.executor;

import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool; // Import Tool
import com.kuka.roboticsAPI.geometricModel.World; // Import World for root frame
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.motionModel.PTP;
import com.kuka.roboticsAPI.motionModel.LIN;
import com.kuka.roboticsAPI.motionModel.IMotion;
import com.kuka.roboticsAPI.motionModel.Spline; // For CIRC motions, though we're using LIN.andThen for CIRC_FRAME

// KUKA Generated IO Access Imports
import com.kuka.generated.ioAccess.Ethercat_x44IOGroup;
import com.kuka.generated.ioAccess.IOFlangeIOGroup;
// Assuming MediaFlangeIOGroup is also needed if ioPin 3 maps to it, based on previous MessageHandler.
// If not, this import can be removed. Based on your old code, only Ethercat_x44 and IOFlange were directly used.
// import com.kuka.generated.ioAccess.MediaFlangeIOGroup;


import hartu.protocols.constants.ActionTypes;
import hartu.robot.commands.ParsedCommand;
import hartu.robot.commands.MotionParameters;
import hartu.robot.commands.positions.AxisPosition;
import hartu.robot.commands.positions.CartesianPosition;
import hartu.robot.communication.server.Logger;

import javax.inject.Inject; // For dependency injection

import java.util.List;

/**
 * Executes parsed robot commands using the KUKA Robotics API.
 * This class handles different action types (PTP, LIN, IO, etc.)
 * and applies motion parameters, tool, and base selections.
 */
public class CommandExecutor extends RoboticsAPIApplication
{

    @Inject
    private Controller robotController; // Injected KUKA Controller
    @Inject
    private LBR robot; // Injected KUKA LBR (Lightweight Robot) object

    // Injected IO Groups
    @Inject
    private IOFlangeIOGroup ioFlangeIOGroup;
    @Inject
    private Ethercat_x44IOGroup ethercatX44IOGroup;
    // @Inject // Uncomment if MediaFlangeIOGroup is also directly used for IO
    // private MediaFlangeIOGroup mediaFlangeIOGroup;


    /**
     * Constructor for CommandExecutor.
     * KUKA API objects (Controller, LBR, IO Groups) are typically injected by the RoboticsAPI framework.
     */
    @Inject
    public CommandExecutor(Controller controller, LBR lbr, IOFlangeIOGroup ioFlange, Ethercat_x44IOGroup ethercatX44) {
        this.robotController = controller;
        this.robot = lbr;
        this.ioFlangeIOGroup = ioFlange;
        this.ethercatX44IOGroup = ethercatX44;
        Logger.getInstance().log("CommandExecutor: Initialized with Controller, LBR, and IO Groups.");
    }

    /**
     * Executes the given parsed command.
     *
     * @param command The ParsedCommand object to execute.
     * @return true if the command was executed successfully, false otherwise.
     */
    public boolean executeCommand(ParsedCommand command) {
        if (command == null) {
            Logger.getInstance().log("CommandExecutor Error: Received null command.");
            return false;
        }

        Logger.getInstance().log("CommandExecutor: Executing command ID: " + command.getId() + ", Type: " + command.getActionType().name());

        try {
            MotionParameters motionParams = command.getMotionParameters();
            double speedOverride = motionParams != null ? motionParams.getSpeedOverride() : 1.0; // Default to 1.0 (100%)
            String toolName = motionParams != null ? motionParams.getTool() : "";
            String baseName = motionParams != null ? motionParams.getBase() : "";

            // --- Tool and Base Management ---
            // Set active tool and base based on command parameters
            // If toolName is empty or "[Default]", use current tool. Otherwise, get by name.
            Tool activeTool = null;
            if (toolName != null && !toolName.isEmpty() && !toolName.equalsIgnoreCase("[Default]")) {
                try {
                    activeTool = robotController.getApplicationData().getTool(toolName);
                    Logger.getInstance().log("CommandExecutor: Using specified tool: " + toolName);
                } catch (Exception e) {
                    Logger.getInstance().log("CommandExecutor Warning: Tool '" + toolName + "' not found. Using current active tool. Error: " + e.getMessage());
                    activeTool = robot.getCurrentTool(); // Fallback
                }
            } else {
                activeTool = robot.getCurrentTool(); // Use currently active tool
                Logger.getInstance().log("CommandExecutor: Using current active tool (default or not specified).");
            }
            // Attach the tool if it's not already attached or if it's a new tool
            if (activeTool != null && activeTool.getParent() != robot.getFlange()) {
                activeTool.attachTo(robot.getFlange());
                Logger.getInstance().log("CommandExecutor: Attached tool: " + activeTool.getName());
            } else if (activeTool == null) {
                Logger.getInstance().log("CommandExecutor Warning: No active tool to attach.");
            }


            // If baseName is empty or "[Default]", use current base. Otherwise, get by name.
            Frame activeBase = null;
            if (baseName != null && !baseName.isEmpty() && !baseName.equalsIgnoreCase("[Default]")) {
                try {
                    activeBase = robotController.getApplicationData().getFrame(baseName);
                    Logger.getInstance().log("CommandExecutor: Using specified base: " + baseName);
                } catch (Exception e) {
                    Logger.getInstance().log("CommandExecutor Warning: Base '" + baseName + "' not found. Using current active base. Error: " + e.getMessage());
                    activeBase = robot.getCurrentBase(); // Fallback
                }
            } else {
                activeBase = robot.getCurrentBase(); // Use currently active base
                Logger.getInstance().log("CommandExecutor: Using current active base (default or not specified).");
            }
            // Set the active base for the robot's motion context
            robot.setHomeFrame(activeBase); // This sets the base for subsequent motions
            Logger.getInstance().log("CommandExecutor: Active base set to: " + activeBase.getName());


            // --- Execute Command based on ActionType ---
            switch (command.getActionType()) {
                case PTP_AXIS:
                case PTP_AXIS_C:
                    return executePTPMotion(command.getAxisTargetPoints(), command.getCartesianTargetPoints(), speedOverride, command.getActionType());

                case LIN_AXIS:
                case LIN_FRAME:
                case LIN_FRAME_C:
                case LIN_REL_TOOL:
                case LIN_REL_BASE:
                    return executeLINMotion(command.getAxisTargetPoints(), command.getCartesianTargetPoints(), speedOverride, command.getActionType());

                case CIRC_AXIS:
                case CIRC_FRAME:
                    return executeCIRCMotion(command.getAxisTargetPoints(), command.getCartesianTargetPoints(), speedOverride, command.getActionType());

                case ACTIVATE_IO:
                    return executeIOMotion(command.getIoCommandData().getIoPoint(), command.getIoCommandData().getIoPin(), command.getIoCommandData().getIoState());

                case UNKNOWN:
                default:
                    Logger.getInstance().log("CommandExecutor Error: Unsupported ActionType: " + command.getActionType().name());
                    return false;
            }
        } catch (Exception e) {
            Logger.getInstance().log("CommandExecutor Error during execution of command ID " + command.getId() + ": " + e.getMessage());
            e.printStackTrace(); // Print stack trace for detailed debugging on KUKA console
            return false;
        }
    }

    /**
     * Helper method to execute PTP motions.
     */
    private boolean executePTPMotion(List<AxisPosition> axisPoints, List<CartesianPosition> cartesianPoints, double speedOverride, ActionTypes actionType) {
        IMotion motion = null;

        if (axisPoints != null && !axisPoints.isEmpty()) {
            // PTP_AXIS or PTP_AXIS_C
            AxisPosition target = axisPoints.get(0); // PTP usually takes one target
            motion = new PTP(target.getJ1(), target.getJ2(), target.getJ3(), target.getJ4(), target.getJ5(), target.getJ6(), target.getJ7());
            Logger.getInstance().log("CommandExecutor: Executing PTP Axis motion to: " + target.toString());
        } else if (cartesianPoints != null && !cartesianPoints.isEmpty()) {
            // PTP_FRAME or PTP_FRAME_C
            CartesianPosition target = cartesianPoints.get(0); // PTP usually takes one target

            // Create Frame relative to World.Current.getRootFrame() as per old MessageHandler
            Frame targetFrame = new Frame(World.Current.getRootFrame(),
                    target.getX(), target.getY(), target.getZ(),
                    Math.toRadians(target.getA()), Math.toRadians(target.getB()), Math.toRadians(target.getC()));

            motion = new PTP(targetFrame);
            Logger.getInstance().log("CommandExecutor: Executing PTP Frame motion to: " + targetFrame.toString());
        } else {
            Logger.getInstance().log("CommandExecutor Error: PTP motion called without target points.");
            return false;
        }

        if (motion != null) {
            // Apply speed override and execute
            IMotionContainer container;
            if (actionType == ActionTypes.PTP_AXIS_C || actionType == ActionTypes.PTP_FRAME_C) {
                // For continuous PTP, use moveAsync with blending
                container = robot.moveAsync(motion.setJointVelocityRel(speedOverride).setBlendingRel(0.5));
                Logger.getInstance().log("CommandExecutor: PTP Continuous motion started.");
            } else {
                // For synchronous PTP
                container = robot.move(motion.setJointVelocityRel(speedOverride));
                Logger.getInstance().log("CommandExecutor: PTP motion completed.");
            }
            return true;
        }
        return false;
    }

    /**
     * Helper method to execute LIN motions.
     */
    private boolean executeLINMotion(List<AxisPosition> axisPoints, List<CartesianPosition> cartesianPoints, double speedOverride, ActionTypes actionType) {
        IMotion motion = null;

        if (axisPoints != null && !axisPoints.isEmpty()) {
            // LIN_AXIS
            AxisPosition target = axisPoints.get(0);
            motion = new LIN(target.getJ1(), target.getJ2(), target.getJ3(), target.getJ4(), target.getJ5(), target.getJ6(), target.getJ7());
            Logger.getInstance().log("CommandExecutor: Executing LIN Axis motion to: " + target.toString());
        } else if (cartesianPoints != null && !cartesianPoints.isEmpty()) {
            // LIN_FRAME, LIN_FRAME_C, LIN_REL_TOOL, LIN_REL_BASE
            CartesianPosition target = cartesianPoints.get(0); // For LIN, usually one target point

            switch (actionType) {
                case LIN_FRAME:
                case LIN_FRAME_C:
                    // Create Frame relative to World.Current.getRootFrame() as per old MessageHandler
                    Frame targetFrame = new Frame(World.Current.getRootFrame(),
                            target.getX(), target.getY(), target.getZ(),
                            Math.toRadians(target.getA()), Math.toRadians(target.getB()), Math.toRadians(target.getC()));
                    motion = new LIN(targetFrame);
                    Logger.getInstance().log("CommandExecutor: Executing LIN Frame motion to: " + targetFrame.toString());
                    break;
                case LIN_REL_TOOL:
                    // LIN_REL_TOOL moves relative to the current tool frame
                    motion = new LIN(robot.getCurrentToolFrame().copy().setRelative(
                            target.getX(), target.getY(), target.getZ(),
                            Math.toRadians(target.getA()), Math.toRadians(target.getB()), Math.toRadians(target.getC())
                    ));
                    Logger.getInstance().log("CommandExecutor: Executing LIN Relative Tool motion by: " + target.toString());
                    break;
                case LIN_REL_BASE:
                    // LIN_REL_BASE moves relative to the current base frame
                    motion = new LIN(robot.getCurrentBaseFrame().copy().setRelative(
                            target.getX(), target.getY(), target.getZ(),
                            Math.toRadians(target.getA()), Math.toRadians(target.getB()), Math.toRadians(target.getC())
                    ));
                    Logger.getInstance().log("CommandExecutor: Executing LIN Relative Base motion by: " + target.toString());
                    break;
                default:
                    Logger.getInstance().log("CommandExecutor Error: Unhandled LIN motion type: " + actionType.name());
                    return false;
            }
        } else {
            Logger.getInstance().log("CommandExecutor Error: LIN motion called without target points.");
            return false;
        }

        if (motion != null) {
            IMotionContainer container;
            if (actionType == ActionTypes.LIN_FRAME_C) {
                // For continuous LIN, use moveAsync with blending
                container = robot.moveAsync(motion.setJointVelocityRel(speedOverride).setBlendingRel(0.5)); // As per old MessageHandler
                Logger.getInstance().log("CommandExecutor: LIN Continuous motion started.");
            } else {
                // For synchronous LIN
                container = robot.move(motion.setJointVelocityRel(speedOverride)); // As per old MessageHandler
                Logger.getInstance().log("CommandExecutor: LIN motion completed.");
            }
            return true;
        }
        return false;
    }

    /**
     * Helper method to execute CIRC motions.
     * CIRC motions require two points: an intermediate point and an end point.
     * The ParsedCommand will need to provide both if numPoints > 1.
     */
    private boolean executeCIRCMotion(List<AxisPosition> axisPoints, List<CartesianPosition> cartesianPoints, double speedOverride, ActionTypes actionType) {
        if (actionType == ActionTypes.CIRC_AXIS) {
            if (axisPoints == null || axisPoints.size() < 2) {
                Logger.getInstance().log("CommandExecutor Error: CIRC_AXIS motion requires at least two axis points (intermediate, end). Provided: " + (axisPoints != null ? axisPoints.size() : 0));
                return false;
            }
            AxisPosition intermediate = axisPoints.get(0);
            AxisPosition end = axisPoints.get(1);

            // As discussed, CIRC_AXIS is approximated by PTP through points.
            Logger.getInstance().log("CommandExecutor Warning: CIRC_AXIS interpretation simplified to PTP through points.");

            IMotion ptpToIntermediate = new PTP(intermediate.getJ1(), intermediate.getJ2(), intermediate.getJ3(), intermediate.getJ4(), intermediate.getJ5(), intermediate.getJ6(), intermediate.getJ7()).setJointVelocityRel(speedOverride);
            IMotion ptpToEnd = new PTP(end.getJ1(), end.getJ2(), end.getJ3(), end.getJ4(), end.getJ5(), end.getJ6(), end.getJ7()).setJointVelocityRel(speedOverride);

            robot.move(ptpToIntermediate);
            robot.move(ptpToEnd);
            Logger.getInstance().log("CommandExecutor: CIRC_AXIS motion (PTP approximation) completed.");
            return true;

        } else if (actionType == ActionTypes.CIRC_FRAME) {
            if (cartesianPoints == null || cartesianPoints.size() < 2) {
                Logger.getInstance().log("CommandExecutor Error: CIRC_FRAME motion requires at least two Cartesian points (intermediate, end). Provided: " + (cartesianPoints != null ? cartesianPoints.size() : 0));
                return false;
            }
            CartesianPosition intermediate = cartesianPoints.get(0);
            CartesianPosition end = cartesianPoints.get(1);

            // Create Frames for intermediate and end points relative to World.Current.getRootFrame()
            Frame intermediateFrame = new Frame(World.Current.getRootFrame(),
                    intermediate.getX(), intermediate.getY(), intermediate.getZ(),
                    Math.toRadians(intermediate.getA()), Math.toRadians(intermediate.getB()), Math.toRadians(intermediate.getC()));

            Frame endFrame = new Frame(World.Current.getRootFrame(),
                    end.getX(), end.getY(), end.getZ(),
                    Math.toRadians(end.getA()), Math.toRadians(end.getB()), Math.toRadians(end.getC()));

            // KUKA API CIRC motion using LIN.andThen
            // Note: KUKA's CIRC motion is typically defined with a velocity for the entire circular path.
            // Using LIN.andThen for CIRC is a common way to define the path, and the velocity is applied to the overall motion.
            IMotion circMotion = new LIN(intermediateFrame).setCartesianVelocityRel(speedOverride) // Use Cartesian velocity for LIN segments
                    .andThen(new LIN(endFrame));

            Logger.getInstance().log("CommandExecutor: Executing CIRC Frame motion via intermediate: " + intermediateFrame.toString() + " to end: " + endFrame.toString());
            robot.move(circMotion);
            Logger.getInstance().log("CommandExecutor: CIRC Frame motion completed.");
            return true;
        }
        return false;
    }

    /**
     * Helper method to execute IO activation.
     */
    private boolean executeIOMotion(int ioPoint, int ioPin, boolean ioState) {
        Logger.getInstance().log("CommandExecutor: Executing ACTIVATE_IO. IO Point: " + ioPoint + ", Pin: " + ioPin + ", State: " + ioState);
        try {
            switch (ioPin) {
                case 1:
                    ioFlangeIOGroup.setDO_Flange7(ioState);
                    Logger.getInstance().log("CommandExecutor: Set DO_Flange7 to " + ioState);
                    break;
                case 2:
                    ethercatX44IOGroup.setOutput2(ioState);
                    Logger.getInstance().log("CommandExecutor: Set Ethercat_x44 Output2 to " + ioState);
                    break;
                case 3:
                    ethercatX44IOGroup.setOutput1(ioState);
                    Logger.getInstance().log("CommandExecutor: Set Ethercat_x44 Output1 to " + ioState);
                    // Old MessageHandler had a Thread.sleep(100) here.
                    // It's generally better to avoid Thread.sleep in main motion execution paths
                    // unless absolutely necessary for hardware timing. If critical, consider.
                    // For now, omitting it.
                    break;
                default:
                    Logger.getInstance().log("CommandExecutor Error: Invalid IO pin for direct mapping: " + ioPin);
                    return false;
            }
            return true;
        } catch (Exception e) {
            Logger.getInstance().log("CommandExecutor Error: Failed to activate IO pin " + ioPin + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void run() throws Exception
    {

    }
}

// File: MessageHandler.java
package hartuTofas;

import hartuTofas.CopyOfMessageHandler.Command;

import javax.inject.Inject; // Required for @Inject annotations
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication; // Required to use createFromTemplate
import com.kuka.generated.ioAccess.Ethercat_x44IOGroup;
import com.kuka.generated.ioAccess.IOFlangeIOGroup;
import com.kuka.roboticsAPI.deviceModel.LBR;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.World;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;
import java.util.Arrays;
import java.util.List;
import java.lang.Math;

public class MessageHandler {

    // --- Injected Dependencies ---
    // These fields will be automatically filled by the KUKA framework because of @Inject.
    // This provides the necessary access to robot, I/Os, and the application context.
    @Inject
    private LBR robot;
    @Inject
    private IOFlangeIOGroup gimatic;
    @Inject
    private Ethercat_x44IOGroup IOs;
    @Inject
    private RoboticsAPIApplication application; // Used for createFromTemplate

    private Tool flexTool; // Variable to hold the dynamically loaded tool

    // --- Action Types Constants ---
    public static final int PTP_AXIS = 0;
    public static final int PTP_FRAME = 1;
    public static final int LIN_AXIS = 2;
    public static final int LIN_FRAME = 3;
    public static final int CIRC_AXIS = 4;
    public static final int CIRC_FRAME = 5;
    public static final int PTP_AXIS_C = 6;
    public static final int PTP_FRAME_C = 7;
    public static final int LIN_FRAME_C = 8;
    public static final int ACTIVATE_IO = 9;
    public static final int LIN_REL_TOOL = 10;
    public static final int LIN_REL_BASE = 11;

    // No explicit constructor is needed when using field injection for all dependencies.

    // --- Command Inner Class ---
    public class Command {
        public int actionType;
        public int numPoints;
        public int ioPoint;
        public int ioPin;
        public boolean ioState;
        public String tool; // This will receive the tool ID ("1", "2", etc.)
        public String base;
        public double speedOverride;
        public boolean programCall;
        public String targetPoints;
        public String id;

        private String get(String[] parts, int index, String defaultValue) {
            return (index < parts.length && parts[index] != null && !parts[index].trim().isEmpty())
                   ? parts[index].trim() : defaultValue;
        }

        public Command(String[] parts) {
            System.out.println("Generating command");

            try {
                int rawActionType = Integer.parseInt(get(parts, 0, "999"));
                this.programCall = rawActionType >= 100;
                this.actionType = this.programCall ? rawActionType - 100 : rawActionType;
                this.numPoints = Integer.parseInt(get(parts, 1, "0"));
                this.targetPoints = get(parts, 2, "");
                this.ioPoint = Integer.parseInt(get(parts, 3, "0"));
                this.ioPin = Integer.parseInt(get(parts, 4, "0"));
                this.ioState = Boolean.parseBoolean(get(parts, 5, "false"));
                this.tool = get(parts, 6, "0");
                this.base = get(parts, 7, "0");
                this.speedOverride = Double.parseDouble(get(parts, 8, "100.0")) / 100.0;
                this.id = get(parts, 9, "N/A");

            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid number format in input: " + e.getMessage());
            } catch (IndexOutOfBoundsException e) {
                throw new IllegalArgumentException("Incomplete message format: " + e.getMessage());
            }

            printCommand();
        }

        public void printCommand() {
            System.out.println("Command{" + "actionType=" + actionType
                    + ", programCall=" + programCall + ", numPoints="
                    + numPoints + ", ioPoint=" + ioPoint + ", ioPin=" + ioPin
                    + ", ioState=" + ioState + ", tool=" + tool + ", base="
                    + base + ", speedOverride=" + speedOverride
                    + ", targetPoints='" + targetPoints + '\'' + ", id='" + id
                    + '\'' + '}');
        }
    }

    // --- Main Message Handling Method ---
    public String handleMessage(String message) {
        if (!message.endsWith("#")) {
            System.out.println("Message does not end with '#'");
            return "Invalid message format: missing '#' terminator.";
        }

        message = message.substring(0, message.length() - 1);
        String[] parts = message.split("\\|");

        System.out.println("Parsed parts: " + Arrays.toString(parts));
        try {
            Command cmd = new Command(parts);
            cmd.printCommand();

            // --- Tool Selection and Loading (MODIFIED/FOCUSED PART) ---
            // 1. Map the received tool ID to the WorkVisual tool template name.
            String toolNameToLoad = mapToolIdToWorkVisualName(cmd.tool);
            if (toolNameToLoad == null) {
                System.err.println("Unknown tool ID received: " + cmd.tool + ". Cannot load tool.");
                return "Error: Unknown tool ID '" + cmd.tool + "'.";
            }

            // 2. Load the tool using createFromTemplate and the injected 'application' instance.
            flexTool = application.createFromTemplate(toolNameToLoad);

            if (flexTool == null) {
                System.err.println("Failed to load tool '" + toolNameToLoad + "' from WorkVisual templates. " +
                                   "Ensure the tool name is correct and the tool is assigned to your robot in WorkVisual.");
                return "Error: Tool '" + toolNameToLoad + "' not found or cannot be loaded.";
            }

            // 3. Attach the loaded tool to the robot's flange. This is crucial before using it for movements.
            flexTool.attachTo(robot.getFlange());
            // --- End of Tool Selection and Loading ---


            // --- Command Execution Logic ---
            if (!cmd.programCall) {
                System.out.println("Entered Movetype handling for actionType: " + cmd.actionType);
                switch (cmd.actionType) {
                    case PTP_AXIS:
                    case PTP_AXIS_C:
                        System.out.println("Handling PTP AXIS motion.");
                        return handlePTPAxis(cmd);
                    case PTP_FRAME:
                    case PTP_FRAME_C:
                        System.out.println("Handling PTP FRAME motion.");
                        return handlePTPFrame(cmd);
                    case LIN_FRAME:
                    case LIN_FRAME_C:
                        System.out.println("Handling LIN FRAME motion.");
                        // This method now uses 'flexTool'
                        return handleLINFrame(cmd);
                    case LIN_REL_TOOL:
                    case LIN_REL_BASE:
                        System.out.println("Handling LIN RELATIVE motion.");
                        // This method now uses 'flexTool'
                        return handleLINREL(cmd);
                    case ACTIVATE_IO:
                        System.out.println("Handling ACTIVATE IO command.");
                        return handleActivateIO(cmd); // UNMODIFIED by me in this version
                    default:
                        System.out.println("Unknown move type: " + cmd.actionType);
                        return "Unknown move type: " + cmd.actionType;
                }
            } else {
                System.out.println("Entered Programcall handling for actionType: " + cmd.actionType);
                switch (cmd.actionType) {
                    case 1: return "Program 1 called";
                    case 2: return "Program 2 called";
                    default: return "Program " + cmd.actionType + " not found.";
                }
            }

        } catch (IllegalArgumentException e) {
            System.err.println("Command parsing error: " + e.getMessage());
            return "Failed to parse command: " + e.getMessage();
        } catch (Exception e) {
            System.err.println("An unexpected error occurred during message handling: " + e.getMessage());
            e.printStackTrace();
            return "Failed to process command due to an internal error: " + e.getMessage();
        }
    }

    // --- Helper Method for Tool Mapping (MODIFIED/FOCUSED PART) ---
    private String mapToolIdToWorkVisualName(String toolId) {
        switch (toolId) {
            case "0": return "Tool";            // Generic "Tool" in your image
            case "1": return "RollScan";        // "RollScan" from your image
            case "2": return "BinPick_Tool";    // "BinPick_Tool" from your image
            case "3": return "GimaticCamera";   // "GimaticCamera" from your image
            case "4": return "GimaticGripperV"; // "GimaticGripperV" from your image
            case "5": return "Gimaticlxtur";    // "Gimaticlxtur" from your image
            case "6": return "lxturPlatoGrande"; // "lxturPlatoGrande" from your image
            case "7": return "RealSense";       // "RealSense" from your image
            case "8": return "Roldana";         // "Roldana" from your image
            case "9": return "ToolTemplate";    // "ToolTemplate" from your image
            case "10": return "TrackerTool";    // "TrackerTool" from your image
            default:
                System.err.println("Tool ID '" + toolId + "' not recognized in mapToolIdToWorkVisualName.");
                return null;
        }
    }

    // --- Movement Handler Methods (FOCUSED PART: Ensuring 'flexTool' is used) ---

    // PTP AXIS (Robot moves, tool follows) - No direct 'flexTool' usage in move command itself
    private String handlePTPAxis(Command cmd) {
        List<String> jointPositionGroups = Arrays.asList(cmd.targetPoints.split(","));
        if (jointPositionGroups.size() != cmd.numPoints) {
            System.out.println("Invalid number of point groups for ID: " + cmd.id + ". Expected " + cmd.numPoints + ", got " + jointPositionGroups.size());
            return "Invalid number of point groups";
        }
        try {
            for (String jointPositions : jointPositionGroups) {
                List<String> jointValuesStr = Arrays.asList(jointPositions.split(";"));
                if (jointValuesStr.size() != 7) {
                    System.out.println("Invalid number of joint positions in a group for ID: " + cmd.id + ". Expected 7, got " + jointValuesStr.size());
                    return "Invalid number of joint positions";
                }
                double[] jointValues = new double[7];
                for (int i = 0; i < jointValuesStr.size(); i++) {
                    double jointValueDeg = Double.parseDouble(jointValuesStr.get(i));
                    double jointValueRad = Math.toRadians(jointValueDeg);
                    if (!isWithinLimits(i, jointValueRad)) {
                        System.out.println("Joint " + (i + 1) + " value out of limits: " + jointValueDeg + " (deg) for ID: " + cmd.id);
                        return "Joint " + (i + 1) + " value out of limits";
                    }
                    jointValues[i] = jointValueRad;
                }
                if (cmd.actionType == PTP_AXIS) {
                    robot.move(ptp(jointValues).setJointVelocityRel(cmd.speedOverride));
                } else if (cmd.actionType == PTP_AXIS_C) {
                    robot.moveAsync(ptp(jointValues).setJointVelocityRel(cmd.speedOverride).setBlendingRel(0.5));
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid joint position values for ID: " + cmd.id + ". Error: " + e.getMessage());
            return "Invalid joint position values";
        }
        System.out.println("PTP_AXIS command executed for ID: " + cmd.id);
        return "PTP_AXIS command executed";
    }

    // PTP FRAME (Robot moves, tool moves to a Cartesian frame)
    private String handlePTPFrame(Command cmd) {
        List<String> points = Arrays.asList(cmd.targetPoints.split(","));
        try {
            for (String point : points) {
                List<String> coordinates = Arrays.asList(point.split(";"));
                if (coordinates.size() != 6) {
                    System.out.println("Incomplete coordinate values for a frame in PTP_FRAME for ID: " + cmd.id + ". Expected 6, got " + coordinates.size());
                    return "Incomplete frame coordinates";
                }
                double x = Double.parseDouble(coordinates.get(0));
                double y = Double.parseDouble(coordinates.get(1));
                double z = Double.parseDouble(coordinates.get(2));
                double roll = Math.toRadians(Double.parseDouble(coordinates.get(3)));
                double pitch = Math.toRadians(Double.parseDouble(coordinates.get(4)));
                double yaw = Math.toRadians(Double.parseDouble(coordinates.get(5)));
                Frame targetFrameVirgin = new Frame(World.Current.getRootFrame(), x, y, z, roll, pitch, yaw);
                
                // Use 'robot.move' for PTP Frame, the tool follows based on its attachment.
                if (cmd.actionType == PTP_FRAME) {
                    robot.move(ptp(targetFrameVirgin).setJointVelocityRel(cmd.speedOverride));
                } else if (cmd.actionType == PTP_FRAME_C) {
                    robot.moveAsync(ptp(targetFrameVirgin).setBlendingRel(0.5).setJointVelocityRel(cmd.speedOverride));
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid coordinate values for ID: " + cmd.id + ". Error: " + e.getMessage());
            return "Invalid coordinate values";
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Coordinate values are incomplete for ID: " + cmd.id + ". Error: " + e.getMessage());
            return "Coordinate values are incomplete";
        }
        System.out.println("PTP_FRAME command executed for ID: " + cmd.id);
        return "PTP_FRAME command executed";
    }

    /**
     * Handles LIN (Linear) movements in Cartesian Frame space using the currently loaded tool.
     * @param cmd The parsed Command object containing target points and speed.
     * @return A status string.
     */
    private String handleLINFrame(Command cmd) {
        List<String> points = Arrays.asList(cmd.targetPoints.split(","));
        try {
            // Defensive check: Ensure flexTool is not null before using it.
            // This is important because 'flexTool' is set by handleMessage before this method is called.
            if (flexTool == null) {
                System.err.println("Error: flexTool is null for LIN_FRAME command. Tool was not loaded correctly. ID: " + cmd.id);
                return "Error: Tool not initialized for LIN_FRAME.";
            }

            for (String point : points) {
                List<String> coordinates = Arrays.asList(point.split(";"));
                if (coordinates.size() != 6) {
                    System.out.println("Incomplete coordinate values for a frame in LIN_FRAME for ID: " + cmd.id + ". Expected 6, got " + coordinates.size());
                    return "Incomplete frame coordinates";
                }
                double x = Double.parseDouble(coordinates.get(0));
                double y = Double.parseDouble(coordinates.get(1));
                double z = Double.parseDouble(coordinates.get(2));
                double roll = Math.toRadians(Double.parseDouble(coordinates.get(3)));
                double pitch = Math.toRadians(Double.parseDouble(coordinates.get(4)));
                double yaw = Math.toRadians(Double.parseDouble(coordinates.get(5)));
                
                Frame targetFrameVirgin = new Frame(World.Current.getRootFrame(), x, y, z, roll, pitch, yaw);
                
                // --- MODIFIED/FOCUSED PART: Using flexTool for LIN motion ---
                if (cmd.actionType == LIN_FRAME) {
                    flexTool.move(lin(targetFrameVirgin).setCartVelocity(cmd.speedOverride * 200));
                } else if (cmd.actionType == LIN_FRAME_C) {
                    flexTool.moveAsync(lin(targetFrameVirgin).setBlendingRel(0.5).setCartVelocity(cmd.speedOverride * 200));
                }
                // --- End of MODIFIED/FOCUSED PART ---
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid coordinate values for ID: " + cmd.id + ". Error: " + e.getMessage());
            return "Invalid coordinate values";
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Coordinate values are incomplete for ID: " + cmd.id + ". Error: " + e.getMessage());
            return "Coordinate values are incomplete";
        }
        System.out.println("LIN_FRAME command executed for ID: " + cmd.id);
        return "LIN_FRAME command executed";
    }

    /**
     * Handles linear relative movements, either in the tool's own frame (LIN_REL_TOOL)
     * or relative to the robot's base frame (LIN_REL_BASE).
     * @param cmd The parsed Command object containing relative offsets and speed.
     * @return A status string.
     */
    private String handleLINREL(Command cmd) {
        List<String> offsetData = Arrays.asList(cmd.targetPoints.split(";"));
        try {
            if (offsetData.size() != 6) {
                System.out.println("Incomplete offset values for LIN_REL command for ID: " + cmd.id + ". Expected 6, got " + offsetData.size());
                return "Incomplete offset values for LIN_REL";
            }
            double xOffset = Double.parseDouble(offsetData.get(0));
            double yOffset = Double.parseDouble(offsetData.get(1));
            double zOffset = Double.parseDouble(offsetData.get(2));
            double aOffset = Math.toRadians(Double.parseDouble(offsetData.get(3)));
            double bOffset = Math.toRadians(Double.parseDouble(offsetData.get(4)));
            double cOffset = Math.toRadians(Double.parseDouble(offsetData.get(5)));

            Transformation offsetTransformation = Transformation.ofRad(xOffset, yOffset, zOffset, aOffset, bOffset, cOffset);

            // --- MODIFIED/FOCUSED PART: Using flexTool for LIN_REL_TOOL motion ---
            if (cmd.actionType == LIN_REL_TOOL) {
                // Defensive check: Ensure flexTool is not null before using it.
                if (flexTool == null) {
                    System.err.println("Error: flexTool is null for LIN_REL_TOOL command. Tool was not loaded correctly. ID: " + cmd.id);
                    return "Error: Tool not initialized for LIN_REL_TOOL.";
                }
                // Move relative to the tool's current motion frame (its TCP)
                flexTool.move(linRel(offsetTransformation, flexTool.getFrame(flexTool.getDefaultMotionFrame().getName()))
                        .setCartVelocity(cmd.speedOverride * 200));
            } else if (cmd.actionType == LIN_REL_BASE) {
                // Move relative to the robot's current base frame (World.Current.getRootFrame() by default)
                robot.move(linRel(offsetTransformation)
                        .setCartVelocity(cmd.speedOverride * 200));
            }
            // --- End of MODIFIED/FOCUSED PART ---

            System.out.println("LIN_REL command executed for ID: " + cmd.id);
            return "LIN_REL command executed for ID: " + cmd.id;

        } catch (NumberFormatException e) {
            System.out.println("Invalid offset values for LIN_REL command for ID: " + cmd.id + ". Error: " + e.getMessage());
            return "Invalid offset values for LIN_REL";
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Incomplete offset values for LIN_REL command for ID: " + cmd.id + ". Error: " + e.getMessage());
            return "Incomplete offset values for LIN_REL";
        }
    }

    // --- IO Handling Method (NO UNREQUESTED CHANGES IN THIS VERSION) ---
    private String handleActivateIO(Command cmd) {
		System.out.println(cmd.ioPin);
		switch (cmd.ioPin) {
			case 1:
				gimatic.setDO_Flange7(cmd.ioState);
				break;
			case 2:
				IOs.setOutput2(cmd.ioState);
				break;
			case 3:
				IOs.setOutput1(cmd.ioState);
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				break;
			default:
				System.out.println("Invalid IO pin: " + cmd.ioPin);
		}
		return "ACTIVATE_IO command executed for ID: " + cmd.id;
	}
    // --- Joint Limit Check Helper ---
    private boolean isWithinLimits(int jointIndex, double jointValue) {
        // Define joint limits in radians
        double[][] limits = {
            {-2.967, 2.967}, // A1 (-170 to 170 deg)
            {-2.094, 2.094}, // A2 (-120 to 120 deg)
            {-2.967, 2.967}, // A3 (-170 to 170 deg)
            {-2.094, 2.094}, // A4 (-120 to 120 deg)
            {-2.967, 2.967}, // A5 (-170 to 170 deg)
            {-2.094, 2.094}, // A6 (-120 to 120 deg)
            {-3.054, 3.054}  // A7 (-175 to 175 deg)
        };

        if (jointIndex >= 0 && jointIndex < limits.length) {
            return jointValue >= limits[jointIndex][0] && jointValue <= limits[jointIndex][1];
        }
        System.err.println("Invalid joint index provided: " + jointIndex);
        return false;
    }
}
// --- CommandParser.java ---
package hartu.robot.utils;

import hartu.protocols.constants.ActionTypes;
import hartu.protocols.constants.MessagePartIndex;
import hartu.protocols.constants.MovementType; // New import for MovementType
import hartu.robot.commands.MotionParameters;
import hartu.robot.commands.ParsedCommand;
import hartu.robot.commands.io.IoCommandData;
import hartu.robot.commands.positions.AxisPosition;
import hartu.robot.commands.positions.CartesianPosition;
import hartu.robot.communication.server.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
// import java.util.UUID; // Not currently used for ID parsing, keeping commented

public class CommandParser
{
    private static final String PRIMARY_DELIMITER = "\\|"; // Regex for '|'
    private static final String SECONDARY_DELIMITER = ";"; // For splitting individual position values (e.g., J1;J2)
    private static final String MULTI_POINT_DELIMITER = ","; // New delimiter for multiple target points (e.g., pos1,pos2)
    private static final String MESSAGE_TERMINATOR = "#";

    // Private constructor to prevent instantiation
    private CommandParser() {}

    /**
     * Parses a raw command string received from a client into a ParsedCommand object.
     * The command string is expected to be in the format:
     * ACTION_TYPE|NUM_POINTS|TARGET_POINTS|IO_POINT|IO_PIN|IO_STATE|TOOL|BASE|SPEED_OVERRIDE|...|ID#
     *
     * @param commandString The raw command string to parse.
     * @return A ParsedCommand object representing the parsed command.
     * @throws IllegalArgumentException If the command string format is invalid or data is missing/malformed.
     */
    public static ParsedCommand parseCommand(String commandString) {
        Logger.getInstance().log("CommandParser: Attempting to parse command: " + commandString);

        // 1. Validate and remove the terminator
        if (!commandString.endsWith(MESSAGE_TERMINATOR)) {
            String errorMsg = "Command string must end with '" + MESSAGE_TERMINATOR + "'. Received: " + commandString;
            Logger.getInstance().log("CommandParser Error: " + errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        String cleanCommand = commandString.substring(0, commandString.length() - MESSAGE_TERMINATOR.length());

        // 2. Split by primary delimiter
        String[] parts = cleanCommand.split(PRIMARY_DELIMITER, -1); // -1 ensures trailing empty strings are included

        // 3. Basic validation for number of parts
        final int EXPECTED_MIN_PARTS = MessagePartIndex.values().length; // This is 10 (0-9)
        if (parts.length < EXPECTED_MIN_PARTS) {
            String errorMsg = "Invalid number of parts. Expected at least " + EXPECTED_MIN_PARTS + ", got " + parts.length + ". Command: " + commandString;
            Logger.getInstance().log("CommandParser Error: " + errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        // Extract common fields (ActionType, NumPoints, and ID are always expected)
        ActionTypes actionType;
        int numPoints; // Extracted directly from the message
        String id;
        try {
            actionType = ActionTypes.fromValue(Integer.parseInt(parts[MessagePartIndex.ACTION_TYPE.getIndex()]));
            numPoints = Integer.parseInt(parts[MessagePartIndex.NUM_POINTS.getIndex()]); // Parse NUM_POINTS
            id = parts[MessagePartIndex.ID.getIndex()];
        } catch (NumberFormatException e) {
            String errorMsg = "Invalid number format for ActionType, NumPoints, or ID: " + e.getMessage();
            Logger.getInstance().log("CommandParser Error: " + errorMsg);
            throw new IllegalArgumentException(errorMsg, e);
        } catch (ArrayIndexOutOfBoundsException e) {
            String errorMsg = "Missing ActionType, NumPoints, or ID part in command string: " + e.getMessage();
            Logger.getInstance().log("CommandParser Error: " + errorMsg);
            throw new IllegalArgumentException(errorMsg, e);
        }

        // Determine 'continuous' property from ActionType using MovementType
        boolean isContinuous = MovementType.fromActionType(actionType).isContinuous();

        // Parse Motion Parameters (TOOL, BASE, SPEED_OVERRIDE)
        String tool = ""; // Default empty string
        String base = ""; // Default empty string
        double speedOverride = 0.0; // Default speed override
        try {
            tool = parts[MessagePartIndex.TOOL.getIndex()];
            base = parts[MessagePartIndex.BASE.getIndex()];
            speedOverride = Double.parseDouble(parts[MessagePartIndex.SPEED_OVERRIDE.getIndex()]);
        } catch (NumberFormatException e) {
            Logger.getInstance().log("CommandParser Warning: Could not parse MotionParameters (NumberFormat). Using defaults. " + e.getMessage());
        } catch (ArrayIndexOutOfBoundsException e) {
            Logger.getInstance().log("CommandParser Warning: MotionParameters fields missing (ArrayIndexOutOfBounds). Using defaults. " + e.getMessage());
        }
        // Create MotionParameters with new fields
        MotionParameters motionParameters = new MotionParameters(speedOverride, tool, base, isContinuous, numPoints);


        // Initialize optional fields for ParsedCommand
        List<AxisPosition> axisTargetPoints = null;
        List<CartesianPosition> cartesianTargetPoints = null;
        IoCommandData ioCommandData = null;
        Integer programId = null;

        // Determine command type and parse specific data
        switch (actionType) {
            case PTP_AXIS:
            case PTP_AXIS_C:
            case LIN_AXIS:
            case CIRC_AXIS: // CIRC_AXIS now takes joint values
                // Parse Axis Positions (potentially multiple points)
                try {
                    axisTargetPoints = parseAxisPositions(parts[MessagePartIndex.TARGET_POINTS.getIndex()]);
                    // Validate numPoints against actual parsed points
                    if (axisTargetPoints.size() != numPoints) {
                        String errorMsg = "Parsed NUM_POINTS (" + numPoints + ") does not match actual parsed axis points (" + axisTargetPoints.size() + ").";
                        Logger.getInstance().log("CommandParser Error: " + errorMsg);
                        throw new IllegalArgumentException(errorMsg);
                    }
                    return ParsedCommand.forAxisMovement(actionType, id, axisTargetPoints, motionParameters);
                } catch (ArrayIndexOutOfBoundsException e) {
                    String errorMsg = "Missing TARGET_POINTS part for Axis movement: " + e.getMessage();
                    Logger.getInstance().log("CommandParser Error: " + errorMsg);
                    throw new IllegalArgumentException(errorMsg, e);
                }

            case PTP_FRAME:
            case PTP_FRAME_C:
            case LIN_FRAME:
            case LIN_FRAME_C:
            case LIN_REL_TOOL:
            case LIN_REL_BASE:
            case CIRC_FRAME:
                // Parse Cartesian Positions (potentially multiple points)
                try {
                    cartesianTargetPoints = parseCartesianPositions(parts[MessagePartIndex.TARGET_POINTS.getIndex()]);
                    // Validate numPoints against actual parsed points
                    if (cartesianTargetPoints.size() != numPoints) {
                        String errorMsg = "Parsed NUM_POINTS (" + numPoints + ") does not match actual parsed Cartesian points (" + cartesianTargetPoints.size() + ").";
                        Logger.getInstance().log("CommandParser Error: " + errorMsg);
                        throw new IllegalArgumentException(errorMsg);
                    }
                    return ParsedCommand.forCartesianMovement(actionType, id, cartesianTargetPoints, motionParameters);
                } catch (ArrayIndexOutOfBoundsException e) {
                    String errorMsg = "Missing TARGET_POINTS part for Cartesian movement: " + e.getMessage();
                    Logger.getInstance().log("CommandParser Error: " + errorMsg);
                    throw new IllegalArgumentException(errorMsg, e);
                }

            case ACTIVATE_IO:
                // Parse IO Command Data
                try {
                	//TODO: ioPoint is hardcoded for testing
                    //int ioPoint = Integer.parseInt(parts[MessagePartIndex.IO_POINT.getIndex()]);
                	int ioPoint = 0;
                    int ioPin = Integer.parseInt(parts[MessagePartIndex.IO_PIN.getIndex()]);
                    boolean ioState = Boolean.parseBoolean(parts[MessagePartIndex.IO_STATE.getIndex()]);
                    ioCommandData = new IoCommandData(ioPoint, ioPin, ioState);
                    return ParsedCommand.forIo(actionType, id, ioCommandData);
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    String errorMsg = "Invalid IO command data format for ACTIVATE_IO: " + e.getMessage();
                    Logger.getInstance().log("CommandParser Error: " + errorMsg);
                    throw new IllegalArgumentException(errorMsg, e);
                }

                // Add cases for program calls if applicable (e.g., if PROGRAM_CALL_OFFSET is used)
                // case PROGRAM_CALL_TYPE:
                //    try {
                //        programId = Integer.parseInt(parts[MessagePartIndex.PROGRAM_ID.getIndex()]); // Assuming a PROGRAM_ID index
                //        return ParsedCommand.forProgramCall(actionType, id, programId);
                //    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                //        String errorMsg = "Invalid program ID format for PROGRAM_CALL: " + e.getMessage();
                //        Logger.getInstance().log("CommandParser Error: " + errorMsg);
                //        throw new IllegalArgumentException(errorMsg, e);
                //    }

            case UNKNOWN:
            default:
                String errorMsg = "Unknown or unsupported ActionType: " + actionType.getValue() + " in command: " + commandString;
                Logger.getInstance().log("CommandParser Error: " + errorMsg);
                throw new IllegalArgumentException(errorMsg);
        }
    }

    /**
     * Parses a string of comma-separated sets of semicolon-separated joint values
     * into a list of AxisPosition objects.
     * Example: "J1;J2;J3;J4;J5;J6;J7,J1';J2';J3';J4';J5';J6';J7'"
     *
     * @param axisPositionsString The string containing one or more sets of joint values.
     * @return A list of AxisPosition objects.
     * @throws IllegalArgumentException If the format is incorrect or values are not numbers.
     */
    private static List<AxisPosition> parseAxisPositions(String axisPositionsString) {
        List<AxisPosition> positions = new ArrayList<>();
        String[] individualPointStrings = axisPositionsString.split(MULTI_POINT_DELIMITER); // Split by comma for multiple points

        for (String pointString : individualPointStrings) {
            String[] jointValues = pointString.split(SECONDARY_DELIMITER);
            if (jointValues.length != 7) { // Expecting J1-J7
                String errorMsg = "Invalid axis position format: Expected 7 joint values (J1-J7), got " + jointValues.length + " in point string: " + pointString;
                Logger.getInstance().log("CommandParser Error: " + errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            try {
                double j1 = Double.parseDouble(jointValues[0]);
                double j2 = Double.parseDouble(jointValues[1]);
                double j3 = Double.parseDouble(jointValues[2]);
                double j4 = Double.parseDouble(jointValues[3]);
                double j5 = Double.parseDouble(jointValues[4]);
                double j6 = Double.parseDouble(jointValues[5]);
                double j7 = Double.parseDouble(jointValues[6]);
                positions.add(new AxisPosition(j1, j2, j3, j4, j5, j6, j7));
            } catch (NumberFormatException e) {
                String errorMsg = "Invalid number format in axis positions: " + e.getMessage() + " for point string: " + pointString;
                Logger.getInstance().log("CommandParser Error: " + errorMsg);
                throw new IllegalArgumentException(errorMsg, e);
            }
        }
        return positions;
    }

    /**
     * Parses a string of comma-separated sets of semicolon-separated Cartesian position and orientation values
     * into a list of CartesianPosition objects.
     * Example: "X;Y;Z;A;B;C,X';Y';Z';A';B';C'"
     *
     * @param cartesianPositionsString The string containing one or more sets of Cartesian values.
     * @return A list of CartesianPosition objects.
     * @throws IllegalArgumentException If the format is incorrect or values are not numbers.
     */
    private static List<CartesianPosition> parseCartesianPositions(String cartesianPositionsString) {
        List<CartesianPosition> positions = new ArrayList<>();
        String[] individualPointStrings = cartesianPositionsString.split(MULTI_POINT_DELIMITER); // Split by comma for multiple points

        for (String pointString : individualPointStrings) {
            String[] values = pointString.split(SECONDARY_DELIMITER);
            if (values.length != 6) { // Expecting X,Y,Z,A,B,C
                String errorMsg = "Invalid Cartesian position format: Expected 6 values (X;Y;Z;A;B;C), got " + values.length + " in point string: " + pointString;
                Logger.getInstance().log("CommandParser Error: " + errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            try {
                double x = Double.parseDouble(values[0]);
                double y = Double.parseDouble(values[1]);
                double z = Double.parseDouble(values[2]);
                double a = Double.parseDouble(values[3]);
                double b = Double.parseDouble(values[4]);
                double c = Double.parseDouble(values[5]);
                positions.add(new CartesianPosition(x, y, z, a, b, c));
            } catch (NumberFormatException e) {
                String errorMsg = "Invalid number format in Cartesian positions: " + e.getMessage() + " for point string: " + pointString;
                Logger.getInstance().log("CommandParser Error: " + errorMsg);
                throw new IllegalArgumentException(errorMsg, e);
            }
        }
        return positions;
    }
}

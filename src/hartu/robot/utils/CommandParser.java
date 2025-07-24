package hartu.robot.utils;

import com.kuka.roboticsAPI.deviceModel.JointEnum;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.geometricModel.Frame;
import hartu.protocols.constants.ActionTypes;
import hartu.protocols.constants.CommandCategory;
import hartu.protocols.constants.MessagePartIndex;
import hartu.protocols.constants.MovementType;
import hartu.robot.commands.MotionParameters;
import hartu.robot.commands.ParsedCommand;
import hartu.robot.commands.io.IoCommandData;
import hartu.robot.communication.server.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static hartu.protocols.constants.ProtocolConstants.MESSAGE_TERMINATOR;
import static hartu.protocols.constants.ProtocolConstants.MULTI_POINT_DELIMITER;
import static hartu.protocols.constants.ProtocolConstants.PRIMARY_DELIMITER;
import static hartu.protocols.constants.ProtocolConstants.SECONDARY_DELIMITER;

public class CommandParser {

    private CommandParser() {
        // Private constructor
    }

    public static ParsedCommand parseCommand(String commandString) {
        Logger.getInstance().log("PARSER", "Attempting to parse command: " + commandString);

        if (!commandString.endsWith(MESSAGE_TERMINATOR)) {
            String errorMsg = "Command string must end with '" + MESSAGE_TERMINATOR + "'. Received: " + commandString;
            Logger.getInstance().log("PARSER", "Error: " + errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        String cleanCommand = commandString.substring(0, commandString.length() - MESSAGE_TERMINATOR.length());
        String[] parts = cleanCommand.split(PRIMARY_DELIMITER, -1);

        final int EXPECTED_MIN_PARTS = MessagePartIndex.ID.getIndex() + 1;
        if (parts.length < EXPECTED_MIN_PARTS) {
            String errorMsg = "Invalid number of parts. Expected at least " + EXPECTED_MIN_PARTS + ", got " + parts.length + ". Command: " + commandString;
            Logger.getInstance().log("PARSER", "Error: " + errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        ActionTypes actionType;
        int numPoints;
        String id;
        try {
            actionType = ActionTypes.fromValue(Integer.parseInt(parts[MessagePartIndex.ACTION_TYPE.getIndex()]));
            numPoints = Integer.parseInt(parts[MessagePartIndex.NUM_POINTS.getIndex()]);
            id = parts[MessagePartIndex.ID.getIndex()];
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            String errorMsg = "Invalid number format or missing required part: " + e.getMessage();
            Logger.getInstance().log("PARSER", "Error: " + errorMsg);
            throw new IllegalArgumentException(errorMsg, e);
        }

        boolean isContinuous = actionType.isContinuousMovement();

        String tool = "";
        String base = "";
        double speedOverride = 0.0;
        try {
            tool = parts[MessagePartIndex.TOOL.getIndex()];
            base = parts[MessagePartIndex.BASE.getIndex()];
            speedOverride = Double.parseDouble(parts[MessagePartIndex.SPEED_OVERRIDE.getIndex()]);
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            Logger.getInstance().warn("PARSER", "Warning: Basic MotionParameters fields (Tool, Base, SpeedOverride) missing or invalid. Using defaults. " + e.getMessage());
        }

        MotionParameters motionParameters = new MotionParameters(
                speedOverride, tool, base, isContinuous, numPoints,
                null, // jointVelocityRel
                null, // jointAccelerationRel
                null  // blendingRel
        );

        CommandCategory commandCategory = actionType.getCategory();
        List<JointPosition> jointTargetPoints = null;
        List<Frame> cartesianTargetPoints = null;
        IoCommandData ioCommandData = null;
        Integer programId = null;

        if (commandCategory == CommandCategory.MOVEMENT) {
            if (actionType.isJointMotion()) {
                try {
                    jointTargetPoints = parseAxisPositions(parts[MessagePartIndex.TARGET_POINTS.getIndex()]);
                    if (jointTargetPoints.size() != numPoints) {
                        Logger.getInstance().warn("PARSER", "Warning: Parsed NUM_POINTS (" + numPoints + ") does not match actual parsed axis points (" + jointTargetPoints.size() + "). Command ID: " + id);
                    }
                    return ParsedCommand.forAxisMovement(actionType, id, jointTargetPoints, motionParameters);
                } catch (ArrayIndexOutOfBoundsException e) {
                    String errorMsg = "Missing TARGET_POINTS part for Axis movement: " + e.getMessage();
                    Logger.getInstance().log("PARSER", "Error: " + errorMsg);
                    throw new IllegalArgumentException(errorMsg, e);
                }
            } else { // Cartesian Motion
                try {
                    cartesianTargetPoints = parseCartesianPositions(parts[MessagePartIndex.TARGET_POINTS.getIndex()]);
                    if (cartesianTargetPoints.size() != numPoints) {
                        Logger.getInstance().warn("PARSER", "Warning: Parsed NUM_POINTS (" + numPoints + ") does not match actual parsed Cartesian points (" + cartesianTargetPoints.size() + "). Command ID: " + id);
                    }
                    return ParsedCommand.forCartesianMovement(actionType, id, cartesianTargetPoints, motionParameters);
                } catch (ArrayIndexOutOfBoundsException e) {
                    String errorMsg = "Missing TARGET_POINTS part for Cartesian movement: " + e.getMessage();
                    Logger.getInstance().log("PARSER", "Error: " + errorMsg);
                    throw new IllegalArgumentException(errorMsg, e);
                }
            }
        } else if (commandCategory == CommandCategory.IO) {
            if (actionType == ActionTypes.ACTIVATE_IO) {
                try {
                    int ioPoint = Integer.parseInt(parts[MessagePartIndex.IO_POINT.getIndex()]);
                    int ioPin = Integer.parseInt(parts[MessagePartIndex.IO_PIN.getIndex()]);
                    boolean ioState = Boolean.parseBoolean(parts[MessagePartIndex.IO_STATE.getIndex()]);
                    ioCommandData = new IoCommandData(ioPoint, ioPin, ioState);
                    return ParsedCommand.forIo(actionType, id, ioCommandData);
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    String errorMsg = "Invalid IO command data format for ACTIVATE_IO: " + e.getMessage();
                    Logger.getInstance().log("PARSER", "Error: " + errorMsg);
                    throw new IllegalArgumentException(errorMsg, e);
                }
            } else {
                String errorMsg = "IO CommandCategory but unknown ActionType: " + actionType.getValue();
                Logger.getInstance().log("PARSER", "Error: " + errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }
        } else if (commandCategory == CommandCategory.PROGRAM_CALL) {
            try {
                programId = actionType.getValue() - ActionTypes.PROGRAM_CALL_OFFSET;
                return ParsedCommand.forProgramCall(actionType, id, programId);
            } catch (Exception e) {
                String errorMsg = "Invalid Program Call command data format: " + e.getMessage();
                Logger.getInstance().log("PARSER", "Error: " + errorMsg);
                throw new IllegalArgumentException(errorMsg, e);
            }
        } else {
            String errorMsg = "Unknown or unsupported CommandCategory: " + commandCategory + " for ActionType: " + actionType.getValue() + " in command: " + commandString;
            Logger.getInstance().log("PARSER", "Error: " + errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }

    private static List<JointPosition> parseAxisPositions(String axisPositionsString) {
        Logger.getInstance().log("PARSER", "Attempting to parse AxisPositions from string: " + axisPositionsString);
        List<JointPosition> positions = new ArrayList<>();
        String[] individualPointStrings = axisPositionsString.split(MULTI_POINT_DELIMITER);

        for (String pointString : individualPointStrings) {
            String[] jointValues = pointString.split(SECONDARY_DELIMITER);
            if (jointValues.length != 7) {
                String errorMsg = "Invalid axis position format: Expected 7 joint values (J1-J7), got " + jointValues.length + " in point string: " + pointString;
                Logger.getInstance().log("PARSER", "Error: " + errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            try {
                double j1 = Math.toRadians(Double.parseDouble(jointValues[0]));
                double j2 = Math.toRadians(Double.parseDouble(jointValues[1]));
                double j3 = Math.toRadians(Double.parseDouble(jointValues[2]));
                double j4 = Math.toRadians(Double.parseDouble(jointValues[3]));
                double j5 = Math.toRadians(Double.parseDouble(jointValues[4]));
                double j6 = Math.toRadians(Double.parseDouble(jointValues[5]));
                double j7 = Math.toRadians(Double.parseDouble(jointValues[6]));
                positions.add(new JointPosition(j1, j2, j3, j4, j5, j6, j7));
            } catch (NumberFormatException e) {
                String errorMsg = "Invalid number format in axis positions: " + e.getMessage() + " for point string: " + pointString;
                Logger.getInstance().log("PARSER", "Error: " + errorMsg);
                throw new IllegalArgumentException(errorMsg, e);
            }
        }
        Logger.getInstance().log("PARSER", "Parsed axis positions from string: " + positions.size() + " points.");
        return positions;
    }

    private static List<Frame> parseCartesianPositions(String cartesianPositionsString) {
        Logger.getInstance().log("PARSER", "Attempting to parse CartesianPositions from string: " + cartesianPositionsString);
        List<Frame> positions = new ArrayList<>();
        String[] individualPointStrings = cartesianPositionsString.split(MULTI_POINT_DELIMITER);

        for (String pointString : individualPointStrings) {
            String[] values = pointString.split(SECONDARY_DELIMITER);
            if (values.length != 6) {
                String errorMsg = "Invalid Cartesian position format: Expected 6 values (X;Y;Z;A;B;C), got " + values.length + " in point string: " + pointString;
                Logger.getInstance().log("PARSER", "Error: " + errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            try {
                double x = Double.parseDouble(values[0]);
                double y = Double.parseDouble(values[1]);
                double z = Double.parseDouble(values[2]);
                double a = Math.toRadians(Double.parseDouble(values[3]));
                double b = Math.toRadians(Double.parseDouble(values[4]));
                double c = Math.toRadians(Double.parseDouble(values[5]));
                positions.add(new Frame(x, y, z, a, b, c));
            } catch (NumberFormatException e) {
                String errorMsg = "Invalid number format in Cartesian positions: " + e.getMessage() + " for point string: " + pointString;
                Logger.getInstance().log("PARSER", "Error: " + errorMsg);
                throw new IllegalArgumentException(errorMsg, e);
            }
        }
        Logger.getInstance().log("PARSER", "Parsed Cartesian positions from string: " + positions.size() + " points.");
        return positions;
    }
}
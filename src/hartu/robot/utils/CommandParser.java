package hartu.robot.utils;

import hartu.protocols.constants.ActionTypes;
import hartu.protocols.constants.CommandCategory;
import hartu.protocols.constants.MessagePartIndex;
import hartu.protocols.constants.MovementType;
import hartu.robot.commands.MotionParameters;
import hartu.robot.commands.ParsedCommand;
import hartu.robot.commands.io.IoCommandData;
import hartu.robot.commands.positions.AxisPosition;
import hartu.robot.commands.positions.CartesianPosition;
import hartu.robot.communication.server.Logger;

import java.util.ArrayList;
import java.util.List;

import static hartu.protocols.constants.ProtocolConstants.*;

public class CommandParser
{

    private CommandParser()
    {
    }

    public static ParsedCommand parseCommand(String commandString)
    {
        Logger.getInstance().log("PARSER", "Attempting to parse command: " + commandString);

        if (!commandString.endsWith(MESSAGE_TERMINATOR))
        {
            String errorMsg = "Command string must end with '" + MESSAGE_TERMINATOR + "'. Received: " + commandString;
            Logger.getInstance().log("PARSER", "Error: " + errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        String cleanCommand = commandString.substring(0, commandString.length() - MESSAGE_TERMINATOR.length());

        String[] parts = cleanCommand.split(PRIMARY_DELIMITER, -1);

        final int EXPECTED_MIN_PARTS = MessagePartIndex.values().length;
        if (parts.length < EXPECTED_MIN_PARTS)
        {
            String errorMsg = "Invalid number of parts. Expected at least " + EXPECTED_MIN_PARTS + ", got " + parts.length + ". Command: " + commandString;
            Logger.getInstance().log("PARSER", "Error: " + errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        ActionTypes actionType;
        int numPoints;
        String id;
        try
        {
            actionType = ActionTypes.fromValue(Integer.parseInt(parts[MessagePartIndex.ACTION_TYPE.getIndex()]));
            numPoints = Integer.parseInt(parts[MessagePartIndex.NUM_POINTS.getIndex()]);
            id = parts[MessagePartIndex.ID.getIndex()];
        }
        catch (NumberFormatException e)
        {
            String errorMsg = "Invalid number format for ActionType, NumPoints, or ID: " + e.getMessage();
            Logger.getInstance().log("PARSER", "Error: " + errorMsg);
            throw new IllegalArgumentException(errorMsg, e);
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            String errorMsg = "Missing ActionType, NumPoints, or ID part in command string: " + e.getMessage();
            Logger.getInstance().log("PARSER", "Error: " + errorMsg);
            throw new IllegalArgumentException(errorMsg, e);
        }

        boolean isContinuous = MovementType.fromActionType(actionType).isContinuous();

        String tool = "";
        String base = "";
        double speedOverride = 0.0;
        try
        {
            tool = parts[MessagePartIndex.TOOL.getIndex()];
            base = parts[MessagePartIndex.BASE.getIndex()];
            speedOverride = Double.parseDouble(parts[MessagePartIndex.SPEED_OVERRIDE.getIndex()]);
        }
        catch (NumberFormatException e)
        {
            Logger.getInstance().log(
                    "PARSER",
                    "Warning: Could not parse MotionParameters (NumberFormat). Using defaults. " + e.getMessage()
                                    );
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            Logger.getInstance().log(
                    "PARSER",
                    "Warning: MotionParameters fields missing (ArrayIndexOutOfBounds). Using defaults. " + e.getMessage()
                                    );
        }
        MotionParameters motionParameters = new MotionParameters(speedOverride, tool, base, isContinuous, numPoints);

        List<AxisPosition> axisTargetPoints;
        List<CartesianPosition> cartesianTargetPoints;
        IoCommandData ioCommandData;
        Integer programId;

        // Determine CommandCategory and construct ParsedCommand
        CommandCategory commandCategory;

        // Corrected: Use MovementType to check if it's a movement action
        if (MovementType.fromActionType(actionType) != MovementType.UNKNOWN)
        {
            commandCategory = CommandCategory.MOVEMENT;
            if (MovementType.fromActionType(actionType).isAxisMotion())
            {
                try
                {
                    axisTargetPoints = parseAxisPositions(parts[MessagePartIndex.TARGET_POINTS.getIndex()]);
                    if (axisTargetPoints.size() != numPoints)
                    {
                        String errorMsg = "Parsed NUM_POINTS (" + numPoints + ") does not match actual parsed axis points (" + axisTargetPoints.size() + ").";
                        Logger.getInstance().log("PARSER", "Error: " + errorMsg);
                        throw new IllegalArgumentException(errorMsg);
                    }
                    return ParsedCommand.forAxisMovement(actionType, id, axisTargetPoints, motionParameters);
                }
                catch (ArrayIndexOutOfBoundsException e)
                {
                    String errorMsg = "Missing TARGET_POINTS part for Axis movement: " + e.getMessage();
                    Logger.getInstance().log("PARSER", "Error: " + errorMsg);
                    throw new IllegalArgumentException(errorMsg, e);
                }
            }
            else
            { // Cartesian Motion
                try
                {
                    cartesianTargetPoints = parseCartesianPositions(parts[MessagePartIndex.TARGET_POINTS.getIndex()]);
                    if (cartesianTargetPoints.size() != numPoints)
                    {
                        String errorMsg = "Parsed NUM_POINTS (" + numPoints + ") does not match actual parsed Cartesian points (" + cartesianTargetPoints.size() + ").";
                        Logger.getInstance().log("PARSER", "Error: " + errorMsg);
                        throw new IllegalArgumentException(errorMsg);
                    }
                    return ParsedCommand.forCartesianMovement(actionType, id, cartesianTargetPoints, motionParameters);
                }
                catch (ArrayIndexOutOfBoundsException e)
                {
                    String errorMsg = "Missing TARGET_POINTS part for Cartesian movement: " + e.getMessage();
                    Logger.getInstance().log("PARSER", "Error: " + errorMsg);
                    throw new IllegalArgumentException(errorMsg, e);
                }
            }
        }
        else if (actionType == ActionTypes.ACTIVATE_IO)
        {
            commandCategory = CommandCategory.IO;
            try
            {
                int ioPoint = 0; //TODO: Handle ioPoint instead of hardcoding for testing
                int ioPin = Integer.parseInt(parts[MessagePartIndex.IO_PIN.getIndex()]);
                boolean ioState = Boolean.parseBoolean(parts[MessagePartIndex.IO_STATE.getIndex()]);
                ioCommandData = new IoCommandData(ioPoint, ioPin, ioState);
                return ParsedCommand.forIo(actionType, id, ioCommandData);
            }
            catch (NumberFormatException | ArrayIndexOutOfBoundsException e)
            {
                String errorMsg = "Invalid IO command data format for ACTIVATE_IO: " + e.getMessage();
                Logger.getInstance().log("PARSER", "Error: " + errorMsg);
                throw new IllegalArgumentException(errorMsg, e);
            }
        }
        else if (actionType.getValue() >= ActionTypes.PROGRAM_CALL_OFFSET)
        { // Assuming this range indicates program calls
            commandCategory = CommandCategory.PROGRAM_CALL;
            try
            {
                programId = actionType.getValue() - ActionTypes.PROGRAM_CALL_OFFSET; // Derive programId from actionType value
                return ParsedCommand.forProgramCall(actionType, id, programId);
            }
            catch (Exception e)
            { // Catch any parsing errors for program ID
                String errorMsg = "Invalid Program Call command data format: " + e.getMessage();
                Logger.getInstance().log("PARSER", "Error: " + errorMsg);
                throw new IllegalArgumentException(errorMsg, e);
            }
        }
        else
        {
            commandCategory = CommandCategory.UNKNOWN;
            String errorMsg = "Unknown or unsupported ActionType: " + actionType.getValue() + " in command: " + commandString;
            Logger.getInstance().log("PARSER", "Error: " + errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }

    private static List<AxisPosition> parseAxisPositions(String axisPositionsString)
    {
        List<AxisPosition> positions = new ArrayList<>();
        String[] individualPointStrings = axisPositionsString.split(MULTI_POINT_DELIMITER);

        for (String pointString : individualPointStrings)
        {
            String[] jointValues = pointString.split(SECONDARY_DELIMITER);
            if (jointValues.length != 7)
            {
                String errorMsg = "Invalid axis position format: Expected 7 joint values (J1-J7), got " + jointValues.length + " in point string: " + pointString;
                Logger.getInstance().log("PARSER", "Error: " + errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            try
            {
                double j1 = Double.parseDouble(jointValues[0]);
                double j2 = Double.parseDouble(jointValues[1]);
                double j3 = Double.parseDouble(jointValues[2]);
                double j4 = Double.parseDouble(jointValues[3]);
                double j5 = Double.parseDouble(jointValues[4]);
                double j6 = Double.parseDouble(jointValues[5]);
                double j7 = Double.parseDouble(jointValues[6]);
                positions.add(new AxisPosition(j1, j2, j3, j4, j5, j6, j7));
            }
            catch (NumberFormatException e)
            {
                String errorMsg = "Invalid number format in axis positions: " + e.getMessage() + " for point string: " + pointString;
                Logger.getInstance().log("PARSER", "Error: " + errorMsg);
                throw new IllegalArgumentException(errorMsg, e);
            }
        }
        return positions;
    }

    private static List<CartesianPosition> parseCartesianPositions(String cartesianPositionsString)
    {
        List<CartesianPosition> positions = new ArrayList<>();
        String[] individualPointStrings = cartesianPositionsString.split(MULTI_POINT_DELIMITER);

        for (String pointString : individualPointStrings)
        {
            String[] values = pointString.split(SECONDARY_DELIMITER);
            if (values.length != 6)
            {
                String errorMsg = "Invalid Cartesian position format: Expected 6 values (X;Y;Z;A;B;C), got " + values.length + " in point string: " + pointString;
                Logger.getInstance().log("PARSER", "Error: " + errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            try
            {
                double x = Double.parseDouble(values[0]);
                double y = Double.parseDouble(values[1]);
                double z = Double.parseDouble(values[2]);
                double a = Double.parseDouble(values[3]);
                double b = Double.parseDouble(values[4]);
                double c = Double.parseDouble(values[5]);
                positions.add(new CartesianPosition(x, y, z, a, b, c));
            }
            catch (NumberFormatException e)
            {
                String errorMsg = "Invalid number format in Cartesian positions: " + e.getMessage() + " for point string: " + pointString;
                Logger.getInstance().log("PARSER", "Error: " + errorMsg);
                throw new IllegalArgumentException(errorMsg, e);
            }
        }
        return positions;
    }
}

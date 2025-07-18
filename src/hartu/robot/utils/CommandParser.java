package hartu.robot.utils;

import hartu.protocols.constants.ActionTypes;
import hartu.protocols.constants.MessagePartIndex;
import hartu.protocols.constants.MovementType;
import hartu.robot.commands.MotionParameters;
import hartu.robot.commands.ParsedCommand;
import hartu.robot.commands.io.IoCommandData;
import hartu.robot.commands.positions.AxisPosition;
import hartu.robot.commands.positions.CartesianPosition;

import java.util.ArrayList;
import java.util.List;

public class CommandParser {

    public static ParsedCommand parse(String rawCommandString) {
        String[] parts = rawCommandString.split("\\|");

        if (parts.length < MessagePartIndex.values().length -1) {
            throw new IllegalArgumentException("Malformed command string: Not enough parts. Expected " + MessagePartIndex.values().length + " but got " + parts.length);
        }

        ActionTypes actionType = ActionTypes.fromValue(Integer.parseInt(parts[MessagePartIndex.ACTION_TYPE.getIndex()]));
        String id = parts[MessagePartIndex.ID.getIndex()];

        if (actionType.getValue() >= ActionTypes.PROGRAM_CALL_OFFSET) {
            Integer programId = actionType.getValue() - ActionTypes.PROGRAM_CALL_OFFSET;
            return ParsedCommand.forProgramCall(actionType, id, programId);
        } else if (actionType == ActionTypes.ACTIVATE_IO) {
            int ioPoint = Integer.parseInt(parts[MessagePartIndex.IO_POINT.getIndex()]);
            int ioPin = Integer.parseInt(parts[MessagePartIndex.IO_PIN.getIndex()]);
            boolean ioState = Boolean.parseBoolean(parts[MessagePartIndex.IO_STATE.getIndex()]);
            IoCommandData ioCommandData = new IoCommandData(ioPoint, ioPin, ioState);
            return ParsedCommand.forIo(actionType, id, ioCommandData);
        } else { // Movement Commands
            int numPoints = Integer.parseInt(parts[MessagePartIndex.NUM_POINTS.getIndex()]);
            String targetPointsString = parts[MessagePartIndex.TARGET_POINTS.getIndex()];
            double speedOverride = Double.parseDouble(parts[MessagePartIndex.SPEED_OVERRIDE.getIndex()]);
            String tool = parts[MessagePartIndex.TOOL.getIndex()];
            String base = parts[MessagePartIndex.BASE.getIndex()];

            MotionParameters motionParameters = new MotionParameters(speedOverride, tool, base);
            MovementType movementType = MovementType.fromActionType(actionType);

            String[] individualPointStrings = targetPointsString.split(";");
            if (individualPointStrings.length != numPoints) {
                throw new IllegalArgumentException("Number of points declared (" + numPoints + ") does not match actual points provided (" + individualPointStrings.length + ").");
            }

            if (movementType.isAxisMotion()) {
                List<AxisPosition> axisPositions = new ArrayList<AxisPosition>();
                for (String pointStr : individualPointStrings) {
                    String[] jointValues = pointStr.split(",");
                    if (jointValues.length != 7) {
                        throw new IllegalArgumentException("Axis point must have 7 joint values, but got " + jointValues.length + " for point: " + pointStr);
                    }
                    axisPositions.add(new AxisPosition(
                            Double.parseDouble(jointValues[0]),
                            Double.parseDouble(jointValues[1]),
                            Double.parseDouble(jointValues[2]),
                            Double.parseDouble(jointValues[3]),
                            Double.parseDouble(jointValues[4]),
                            Double.parseDouble(jointValues[5]),
                            Double.parseDouble(jointValues[6])
                    ));
                }
                return ParsedCommand.forAxisMovement(actionType, id, axisPositions, motionParameters);
            } else if (movementType.isCartesianMotion()) {
                List<CartesianPosition> cartesianPositions = new ArrayList<CartesianPosition>();
                for (String pointStr : individualPointStrings) {
                    String[] coordValues = pointStr.split(",");
                    if (coordValues.length != 6) {
                        throw new IllegalArgumentException("Cartesian point must have 6 coordinate values (X,Y,Z,A,B,C), but got " + coordValues.length + " for point: " + pointStr);
                    }
                    cartesianPositions.add(new CartesianPosition(
                            Double.parseDouble(coordValues[0]),
                            Double.parseDouble(coordValues[1]),
                            Double.parseDouble(coordValues[2]),
                            Double.parseDouble(coordValues[3]),
                            Double.parseDouble(coordValues[4]),
                            Double.parseDouble(coordValues[5])
                    ));
                }
                return ParsedCommand.forCartesianMovement(actionType, id, cartesianPositions, motionParameters);
            } else {
                throw new IllegalArgumentException("Unsupported movement type for action: " + actionType.name());
            }
        }
    }
}

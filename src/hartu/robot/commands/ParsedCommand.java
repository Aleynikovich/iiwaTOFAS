// --- ParsedCommand.java ---
package hartu.robot.commands;

import hartu.protocols.constants.ActionTypes;
import hartu.robot.commands.io.IoCommandData;
import hartu.robot.commands.positions.AxisPosition;
import hartu.robot.commands.positions.CartesianPosition;

import java.util.List;

public class ParsedCommand
{
    private final ActionTypes actionType;
    private final String id;

    private final List<AxisPosition> axisTargetPoints;
    private final List<CartesianPosition> cartesianTargetPoints;
    private final MotionParameters motionParameters;
    private final IoCommandData ioCommandData;
    private final Integer programId;

    private ParsedCommand(ActionTypes actionType, String id,
                          List<AxisPosition> axisTargetPoints,
                          List<CartesianPosition> cartesianTargetPoints,
                          MotionParameters motionParameters,
                          IoCommandData ioCommandData,
                          Integer programId)
    {
        this.actionType = actionType;
        this.id = id;
        this.axisTargetPoints = axisTargetPoints;
        this.cartesianTargetPoints = cartesianTargetPoints;
        this.motionParameters = motionParameters;
        this.ioCommandData = ioCommandData;
        this.programId = programId;
    }

    public static ParsedCommand forAxisMovement(ActionTypes actionType, String id,
                                                List<AxisPosition> axisTargetPoints,
                                                MotionParameters motionParameters)
    {
        return new ParsedCommand(actionType, id, axisTargetPoints, null, motionParameters, null, null);
    }

    public static ParsedCommand forCartesianMovement(ActionTypes actionType, String id,
                                                     List<CartesianPosition> cartesianTargetPoints,
                                                     MotionParameters motionParameters)
    {
        return new ParsedCommand(actionType, id, null, cartesianTargetPoints, motionParameters, null, null);
    }

    public static ParsedCommand forIo(ActionTypes actionType, String id,
                                      IoCommandData ioCommandData)
    {
        return new ParsedCommand(actionType, id, null, null, null, ioCommandData, null);
    }

    public static ParsedCommand forProgramCall(ActionTypes actionType, String id, Integer programId)
    {
        return new ParsedCommand(actionType, id, null, null, null, null, programId);
    }

    public ActionTypes getActionType() {
        return actionType;
    }

    public String getId() {
        return id;
    }

    public List<AxisPosition> getAxisTargetPoints() {
        return axisTargetPoints;
    }

    public List<CartesianPosition> getCartesianTargetPoints() {
        return cartesianTargetPoints;
    }

    public MotionParameters getMotionParameters() {
        return motionParameters;
    }

    public IoCommandData getIoCommandData() {
        return ioCommandData;
    }

    public Integer getProgramId() {
        return programId;
    }

    public boolean isMovementCommand() {
        return (axisTargetPoints != null || cartesianTargetPoints != null) && programId == null;
    }

    public boolean isIoCommand() {
        return ioCommandData != null;
    }

    public boolean isProgramCall() {
        return programId != null;
    }

    public int getProgramCallId() {
        if (!isProgramCall()) {
            throw new IllegalStateException("This command is not a program call.");
        }
        return programId.intValue();
    }

    /**
     * Converts the ParsedCommand object into a JSON string.
     * This method manually constructs the JSON string to avoid external library dependencies.
     *
     * @return A JSON string representation of the ParsedCommand.
     */
    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"actionType\": \"").append(actionType.name()).append("\",");
        json.append("\"actionValue\": ").append(actionType.getValue()).append(",");
        json.append("\"id\": \"").append(id).append("\"");

        if (isMovementCommand()) {
            json.append(",\"commandType\": \"Movement\"");
            if (axisTargetPoints != null && !axisTargetPoints.isEmpty()) {
                json.append(",\"axisTargetPoints\": [");
                for (int i = 0; i < axisTargetPoints.size(); i++) {
                    AxisPosition pos = axisTargetPoints.get(i);
                    json.append("{");
                    json.append("\"J1\": ").append(pos.getJ1()).append(",");
                    json.append("\"J2\": ").append(pos.getJ2()).append(",");
                    json.append("\"J3\": ").append(pos.getJ3()).append(",");
                    json.append("\"J4\": ").append(pos.getJ4()).append(",");
                    json.append("\"J5\": ").append(pos.getJ5()).append(",");
                    json.append("\"J6\": ").append(pos.getJ6()).append(",");
                    json.append("\"J7\": ").append(pos.getJ7());
                    json.append("}");
                    if (i < axisTargetPoints.size() - 1) {
                        json.append(",");
                    }
                }
                json.append("]");
            }
            if (cartesianTargetPoints != null && !cartesianTargetPoints.isEmpty()) {
                json.append(",\"cartesianTargetPoints\": [");
                for (int i = 0; i < cartesianTargetPoints.size(); i++) {
                    CartesianPosition pos = cartesianTargetPoints.get(i);
                    json.append("{");
                    json.append("\"X\": ").append(pos.getX()).append(",");
                    json.append("\"Y\": ").append(pos.getY()).append(",");
                    json.append("\"Z\": ").append(pos.getZ()).append(",");
                    json.append("\"A\": ").append(pos.getA()).append(",");
                    json.append("\"B\": ").append(pos.getB()).append(",");
                    json.append("\"C\": ").append(pos.getC());
                    json.append("}");
                    if (i < cartesianTargetPoints.size() - 1) {
                        json.append(",");
                    }
                }
                json.append("]");
            }
            if (motionParameters != null) {
                json.append(",\"motionParameters\": {");
                json.append("\"speedOverride\": ").append(motionParameters.getSpeedOverride()).append(",");
                json.append("\"tool\": \"").append(motionParameters.getTool()).append("\",");
                json.append("\"base\": \"").append(motionParameters.getBase()).append("\",");
                json.append("\"continuous\": ").append(motionParameters.isContinuous()).append(",");
                json.append("\"numPoints\": ").append(motionParameters.getNumPoints());
                json.append("}");
            }
        } else if (isIoCommand()) {
            json.append(",\"commandType\": \"IO\"");
            if (ioCommandData != null) {
                json.append(",\"ioCommandData\": {");
                json.append("\"ioPoint\": ").append(ioCommandData.getIoPoint()).append(",");
                json.append("\"ioPin\": ").append(ioCommandData.getIoPin()).append(",");
                json.append("\"ioState\": ").append(ioCommandData.getIoState());
                json.append("}");
            }
        } else if (isProgramCall()) {
            json.append(",\"commandType\": \"ProgramCall\"");
            json.append(",\"programId\": ").append(programId);
        } else {
            json.append(",\"commandType\": \"Unknown\"");
        }

        json.append("}");
        return json.toString();
    }

    @Override
    public String toString() {
        // For now, toString() will just return the JSON representation.
        // If you need a different non-JSON string representation for other purposes,
        // you can implement it here.
        return toJson();
    }
}

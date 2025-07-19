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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ParsedCommand {\n");
        sb.append("  ActionType: ").append(actionType).append(" (").append(actionType.getValue()).append(")\n");
        sb.append("  ID: ").append(id).append("\n");

        if (isMovementCommand()) {
            sb.append("  --- Movement Command ---\n");
            if (axisTargetPoints != null && !axisTargetPoints.isEmpty()) {
                sb.append("  Axis Target Points (").append(axisTargetPoints.size()).append("):\n");
                for (int i = 0; i < axisTargetPoints.size(); i++) {
                    AxisPosition pos = axisTargetPoints.get(i);
                    sb.append("    Point ").append(i + 1).append(": J1=").append(pos.getJ1())
                            .append(", J2=").append(pos.getJ2())
                            .append(", J3=").append(pos.getJ3())
                            .append(", J4=").append(pos.getJ4())
                            .append(", J5=").append(pos.getJ5())
                            .append(", J6=").append(pos.getJ6())
                            .append(", J7=").append(pos.getJ7()).append("\n");
                }
            }
            if (cartesianTargetPoints != null && !cartesianTargetPoints.isEmpty()) {
                sb.append("  Cartesian Target Points (").append(cartesianTargetPoints.size()).append("):\n");
                for (int i = 0; i < cartesianTargetPoints.size(); i++) {
                    CartesianPosition pos = cartesianTargetPoints.get(i);
                    sb.append("    Point ").append(i + 1).append(": X=").append(pos.getX())
                            .append(", Y=").append(pos.getY())
                            .append(", Z=").append(pos.getZ())
                            .append(", A=").append(pos.getA())
                            .append(", B=").append(pos.getB())
                            .append(", C=").append(pos.getC()).append("\n");
                }
            }
            if (motionParameters != null) {
                sb.append("  Motion Parameters:\n");
                sb.append("    Speed Override: ").append(motionParameters.getSpeedOverride()).append("\n");
                sb.append("    Tool: ").append(motionParameters.getTool().isEmpty() ? "[Default]" : motionParameters.getTool()).append("\n");
                sb.append("    Base: ").append(motionParameters.getBase().isEmpty() ? "[Default]" : motionParameters.getBase()).append("\n");
                sb.append("    Continuous: ").append(motionParameters.isContinuous()).append("\n");
                sb.append("    Num Points: ").append(motionParameters.getNumPoints()).append("\n");
            }
        } else if (isIoCommand()) {
            sb.append("  --- IO Command ---\n");
            if (ioCommandData != null) {
                sb.append("  IO Data:\n");
                sb.append("    IO Point: ").append(ioCommandData.getIoPoint()).append("\n");
                sb.append("    IO Pin: ").append(ioCommandData.getIoPin()).append("\n");
                sb.append("    IO State: ").append(ioCommandData.getIoState()).append("\n");
            }
        } else if (isProgramCall()) {
            sb.append("  --- Program Call ---\n");
            sb.append("  Program ID: ").append(programId).append("\n");
        } else {
            sb.append("  --- Unrecognized Command Type ---\n");
        }

        sb.append("}");
        return sb.toString();
    }
}

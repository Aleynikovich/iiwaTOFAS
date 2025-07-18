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
}

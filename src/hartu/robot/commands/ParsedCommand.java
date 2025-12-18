package hartu.robot.commands;

import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.geometricModel.Frame;
import hartu.protocols.constants.ActionTypes;
import hartu.protocols.constants.CommandCategory;
import hartu.robot.commands.io.IoCommandData;
import hartu.robot.communication.server.Logger;

import java.util.List;

public class ParsedCommand
{
    private final ActionTypes actionType;
    private final String id;
    private final CommandCategory commandCategory;

    private final List<JointPosition> axisTargetPoints;
    private final List<Frame> cartesianTargetPoints;
    private final MotionParameters motionParameters;
    private final IoCommandData ioCommandData;
    private final Integer programId;
    private final BaseCoordinateData baseCoordinateData;

    private ParsedCommand(ActionTypes actionType, String id, CommandCategory commandCategory, List<JointPosition> axisTargetPoints, List<Frame> cartesianTargetPoints, MotionParameters motionParameters, IoCommandData ioCommandData, Integer programId, BaseCoordinateData baseCoordinateData)
    {
        this.actionType = actionType;
        this.id = id;
        this.commandCategory = commandCategory;
        this.axisTargetPoints = axisTargetPoints;
        this.cartesianTargetPoints = cartesianTargetPoints;
        this.motionParameters = motionParameters;
        this.ioCommandData = ioCommandData;
        this.programId = programId;
        this.baseCoordinateData = baseCoordinateData;
    }

    public static ParsedCommand forAxisMovement(ActionTypes actionType, String id, List<JointPosition> axisTargetPoints, MotionParameters motionParameters)
    {
        return new ParsedCommand(actionType, id, actionType.getCategory(), axisTargetPoints, null, motionParameters, null, null, null);
    }

    public static ParsedCommand forCartesianMovement(ActionTypes actionType, String id, List<Frame> cartesianTargetPoints, MotionParameters motionParameters)
    {
        return new ParsedCommand(actionType, id, actionType.getCategory(), null, cartesianTargetPoints, motionParameters, null, null, null);
    }

    public static ParsedCommand forIo(ActionTypes actionType, String id, IoCommandData ioCommandData)
    {
        return new ParsedCommand(actionType, id, actionType.getCategory(), null, null, null, ioCommandData, null, null);
    }

    public static ParsedCommand forProgramCall(ActionTypes actionType, String id, Integer programId)
    {
        return new ParsedCommand(actionType, id, actionType.getCategory(), null, null, null, null, programId, null);
    }

    public static ParsedCommand forProgramCallWithBaseData(ActionTypes actionType, String id, Integer programId, BaseCoordinateData baseCoordinateData)
    {
        return new ParsedCommand(actionType, id, actionType.getCategory(), null, null, null, null, programId, baseCoordinateData);
    }

    public ActionTypes getActionType()
    {
        return actionType;
    }

    public String getId()
    {
        return id;
    }

    public CommandCategory getCommandCategory()
    {
        return commandCategory;
    }

    // Existing getters (can be deprecated or removed if no longer directly used outside ParsedCommand)
    public List<JointPosition> getAxisTargetPoints()
    {
        return axisTargetPoints;
    }

    public List<Frame> getCartesianTargetPoints()
    {
        return cartesianTargetPoints;
    }

    public MotionParameters getMotionParameters()
    {
        return motionParameters;
    }

    public IoCommandData getIoCommandData()
    {
        return ioCommandData;
    }

    public Integer getProgramId()
    {
        return programId;
    }

    public BaseCoordinateData getBaseCoordinateData()
    {
        return baseCoordinateData;
    }

    public boolean hasBaseCoordinateData()
    {
        return baseCoordinateData != null;
    }

    public boolean isMovementCommand()
    {
        return this.commandCategory == CommandCategory.MOVEMENT;
    }

    public boolean isIoCommand()
    {
        return this.commandCategory == CommandCategory.IO;
    }

    public boolean isProgramCall()
    {
        return this.commandCategory == CommandCategory.PROGRAM_CALL;
    }

    public int getProgramCallId()
    {
        if (!isProgramCall())
        {
            throw new IllegalStateException("This command is not a program call.");
        }
        return programId;
    }

    public boolean isInputReadCommand()
    {
        return this.actionType == ActionTypes.DIGITAL_INPUT || this.actionType == ActionTypes.ANALOG_INPUT;
    }

    @Override
    public String toString()
    {
        try
        {

            StringBuilder sb = new StringBuilder();
            sb.append("  ActionType: ").append(actionType).append(" (").append(actionType.getValue()).append(")\n");
            //sb.append("  Category: ").append(commandCategory).append("\n");
            //sb.append("  ID: ").append(id).append("\n");

            if (isMovementCommand())
            {
                sb.append("  --- Movement Command ---\n");

                if (axisTargetPoints != null)
                {
                    sb.append("  Joint Target Points (").append(axisTargetPoints.size()).append("):\n");
                    for (int i = 0; i < axisTargetPoints.size(); i++)
                    {
                        JointPosition pos = axisTargetPoints.get(i);
                        // %4d means "integer with minimum width of 4"
                        // %n adds a new line
                        sb.append(String.format("    Point %2d: J1=%4d, J2=%4d, J3=%4d, J4=%4d, J5=%4d, J6=%4d, J7=%4d%n",
                                i + 1,
                                Math.round(Math.toDegrees(pos.get(0))),
                                Math.round(Math.toDegrees(pos.get(1))),
                                Math.round(Math.toDegrees(pos.get(2))),
                                Math.round(Math.toDegrees(pos.get(3))),
                                Math.round(Math.toDegrees(pos.get(4))),
                                Math.round(Math.toDegrees(pos.get(5))),
                                Math.round(Math.toDegrees(pos.get(6)))
                        ));
                    }
                } else if (cartesianTargetPoints != null)
                {
                    sb.append("  Frame Target Points (").append(cartesianTargetPoints.size()).append("):\n");
                    for (int i = 0; i < cartesianTargetPoints.size(); i++)
                    {
                        Frame pos = cartesianTargetPoints.get(i);
                        // XYZ usually need more space (e.g., 1500mm), so using %5d
                        sb.append(String.format("    Point %2d: X=%5d, Y=%5d, Z=%5d, A=%4d, B=%4d, C=%4d%n",
                                i + 1,
                                Math.round(pos.getX()),
                                Math.round(pos.getY()),
                                Math.round(pos.getZ()),
                                Math.round(Math.toDegrees(pos.getAlphaRad())),
                                Math.round(Math.toDegrees(pos.getBetaRad())),
                                Math.round(Math.toDegrees(pos.getGammaRad()))
                        ));
                    }
                }

                if (motionParameters != null)
                {
                    sb.append("  Motion Parameters:\n");
                    sb.append("    Speed Override: ").append(motionParameters.getSpeedOverride()).append("\n");
                    sb.append("    Tool: ").append(motionParameters.getTool().isEmpty() ? "[Default]" : motionParameters.getTool()).append(
                            "\n");
                    sb.append("    Base: ").append(motionParameters.getBase().isEmpty() ? "[Default]" : motionParameters.getBase()).append(
                            "\n");
                    //sb.append("    Continuous: ").append(motionParameters.isContinuous()).append("\n");
                    //sb.append("    Num Points: ").append(motionParameters.getNumPoints()).append("\n");
                }
            } else if (isIoCommand())
            {
                sb.append("  --- IO Command ---\n");
                if (ioCommandData != null)
                {
                    sb.append("  IO Data:\n");
                    sb.append("    IO Point: ").append(ioCommandData.getIoPoint()).append("\n");
                    sb.append("    IO Pin: ").append(ioCommandData.getIoPin()).append("\n");
                    sb.append("    IO State: ").append(ioCommandData.getIoState()).append("\n");
                }
            } else if (isProgramCall())
            {
                sb.append("  --- Program Call ---\n");
                sb.append("  Program ID: ").append(programId).append("\n");
                if (baseCoordinateData != null)
                {
                    sb.append("  Base Coordinate Data: ").append(baseCoordinateData).append("\n");
                }
            } else
            {
                sb.append("  --- Unrecognized Command Type ---\n");
            }
            return sb.toString();
        } catch (Exception e)
        {
            Logger.getInstance().error("PARSER", "Error parsing command: " + e.getMessage());
        }
        return "Lel";
    }
}
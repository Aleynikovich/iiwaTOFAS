package hartu.robot.commands;

import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.geometricModel.Frame;
import hartu.protocols.constants.ActionTypes;
import hartu.protocols.constants.CommandCategory;
import hartu.robot.commands.io.IoCommandData;
import hartu.robot.communication.server.Logger;

import java.util.Arrays;
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

    private ParsedCommand(ActionTypes actionType, String id, CommandCategory commandCategory, List<JointPosition> axisTargetPoints, List<Frame> cartesianTargetPoints, MotionParameters motionParameters, IoCommandData ioCommandData, Integer programId)
    {
        this.actionType = actionType;
        this.id = id;
        this.commandCategory = commandCategory;
        this.axisTargetPoints = axisTargetPoints;
        this.cartesianTargetPoints = cartesianTargetPoints;
        this.motionParameters = motionParameters;
        this.ioCommandData = ioCommandData;
        this.programId = programId;
    }

    public static ParsedCommand forAxisMovement(ActionTypes actionType, String id, List<JointPosition> axisTargetPoints, MotionParameters motionParameters)
    {
        return new ParsedCommand(actionType, id, actionType.getCategory(), axisTargetPoints, null, motionParameters, null, null);
    }

    public static ParsedCommand forCartesianMovement(ActionTypes actionType, String id, List<Frame> cartesianTargetPoints, MotionParameters motionParameters)
    {
        return new ParsedCommand(actionType, id, actionType.getCategory(), null, cartesianTargetPoints, motionParameters, null, null);
    }

    public static ParsedCommand forIo(ActionTypes actionType, String id, IoCommandData ioCommandData)
    {
        return new ParsedCommand(actionType, id, actionType.getCategory(), null, null, null, ioCommandData, null);
    }

    public static ParsedCommand forProgramCall(ActionTypes actionType, String id, Integer programId)
    {
        return new ParsedCommand(actionType, id, actionType.getCategory(), null, null, null, null, programId);
    }

    public ActionTypes getActionType()
    {
        return actionType;
    }

    public String getId()
    {
        return id;
    }

    public CommandCategory getCommandCategory() {
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

    @Override
    public String toString()
    {
        try {

            StringBuilder sb = new StringBuilder();
            sb.append("\nParsedCommand {\n");
            sb.append("  ActionType: ").append(actionType).append(" (").append(actionType.getValue()).append(")\n");
            sb.append("  Category: ").append(commandCategory).append("\n");
            sb.append("  ID: ").append(id).append("\n");

            if (isMovementCommand())
            {
                sb.append("  --- Movement Command ---\n");

                if (axisTargetPoints != null)
                {
                    sb.append("  Joint Target Points (").append(axisTargetPoints.size()).append("):\n");
                    for (int i = 0; i < axisTargetPoints.size(); i++)
                    {
                        JointPosition pos = axisTargetPoints.get(i);
                        sb.append("    Point ").append(i + 1).append(": J1=").append(pos.get(0)).append(", J2=").append(pos.get(1)).append(
                                ", J3=").append(pos.get(2)).append(", J4=").append(pos.get(3)).append(", J5=").append(pos.get(4)).append(
                                ", J6=").append(pos.get(5)).append(", J7=").append(pos.get(6)).append("\n");
                    }
                }

                else if (cartesianTargetPoints != null)
                {
                    sb.append("  Frame Target Points (").append(cartesianTargetPoints.size()).append("):\n");
                    for (int i = 0; i < cartesianTargetPoints.size(); i++)
                    {
                        Frame pos = cartesianTargetPoints.get(i);
                        sb.append("    Point ").append(i + 1).append(": X=").append(pos.getX()).append(", Y=").append(pos.getY()).append(
                                ", Z=").append(pos.getZ()).append(", A=").append(Math.toDegrees(pos.getAlphaRad())).append(", B=").append(Math.toDegrees(pos.getBetaRad())).append(
                                ", C=").append(Math.toDegrees(pos.getGammaRad())).append("\n");
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
                    sb.append("    Continuous: ").append(motionParameters.isContinuous()).append("\n");
                    sb.append("    Num Points: ").append(motionParameters.getNumPoints()).append("\n");
                }
            }
            else if (isIoCommand())
            {
                sb.append("  --- IO Command ---\n");
                if (ioCommandData != null)
                {
                    sb.append("  IO Data:\n");
                    sb.append("    IO Point: ").append(ioCommandData.getIoPoint()).append("\n");
                    sb.append("    IO Pin: ").append(ioCommandData.getIoPin()).append("\n");
                    sb.append("    IO State: ").append(ioCommandData.getIoState()).append("\n");
                }
            }
            else if (isProgramCall())
            {
                sb.append("  --- Program Call ---\n");
                sb.append("  Program ID: ").append(programId).append("\n");
            }
            else
            {
                sb.append("  --- Unrecognized Command Type ---\n");
            }

            sb.append("}");
            return sb.toString();
        }
        catch (Exception e) {
            Logger.getInstance().error("PARSER", "Error parsing command: " + e.getMessage());
        }
        return "Lel";
    }
}
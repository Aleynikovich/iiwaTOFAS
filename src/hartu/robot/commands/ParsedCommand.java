package hartu.robot.commands;

import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.motionModel.IMotion;
import com.kuka.roboticsAPI.motionModel.RobotMotion;
import hartu.protocols.constants.ActionTypes;
import hartu.protocols.constants.CommandCategory;
import hartu.robot.commands.io.IoCommandData;
import hartu.robot.commands.positions.AxisPosition;
import hartu.robot.commands.positions.CartesianPosition;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.linRel;

public class ParsedCommand
{
    private final ActionTypes actionType;
    private final String id;
    private final CommandCategory commandCategory;
    private final List<AxisPosition> axisTargetPoints;
    private final List<CartesianPosition> cartesianTargetPoints;
    private final MotionParameters motionParameters;
    private final IoCommandData ioCommandData;
    private final Integer programId;
    @Inject
    private LBR iiwa;

    private ParsedCommand(ActionTypes actionType, String id, CommandCategory commandCategory, List<AxisPosition> axisTargetPoints, List<CartesianPosition> cartesianTargetPoints, MotionParameters motionParameters, IoCommandData ioCommandData, Integer programId)
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

    public static ParsedCommand forAxisMovement(ActionTypes actionType, String id, List<AxisPosition> axisTargetPoints, MotionParameters motionParameters)
    {
        return new ParsedCommand(
                actionType,
                id,
                actionType.getCategory(),
                axisTargetPoints,
                null,
                motionParameters,
                null,
                null
        );
    }

    public static ParsedCommand forCartesianMovement(ActionTypes actionType, String id, List<CartesianPosition> cartesianTargetPoints, MotionParameters motionParameters)
    {
        return new ParsedCommand(
                actionType,
                id,
                actionType.getCategory(),
                null,
                cartesianTargetPoints,
                motionParameters,
                null,
                null
        );
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

    public CommandCategory getCommandCategory()
    {
        return commandCategory;
    }

    /**
     * Returns the list of target points for movement commands.
     * The specific type of the list (AxisPosition or CartesianPosition)
     * depends on whether it's an axis or cartesian motion.
     * Callers should check isAxisMovement() or isCartesianMovement()
     * and cast accordingly.
     *
     * @return A List<?> containing AxisPosition or CartesianPosition objects,
     * or null if it's not a movement command.
     */

    // Existing getters (can be deprecated or removed if no longer directly used outside ParsedCommand)
    public List<AxisPosition> getAxisTargetPoints()
    {
        return axisTargetPoints;
    }

    public List<CartesianPosition> getCartesianTargetPoints()
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

    public List<IMotion> getMotionList()
    {
        List<IMotion> motionsToExecute = new ArrayList<>();
        if (isMovementCommand())
        {
            if (axisTargetPoints != null)
            {
                for (AxisPosition axisTargetPoint : axisTargetPoints)
                {
                    RobotMotion<?> currentMotion = null;
                    if (actionType == ActionTypes.PTP_AXIS || actionType == ActionTypes.PTP_AXIS_C)
                    {
                        currentMotion = ptp(axisTargetPoint.toJointPosition());
                    }
                    else if (actionType == ActionTypes.LIN_AXIS)
                    {
                        currentMotion = lin(iiwa.getForwardKinematic(axisTargetPoint.toJointPosition()));
                    }
                    if (currentMotion != null)
                    {
                        motionsToExecute.add(currentMotion.setBlendingRel(0.5)); // Apply blending
                    }
                }
            }
            else if (cartesianTargetPoints != null)
            {
                for (CartesianPosition cartPos : cartesianTargetPoints)
                {
                    RobotMotion<?> currentMotion = null;
                    if (actionType == ActionTypes.PTP_FRAME || actionType == ActionTypes.PTP_FRAME_C)
                    {
                        currentMotion = ptp(cartPos.toFrame(iiwa.getFlange()));
                    }
                    else if (actionType == ActionTypes.LIN_FRAME || actionType == ActionTypes.LIN_FRAME_C)
                    {
                        currentMotion = lin(cartPos.toFrame(iiwa.getFlange()));
                    }
                    else if (actionType == ActionTypes.LIN_REL_TOOL)
                    {
                        currentMotion = linRel(cartPos.getX(), cartPos.getY(), cartPos.getZ(), iiwa.getFlange());
                    }
                    else if (actionType == ActionTypes.LIN_REL_BASE)
                    {
                        currentMotion = linRel(cartPos.getX(), cartPos.getY(), cartPos.getZ(), iiwa.getRootFrame());
                    }
                    if (currentMotion != null)
                    {
                        motionsToExecute.add(currentMotion.setBlendingRel(0.5)); // Apply blending
                    }
                }
            }

            return motionsToExecute;
        }
        return null;
    }

    public List<?> getMovementTargetPoints()
    {
        if (isMovementCommand())
        {
            if (axisTargetPoints != null)
            {
                return axisTargetPoints;
            }
            else if (cartesianTargetPoints != null)
            {
                return cartesianTargetPoints;
            }
        }
        return null;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("\nParsedCommand {\n");
        sb.append("  ActionType: ").append(actionType).append(" (").append(actionType.getValue()).append(")\n");
        sb.append("  Category: ").append(commandCategory).append("\n");
        sb.append("  ID: ").append(id).append("\n");

        if (isMovementCommand())
        {
            sb.append("  --- Movement Command ---\n");
            // Use the new getMovementTargetPoints for toString as well
            List<?> targetPoints = getMovementTargetPoints();
            if (targetPoints != null && !targetPoints.isEmpty())
            {
                if (axisTargetPoints != null)
                { // Check original field to know the type
                    sb.append("  Axis Target Points (").append(axisTargetPoints.size()).append("):\n");
                    for (int i = 0; i < axisTargetPoints.size(); i++)
                    {
                        AxisPosition pos = axisTargetPoints.get(i);
                        sb.append("    Point ").append(i + 1).append(": J1=").append(pos.getJ1()).append(", J2=").append(
                                pos.getJ2()).append(
                                ", J3=").append(pos.getJ3()).append(", J4=").append(pos.getJ4()).append(", J5=").append(
                                pos.getJ5()).append(
                                ", J6=").append(pos.getJ6()).append(", J7=").append(pos.getJ7()).append("\n");
                    }
                }
                else if (cartesianTargetPoints != null)
                { // Check original field to know the type
                    sb.append("  Cartesian Target Points (").append(cartesianTargetPoints.size()).append("):\n");
                    for (int i = 0; i < cartesianTargetPoints.size(); i++)
                    {
                        CartesianPosition pos = cartesianTargetPoints.get(i);
                        sb.append("    Point ").append(i + 1).append(": X=").append(pos.getX()).append(", Y=").append(
                                pos.getY()).append(
                                ", Z=").append(pos.getZ()).append(", A=").append(pos.getADeg()).append(", B=").append(
                                pos.getBDeg()).append(
                                ", C=").append(pos.getCDeg()).append("\n");
                    }
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
}

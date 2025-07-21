package hartu.robot.commands;

import hartu.protocols.constants.ActionTypes;
import hartu.protocols.constants.CommandCategory;
import hartu.robot.commands.io.IoCommandData;
import hartu.robot.commands.positions.AxisPosition;
import hartu.robot.commands.positions.CartesianPosition;
import hartu.robot.commands.positions.PositionClass; // Import PositionClass

import java.util.List;

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
        return new ParsedCommand(actionType, id, actionType.getCategory(), axisTargetPoints, null, motionParameters, null, null);
    }

    public static ParsedCommand forCartesianMovement(ActionTypes actionType, String id, List<CartesianPosition> cartesianTargetPoints, MotionParameters motionParameters)
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

    /**
     * Returns a MovementTargets object containing the Class type of the target points
     * and the list of those points for movement commands.
     *
     * @return A MovementTargets object if it's a movement command with points, otherwise null.
     */
    public MovementTargets<? extends PositionClass> getMovementTargetPoints() { // Updated return type
        if (isMovementCommand()) {
            if (axisTargetPoints != null) {
                return new MovementTargets<>(AxisPosition.class, axisTargetPoints);
            } else if (cartesianTargetPoints != null) {
                return new MovementTargets<>(CartesianPosition.class, cartesianTargetPoints);
            }
        }
        return null; // Not a movement command or no points
    }

    /**
     * Returns the first target position as a generic PositionClass object.
     * This is useful when you need to access common properties or methods
     * of any position type without knowing its specific concrete class upfront.
     *
     * @return The first PositionClass object in the target list, or null if
     * it's not a movement command or has no target points.
     */
    public PositionClass getFirstMovementPosition() {
        MovementTargets<? extends PositionClass> movementTargets = getMovementTargetPoints();
        if (movementTargets != null && movementTargets.getTargets() != null && !movementTargets.getTargets().isEmpty()) {
            return movementTargets.getTargets().get(0);
        }
        return null;
    }

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
            MovementTargets<? extends PositionClass> movementTargets = getMovementTargetPoints();
            if (movementTargets != null && movementTargets.getTargets() != null && !movementTargets.getTargets().isEmpty()) {
                if (movementTargets.getTargetClass() == AxisPosition.class) {
                    List<AxisPosition> axisPoints = (List<AxisPosition>) movementTargets.getTargets();
                    sb.append("  Axis Target Points (").append(axisPoints.size()).append("):\n");
                    for (int i = 0; i < axisPoints.size(); i++)
                    {
                        AxisPosition pos = axisPoints.get(i);
                        sb.append("    Point ").append(i + 1).append(": J1=").append(pos.getJ1()).append(", J2=").append(pos.getJ2()).append(
                                ", J3=").append(pos.getJ3()).append(", J4=").append(pos.getJ4()).append(", J5=").append(pos.getJ5()).append(
                                ", J6=").append(pos.getJ6()).append(", J7=").append(pos.getJ7()).append("\n");
                    }
                } else if (movementTargets.getTargetClass() == CartesianPosition.class) {
                    List<CartesianPosition> cartesianPoints = (List<CartesianPosition>) movementTargets.getTargets();
                    sb.append("  Cartesian Target Points (").append(cartesianPoints.size()).append("):\n");
                    for (int i = 0; i < cartesianPoints.size(); i++)
                    {
                        CartesianPosition pos = cartesianPoints.get(i);
                        sb.append("    Point ").append(i + 1).append(": X=").append(pos.getX()).append(", Y=").append(pos.getY()).append(
                                ", Z=").append(pos.getZ()).append(", A=").append(pos.getADeg()).append(", B=").append(pos.getBDeg()).append(
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
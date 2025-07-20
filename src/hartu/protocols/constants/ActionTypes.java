package hartu.protocols.constants;

/**
 * Defines the types of actions a robot can perform.
 * Includes a constant for distinguishing program calls from direct commands.
 */
public enum ActionTypes
{

    PTP_AXIS(0),
    PTP_FRAME(1),
    LIN_AXIS(2),
    LIN_FRAME(3),
    CIRC_AXIS(4),
    CIRC_FRAME(5),
    PTP_AXIS_C(6),
    PTP_FRAME_C(7),
    LIN_FRAME_C(8),
    ACTIVATE_IO(9),
    LIN_REL_TOOL(10),
    LIN_REL_BASE(11),
    UNKNOWN(999);

    public static final int PROGRAM_CALL_OFFSET = 100;

    private final int value;

    ActionTypes(int value)
    {
        this.value = value;
    }

    public static ActionTypes fromValue(int value)
    {
        for (ActionTypes type : ActionTypes.values())
        {
            if (type.value == value)
            {
                return type;
            }
        }
        return UNKNOWN;
    }

    public int getValue()
    {
        return value;
    }

    /**
     * Returns the high-level category for this ActionType.
     * This centralizes the mapping from specific actions to broader command types.
     * @return The CommandCategory for this action.
     */
    public CommandCategory getCategory() {
        // Use a switch statement for clear mapping
        switch (this) {
            case PTP_AXIS:
            case PTP_FRAME:
            case LIN_AXIS:
            case LIN_FRAME:
            case CIRC_AXIS:
            case CIRC_FRAME:
            case PTP_AXIS_C:
            case PTP_FRAME_C:
            case LIN_FRAME_C:
            case LIN_REL_TOOL:
            case LIN_REL_BASE:
                return CommandCategory.MOVEMENT;
            case ACTIVATE_IO:
                return CommandCategory.IO;
            default:
                // For UNKNOWN or actions within the PROGRAM_CALL_OFFSET range
                if (this.value > PROGRAM_CALL_OFFSET && this != UNKNOWN) {
                    return CommandCategory.PROGRAM_CALL;
                }
                return CommandCategory.UNKNOWN;
        }
    }
}

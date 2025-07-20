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
}
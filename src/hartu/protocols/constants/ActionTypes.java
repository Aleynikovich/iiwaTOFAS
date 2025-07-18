package hartu.protocols.constants;

/**
 * Defines the types of actions a robot can perform.
 * Includes a constant for distinguishing program calls from direct commands.
 */
public enum ActionTypes {
    // Movement Actions
    PTP_AXIS(0),
    PTP_FRAME(1),
    LIN_AXIS(2),
    LIN_FRAME(3),
    CIRC_AXIS(4),
    CIRC_FRAME(5),
    PTP_AXIS_C(6), // Continuous PTP Axis
    PTP_FRAME_C(7), // Continuous PTP Frame
    LIN_FRAME_C(8), // Continuous LIN Frame

    // IO Actions
    ACTIVATE_IO(9),

    // Relative Linear Movements
    LIN_REL_TOOL(10), // Linear Relative to Tool
    LIN_REL_BASE(11), // Linear Relative to Base

    // Other Actions (e.g., Program Control)
    UNKNOWN(999);

    // Constant to differentiate program calls from direct commands
    // If an action value is > PROGRAM_CALL_OFFSET, it's considered a program call
    public static final int PROGRAM_CALL_OFFSET = 100;

    private final int value;

    ActionTypes(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ActionTypes fromValue(int value) {
        for (ActionTypes type : ActionTypes.values()) {
            if (type.value == value) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
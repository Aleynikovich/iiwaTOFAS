package hartu.protocols.constants;

/**
 * Defines the types of actions a robot can perform.
 * Each enum constant is a rich object holding multiple properties like its
 * integer value, motion type, and command category.
 */
public enum ActionTypes {

    // --- Point-to-Point (PTP) Movements ---
    PTP_AXIS(1, true, false, false, MovementType.PTP),
    PTP_FRAME(2, false, true, false, MovementType.PTP),
    PTP_AXIS_C(7, true, false, true, MovementType.PTP),
    PTP_FRAME_C(8, false, true, true, MovementType.PTP),

    // --- Linear (LIN) Movements ---
    LIN_AXIS(3, true, false, false, MovementType.LIN),
    LIN_FRAME(4, false, true, false, MovementType.LIN),
    LIN_FRAME_C(9, false, true, true, MovementType.LIN),
    LIN_REL_TOOL(11, false, false, false, MovementType.LIN),
    LIN_REL_BASE(12, false, false, false, MovementType.LIN),

    // --- Circular (CIRC) Movements ---
    CIRC_AXIS(5, true, false, false, MovementType.CIRC),
    CIRC_FRAME(6, false, true, false, MovementType.CIRC),

    // --- Other Commands ---
    ACTIVATE_IO(10, false, false, false, MovementType.UNKNOWN),

    // --- Default and Program Call ---
    UNKNOWN(999, false, false, false, MovementType.UNKNOWN);

    /**
     * A constant for distinguishing program calls from direct commands.
     * Program calls are expected to have a value greater than this offset.
     */
    public static final int PROGRAM_CALL_OFFSET = 100;

    private final int value;
    private final boolean isJointMotion;
    private final boolean isCartesianMotion;
    private final boolean isContinuousMovement;
    private final MovementType movementType;

    /**
     * Constructs an ActionTypes enum constant with all its properties.
     *
     * @param value The unique integer value for the action.
     * @param isJointMotion True if the command involves joint-space movement.
     * @param isCartesianMotion True if the command involves cartesian-space movement.
     * @param isContinuousMovement True if the command is for continuous motion.
     * @param movementType The sub-category of the movement (PTP, LIN, CIRC).
     */
    ActionTypes(int value, boolean isJointMotion, boolean isCartesianMotion, boolean isContinuousMovement, MovementType movementType) {
        this.value = value;
        this.isJointMotion = isJointMotion;
        this.isCartesianMotion = isCartesianMotion;
        this.isContinuousMovement = isContinuousMovement;
        this.movementType = movementType;
    }

    /**
     * Retrieves the ActionType constant for a given integer value.
     * This is useful for deserializing commands from a protocol.
     *
     * @param value The integer representation of the action type.
     * @return The corresponding ActionType, or {@link #UNKNOWN} if no match is found.
     */
    public static ActionTypes fromValue(int value) {
        for (ActionTypes type : ActionTypes.values()) {
            if (type.value == value) {
                return type;
            }
        }
        return UNKNOWN;
    }

    // --- Public Getter Methods ---

    public int getValue() {
        return value;
    }

    public boolean isJointMotion() {
        return isJointMotion;
    }

    public boolean isCartesianMotion() {
        return isCartesianMotion;
    }

    public boolean isContinuousMovement() {
        return isContinuousMovement;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    /**
     * Returns the high-level category for this ActionType.
     * This centralizes the mapping from specific actions to broader command types,
     * such as Movement, IO, or Program Calls.
     *
     * @return The CommandCategory for this action.
     */
    public CommandCategory getCategory() {
        // The switch statement is a clean and type-safe way to map actions to categories.
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
                if (this.value >= PROGRAM_CALL_OFFSET) {
                    return CommandCategory.PROGRAM_CALL;
                }
                return CommandCategory.UNKNOWN;
        }
    }
}
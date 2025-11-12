package hartu.protocols.constants;

public enum ActionTypes {

    // --- Point-to-Point (PTP) Movements ---
    PTP_AXIS(0, true, false, false, MovementType.PTP),
    PTP_FRAME(1, false, true, false, MovementType.PTP),
    LIN_AXIS(2, true, false, false, MovementType.LIN),
    LIN_FRAME(3, false, true, false, MovementType.LIN),
    CIRC_AXIS(4, true, false, false, MovementType.CIRC),
    CIRC_FRAME(5, false, true, false, MovementType.CIRC),
    PTP_AXIS_C(6, true, false, true, MovementType.PTP),
    PTP_FRAME_C(7, false, true, true, MovementType.PTP),
    LIN_FRAME_C(8, false, true, true, MovementType.LIN),

    // --- IO Commands ---
    ACTIVATE_IO(9, false, false, false, MovementType.UNKNOWN),
    LIN_FRAME_REL_TOOL(10, false, true, false, MovementType.LIN),
    LIN_FRAME_REL_BASE(11, false, true, false, MovementType.LIN),
    DIGITAL_INPUT(12, false, false, false, MovementType.UNKNOWN),
    ANALOG_INPUT(13, false, false, false, MovementType.UNKNOWN),

    // --- Default and Program Call ---
    // Program call numbers are added to this offset. E.g., PROGRAM_CALL_1 is 100
    PROGRAM_CALL_OFFSET(100, false, false, false, MovementType.UNKNOWN),
    UNKNOWN(999, false, false, false, MovementType.UNKNOWN);

    private final int value;
    private final boolean isJointMotion;
    private final boolean isCartesianMotion;
    private final boolean isContinuousMovement;
    private final MovementType movementType;

    ActionTypes(int value, boolean isJointMotion, boolean isCartesianMotion, boolean isContinuousMovement, MovementType movementType) {
        this.value = value;
        this.isJointMotion = isJointMotion;
        this.isCartesianMotion = isCartesianMotion;
        this.isContinuousMovement = isContinuousMovement;
        this.movementType = movementType;
    }

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

    public CommandCategory getCategory() {
        if (value >= 0 && value <= 8) {
            return CommandCategory.MOVEMENT;
        }
        if (value >= 9 && value <= 13) {
            return CommandCategory.IO;
        }
        if (value >= PROGRAM_CALL_OFFSET.value) {
            return CommandCategory.PROGRAM_CALL;
        }
        return CommandCategory.UNKNOWN;
    }

    public static ActionTypes fromValue(int value) {
        for (ActionTypes type : ActionTypes.values()) {
            if (type.value == value) {
                return type;
            }
        }
        // Handle program calls
        if (value >= PROGRAM_CALL_OFFSET.value) {
            return ActionTypes.PROGRAM_CALL_OFFSET;
        }
        return UNKNOWN;
    }
}
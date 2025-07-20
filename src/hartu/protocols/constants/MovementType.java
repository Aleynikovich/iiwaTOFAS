package hartu.protocols.constants;

public enum MovementType
{
    PTP_AXIS(false, false, true, false),
    PTP_FRAME(false, false, false, true),
    LIN_AXIS(false, false, true, false),
    LIN_FRAME(false, false, false, true),
    CIRC_AXIS(false, false, true, false),
    CIRC_FRAME(false, false, false, true),
    PTP_AXIS_C(true, false, true, false),
    PTP_FRAME_C(true, false, false, true),
    LIN_FRAME_C(true, false, false, true),
    LIN_REL_TOOL(false, true, false, true),
    LIN_REL_BASE(false, true, false, true),
    UNKNOWN(false, false, false, false);

    private final boolean continuous;
    private final boolean relative;
    private final boolean axisMotion;
    private final boolean cartesianMotion;

    MovementType(boolean continuous, boolean relative, boolean axisMotion, boolean cartesianMotion)
    {
        this.continuous = continuous;
        this.relative = relative;
        this.axisMotion = axisMotion;
        this.cartesianMotion = cartesianMotion;
    }

    public static MovementType fromActionType(ActionTypes actionType)
    {
        switch (actionType)
        {
            case PTP_AXIS:
                return PTP_AXIS;
            case PTP_FRAME:
                return PTP_FRAME;
            case LIN_AXIS:
                return LIN_AXIS;
            case LIN_FRAME:
                return LIN_FRAME;
            case CIRC_AXIS:
                return CIRC_AXIS;
            case CIRC_FRAME:
                return CIRC_FRAME;
            case PTP_AXIS_C:
                return PTP_AXIS_C;
            case PTP_FRAME_C:
                return PTP_FRAME_C;
            case LIN_FRAME_C:
                return LIN_FRAME_C;
            case LIN_REL_TOOL:
                return LIN_REL_TOOL;
            case LIN_REL_BASE:
                return LIN_REL_BASE;
            default:
                return UNKNOWN;
        }
    }

    public boolean isContinuous()
    {
        return continuous;
    }

    public boolean isRelative()
    {
        return relative;
    }

    public boolean isAxisMotion()
    {
        return axisMotion;
    }

    public boolean isCartesianMotion()
    {
        return cartesianMotion;
    }
}
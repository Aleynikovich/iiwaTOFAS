package hartu.protocols.constants;

public final class ActionTypes {
    public static final int PTP_AXIS = 0;
    public static final int PTP_FRAME = 1;
    public static final int LIN_AXIS = 2;
    public static final int LIN_FRAME = 3;
    public static final int CIRC_AXIS = 4;
    public static final int CIRC_FRAME = 5;
    public static final int PTP_AXIS_C = 6;
    public static final int PTP_FRAME_C = 7;
    public static final int LIN_FRAME_C = 8;
    public static final int ACTIVATE_IO = 9;
    public static final int LIN_REL_TOOL = 10;
    public static final int LIN_REL_BASE = 11;

    public static final int UNKNOWN = 999;

    private ActionTypes() {
        // Private constructor to prevent instantiation
    }
}
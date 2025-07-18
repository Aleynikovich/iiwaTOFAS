package hartu.protocols.constants;

/**
 * Defines the constant indices for different parts within a raw message array.
 * This improves readability and maintainability by replacing "magic numbers"
 * with meaningful enum names.
 */
public enum MessagePartIndex {
    ACTION_TYPE(0),
    NUM_POINTS(1),
    TARGET_POINTS(2),
    IO_POINT(3),
    IO_PIN(4),
    IO_STATE(5),
    TOOL(6),
    BASE(7),
    SPEED_OVERRIDE(8),
    ID(9);

    private final int index;

    MessagePartIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
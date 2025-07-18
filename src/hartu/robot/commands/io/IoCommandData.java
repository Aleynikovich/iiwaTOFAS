package hartu.robot.commands.io;

public class IoCommandData {
    private final int ioPoint;
    private final int ioPin;
    private final boolean ioState;

    public IoCommandData(int ioPoint, int ioPin, boolean ioState) {
        this.ioPoint = ioPoint;
        this.ioPin = ioPin;
        this.ioState = ioState;
    }

    public int getIoPoint() {
        return ioPoint;
    }

    public int getIoPin() {
        return ioPin;
    }

    public boolean getIoState() {
        return ioState;
    }
}

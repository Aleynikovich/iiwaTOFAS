package hartu.protocols.definitions.parseddata;

public class IOParsedData extends ParsedSpecificData {
    public final int ioPoint;
    public final int ioPin;
    public final boolean ioState;

    public IOParsedData(int ioPoint, int ioPin, boolean ioState) {
        this.ioPoint = ioPoint;
        this.ioPin = ioPin;
        this.ioState = ioState;
    }

    @Override
    public String toString() {
        return "IOParsedData [ioPoint=" + ioPoint + ", ioPin=" + ioPin + ", ioState=" + ioState + "]";
    }
}
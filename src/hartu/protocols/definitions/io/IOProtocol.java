package hartu.protocols.definitions.io;

import hartu.protocols.definitions.MessageProtocol;
import hartu.protocols.definitions.parseddata.IOParsedData;
import hartu.protocols.definitions.parseddata.ParsedSpecificData;

public abstract class IOProtocol extends MessageProtocol {

    public IOProtocol(String[] rawParts) throws NumberFormatException {
        super(rawParts);
    }

    @Override
    protected ParsedSpecificData parseSpecificFields(String[] rawParts) throws NumberFormatException {
        int ioPoint = Integer.parseInt(getPart(rawParts, 3, "0"));
        int ioPin = Integer.parseInt(getPart(rawParts, 4, "0"));
        boolean ioState = Boolean.parseBoolean(getPart(rawParts, 5, "false"));
        return new IOParsedData(ioPoint, ioPin, ioState);
    }

    // Public getters to access the specific data, casting from specificData
    public int getIoPoint() {
        return ((IOParsedData) specificData).ioPoint;
    }

    public int getIoPin() {
        return ((IOParsedData) specificData).ioPin;
    }

    public boolean getIoState() {
        return ((IOParsedData) specificData).ioState;
    }

    @Override
    public String toString() {
        return "IOProtocol [id=" + id + ", actionType=" + actionType.name() + ", programCall=" + programCall +
                ", ioPoint=" + getIoPoint() + ", ioPin=" + getIoPin() + ", ioState=" + getIoState() + "]";
    }
}
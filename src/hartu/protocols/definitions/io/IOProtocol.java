package hartu.protocols.definitions.io;

import hartu.protocols.constants.MessagePartIndex; // Import the new enum
import hartu.protocols.definitions.MessageProtocol;
import hartu.protocols.definitions.parseddata.IOParsedData;
import hartu.protocols.definitions.parseddata.ParsedSpecificData;

public abstract class IOProtocol extends MessageProtocol {

    public IOProtocol(String[] rawParts) throws NumberFormatException {
        super(rawParts);
    }

    @Override
    protected ParsedSpecificData parseSpecificFields(String[] rawParts) throws NumberFormatException {
        int ioPoint = Integer.parseInt(getPart(rawParts, MessagePartIndex.IO_POINT.getIndex(), "0"));
        int ioPin = Integer.parseInt(getPart(rawParts, MessagePartIndex.IO_PIN.getIndex(), "0"));
        boolean ioState = Boolean.parseBoolean(getPart(rawParts, MessagePartIndex.IO_STATE.getIndex(), "false"));
        return new IOParsedData(ioPoint, ioPin, ioState);
    }

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
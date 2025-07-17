package hartu.protocols.definitions.io;

import hartu.protocols.definitions.MessageProtocol;

public abstract class IOProtocol extends MessageProtocol {
    public int ioPoint;
    public int ioPin;
    public boolean ioState;

    public IOProtocol(String[] rawParts) throws NumberFormatException {
        super(rawParts);
    }

    @Override
    protected void parseSpecificFields(String[] rawParts) throws NumberFormatException {
        this.ioPoint = Integer.parseInt(getPart(rawParts, 3, "0"));
        this.ioPin = Integer.parseInt(getPart(rawParts, 4, "0"));
        this.ioState = Boolean.parseBoolean(getPart(rawParts, 5, "false"));
    }

    @Override
    public String toString() {
        return "IOProtocol [id=" + id + ", actionType=" + actionType + ", programCall=" + programCall +
                ", ioPoint=" + ioPoint + ", ioPin=" + ioPin + ", ioState=" + ioState + "]";
    }
}
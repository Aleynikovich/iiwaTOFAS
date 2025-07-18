package hartu.protocols.definitions.io;

import hartu.protocols.constants.ActionTypes;

public class IOAction extends IOProtocol {

    public IOAction(String[] rawParts) throws NumberFormatException {
        super(rawParts);
        // This check ensures that an IOAction instance specifically represents an ACTIVATE_IO command.
        // If other IO-related action types were introduced, new concrete classes extending IOProtocol
        // would be created for them, or this check would be modified.
        if (this.actionType != ActionTypes.ACTIVATE_IO) {
            throw new IllegalArgumentException("IOAction created with incorrect action type: " + this.actionType.name());
        }
    }

    @Override
    public String toString() {
        return "IOAction [id=" + id + ", actionType=" + actionType.name() + ", programCall=" + programCall +
                ", ioPoint=" + getIoPoint() + ", ioPin=" + getIoPin() + ", ioState=" + getIoState() + "]";
    }
}
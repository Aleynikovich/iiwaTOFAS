package hartu.protocols.definitions.io;

import hartu.protocols.constants.ActionTypes;

public class IOAction extends IOProtocol {

    public IOAction(String[] rawParts) throws NumberFormatException {
        super(rawParts);
        if (this.actionType != ActionTypes.ACTIVATE_IO) {
            throw new IllegalArgumentException("IOAction created with incorrect action type: " + this.actionType);
        }
    }

    @Override
    public String toString() {
        return "IOAction [id=" + id + ", actionType=" + actionType + // Removed programCall from here
                ", ioPoint=" + ioPoint + ", ioPin=" + ioPin + ", ioState=" + ioState + "]";
    }
}
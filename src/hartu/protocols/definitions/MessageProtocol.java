package hartu.protocols.definitions;

import hartu.protocols.constants.ActionTypes;

public abstract class MessageProtocol
{
    public final int actionType;
    public final boolean programCall;
    public final String id;

    protected String getPart(String[] parts, int index, String defaultValue) {
        return (index < parts.length && parts[index] != null && !parts[index].trim().isEmpty()) ? parts[index].trim() : defaultValue;
    }

    public MessageProtocol(String[] rawParts) throws NumberFormatException {
        int rawActionType = Integer.parseInt(getPart(rawParts, 0, String.valueOf(ActionTypes.UNKNOWN)));
        this.programCall = rawActionType > 100;
        this.actionType = this.programCall ? rawActionType - 100 : rawActionType;
        this.id = getPart(rawParts, 9, "N/A");

        parseSpecificFields(rawParts);
    }

    protected abstract void parseSpecificFields(String[] rawParts) throws NumberFormatException;

    @Override
    public abstract String toString();
}


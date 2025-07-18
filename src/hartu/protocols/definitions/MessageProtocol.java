package hartu.protocols.definitions;

import hartu.protocols.constants.ActionTypes; // Import ActionTypes for the constant
import hartu.protocols.constants.MessagePartIndex;
import hartu.protocols.definitions.parseddata.ParsedSpecificData;

public abstract class MessageProtocol
{
    public final ActionTypes actionType;
    public final boolean programCall;
    public final String id;
    public final ParsedSpecificData specificData;

    protected String getPart(String[] parts, int index, String defaultValue) {
        return (index < parts.length && parts[index] != null && !parts[index].trim().isEmpty()) ? parts[index].trim() : defaultValue;
    }

    public MessageProtocol(String[] rawParts) throws NumberFormatException {
        int rawActionIntValue = Integer.parseInt(getPart(rawParts, MessagePartIndex.ACTION_TYPE.getIndex(), String.valueOf(ActionTypes.UNKNOWN.getValue())));

        // Use the constant PROGRAM_CALL_OFFSET
        this.programCall = rawActionIntValue > ActionTypes.PROGRAM_CALL_OFFSET;
        this.actionType = ActionTypes.fromValue(this.programCall ? rawActionIntValue - ActionTypes.PROGRAM_CALL_OFFSET : rawActionIntValue);

        this.id = getPart(rawParts, MessagePartIndex.ID.getIndex(), "N/A");

        this.specificData = parseSpecificFields(rawParts);
    }

    protected abstract ParsedSpecificData parseSpecificFields(String[] rawParts) throws NumberFormatException;

    @Override
    public abstract String toString();
}
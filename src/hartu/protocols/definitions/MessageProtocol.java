package hartu.protocols.definitions;

import hartu.protocols.constants.ActionTypes;
import hartu.protocols.definitions.parseddata.ParsedSpecificData; // Import the new base parsed data class

public abstract class MessageProtocol
{
    public final ActionTypes actionType;
    public final boolean programCall;
    public final String id;
    public final ParsedSpecificData specificData; // New final field for parsed specific data

    protected String getPart(String[] parts, int index, String defaultValue) {
        return (index < parts.length && parts[index] != null && !parts[index].trim().isEmpty()) ? parts[index].trim() : defaultValue;
    }

    public MessageProtocol(String[] rawParts) throws NumberFormatException {
        int rawActionIntValue = Integer.parseInt(getPart(rawParts, 0, String.valueOf(ActionTypes.UNKNOWN.getValue())));
        this.programCall = rawActionIntValue > 100;
        this.actionType = ActionTypes.fromValue(this.programCall ? rawActionIntValue - 100 : rawActionIntValue);
        this.id = getPart(rawParts, 9, "N/A");

        // Now, parseSpecificFields returns the data, which is then assigned to the final field
        this.specificData = parseSpecificFields(rawParts);
    }

    // This method now returns a ParsedSpecificData object
    protected abstract ParsedSpecificData parseSpecificFields(String[] rawParts) throws NumberFormatException;

    @Override
    public abstract String toString();
}
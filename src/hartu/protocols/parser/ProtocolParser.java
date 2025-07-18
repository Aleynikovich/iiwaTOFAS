package hartu.protocols.parser;

import hartu.protocols.constants.ActionTypes; // Import ActionTypes for the constant
import hartu.protocols.constants.MessagePartIndex;
import hartu.protocols.definitions.MessageProtocol;
import hartu.protocols.definitions.io.IOAction;
import hartu.protocols.definitions.movement.MovementMessage;
import hartu.communication.client.LoggerClient;

public class ProtocolParser {

    private static LoggerClient loggerClient;

    public static void setLoggerClient(LoggerClient client) {
        ProtocolParser.loggerClient = client;
    }

    public static MessageProtocol parseMessage(String[] rawParts) throws IllegalArgumentException, NumberFormatException {
        if (rawParts == null || rawParts.length == 0) {
            if (loggerClient != null) {
                loggerClient.sendMessage("ERROR: Raw message parts cannot be null or empty.");
            }
            throw new IllegalArgumentException("Raw message parts cannot be null or empty.");
        }

        int rawActionIntValue = Integer.parseInt(rawParts[MessagePartIndex.ACTION_TYPE.getIndex()].trim());

        boolean isProgramCall = rawActionIntValue > ActionTypes.PROGRAM_CALL_OFFSET;
        ActionTypes actionType = ActionTypes.fromValue(isProgramCall ? rawActionIntValue - ActionTypes.PROGRAM_CALL_OFFSET : rawActionIntValue);

        MessageProtocol parsedMessage;
        switch (actionType) {
            case ACTIVATE_IO:
                parsedMessage = new IOAction(rawParts);
                break;
            case PTP_AXIS:
            case PTP_FRAME:
            case LIN_AXIS:
            case LIN_FRAME:
            case CIRC_AXIS:
            case CIRC_FRAME:
            case PTP_AXIS_C:
            case PTP_FRAME_C:
            case LIN_FRAME_C:
            case LIN_REL_TOOL:
            case LIN_REL_BASE:
                parsedMessage = new MovementMessage(rawParts);
                break;
            case UNKNOWN:
            default:
                String errorMessage = "ERROR: Unknown or unsupported action type: " + actionType.name() + " (raw value: " + rawActionIntValue + ")";
                if (loggerClient != null) {
                    loggerClient.sendMessage(errorMessage);
                }
                throw new IllegalArgumentException(errorMessage);
        }

        if (loggerClient != null) {
            loggerClient.sendMessage("Parsed Message: " + parsedMessage.toString());
        }

        return parsedMessage;
    }
}
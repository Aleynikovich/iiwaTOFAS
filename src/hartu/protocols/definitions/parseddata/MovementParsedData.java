package hartu.protocols.definitions.parseddata;

import hartu.protocols.definitions.coordinates.TargetPosition;

public class MovementParsedData extends ParsedSpecificData {
    public final TargetPosition targetPosition;
    public final double speedOverride;
    public final String toolName;
    public final String baseFramePath; // Path to the base frame for the movement

    public MovementParsedData(TargetPosition targetPosition, double speedOverride, String toolName, String baseFramePath) {
        this.targetPosition = targetPosition;
        this.speedOverride = speedOverride;
        this.toolName = toolName;
        this.baseFramePath = baseFramePath;
    }

    @Override
    public String toString() {
        return "MovementParsedData [targetPosition=" + (targetPosition != null ? targetPosition.toString() : "N/A") +
                ", speedOverride=" + speedOverride +
                ", toolName=" + (toolName != null ? toolName : "N/A") +
                ", baseFramePath=" + (baseFramePath != null ? baseFramePath : "N/A") + "]";
    }
}
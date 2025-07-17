package hartu.protocols.definitions.parseddata;

import hartu.protocols.definitions.coordinates.TargetPosition;

public class MovementParsedData extends ParsedSpecificData {
    public final TargetPosition targetPosition;
    public final double speedOverride;

    public MovementParsedData(TargetPosition targetPosition, double speedOverride) {
        this.targetPosition = targetPosition;
        this.speedOverride = speedOverride;
    }

    @Override
    public String toString() {
        return "MovementParsedData [targetPosition=" + (targetPosition != null ? targetPosition.toString() : "N/A") +
                ", speedOverride=" + speedOverride + "]";
    }
}
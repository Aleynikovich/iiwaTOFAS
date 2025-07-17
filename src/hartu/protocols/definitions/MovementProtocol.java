package hartu.protocols.definitions;

import hartu.protocols.constants.MovementType;
import hartu.protocols.definitions.coordinates.AxisPosition;
import hartu.protocols.definitions.coordinates.FramePosition;
import hartu.protocols.definitions.coordinates.TargetPosition;
import hartu.protocols.definitions.parseddata.MovementParsedData;
import hartu.protocols.definitions.parseddata.ParsedSpecificData;

public abstract class MovementProtocol extends MessageProtocol {
    public final MovementType movementType;

    public MovementProtocol(String[] rawParts) throws NumberFormatException {
        super(rawParts);
        this.movementType = MovementType.fromActionType(this.actionType);
    }

    @Override
    protected ParsedSpecificData parseSpecificFields(String[] rawParts) throws NumberFormatException {
        TargetPosition parsedTargetPosition = null;

        if (movementType.isAxisMotion()) {
            String axisValuesString = getPart(rawParts, 2, "");
            String[] axisParts = axisValuesString.split(",");
            double[] axisValues = new double[axisParts.length];
            for (int i = 0; i < axisParts.length; i++) {
                axisValues[i] = Double.parseDouble(axisParts[i].trim());
            }
            parsedTargetPosition = new AxisPosition(axisValues);
        } else if (movementType.isFrameMotion()) {
            String frameValuesString = getPart(rawParts, 2, "");
            String[] frameParts = frameValuesString.split(",");
            if (frameParts.length != 6) {
                throw new IllegalArgumentException("Frame position string must contain 6 values (X,Y,Z,A,B,C). Found: " + frameParts.length);
            }
            double x = Double.parseDouble(frameParts[0].trim());
            double y = Double.parseDouble(frameParts[1].trim());
            double z = Double.parseDouble(frameParts[2].trim());
            double a = Double.parseDouble(frameParts[3].trim());
            double b = Double.parseDouble(frameParts[4].trim());
            double c = Double.parseDouble(frameParts[5].trim());
            parsedTargetPosition = new FramePosition(x, y, z, a, b, c);
        }

        double parsedSpeedOverride = Double.parseDouble(getPart(rawParts, 8, "100.0")) / 100.0;

        return new MovementParsedData(parsedTargetPosition, parsedSpeedOverride);
    }

    // Public getters to access the specific data, casting from specificData
    public TargetPosition getTargetPosition() {
        return ((MovementParsedData) specificData).targetPosition;
    }

    public double getSpeedOverride() {
        return ((MovementParsedData) specificData).speedOverride;
    }

    public boolean isContinuous() { return this.movementType.isContinuous(); }
    public boolean isRelative() { return this.movementType.isRelative(); }
    public boolean isAxisMotion() { return this.movementType.isAxisMotion(); }
    public boolean isFrameMotion() { return this.movementType.isFrameMotion(); }

    @Override
    public String toString() {
        return "MovementProtocol [id=" + id + ", actionType=" + actionType.name() +
                ", movementType=" + movementType.name() +
                ", targetPosition=" + (getTargetPosition() != null ? getTargetPosition().toString() : "N/A") +
                ", speedOverride=" + getSpeedOverride() + "]";
    }
}
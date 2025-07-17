package hartu.protocols.definitions.movement;

import hartu.protocols.definitions.MessageProtocol;

public abstract class MovementProtocol extends MessageProtocol
{
    public  int numPoints;
    public  String targetPoints;
    public  double speedOverride;

    public MovementProtocol(String[] rawParts) throws NumberFormatException {
        super(rawParts);
    }

    @Override
    protected void parseSpecificFields(String[] rawParts) throws NumberFormatException {
        this.numPoints = Integer.parseInt(getPart(rawParts, 1, "0"));
        this.targetPoints = getPart(rawParts, 2, "");
        this.speedOverride = Double.parseDouble(getPart(rawParts, 8, "100.0")) / 100.0;
    }

    public abstract boolean isContinuous();
    public abstract boolean isRelative();
    public abstract boolean isAxisMotion();
    public abstract boolean isFrameMotion();

    @Override
    public String toString() {
        return "MovementProtocol [id=" + id + ", actionType=" + actionType + ", programCall=" + programCall +
                ", numPoints=" + numPoints + ", targetPoints='" + targetPoints +
                "', speedOverride=" + speedOverride +
                ", continuous=" + isContinuous() + ", relative=" + isRelative() +
                ", axis=" + isAxisMotion() + ", frame=" + isFrameMotion() + "]";
    }
}
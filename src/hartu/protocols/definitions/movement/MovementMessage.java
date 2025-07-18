package hartu.protocols.definitions.movement;

import hartu.protocols.definitions.MovementProtocol;

public class MovementMessage extends MovementProtocol {
    public MovementMessage(String[] rawParts) throws NumberFormatException {
        super(rawParts);
    }

    @Override
    public String toString() {
        return "MovementMessage [id=" + id + ", actionType=" + actionType.name() +
                ", movementType=" + movementType.name() +
                ", targetPosition=" + (getTargetPosition() != null ? getTargetPosition().toString() : "N/A") +
                ", speedOverride=" + getSpeedOverride() + "]";
    }
}
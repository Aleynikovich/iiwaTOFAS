package hartu.protocols.constants;

/**
 * Defines the sub-types of movement commands (e.g., Point-to-Point, Linear, Circular).
 * This provides a more granular classification than CommandCategory.MOVEMENT.
 */
public enum MovementType {
    PTP,
    LIN,
    CIRC,
    UNKNOWN
}
package hartu.robot.executor.kitting;

import hartu.protocols.constants.WorkpieceType;

/**
 * Represents a single position in a kitting box.
 * Each position can hold one workpiece of a specific type.
 */
public class KittingPosition
{
    private final String frameNameApproach;
    private final String frameNamePlace;
    private final WorkpieceType allowedWorkpieceType;
    private boolean occupied;

    /**
     * Creates a new kitting position.
     *
     * @param frameNameApproach    The name of the approach frame (e.g., "PlaceAxis1_1")
     * @param frameNamePlace       The name of the place/drop frame (e.g., "PlaceAxis1_2")
     * @param allowedWorkpieceType The type of workpiece this position can hold
     */
    public KittingPosition(String frameNameApproach, String frameNamePlace, WorkpieceType allowedWorkpieceType)
    {
        this.frameNameApproach = frameNameApproach;
        this.frameNamePlace = frameNamePlace;
        this.allowedWorkpieceType = allowedWorkpieceType;
        this.occupied = false;
    }

    /**
     * Gets the approach frame name.
     *
     * @return The approach frame name
     */
    public String getFrameNameApproach()
    {
        return frameNameApproach;
    }

    /**
     * Gets the place/drop frame name.
     *
     * @return The place frame name
     */
    public String getFrameNamePlace()
    {
        return frameNamePlace;
    }

    /**
     * Gets the allowed workpiece type for this position.
     *
     * @return The WorkpieceType this position can hold
     */
    public WorkpieceType getAllowedWorkpieceType()
    {
        return allowedWorkpieceType;
    }

    /**
     * Checks if this position is occupied.
     *
     * @return True if occupied, false if available
     */
    public boolean isOccupied()
    {
        return occupied;
    }

    /**
     * Marks this position as occupied.
     */
    public void setOccupied()
    {
        this.occupied = true;
    }

    /**
     * Marks this position as available.
     */
    public void setAvailable()
    {
        this.occupied = false;
    }

    /**
     * Checks if this position can accept the given workpiece type.
     *
     * @param workpieceType The workpiece type to check
     * @return True if this position accepts the workpiece type and is not occupied
     */
    public boolean canAccept(WorkpieceType workpieceType)
    {
        return !occupied && allowedWorkpieceType == workpieceType;
    }

    @Override
    public String toString()
    {
        return "KittingPosition{" +
                "approach='" + frameNameApproach + '\'' +
                ", place='" + frameNamePlace + '\'' +
                ", type=" + allowedWorkpieceType.getName() +
                ", occupied=" + occupied +
                '}';
    }
}

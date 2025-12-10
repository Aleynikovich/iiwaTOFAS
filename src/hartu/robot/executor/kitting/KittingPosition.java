package hartu.robot.executor.kitting;

import hartu.protocols.constants.WorkpieceType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a single position in a kitting box.
 * Each position can hold one workpiece of a specific type.
 * Positions can have a flexible number of frames to define the placement trajectory.
 */
public class KittingPosition
{
    private final List<String> frameNames;
    private final WorkpieceType allowedWorkpieceType;
    private boolean occupied;

    /**
     * Creates a new kitting position with a flexible trajectory.
     *
     * @param frameNames           List of frame names defining the placement trajectory (e.g., ["PlaceAxis1_1", "PlaceAxis1_2"])
     * @param allowedWorkpieceType The type of workpiece this position can hold
     */
    public KittingPosition(List<String> frameNames, WorkpieceType allowedWorkpieceType)
    {
        if (frameNames == null || frameNames.isEmpty())
        {
            throw new IllegalArgumentException("Frame names list cannot be null or empty");
        }
        this.frameNames = new ArrayList<>(frameNames);
        this.allowedWorkpieceType = allowedWorkpieceType;
        this.occupied = false;
    }

    /**
     * Creates a new kitting position with a flexible trajectory (varargs version).
     *
     * @param allowedWorkpieceType The type of workpiece this position can hold
     * @param frameNames           Variable number of frame names defining the placement trajectory
     */
    public KittingPosition(WorkpieceType allowedWorkpieceType, String... frameNames)
    {
        if (frameNames == null || frameNames.length == 0)
        {
            throw new IllegalArgumentException("Frame names cannot be null or empty");
        }
        this.frameNames = new ArrayList<>(Arrays.asList(frameNames));
        this.allowedWorkpieceType = allowedWorkpieceType;
        this.occupied = false;
    }

    /**
     * Gets all frame names in the placement trajectory.
     *
     * @return List of frame names (immutable copy)
     */
    public List<String> getFrameNames()
    {
        return new ArrayList<>(frameNames);
    }

    /**
     * Gets the approach frame name (first frame in trajectory).
     * 
     * Note: This method assumes frameNames is non-empty, which is guaranteed by the constructor.
     *
     * @return The approach frame name
     * @deprecated Use getFrameNames() for flexible trajectories
     */
    @Deprecated
    public String getFrameNameApproach()
    {
        // Constructor guarantees frameNames is non-empty, so this is safe
        return frameNames.get(0);
    }

    /**
     * Gets the place/drop frame name (last frame in trajectory).
     * 
     * Note: This method assumes frameNames is non-empty, which is guaranteed by the constructor.
     *
     * @return The place frame name
     * @deprecated Use getFrameNames() for flexible trajectories
     */
    @Deprecated
    public String getFrameNamePlace()
    {
        // Constructor guarantees frameNames is non-empty, so this is safe
        return frameNames.get(frameNames.size() - 1);
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
        StringBuilder sb = new StringBuilder();
        sb.append("KittingPosition{");
        sb.append("frames=[");
        for (int i = 0; i < frameNames.size(); i++)
        {
            sb.append("'").append(frameNames.get(i)).append("'");
            if (i < frameNames.size() - 1)
            {
                sb.append(", ");
            }
        }
        sb.append("], type=").append(allowedWorkpieceType.getName());
        sb.append(", occupied=").append(occupied);
        sb.append('}');
        return sb.toString();
    }
}

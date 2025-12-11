package hartu.robot.executor.kitting;

import hartu.protocols.constants.WorkpieceType;
import hartu.robot.communication.server.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages a kitting box with multiple positions for different workpiece types.
 * Tracks which positions are occupied and provides methods to find available positions.
 * <p>
 * The box is reset (all positions marked available) when CommandExecutor restarts.
 */
public class KittingBox
{
    private final BoxType boxType;
    private final List<KittingPosition> positions;

    /**
     * Creates a new kitting box with the standard configuration.
     * Standard box has 4 positions:
     * - 2 positions for Axis workpieces
     * - 2 positions for Drum/Disk workpieces
     *
     * @param boxType The type of box
     */
    public KittingBox(BoxType boxType)
    {
        this.boxType = boxType;
        this.positions = new ArrayList<>();
        initializeStandardBox();
    }

    /**
     * Initializes the standard box configuration.
     * Creates positions based on the taught frames in RoboticsAPI.data.xml.
     * Each workpiece type can have a different number of frames in its trajectory.
     */
    private void initializeStandardBox()
    {
        // Two positions for Axis workpieces (2 frames each: approach, place)
        positions.add(new KittingPosition(WorkpieceType.AXIS, "PlaceAxis1_1", "PlaceAxis1_2"));
        positions.add(new KittingPosition(WorkpieceType.AXIS, "PlaceAxis2_1", "PlaceAxis2_2"));

        // Two positions for Drum workpieces (3 frames each: approach, intermediate, place)
        positions.add(new KittingPosition(WorkpieceType.DRUM, "PlaceDrum1_1", "PlaceDrum1_2", "PlaceDrum1_3"));
        positions.add(new KittingPosition(WorkpieceType.DRUM, "PlaceDrum2_1", "PlaceDrum2_2", "PlaceDrum2_3"));

        // Two positions for Disk workpieces (2 frames each: approach, place)
        positions.add(new KittingPosition(WorkpieceType.DISK, "PlaceDisk1_1", "PlaceDisk1_2"));
        positions.add(new KittingPosition(WorkpieceType.DISK, "PlaceDisk2_1", "PlaceDisk2_2"));

        Logger.getInstance().debug("KITTING", "Initialized standard kitting box with " + positions.size() + " positions");
    }

    /**
     * Finds the next available position for the given workpiece type.
     *
     * @param workpieceType The type of workpiece to place
     * @return The first available KittingPosition, or null if no positions are available
     */
    public KittingPosition findAvailablePosition(WorkpieceType workpieceType)
    {
        for (KittingPosition position : positions)
        {
            if (position.canAccept(workpieceType))
            {
                Logger.getInstance().debug("KITTING", "Found available position: " + position.toString());
                return position;
            }
        }
        Logger.getInstance().warn("KITTING", "No available position found for workpiece type: " + workpieceType.getName());
        return null;
    }

    /**
     * Marks a position as occupied after successfully placing a workpiece.
     *
     * @param position The position to mark as occupied
     */
    public void markPositionOccupied(KittingPosition position)
    {
        if (position != null)
        {
            position.setOccupied();
            Logger.getInstance().debug("KITTING", "Marked position as occupied: " + position.toString());
        }
    }

    /**
     * Resets the box by marking all positions as available.
     * Called when CommandExecutor restarts.
     */
    public void reset()
    {
        for (KittingPosition position : positions)
        {
            position.setAvailable();
        }
        Logger.getInstance().debug("KITTING", "Reset kitting box - all positions now available");
    }

    /**
     * Gets the box type.
     *
     * @return The BoxType
     */
    public BoxType getBoxType()
    {
        return boxType;
    }

    /**
     * Gets the count of available positions for a specific workpiece type.
     *
     * @param workpieceType The workpiece type to check
     * @return The number of available positions
     */
    public int getAvailablePositionCount(WorkpieceType workpieceType)
    {
        int count = 0;
        for (KittingPosition position : positions)
        {
            if (position.canAccept(workpieceType))
            {
                count++;
            }
        }
        return count;
    }

    /**
     * Gets all positions in the box.
     *
     * @return List of all KittingPositions
     */
    public List<KittingPosition> getAllPositions()
    {
        return new ArrayList<>(positions);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("KittingBox{type=").append(boxType.getName()).append(", positions=[\n");
        for (KittingPosition position : positions)
        {
            sb.append("  ").append(position.toString()).append("\n");
        }
        sb.append("]}");
        return sb.toString();
    }
}

package hartu.protocols.constants;

/**
 * Defines workpiece types that can be transmitted from ROS nodes to the robot.
 * Used with program call commands (action >= 140) to identify what type of workpiece
 * the robot should work with, along with base coordinate data.
 */
public enum WorkpieceType
{
    UNKNOWN(0, "Unknown"),
    AXIS(1, "Axis"),
    DRUM(2, "Drum"),
    DISK(3, "Disk");

    private final int id;
    private final String name;

    WorkpieceType(int id, String name)
    {
        this.id = id;
        this.name = name;
    }

    /**
     * Gets the workpiece type from its ID value.
     *
     * @param id The workpiece ID (1=Axis, 2=Drum, 3=Disk)
     * @return The corresponding WorkpieceType, or UNKNOWN if not found
     */
    public static WorkpieceType fromId(int id)
    {
        for (WorkpieceType type : WorkpieceType.values())
        {
            if (type.id == id)
            {
                return type;
            }
        }
        return UNKNOWN;
    }

    public int getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }
}

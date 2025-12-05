package hartu.robot.executor.kitting;

/**
 * Defines the different types of kitting boxes.
 * Each box type may have different dimensions and trajectories.
 */
public enum BoxType
{
    /**
     * Standard box type for testing purposes.
     * Currently the only supported box type.
     */
    STANDARD(1, "Standard");

    private final int id;
    private final String name;

    BoxType(int id, String name)
    {
        this.id = id;
        this.name = name;
    }

    /**
     * Gets the box type ID.
     *
     * @return The box type ID
     */
    public int getId()
    {
        return id;
    }

    /**
     * Gets the box type name.
     *
     * @return The box type name
     */
    public String getName()
    {
        return name;
    }
}

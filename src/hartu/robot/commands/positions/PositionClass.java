package hartu.robot.commands.positions;

/**
 * Abstract base class for different types of robot positions (e.g., AxisPosition, CartesianPosition).
 * Provides a common type hierarchy for position data.
 */
public abstract class PositionClass
{
    /**
     * Returns the runtime Class object of the specific position type.
     * For example, for an instance of AxisPosition, this would return AxisPosition.class.
     * @return The Class object representing the concrete type of this position.
     */
    public abstract Class<?> getTypeClass();
}

package hartu.robot.commands;

import hartu.robot.commands.positions.PositionClass; // Import PositionClass
import java.util.List;

/**
 * A simple container class to hold the Class type of movement target points
 * and the list of those points. Acts as a type-safe tuple.
 *
 * @param <T> The specific type of position (e.g., AxisPosition, CartesianPosition).
 */
public class MovementTargets<T extends PositionClass> { // Now T extends PositionClass
    private final Class<T> targetClass;
    private final List<T> targets;

    /**
     * Constructs a MovementTargets object.
     * @param targetClass The Class object representing the type of positions in the list.
     * @param targets The list of movement target points.
     */
    public MovementTargets(Class<T> targetClass, List<T> targets) {
        this.targetClass = targetClass;
        this.targets = targets;
    }

    /**
     * Returns the Class object representing the type of positions in the list.
     * @return The Class of the target positions.
     */
    public Class<T> getTargetClass() {
        return targetClass;
    }

    /**
     * Returns the list of movement target points.
     * @return The list of target positions.
     */
    public List<T> getTargets() {
        return targets;
    }
}

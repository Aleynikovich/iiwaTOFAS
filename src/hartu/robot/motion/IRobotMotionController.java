package hartu.robot.motion;

import hartu.protocols.constants.AngularUnit;
import hartu.protocols.constants.LinearUnit;
import hartu.protocols.definitions.coordinates.AxisPosition;
import hartu.protocols.definitions.coordinates.FramePosition;

/**
 * Defines the contract for controlling a robot's motion.
 * Implementations will interact with the actual robot hardware or simulation.
 */
public interface IRobotMotionController {

    /**
     * Executes a Point-to-Point (PTP) movement to a specified axis position.
     * The robot moves all axes simultaneously to reach the target.
     *
     * @param targetPosition The target axis position.
     * @param speedOverride A value from 0.0 to 1.0 (0% to 100%) overriding the default speed.
     * @param isRelative True if the movement is relative to the current position, false for absolute.
     * @param isContinuous True if the movement is continuous (blended), false for discrete.
     * @throws Exception if the movement command fails.
     */
    void movePTP(AxisPosition targetPosition, double speedOverride, boolean isRelative, boolean isContinuous) throws Exception;

    /**
     * Executes a Point-to-Point (PTP) movement to a specified frame position.
     * The robot moves all axes simultaneously to reach the target Cartesian coordinates.
     *
     * @param targetPosition The target frame position.
     * @param speedOverride A value from 0.0 to 1.0 (0% to 100%) overriding the default speed.
     * @param isRelative True if the movement is relative to the current position, false for absolute.
     * @param isContinuous True if the movement is continuous (blended), false for discrete.
     * @throws Exception if the movement command fails.
     */
    void movePTP(FramePosition targetPosition, double speedOverride, boolean isRelative, boolean isContinuous) throws Exception;

    /**
     * Executes a Linear (LIN) movement to a specified axis position.
     * The robot moves in a straight line in joint space to reach the target.
     *
     * @param targetPosition The target axis position.
     * @param speedOverride A value from 0.0 to 1.0 (0% to 100%) overriding the default speed.
     * @param isRelative True if the movement is relative to the current position, false for absolute.
     * @param isContinuous True if the movement is continuous (blended), false for discrete.
     * @throws Exception if the movement command fails.
     */
    void moveLIN(AxisPosition targetPosition, double speedOverride, boolean isRelative, boolean isContinuous) throws Exception;

    /**
     * Executes a Linear (LIN) movement to a specified frame position.
     * The robot moves in a straight line in Cartesian space to reach the target.
     *
     * @param targetPosition The target frame position.
     * @param speedOverride A value from 0.0 to 1.0 (0% to 100%) overriding the default speed.
     * @param isRelative True if the movement is relative to the current position, false for absolute.
     * @param isContinuous True if the movement is continuous (blended), false for discrete.
     * @throws Exception if the movement command fails.
     */
    void moveLIN(FramePosition targetPosition, double speedOverride, boolean isRelative, boolean isContinuous) throws Exception;

    /**
     * Executes a Circular (CIRC) movement to an intermediate and final axis position.
     * (Note: Full CIRC implementation usually requires an intermediate point and end point,
     * or center point and end point. This simplified version assumes a single target for now,
     * but the interface is ready for expansion.)
     *
     * @param targetPosition The target axis position.
     * @param speedOverride A value from 0.0 to 1.0 (0% to 100%) overriding the default speed.
     * @param isRelative True if the movement is relative to the current position, false for absolute.
     * @param isContinuous True if the movement is continuous (blended), false for discrete.
     * @throws Exception if the movement command fails.
     */
    void moveCIRC(AxisPosition targetPosition, double speedOverride, boolean isRelative, boolean isContinuous) throws Exception;

    /**
     * Executes a Circular (CIRC) movement to an intermediate and final frame position.
     *
     * @param targetPosition The target frame position.
     * @param speedOverride A value from 0.0 to 1.0 (0% to 100%) overriding the default speed.
     * @param isRelative True if the movement is relative to the current position, false for absolute.
     * @param isContinuous True if the movement is continuous (blended), false for discrete.
     * @throws Exception if the movement command fails.
     */
    void moveCIRC(FramePosition targetPosition, double speedOverride, boolean isRelative, boolean isContinuous) throws Exception;
}
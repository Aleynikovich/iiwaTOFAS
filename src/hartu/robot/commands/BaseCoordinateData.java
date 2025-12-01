package hartu.robot.commands;

import com.kuka.roboticsAPI.geometricModel.Frame;
import hartu.protocols.constants.WorkpieceType;

/**
 * Holds base coordinate data transmitted from ROS nodes for program call commands.
 * This data includes the workpiece location (as a Frame in xyzRPY format) and 
 * the workpiece type being processed.
 * 
 * Used with program call commands (action >= 140) to transmit base data calculated
 * by computer vision ROS nodes.
 * 
 * Note: Position values (X, Y, Z) are in millimeters.
 * Orientation values (A, B, C) are received in degrees but stored internally as radians
 * (as per KUKA Frame conventions).
 */
public class BaseCoordinateData
{
    private final Frame coordinateFrame;
    private final WorkpieceType workpieceType;

    /**
     * Creates a new BaseCoordinateData instance.
     *
     * @param coordinateFrame The coordinate frame (position in mm, orientation in radians internally)
     * @param workpieceType   The type of workpiece (Axis, Drum, Disk)
     */
    public BaseCoordinateData(Frame coordinateFrame, WorkpieceType workpieceType)
    {
        this.coordinateFrame = coordinateFrame;
        this.workpieceType = workpieceType;
    }

    /**
     * Gets the coordinate frame containing the base position and orientation.
     *
     * @return The coordinate Frame (X, Y, Z in mm; A, B, C in radians internally)
     */
    public Frame getCoordinateFrame()
    {
        return coordinateFrame;
    }

    /**
     * Gets the workpiece type.
     *
     * @return The WorkpieceType enum value
     */
    public WorkpieceType getWorkpieceType()
    {
        return workpieceType;
    }

    @Override
    public String toString()
    {
        if (coordinateFrame == null)
        {
            return "BaseCoordinateData{frame=null, workpieceType=" + workpieceType + "}";
        }
        return "BaseCoordinateData{" +
                "X=" + coordinateFrame.getX() +
                ", Y=" + coordinateFrame.getY() +
                ", Z=" + coordinateFrame.getZ() +
                ", A=" + Math.toDegrees(coordinateFrame.getAlphaRad()) +
                ", B=" + Math.toDegrees(coordinateFrame.getBetaRad()) +
                ", C=" + Math.toDegrees(coordinateFrame.getGammaRad()) +
                ", workpieceType=" + workpieceType.getName() +
                "}";
    }
}

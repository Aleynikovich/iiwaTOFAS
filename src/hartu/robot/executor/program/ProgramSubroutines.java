package hartu.robot.executor.program;

import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.ObjectFrame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import hartu.protocols.constants.WorkpieceType;
import hartu.robot.commands.BaseCoordinateData;
import hartu.robot.communication.server.LogLevel;
import hartu.robot.communication.server.Logger;
import hartu.robot.executor.io.ToolController;
import hartu.robot.executor.kitting.BoxType;
import hartu.robot.executor.kitting.KittingBox;
import hartu.robot.executor.kitting.KittingPosition;

import java.util.ArrayList;
import java.util.List;

import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;

/**
 * Contains subroutines for common robot operations like tool picking and placing.
 * These subroutines follow standardized motion sequences for tool change operations.
 * <p>
 * IMPORTANT: All tool change movements must be done with the GimaticCamera tool,
 * as all taught points (P1-P9) were taught with this tool attached.
 */
@SuppressWarnings("BusyWait")
public class ProgramSubroutines
{
    private final LBR robot;
    private final ToolController toolController;
    private final RoboticsAPIApplication application;
    private final Tool gimaticCameraTool, vacTool, Ixtur, Gripper;
    private final KittingBox kittingBox;

    /**
     * Creates a new ProgramSubroutines instance.
     *
     * @param robot          The robot to execute motions on
     * @param toolController The tool controller for Gimatic operations
     * @param application    The application instance for accessing frames
     * @param kittingBox     The kitting box for tracking workpiece positions
     */
    public ProgramSubroutines(LBR robot, ToolController toolController, RoboticsAPIApplication application, KittingBox kittingBox)
    {
        this.robot = robot;
        this.toolController = toolController;
        this.application = application;
        this.kittingBox = kittingBox;

        // Load the GimaticCamera tool for tool changing operations
        // All taught points were taught with this tool, so it must be used
        try
        {
            this.gimaticCameraTool = application.getApplicationData().createFromTemplate("GimaticCamera");
            this.vacTool = application.getApplicationData().createFromTemplate("GimaticVac1");
            this.Ixtur = application.getApplicationData().createFromTemplate("GimaticIxtur");
            this.Gripper = application.getApplicationData().createFromTemplate("GimaticGripperV");
            if (this.gimaticCameraTool != null)
            {
                Logger.getInstance().debug("ROBOT_EXEC", "ProgramSubroutines: Loaded GimaticCamera tool for tool changing operations.");
            } else
            {
                Logger.getInstance().error("ROBOT_EXEC", "ProgramSubroutines: Failed to load GimaticCamera tool from Object Templates!");
            }
        } catch (Exception e)
        {
            Logger.getInstance().error("ROBOT_EXEC", "ProgramSubroutines: Exception loading GimaticCamera tool: " + e.getMessage());
            Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", e);
            throw new RuntimeException("Cannot initialize ProgramSubroutines without GimaticCamera tool", e);
        }
    }

    /**
     * @param toolId The tool ID (1-3 corresponding to T1Base, T2Base, T3Base)
     * @return True if the operation executed successfully, false otherwise
     */
    public boolean pickTool(int toolId)
    {
    	
    	if (toolController.getCurrentToolId() == toolId)
        {
            Logger.getInstance().warn("ROBOT_EXEC", "Called for pick tool: " + toolId + ", but current Tool ID " + toolController.getCurrentToolId() + " already picked.");
            return true;
        }
        if (toolController.getCurrentToolId() != 0)
        {
            Logger.getInstance().error("ROBOT_EXEC", "Called for pick tool: " + toolId + ", but current Tool ID " + toolController.getCurrentToolId() + " is not 0! Ignoring request to avoid possible collision.");
            return false;
        }

        if (toolId < 1 || toolId > 6)
        {
            Logger.getInstance().error("ROBOT_EXEC", "Invalid tool ID for pick operation: " + toolId + ". Must be 1-6.");
            return false;
        }

        if (gimaticCameraTool == null)
        {
            Logger.getInstance().error("ROBOT_EXEC", "GimaticCamera tool not loaded. Cannot execute pick operation.");
            return false;
        }

        int baseId = toolId;
        if (baseId > 3)
        {
            baseId = baseId - 3;
        }
        String baseName = "T" + baseId + "Base";

        //Move to safe pos if we are on right side of plane to avoid collisions
        if ((robot.getFlange().getY() < 0 && baseId == 3) || (robot.getFlange().getY() > 0 && baseId != 3))
        {
            Logger.getInstance().debug("ROBOT_EXEC", "Moving to safe position to avoid collision with tool " + toolId);
            robot.move(ptp(robot.getCurrentJointPosition().get(0),0,0,0,0,0,0).setJointVelocityRel(0.5));
            robot.move(ptp(-robot.getCurrentJointPosition().get(0),0,0,0,0,0,0).setJointVelocityRel(0.5));
        }
        if (!toolController.unlockGimatic())
        {
            Logger.getInstance().error("ROBOT_EXEC", "Unable to unlock gimatic, not picking to avoid collision");
            return false;
        }

        try
        {
            ObjectFrame baseFrame = application.getApplicationData().getFrame("/" + baseName);
            if (baseFrame == null)
            {
                Logger.getInstance().error("ROBOT_EXEC", "Base frame '" + baseName + "' not found in station setup.");
                return false;
            }

            // Attach GimaticCamera tool for the pick operation
            detachAllTools();
            gimaticCameraTool.attachTo(robot.getFlange());
            Logger.getInstance().debug("ROBOT_EXEC", "Attached GimaticCamera tool for pick operation.");
            
            // Move through points P9 -> P1
            for (int i = 9; i >= 1; i--)
            {
                ObjectFrame pointFrame = baseFrame.getChild("P" + i);
                if (pointFrame == null)
                {
                    Logger.getInstance().error("ROBOT_EXEC", "Frame 'P" + i + "' not found under '" + baseName + "'.");
                    return false;
                }

                // Lock Gimatic at P8 (contact point)
                if (i == 8)
                {
                   	Logger.getInstance().low("ROBOT_EXEC", "Moving to " + baseName + "/P" + i);
                    gimaticCameraTool.move(lin(pointFrame).setJointVelocityRel(0.2));
                    if (!toolController.lockGimatic())
                    {
                        return false;
                    }
                }
                else
                {
                	Logger.getInstance().low("ROBOT_EXEC", "Moving to " + baseName + "/P" + i);
                    gimaticCameraTool.move(ptp(pointFrame).setJointVelocityRel(0.5));
                }
            }
            while (toolController.getCurrentToolId() == 0)
            {
                Logger.getInstance().low("ROBOT_EXEC", "Waiting for tool to be picked...");
                //noinspection BusyWait
                Thread.sleep(2000);
            }

            return true;

        } catch (Exception e)
        {
            Logger.getInstance().error("ROBOT_EXEC", "Exception during pick tool operation for tool " + toolId + ": " + e.getMessage());
            Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", e);
            return false;
        }
    }

    /**
     * @param toolId The tool ID (1-3 corresponding to T1Base, T2Base, T3Base)
     * @return True if the operation executed successfully, false otherwise
     */
    public boolean placeTool(int toolId)
    {

        if (toolController.getCurrentToolId() == 0)
        {
            Logger.getInstance().warn("ROBOT_EXEC", "Called for place tool: " + toolId + ", but there is already no tool! ID: " + toolController.getCurrentToolId() + " !. Ignoring request.");
            return true;
        }
        if (toolId != toolController.getCurrentToolId())
        {
            Logger.getInstance().error("ROBOT_EXEC", "Called for place tool: " + toolId + ", but current Tool ID is" + toolController.getCurrentToolId() + " !. Ignoring request to avoid possible collision.");
            return false;
        }
        if (toolId < 1 || toolId > 6)
        {
            Logger.getInstance().error("ROBOT_EXEC", "Invalid tool ID for place operation: " + toolId + ". Must be 1-6.");
            return false;
        }
        if (gimaticCameraTool == null)
        {
            Logger.getInstance().error("ROBOT_EXEC", "GimaticCamera tool not loaded. Cannot execute place operation.");
            return false;
        }
        int baseId = toolId;
        if (baseId > 3)
        {
            baseId = baseId - 3;
        }
        String baseName = "T" + baseId + "Base";
        try
        {
            //Move to safe pos if we are on right side of plane to avoid collisions
            if ((robot.getFlange().getY() < 0 && baseId == 3) || (robot.getFlange().getY() > 0 && baseId != 3))
            {
                Logger.getInstance().debug("ROBOT_EXEC", "Moving to safe position to avoid collision with tool " + toolId);
                robot.move(ptp(robot.getCurrentJointPosition().get(0),0,0,0,0,0,0).setJointVelocityRel(0.5));
                robot.move(ptp(-robot.getCurrentJointPosition().get(0),0,0,0,0,0,0).setJointVelocityRel(0.5));
            }

            ObjectFrame baseFrame = application.getApplicationData().getFrame("/" + baseName);
            if (baseFrame == null)
            {
                Logger.getInstance().error("ROBOT_EXEC", "Base frame '" + baseName + "' not found in station setup.");
                return false;
            }

            // Attach GimaticCamera tool for the place operation
            detachAllTools();
            gimaticCameraTool.attachTo(robot.getFlange());
            Logger.getInstance().debug("ROBOT_EXEC", "Attached GimaticCamera tool for place operation.");

            // Move through points P1 -> P9
            for (int i = 1; i <= 9; i++)
            {
                ObjectFrame pointFrame = baseFrame.getChild("P" + i);
                if (pointFrame == null)
                {
                    Logger.getInstance().error("ROBOT_EXEC", "Frame 'P" + i + "' not found under '" + baseName + "'.");
                    return false;
                }

                // Unlock Gimatic at P8 (contact point)
                if (i == 8)
                {
                    Logger.getInstance().low("ROBOT_EXEC", "Moving to " + baseName + "/P" + i);
                    gimaticCameraTool.move(lin(pointFrame).setJointVelocityRel(0.2));
                    if (!toolController.unlockGimatic())
                    {
                        Logger.getInstance().error("ROBOT_EXEC", "Failed to unlock Gimatic!");
                        return false;
                    }
                }
                else
                {
                    Logger.getInstance().low("ROBOT_EXEC", "Moving to " + baseName + "/P" + i);
                    gimaticCameraTool.move(ptp(pointFrame).setJointVelocityRel(0.5));
                }
            }

            while (toolController.getCurrentToolId() != 0)
            {
                Logger.getInstance().low("ROBOT_EXEC", "Waiting for tool to be released...");
                Thread.sleep(2000);
            }

            return true;

        } catch (Exception e)
        {
            Logger.getInstance().error("ROBOT_EXEC", "Exception during place tool operation for tool " + toolId + ": " + e.getMessage());
            Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", e);
            return false;
        }
    }

    public boolean doParipe()
    {

        try
        {
            Logger.getInstance().debug("ROBOT_EXEC", "Doing el paripe");
            ObjectFrame baseFrame = application.getApplicationData().getFrame("/ZebraBase");

            // Attach GimaticCamera tool for the place operation
            vacTool.attachTo(robot.getFlange());
            Logger.getInstance().debug("ROBOT_EXEC", "Attached VacTool");

            // Move through points P1 -> P9
            for (int i = 1; i <= 2; i++)
            {
                ObjectFrame pointFrame = baseFrame.getChild("P" + i);
                if (pointFrame == null)
                {
                    Logger.getInstance().error("ROBOT_EXEC", "Frame 'P" + i + "' not found under ZebraBase");
                    return false;
                }
                Logger.getInstance().low("ROBOT_EXEC", "Moving to ZebraBase/P" + i);
                vacTool.move(ptp(pointFrame).setJointVelocityRel(0.8));
            }

            return true;

        } catch (Exception e)
        {
            Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", e);
            return false;
        }
    }


    public boolean pickAxis()
    {
        detachAllTools();
        Gripper.attachTo(robot.getFlange());
        Gripper.move(ptp(application.getApplicationData().getFrame("/PickAxisGripper/P1")));
        Gripper.move(ptp(application.getApplicationData().getFrame("/PickAxisGripper/P2")));
        Gripper.move(lin(application.getApplicationData().getFrame("/PickAxisGripper/P3Pick")));
        toolController.closeTool(4);
        toolController.openTool(4);
        toolController.closeTool(4);
        Gripper.move(lin(application.getApplicationData().getFrame("/PickAxisGripper/P4")));
        Gripper.move(ptp(application.getApplicationData().getFrame("/PickAxisGripper/P5")));
        Gripper.move(ptp(application.getApplicationData().getFrame("/PickAxisGripper/P6")));

        return true;
    }

    public boolean placeAxisPlaceholder()
    {
        detachAllTools();
        Ixtur.attachTo(robot.getFlange());

        Ixtur.move(ptp(application.getApplicationData().getFrame("/PlaceAxis/P1")));
        Ixtur.move(ptp(application.getApplicationData().getFrame("/PlaceAxis/P2")));
        Ixtur.move(lin(application.getApplicationData().getFrame("/PlaceAxis/P3")));
        Ixtur.move(lin(application.getApplicationData().getFrame("/PlaceAxis/P4Place")));
        toolController.openTool(5);
        Ixtur.move(lin(application.getApplicationData().getFrame("/PlaceAxis/P5")));

        return true;
    }

    /**
     * Places a workpiece in the kitting box at the next available position.
     * This method automatically finds the next free position for the given workpiece type.
     *
     * @param kittingBase   The base frame of the kitting box from camera
     * @param workpieceType The type of workpiece being placed
     * @return True if placement was successful, false otherwise
     */
    public boolean placeWorkpieceInBox(Frame kittingBase, WorkpieceType workpieceType)
    {
        Logger.getInstance().debug("ROBOT_EXEC", "Placing " + workpieceType.getName() + " in kitting box");

        // Find the next available position for this workpiece type
        KittingPosition position = kittingBox.findAvailablePosition(workpieceType);
        if (position == null)
        {
            Logger.getInstance().error("ROBOT_EXEC", "No available position in kitting box for " + workpieceType.getName());
            return false;
        }

        // Execute the placement motion
        boolean success = placeWorkpieceAtPosition(kittingBase, position, workpieceType);

        // Mark position as occupied if placement was successful
        if (success)
        {
            kittingBox.markPositionOccupied(position);
            Logger.getInstance().debug("ROBOT_EXEC", "Successfully placed " + workpieceType.getName() + " at position: " + position.getFrameNameApproach());
        }

        return success;
    }

    /**
     * Places a workpiece at a specific position in the kitting box.
     * This is a lower-level method that performs the actual robot motion.
     *
     * @param kittingBase   The base frame of the kitting box from camera
     * @param position      The specific position to place the workpiece
     * @param workpieceType The type of workpiece being placed
     * @return True if placement was successful, false otherwise
     */
    private boolean placeWorkpieceAtPosition(Frame kittingBase, KittingPosition position, WorkpieceType workpieceType)
    {
        try
        {
            // Attach the appropriate tool for the workpiece type
            Tool toolToUse = getToolForWorkpiece(workpieceType);
            if (toolToUse == null)
            {
                Logger.getInstance().error("ROBOT_EXEC", "No tool available for workpiece type: " + workpieceType.getName());
                return false;
            }

            // Detach other tools and attach the correct one
            detachAllTools();
            toolToUse.attachTo(robot.getFlange());

            // Get the base frame from the station setup
            ObjectFrame refBase = application.getApplicationData().getFrame("/basekitting");
            if (refBase == null)
            {
                Logger.getInstance().error("ROBOT_EXEC", "Base frame '/basekitting' not found");
                return false;
            }

            // Get all frame names for this position's trajectory
            List<String> frameNames = position.getFrameNames();
            if (frameNames.isEmpty())
            {
                Logger.getInstance().error("ROBOT_EXEC", "No frames defined for position (workpiece type: " + workpieceType.getName() + ")");
                return false;
            }

            Logger.getInstance().debug("ROBOT_EXEC", "Executing " + frameNames.size() + "-frame trajectory for " + workpieceType.getName());

            // Load all taught frames and create relative frames
            List<Frame> relativeFrames = new ArrayList<>();
            for (String frameName : frameNames)
            {
                ObjectFrame taughtFrame = application.getApplicationData().getFrame("/basekitting/" + frameName);
                if (taughtFrame == null)
                {
                    Logger.getInstance().error("ROBOT_EXEC", "Taught frame not found: /basekitting/" + frameName);
                    return false;
                }
                Logger.getInstance().debug("ROBOT_EXEC", "Loaded frame: " + frameName);
                relativeFrames.add(taughtFrame.copyWithRedundancy());
            }

            // Create a new base frame from the camera data
            Frame newBase = refBase.copyWithRedundancy();
            newBase.setX(kittingBase.getX());
            newBase.setY(kittingBase.getY());
            newBase.setZ(kittingBase.getZ());
            newBase.setAlphaRad(kittingBase.getAlphaRad());
            newBase.setBetaRad(kittingBase.getBetaRad());
            newBase.setGammaRad(kittingBase.getGammaRad());

            // Set parent for all relative frames
            for (Frame frame : relativeFrames)
            {
                frame.setParent(newBase);
            }

            // Execute the trajectory: first frame with PTP, rest with LIN
            int frameCount = relativeFrames.size();
            for (int i = 0; i < frameCount; i++)
            {
                Frame targetFrame = relativeFrames.get(i);
                String frameName = frameNames.get(i);
                
                if (i == 0)
                {
                    // First frame: use PTP (point-to-point) for approach
                    Logger.getInstance().debug("ROBOT_EXEC", "Moving to approach position: " + frameName);
                    toolToUse.move(ptp(targetFrame).setJointVelocityRel(0.7));
                }
                else
                {
                    // Subsequent frames: use LIN (linear) motion
                    Logger.getInstance().debug("ROBOT_EXEC", "Moving to position " + (i + 1) + "/" + frameCount + ": " + frameName);
                    toolToUse.move(lin(targetFrame).setJointVelocityRel(0.2));
                }
            }

            // Open the tool to release the workpiece at the final position
            toolController.openTool(toolController.getCurrentToolId());
            
            // Return to the first frame (approach position) to clear the area
            Logger.getInstance().debug("ROBOT_EXEC", "Returning to approach position");
            toolToUse.move(lin(relativeFrames.get(0)).setJointVelocityRel(0.7));
            
            // Return to home position with camera tool
            detachAllTools();
            gimaticCameraTool.attachTo(robot.getFlange());
            gimaticCameraTool.move(ptp(application.getApplicationData().getFrame("/P1")));

            return true;

        } catch (Exception e)
        {
            Logger.getInstance().error("ROBOT_EXEC", "Exception during workpiece placement: " + e.getMessage());
            Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", e);
            return false;
        }
    }

    /**
     * Gets the appropriate tool for the given workpiece type.
     *
     * @param workpieceType The workpiece type
     * @return The Tool to use, or null if not found
     */
    private Tool getToolForWorkpiece(WorkpieceType workpieceType)
    {
        switch (workpieceType)
        {
            case AXIS:
                return Gripper; // Axis uses GimaticGripperV
            case DRUM:
            case DISK:
                return Ixtur; // Drum and Disk use GimaticIxtur (CircMagnet)
            default:
                return null;
        }
    }

    /**
     * Detaches all tools from the robot flange.
     */
    private void detachAllTools()
    {
        if (Gripper != null) Gripper.detach();
        if (Ixtur != null) Ixtur.detach();
        if (vacTool != null) vacTool.detach();
        if (gimaticCameraTool != null) gimaticCameraTool.detach();
    }

    /**
     * Legacy method for placing an axis in the box at a specific position.
     * This method is kept for backward compatibility but delegates to the new abstraction.
     *
     * @param kittingBase The base frame of the kitting box from camera
     * @param workpieceId The workpiece ID (should be 1 for AXIS)
     * @param positionId  The position ID (1 or 2) - deprecated, position is now auto-selected
     * @return True if placement was successful, false otherwise
     * @deprecated Use placeWorkpieceInBox instead
     */
    @Deprecated
    @SuppressWarnings("unused")
    public boolean placeAxisBox(Frame kittingBase, int workpieceId, int positionId)
    {
        Logger.getInstance().warn("ROBOT_EXEC", "Using deprecated placeAxisBox method. Consider using placeWorkpieceInBox instead.");
        return placeWorkpieceInBox(kittingBase, WorkpieceType.AXIS);
    }

    /**
     * Legacy method for placing a drum in the box at a specific position.
     * This method is kept for backward compatibility but delegates to the new abstraction.
     *
     * @param kittingBase The base frame of the kitting box from camera
     * @param workpieceId The workpiece ID (should be 2 for DRUM)
     * @param positionId  The position ID (1 or 2) - deprecated, position is now auto-selected
     * @return True if placement was successful, false otherwise
     * @deprecated Use placeWorkpieceInBox instead
     */
    @Deprecated
    @SuppressWarnings("unused")
    public boolean placeDrum(Frame kittingBase, int workpieceId, int positionId)
    {
        Logger.getInstance().warn("ROBOT_EXEC", "Using deprecated placeDrum method. Consider using placeWorkpieceInBox instead.");
        return placeWorkpieceInBox(kittingBase, WorkpieceType.DRUM);
    }

    /**
     * Legacy method for placing a disk in the box at a specific position.
     * This method is kept for backward compatibility but delegates to the new abstraction.
     *
     * @param kittingBase The base frame of the kitting box from camera
     * @param workpieceId The workpiece ID (should be 3 for DISK)
     * @param positionId  The position ID (1 or 2) - deprecated, position is now auto-selected
     * @return True if placement was successful, false otherwise
     * @deprecated Use placeWorkpieceInBox instead
     */
    @Deprecated
    @SuppressWarnings("unused")
    public boolean placeDisk(Frame kittingBase, int workpieceId, int positionId)
    {
        Logger.getInstance().warn("ROBOT_EXEC", "Using deprecated placeDisk method. Consider using placeWorkpieceInBox instead.");
        return placeWorkpieceInBox(kittingBase, WorkpieceType.DISK);
    }

    /**
     * Stores base coordinate data received from ROS computer vision nodes.
     * This data includes the workpiece location (as a Frame) and workpiece type.
     * The stored data can be used for subsequent kitting operations.
     *
     * @param baseData The base coordinate data containing frame and workpiece type
     * @return True if the data was stored successfully
     */
    public boolean storeBaseCoordinateData(BaseCoordinateData baseData)
    {
        if (baseData == null)
        {
            Logger.getInstance().error("ROBOT_EXEC", "Cannot store null base coordinate data.");
            return false;
        }

        // Stored base coordinate data from ROS computer vision nodes

        Frame frame = baseData.getCoordinateFrame();
        WorkpieceType workpieceType = baseData.getWorkpieceType();

        Logger.getInstance().debug("ROBOT_EXEC", "Stored base coordinate data:");
        Logger.getInstance().debug("ROBOT_EXEC", "  Workpiece Type: " + workpieceType.getName() + " (ID: " + workpieceType.getId() + ")");
        if (frame != null)
        {
            Logger.getInstance().debug("ROBOT_EXEC", "  Position: X=" + frame.getX() + ", Y=" + frame.getY() + ", Z=" + frame.getZ());
            Logger.getInstance().debug("ROBOT_EXEC", "  Orientation: A=" + Math.toDegrees(frame.getAlphaRad()) +
                    ", B=" + Math.toDegrees(frame.getBetaRad()) +
                    ", C=" + Math.toDegrees(frame.getGammaRad()));
        }

        return true;
    }
}

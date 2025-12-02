package hartu.robot.executor.program;

import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.ObjectFrame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import hartu.protocols.constants.WorkpieceType;
import hartu.robot.commands.BaseCoordinateData;
import hartu.robot.communication.server.Logger;
import hartu.robot.executor.io.ToolController;

import static com.kuka.roboticsAPI.motionModel.BasicMotions.ptp;

/**
 * Contains subroutines for common robot operations like tool picking and placing.
 * These subroutines follow standardized motion sequences for tool change operations.
 * <p>
 * IMPORTANT: All tool change movements must be done with the GimaticCamera tool,
 * as all taught points (P1-P9) were taught with this tool attached.
 */
public class ProgramSubroutines
{
    private final LBR robot;
    private final ToolController toolController;
    private final RoboticsAPIApplication application;
    private final Tool gimaticCameraTool, vacTool;

    // Stored base coordinate data from ROS computer vision nodes
    private BaseCoordinateData currentBaseCoordinateData;

    /**
     * Creates a new ProgramSubroutines instance.
     *
     * @param robot          The robot to execute motions on
     * @param toolController The tool controller for Gimatic operations
     * @param application    The application instance for accessing frames
     */
    public ProgramSubroutines(LBR robot, ToolController toolController, RoboticsAPIApplication application)
    {
        this.robot = robot;
        this.toolController = toolController;
        this.application = application;

        // Load the GimaticCamera tool for tool changing operations
        // All taught points were taught with this tool, so it must be used
        try
        {
            this.gimaticCameraTool = application.getApplicationData().createFromTemplate("GimaticCamera");
            this.vacTool = application.getApplicationData().createFromTemplate("GimaticVac1");
            if (this.gimaticCameraTool != null)
            {
                Logger.getInstance().log("ROBOT_EXEC", "ProgramSubroutines: Loaded GimaticCamera tool for tool changing operations.");
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

        if (toolController.getCurrentToolId() != 0)
        {
            Logger.getInstance().error("ROBOT_EXEC", "Called for pick tool: " + toolId + ", but Tool ID " + toolController.getCurrentToolId() + " is not 0! Ignoring request.");
            return false;
        }

        if (toolId < 1 || toolId > 3)
        {
            Logger.getInstance().error("ROBOT_EXEC", "Invalid tool ID for pick operation: " + toolId + ". Must be 1-3.");
            return false;
        }

        if (gimaticCameraTool == null)
        {
            Logger.getInstance().error("ROBOT_EXEC", "GimaticCamera tool not loaded. Cannot execute pick operation.");
            return false;
        }

        String baseName = "T" + toolId + "Base";

        try
        {
            ObjectFrame baseFrame = application.getApplicationData().getFrame("/" + baseName);
            if (baseFrame == null)
            {
                Logger.getInstance().error("ROBOT_EXEC", "Base frame '" + baseName + "' not found in station setup.");
                return false;
            }

            // Attach GimaticCamera tool for the pick operation
            gimaticCameraTool.attachTo(robot.getFlange());
            Logger.getInstance().log("ROBOT_EXEC", "Attached GimaticCamera tool for pick operation.");

            // Move through points P9 -> P1
            for (int i = 9; i >= 1; i--)
            {
                ObjectFrame pointFrame = baseFrame.getChild("P" + i);
                if (pointFrame == null)
                {
                    Logger.getInstance().error("ROBOT_EXEC", "Frame 'P" + i + "' not found under '" + baseName + "'.");
                    return false;
                }

                Logger.getInstance().log("ROBOT_EXEC", "Moving to " + baseName + "/P" + i);
                gimaticCameraTool.move(ptp(pointFrame).setJointVelocityRel(0.2));

                // Lock Gimatic at P8 (contact point)
                if (i == 8)
                {
                    if (!toolController.lockGimatic())
                    {
                        return false;
                    }
                }
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
            return false;
        }
        if (toolId != toolController.getCurrentToolId())
        {
            Logger.getInstance().error("ROBOT_EXEC", "Called for place tool: " + toolId + ", but current Tool ID is" + toolController.getCurrentToolId() + " !. Ignoring request to avoid possible collision.");
            return false;
        }

        if (toolId < 1 || toolId > 3)
        {
            Logger.getInstance().error("ROBOT_EXEC", "Invalid tool ID for place operation: " + toolId + ". Must be 1-3.");
            return false;
        }

        if (gimaticCameraTool == null)
        {
            Logger.getInstance().error("ROBOT_EXEC", "GimaticCamera tool not loaded. Cannot execute place operation.");
            return false;
        }

        String baseName = "T" + toolId + "Base";

        try
        {
            ObjectFrame baseFrame = application.getApplicationData().getFrame("/" + baseName);
            if (baseFrame == null)
            {
                Logger.getInstance().error("ROBOT_EXEC", "Base frame '" + baseName + "' not found in station setup.");
                return false;
            }

            // Attach GimaticCamera tool for the place operation
            gimaticCameraTool.attachTo(robot.getFlange());
            Logger.getInstance().log("ROBOT_EXEC", "Attached GimaticCamera tool for place operation.");

            // Move through points P1 -> P9
            for (int i = 1; i <= 9; i++)
            {
                ObjectFrame pointFrame = baseFrame.getChild("P" + i);
                if (pointFrame == null)
                {
                    Logger.getInstance().error("ROBOT_EXEC", "Frame 'P" + i + "' not found under '" + baseName + "'.");
                    return false;
                }

                Logger.getInstance().log("ROBOT_EXEC", "Moving to " + baseName + "/P" + i);
                gimaticCameraTool.move(ptp(pointFrame).setJointVelocityRel(0.2));

                // Unlock Gimatic at P8 (contact point)
                if (i == 8)
                {
                    if (!toolController.unlockGimatic())
                    {
                        return false;
                    }
                }
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
            Logger.getInstance().log("ROBOT_EXEC", "Doing el paripe");
            ObjectFrame baseFrame = application.getApplicationData().getFrame("/ZebraBase");

            // Attach GimaticCamera tool for the place operation
            vacTool.attachTo(robot.getFlange());
            Logger.getInstance().log("ROBOT_EXEC", "Attached VacTool");

            // Move through points P1 -> P9
            for (int i = 1; i <= 2; i++)
            {
                ObjectFrame pointFrame = baseFrame.getChild("P" + i);
                if (pointFrame == null)
                {
                    Logger.getInstance().error("ROBOT_EXEC", "Frame 'P" + i + "' not found under ZebraBase");
                    return false;
                }
                Logger.getInstance().log("ROBOT_EXEC", "Moving to ZebraBase/P" + i);
                vacTool.move(ptp(pointFrame).setJointVelocityRel(0.8));
            }

            return true;

        } catch (Exception e)
        {
            Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", e);
            return false;
        }
    }

    /**
     * Logs the subroutine call with frame and workpiece information.
     *
     * @param methodName  The name of the subroutine being called
     * @param frame       The Frame parameter
     * @param workpieceId The workpiece ID parameter
     */
    private void logSubroutineCall(String methodName, Frame frame, int workpieceId)
    {
        Logger.getInstance().log("ROBOT_EXEC", methodName + " called with frame: " +
                (frame != null ? frame.toString() : "null") + ", workpieceId: " + workpieceId);
    }

    public boolean pickAxis(Frame frame, int workpieceId)
    {
        logSubroutineCall("pickAxis", frame, workpieceId);

        return true;
    }

    public boolean placeAxisPlaceholder(Frame frame, int workpieceId)
    {

        return true;
    }

    public boolean placeAxisBox(Frame frame, int workpieceId)
    {
        return true;
    }

    public boolean placeDrum(Frame frame, int workpieceId)
    {

        return true;
    }

    public boolean placeDisk(Frame frame, int workpieceId)
    {
        return true;
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

        this.currentBaseCoordinateData = baseData;
        
        Frame frame = baseData.getCoordinateFrame();
        WorkpieceType workpieceType = baseData.getWorkpieceType();
        
        Logger.getInstance().log("ROBOT_EXEC", "Stored base coordinate data:");
        Logger.getInstance().log("ROBOT_EXEC", "  Workpiece Type: " + workpieceType.getName() + " (ID: " + workpieceType.getId() + ")");
        if (frame != null)
        {
            Logger.getInstance().log("ROBOT_EXEC", "  Position: X=" + frame.getX() + ", Y=" + frame.getY() + ", Z=" + frame.getZ());
            Logger.getInstance().log("ROBOT_EXEC", "  Orientation: A=" + Math.toDegrees(frame.getAlphaRad()) + 
                                     ", B=" + Math.toDegrees(frame.getBetaRad()) + 
                                     ", C=" + Math.toDegrees(frame.getGammaRad()));
        }
        
        return true;
    }

    /**
     * Gets the currently stored base coordinate data.
     *
     * @return The stored BaseCoordinateData, or null if none has been stored
     */
    public BaseCoordinateData getCurrentBaseCoordinateData()
    {
        return currentBaseCoordinateData;
    }

    /**
     * Checks if there is stored base coordinate data available.
     *
     * @return True if base coordinate data is available
     */
    public boolean hasStoredBaseCoordinateData()
    {
        return currentBaseCoordinateData != null;
    }

    /**
     * Clears the stored base coordinate data.
     */
    public void clearBaseCoordinateData()
    {
        this.currentBaseCoordinateData = null;
        Logger.getInstance().log("ROBOT_EXEC", "Cleared stored base coordinate data.");
    }
}

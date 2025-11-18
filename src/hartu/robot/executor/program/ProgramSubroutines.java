package hartu.robot.executor.program;

import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.ObjectFrame;
import hartu.robot.communication.server.Logger;
import hartu.robot.executor.io.ToolController;

import static com.kuka.roboticsAPI.motionModel.BasicMotions.ptp;

/**
 * Contains subroutines for common robot operations like tool picking and placing.
 * These subroutines follow standardized motion sequences for tool change operations.
 */
public class ProgramSubroutines
{
    private final LBR robot;
    private final ToolController toolController;
    private final RoboticsAPIApplication application;
    
    /**
     * Creates a new ProgramSubroutines instance.
     * 
     * @param robot The robot to execute motions on
     * @param toolController The tool controller for Gimatic operations
     * @param application The application instance for accessing frames
     */
    public ProgramSubroutines(LBR robot, ToolController toolController, RoboticsAPIApplication application) {
        this.robot = robot;
        this.toolController = toolController;
        this.application = application;
    }
    
    /**
     * Picks up a tool from its storage base using a standardized motion sequence.
     * Motion sequence: T#Base/P9 → T#Base/P8 → T#Base/P1
     * At P8 (contact point), the Gimatic tool changer is locked.
     * 
     * @param toolId The tool ID (1-3 corresponding to T1Base, T2Base, T3Base)
     * @return True if the operation executed successfully, false otherwise
     */
    public boolean pickTool(int toolId) {
        if (toolId < 1 || toolId > 3) {
            Logger.getInstance().error("ROBOT_EXEC", "Invalid tool ID for pick operation: " + toolId + ". Must be 1-3.");
            return false;
        }
        
        String baseName = "T" + toolId + "Base";
        Logger.getInstance().log("ROBOT_EXEC", "Starting pick tool sequence for tool " + toolId + " from " + baseName);
        
        try {
            // Get the base frame for the tool
            ObjectFrame baseFrame = application.getApplicationData().getFrame("/" + baseName);
            if (baseFrame == null) {
                Logger.getInstance().error("ROBOT_EXEC", "Base frame '" + baseName + "' not found in station setup.");
                return false;
            }
            
            // P9: Approach position (safe distance from tool)
            ObjectFrame p9Frame = baseFrame.getChild("P9");
            if (p9Frame == null) {
                Logger.getInstance().error("ROBOT_EXEC", "Frame 'P9' not found under '" + baseName + "'.");
                return false;
            }
            Logger.getInstance().log("ROBOT_EXEC", "Moving to " + baseName + "/P9 (approach position)");
            robot.move(ptp(p9Frame).setJointVelocityRel(0.2));
            
            // P8: Contact position (where Gimatic locks)
            ObjectFrame p8Frame = baseFrame.getChild("P8");
            if (p8Frame == null) {
                Logger.getInstance().error("ROBOT_EXEC", "Frame 'P8' not found under '" + baseName + "'.");
                return false;
            }
            Logger.getInstance().log("ROBOT_EXEC", "Moving to " + baseName + "/P8 (contact position)");
            robot.move(ptp(p8Frame).setJointVelocityRel(0.1));
            
            // Lock Gimatic at contact position
            Logger.getInstance().log("ROBOT_EXEC", "Locking Gimatic tool changer at " + baseName + "/P8");
            if (!toolController.lockGimatic()) {
                Logger.getInstance().error("ROBOT_EXEC", "Failed to lock Gimatic at " + baseName + "/P8");
                return false;
            }
            
            // P1: Final position (tool picked)
            ObjectFrame p1Frame = baseFrame.getChild("P1");
            if (p1Frame == null) {
                Logger.getInstance().error("ROBOT_EXEC", "Frame 'P1' not found under '" + baseName + "'.");
                return false;
            }
            Logger.getInstance().log("ROBOT_EXEC", "Moving to " + baseName + "/P1 (final position)");
            robot.move(ptp(p1Frame).setJointVelocityRel(0.2));
            
            Logger.getInstance().log("ROBOT_EXEC", "Successfully picked tool " + toolId + " from " + baseName);
            return true;
            
        } catch (Exception e) {
            Logger.getInstance().error("ROBOT_EXEC", "Exception during pick tool operation for tool " + toolId + ": " + e.getMessage());
            Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", e);
            return false;
        }
    }
    
    /**
     * Places the currently held tool back to its storage base using a standardized motion sequence.
     * Motion sequence: T#Base/P1 → T#Base/P8 → T#Base/P9
     * At P8 (contact point), the Gimatic tool changer is unlocked.
     * 
     * @param toolId The tool ID (1-3 corresponding to T1Base, T2Base, T3Base)
     * @return True if the operation executed successfully, false otherwise
     */
    public boolean placeTool(int toolId) {
        if (toolId < 1 || toolId > 3) {
            Logger.getInstance().error("ROBOT_EXEC", "Invalid tool ID for place operation: " + toolId + ". Must be 1-3.");
            return false;
        }
        
        String baseName = "T" + toolId + "Base";
        Logger.getInstance().log("ROBOT_EXEC", "Starting place tool sequence for tool " + toolId + " to " + baseName);
        
        try {
            // Get the base frame for the tool
            ObjectFrame baseFrame = application.getApplicationData().getFrame("/" + baseName);
            if (baseFrame == null) {
                Logger.getInstance().error("ROBOT_EXEC", "Base frame '" + baseName + "' not found in station setup.");
                return false;
            }
            
            // P1: Starting position (tool held)
            ObjectFrame p1Frame = baseFrame.getChild("P1");
            if (p1Frame == null) {
                Logger.getInstance().error("ROBOT_EXEC", "Frame 'P1' not found under '" + baseName + "'.");
                return false;
            }
            Logger.getInstance().log("ROBOT_EXEC", "Moving to " + baseName + "/P1 (starting position)");
            robot.move(ptp(p1Frame).setJointVelocityRel(0.2));
            
            // P8: Contact position (where Gimatic unlocks)
            ObjectFrame p8Frame = baseFrame.getChild("P8");
            if (p8Frame == null) {
                Logger.getInstance().error("ROBOT_EXEC", "Frame 'P8' not found under '" + baseName + "'.");
                return false;
            }
            Logger.getInstance().log("ROBOT_EXEC", "Moving to " + baseName + "/P8 (contact position)");
            robot.move(ptp(p8Frame).setJointVelocityRel(0.1));
            
            // Unlock Gimatic at contact position
            Logger.getInstance().log("ROBOT_EXEC", "Unlocking Gimatic tool changer at " + baseName + "/P8");
            if (!toolController.unlockGimatic()) {
                Logger.getInstance().error("ROBOT_EXEC", "Failed to unlock Gimatic at " + baseName + "/P8");
                return false;
            }
            
            // P9: Final position (safe distance from tool)
            ObjectFrame p9Frame = baseFrame.getChild("P9");
            if (p9Frame == null) {
                Logger.getInstance().error("ROBOT_EXEC", "Frame 'P9' not found under '" + baseName + "'.");
                return false;
            }
            Logger.getInstance().log("ROBOT_EXEC", "Moving to " + baseName + "/P9 (final position)");
            robot.move(ptp(p9Frame).setJointVelocityRel(0.2));
            
            Logger.getInstance().log("ROBOT_EXEC", "Successfully placed tool " + toolId + " at " + baseName);
            return true;
            
        } catch (Exception e) {
            Logger.getInstance().error("ROBOT_EXEC", "Exception during place tool operation for tool " + toolId + ": " + e.getMessage());
            Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", e);
            return false;
        }
    }
}

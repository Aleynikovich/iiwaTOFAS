package hartu.robot.executor.program;

import javax.inject.Inject;
import javax.inject.Named;

import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.ObjectFrame;
import com.kuka.roboticsAPI.geometricModel.Tool;

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
    
    @Inject
    @Named("GimaticCamera")
    private Tool gimatic;
 
    
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
     * Motion sequence: T#Base/P9 → P8 → P7 → P6 → P5 → P4 → P3 → P2 → P1
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
        
        try {
            ObjectFrame baseFrame = application.getApplicationData().getFrame("/" + baseName);
            if (baseFrame == null) {
                Logger.getInstance().error("ROBOT_EXEC", "Base frame '" + baseName + "' not found in station setup.");
                return false;
            }
            
            // Move through points P9 -> P1
            for (int i = 9; i >= 1; i--) {
                ObjectFrame pointFrame = baseFrame.getChild("P" + i);
                if (pointFrame == null) {
                    Logger.getInstance().error("ROBOT_EXEC", "Frame 'P" + i + "' not found under '" + baseName + "'.");
                    return false;
                }
                gimatic.attachTo(robot.getFlange());
                gimatic.move(ptp(pointFrame).setJointVelocityRel(0.2));
                
                // Lock Gimatic at P8 (contact point)
                if (i == 8) {
                    if (!toolController.lockGimatic()) {
                        return false;
                    }
                }
            }
            
            return true;
            
        } catch (Exception e) {
            Logger.getInstance().error("ROBOT_EXEC", "Exception during pick tool operation for tool " + toolId + ": " + e.getMessage());
            Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", e);
            return false;
        }
    }
    
    /**
     * Places the currently held tool back to its storage base using a standardized motion sequence.
     * Motion sequence: T#Base/P1 → P2 → P3 → P4 → P5 → P6 → P7 → P8 → P9
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
        
        try {
            ObjectFrame baseFrame = application.getApplicationData().getFrame("/" + baseName);
            if (baseFrame == null) {
                Logger.getInstance().error("ROBOT_EXEC", "Base frame '" + baseName + "' not found in station setup.");
                return false;
            }
            
            // Move through points P1 -> P9
            for (int i = 1; i <= 9; i++) {
                ObjectFrame pointFrame = baseFrame.getChild("P" + i);
                if (pointFrame == null) {
                    Logger.getInstance().error("ROBOT_EXEC", "Frame 'P" + i + "' not found under '" + baseName + "'.");
                    return false;
                }
                robot.move(ptp(pointFrame).setJointVelocityRel(0.2));
                
                // Unlock Gimatic at P8 (contact point)
                if (i == 8) {
                    if (!toolController.unlockGimatic()) {
                        return false;
                    }
                }
            }
            
            return true;
            
        } catch (Exception e) {
            Logger.getInstance().error("ROBOT_EXEC", "Exception during place tool operation for tool " + toolId + ": " + e.getMessage());
            Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", e);
            return false;
        }
    }
}

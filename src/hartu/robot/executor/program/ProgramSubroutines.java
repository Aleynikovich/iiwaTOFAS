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
 * 
 * IMPORTANT: All tool change movements must be done with the GimaticCamera tool,
 * as all taught points (P1-P9) were taught with this tool attached.
 */
public class ProgramSubroutines
{
    private final LBR robot;
    private final ToolController toolController;
    private final RoboticsAPIApplication application;
    private final Tool gimaticCameraTool, vacTool;
    
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
        
        // Load the GimaticCamera tool for tool changing operations
        // All taught points were taught with this tool, so it must be used
        try {
            this.gimaticCameraTool = application.getApplicationData().createFromTemplate("GimaticCamera");
            this.vacTool = application.getApplicationData().createFromTemplate("GimaticVac1");
            if (this.gimaticCameraTool != null) {
                Logger.getInstance().log("ROBOT_EXEC", "ProgramSubroutines: Loaded GimaticCamera tool for tool changing operations.");
            } else {
                Logger.getInstance().error("ROBOT_EXEC", "ProgramSubroutines: Failed to load GimaticCamera tool from Object Templates!");
            }
        } catch (Exception e) {
            Logger.getInstance().error("ROBOT_EXEC", "ProgramSubroutines: Exception loading GimaticCamera tool: " + e.getMessage());
            Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", e);
            throw new RuntimeException("Cannot initialize ProgramSubroutines without GimaticCamera tool", e);
        }
    }
    
    /**
     * Picks up a tool from its storage base using a standardized motion sequence.
     * Motion sequence: T#Base/P9 → P8 → P7 → P6 → P5 → P4 → P3 → P2 → P1
     * At P8 (contact point), the Gimatic tool changer is locked.
     * 
     * IMPORTANT: This method uses the GimaticCamera tool for all movements,
     * as all taught points were taught with this tool attached.
     * 
     * @param toolId The tool ID (1-3 corresponding to T1Base, T2Base, T3Base)
     * @return True if the operation executed successfully, false otherwise
     */
    public boolean pickTool(int toolId) {
    	
    	if (toolController.getCurrentToolId() != 0){
    		Logger.getInstance().error("ROBOT_EXEC", "Called for pick tool: " + toolId + ", but Tool ID " + toolController.getCurrentToolId() + " is not 0! Ignoring request.");
            return false;
    	}

        if (toolId < 1 || toolId > 3) {
            Logger.getInstance().error("ROBOT_EXEC", "Invalid tool ID for pick operation: " + toolId + ". Must be 1-3.");
            return false;
        }
        
        if (gimaticCameraTool == null) {
            Logger.getInstance().error("ROBOT_EXEC", "GimaticCamera tool not loaded. Cannot execute pick operation.");
            return false;
        }
        
        String baseName = "T" + toolId + "Base";
        
        try {
            ObjectFrame baseFrame = application.getApplicationData().getFrame("/" + baseName);
            if (baseFrame == null) {
                Logger.getInstance().error("ROBOT_EXEC", "Base frame '" + baseName + "' not found in station setup.");
                return false;
            }
            
            // Attach GimaticCamera tool for the pick operation
            gimaticCameraTool.attachTo(robot.getFlange());
            Logger.getInstance().log("ROBOT_EXEC", "Attached GimaticCamera tool for pick operation.");
            
            // Move through points P9 -> P1
            for (int i = 9; i >= 1; i--) {
                ObjectFrame pointFrame = baseFrame.getChild("P" + i);
                if (pointFrame == null) {
                    Logger.getInstance().error("ROBOT_EXEC", "Frame 'P" + i + "' not found under '" + baseName + "'.");
                    return false;
                }
                
                Logger.getInstance().log("ROBOT_EXEC", "Moving to " + baseName + "/P" + i);
                gimaticCameraTool.move(ptp(pointFrame).setJointVelocityRel(0.2));
                
                // Lock Gimatic at P8 (contact point)
                if (i == 8) {
                    if (!toolController.lockGimatic()) {
                        return false;
                    }
                }
            }
            
            int pickAttempt = 1;
            while (toolController.getCurrentToolId() == 0) {
            	Logger.getInstance().error("ROBOT_EXEC", "Pick tool program completed for tool ID: " + toolId +
            			"but current toolID is still" + toolController.getCurrentToolId() + "retrying in 2 seconds.");
            	Thread.sleep(2000);
            	pickAttempt++;
            	if (pickAttempt > 3) {
            		Logger.getInstance().error("ROBOT_EXEC", "Pick tool program failed after " + pickAttempt + "attempts.");
            		return false;
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
     * IMPORTANT: This method uses the GimaticCamera tool for all movements,
     * as all taught points were taught with this tool attached.
     * 
     * @param toolId The tool ID (1-3 corresponding to T1Base, T2Base, T3Base)
     * @return True if the operation executed successfully, false otherwise
     */
    public boolean placeTool(int toolId) {
    	
    	if (toolController.getCurrentToolId() == 0){
    		Logger.getInstance().warn("ROBOT_EXEC", "Called for place tool: " + toolId + ", but there is already no tool! ID: " + toolController.getCurrentToolId() + " !. Ignoring request.");
            return false;
    	}
    	if (toolId != toolController.getCurrentToolId()){
    		Logger.getInstance().error("ROBOT_EXEC", "Called for place tool: " + toolId + ", but current Tool ID is" + toolController.getCurrentToolId() + " !. Ignoring request to avoid possible collision.");
            return false;
    	}
    	
        if (toolId < 1 || toolId > 3) {
            Logger.getInstance().error("ROBOT_EXEC", "Invalid tool ID for place operation: " + toolId + ". Must be 1-3.");
            return false;
        }
        
        if (gimaticCameraTool == null) {
            Logger.getInstance().error("ROBOT_EXEC", "GimaticCamera tool not loaded. Cannot execute place operation.");
            return false;
        }
        
        String baseName = "T" + toolId + "Base";
        
        try {
            ObjectFrame baseFrame = application.getApplicationData().getFrame("/" + baseName);
            if (baseFrame == null) {
                Logger.getInstance().error("ROBOT_EXEC", "Base frame '" + baseName + "' not found in station setup.");
                return false;
            }
            
            // Attach GimaticCamera tool for the place operation
            gimaticCameraTool.attachTo(robot.getFlange());
            Logger.getInstance().log("ROBOT_EXEC", "Attached GimaticCamera tool for place operation.");
            
            // Move through points P1 -> P9
            for (int i = 1; i <= 9; i++) {
                ObjectFrame pointFrame = baseFrame.getChild("P" + i);
                if (pointFrame == null) {
                    Logger.getInstance().error("ROBOT_EXEC", "Frame 'P" + i + "' not found under '" + baseName + "'.");
                    return false;
                }
                
                Logger.getInstance().log("ROBOT_EXEC", "Moving to " + baseName + "/P" + i);
                gimaticCameraTool.move(ptp(pointFrame).setJointVelocityRel(0.2));
                
                // Unlock Gimatic at P8 (contact point)
                if (i == 8) {
                    if (!toolController.unlockGimatic()) {
                        return false;
                    }
                }
            }
            int releaseAttempt = 1;
            while (toolController.getCurrentToolId() != 0) {
            	Logger.getInstance().error("ROBOT_EXEC", "Release tool program completed for tool ID: " + toolId + "but current toolID is still" + toolController.getCurrentToolId() + "retrying in 2 seconds.");
            	Thread.sleep(2000);
            	releaseAttempt++;
            	if (releaseAttempt > 3) {
            		Logger.getInstance().error("ROBOT_EXEC", "Release tool program failed after " + releaseAttempt + "attempts.");
            		return false;
            	}
            }
            return true;
            
        } catch (Exception e) {
            Logger.getInstance().error("ROBOT_EXEC", "Exception during place tool operation for tool " + toolId + ": " + e.getMessage());
            Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", e);
            return false;
        }
    }
    
    public boolean doParipe() {
        
        try {
        	Logger.getInstance().log("ROBOT_EXEC", "Doing el paripe");
            ObjectFrame baseFrame = application.getApplicationData().getFrame("/ZebraBase");
            
            // Attach GimaticCamera tool for the place operation
            vacTool.attachTo(robot.getFlange());
            Logger.getInstance().log("ROBOT_EXEC", "Attached VacTool");
            
            // Move through points P1 -> P9
            for (int i = 1; i <= 2; i++) {
                ObjectFrame pointFrame = baseFrame.getChild("P" + i);
                if (pointFrame == null) {
                    Logger.getInstance().error("ROBOT_EXEC", "Frame 'P" + i + "' not found under ZebraBase");
                    return false;
                }
                Logger.getInstance().log("ROBOT_EXEC", "Moving to ZebraBase/P" + i);
                vacTool.move(ptp(pointFrame).setJointVelocityRel(0.8));
            }
            
            return true;
            
        } catch (Exception e) {
            Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", e);
            return false;
        }
    }
}

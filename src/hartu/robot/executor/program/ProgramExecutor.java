package hartu.robot.executor.program;

import hartu.robot.commands.ParsedCommand;
import hartu.robot.communication.server.Logger;
import hartu.robot.executor.io.ToolController;

/**
 * Handles execution of program call commands.
 * Maps program IDs to specific operations like tool changing and gripper control.
 */
public class ProgramExecutor {
    
    private final ToolController toolController;
    
    /**
     * Creates a new ProgramExecutor.
     * 
     * @param toolController The tool controller for executing tool operations
     */
    public ProgramExecutor(ToolController toolController) {
        this.toolController = toolController;
    }
    
    /**
     * Executes a program call command.
     * 
     * @param command The ParsedCommand containing program ID
     * @return True if execution was successful, false otherwise
     */
    public boolean executeProgramCall(ParsedCommand command) {
        Integer programId = command.getProgramId();
        if (programId == null) {
            Logger.getInstance().error("ROBOT_EXEC", "Program call command ID " + command.getId() + " has no program ID.");
            return false;
        }

        Logger.getInstance().log("ROBOT_EXEC", "Executing program call command ID " + command.getId() + " with program ID: " + programId);

        try {
            if (programId >= 1 && programId <= 6) {
                return pickTool(programId);
            } else if (programId >= 11 && programId <= 16) {
                return placeCurrentTool(); 
            } else if (programId == 101) {
                return toolController.openTool(0);
            } else if (programId == 102) {
                return toolController.closeTool(0);
            } else {
                Logger.getInstance().error("ROBOT_EXEC", "Invalid program ID: " + programId);
                return false;
            }
        } catch (Throwable t) {
            Logger.getInstance().error("ROBOT_EXEC", "Program call command ID " + command.getId() + " failed with exception: " + t.getClass().getName() + " - " + t.getMessage());
            Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", t instanceof Exception ? (Exception)t : new Exception("Throwable wrapper", t));
            return false;
        }
    }
    
    /**
     * Places the currently attached tool back to its storage position.
     * First detects which tool is attached by reading digital inputs,
     * then moves to the tool's storage position and unlocks the Gimatic tool changer.
     * 
     * @return True if the operation executed successfully, false otherwise.
     */
    private boolean placeCurrentTool() {
    	Logger.getInstance().log("ROBOT_EXEC", "Placing tool");
        return false;
    }

    /**
     * Picks up tool from its storage position.
     * Moves to tool position and locks the Gimatic tool changer.
     * Uses tool-specific base coordinate system but same motion pattern.
     * 
     * @param toolId The ID of the tool to pick (1-99)
     * @return True if the operation executed successfully, false otherwise.
     */
    private boolean pickTool(int toolId) {
    	Logger.getInstance().log("ROBOT_EXEC", "Picking tool " + toolId);
        return false;
    }
}

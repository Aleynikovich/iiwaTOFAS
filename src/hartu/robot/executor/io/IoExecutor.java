package hartu.robot.executor.io;

import hartu.robot.commands.ParsedCommand;
import hartu.robot.commands.io.IoCommandData;
import hartu.robot.communication.server.Logger;

/**
 * Handles execution of IO commands.
 * Maps IO pin numbers to specific tool operations.
 */
public class IoExecutor {
    
    private final ToolController toolController;
    
    /**
     * Creates a new IoExecutor.
     * 
     * @param toolController The tool controller for executing tool operations
     */
    public IoExecutor(ToolController toolController) {
        this.toolController = toolController;
    }
    
    /**
     * Executes an IO command.
     * 
     * @param command The ParsedCommand containing IO data
     * @return True if execution was successful, false otherwise
     */
    public boolean executeIoCommand(ParsedCommand command) {
        IoCommandData ioData = command.getIoCommandData();
        if (ioData == null) {
            Logger.getInstance().error("ROBOT_EXEC", "IO command ID " + command.getId() + " has no IO data.");
            return false;
        }

        int ioPin = ioData.getIoPin();
        boolean ioState = ioData.getIoState();

        Logger.getInstance().log("ROBOT_EXEC", "Executing IO command ID " + command.getId() + ". Pin: " + ioPin + ", State: " + ioState);

        try {
            switch (ioPin) {
                case 1:
                    return toolController.closeTool(0);
                case 2:
                    return toolController.openTool(0);
                case 3:
                    return true;
                case 10:
                    // Lock Gimatic tool changer
                    return toolController.lockGimatic();
                case 11:
                    // Unlock Gimatic tool changer
                    return toolController.unlockGimatic();
                case 12:
                    // Open tool (activate vacuum) - default to tool 1
                    return toolController.openTool(1);
                case 13:
                    // Close tool (blow air) - default to tool 1
                    return toolController.closeTool(1);
                default:
                    Logger.getInstance().error("ROBOT_EXEC", "Invalid IO pin in parsed command for direct mapping: " + ioPin + " for command ID " + command.getId());
                    return false;
            }
        } catch (Throwable t) {
            // Catch ALL exceptions to ensure IO failures don't crash the robot
            Logger.getInstance().error("ROBOT_EXEC", "IO command ID " + command.getId() + " failed with exception: " + t.getClass().getName() + " - " + t.getMessage());
            Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", t instanceof Exception ? (Exception)t : new Exception("Throwable wrapper", t));
            return false;
        }
    }
}

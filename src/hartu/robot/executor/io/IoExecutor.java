package hartu.robot.executor.io;

import hartu.robot.commands.ParsedCommand;
import hartu.robot.commands.io.IoCommandData;
import hartu.robot.communication.server.Logger;
import hartu.robot.io.IOList;

/**
 * Handles execution of IO commands.
 * Uses IOList for all IO operations.
 */
public class IoExecutor {
    
    private final ToolController toolController;
    private final IOList ioList;
    
    // Store the last input state read for response formatting
    private boolean lastInputState = false;
    
    /**
     * Creates a new IoExecutor.
     * 
     * @param toolController The tool controller for executing tool operations
     * @param ioList The IOList for accessing all IOs
     */
    public IoExecutor(ToolController toolController, IOList ioList) {
        this.toolController = toolController;
        this.ioList = ioList;
    }
    
    /**
     * Gets the last input state that was read.
     * This is used by the response handler to format the response correctly.
     * 
     * @return The last input state (true/false)
     */
    public boolean getLastInputState() {
        return lastInputState;
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

        try {
            if (ioData.isOutputCommand()) {
                // ACTIVATE_IO (command 9) - Set digital output
                boolean ioState = ioData.getIoState();
                Logger.getInstance().log("ROBOT_EXEC", "Executing ACTIVATE_IO command ID " + command.getId() + ". Pin: " + ioPin + ", State: " + ioState);
                return executeOutputCommand(ioPin, ioState, command.getId());
            } else if (ioData.isDigitalInputCommand()) {
                // DIGITAL_INPUT (command 12) - Read digital input
                Logger.getInstance().log("ROBOT_EXEC", "Executing DIGITAL_INPUT command ID " + command.getId() + ". Pin: " + ioPin);
                return executeDigitalInputCommand(ioPin, command.getId());
            } else if (ioData.isAnalogInputCommand()) {
                // ANALOG_INPUT (command 13) - Read analog input (not yet implemented)
                Logger.getInstance().log("ROBOT_EXEC", "Executing ANALOG_INPUT command ID " + command.getId() + ". Pin: " + ioPin);
                return executeAnalogInputCommand(ioPin, command.getId());
            } else {
                Logger.getInstance().error("ROBOT_EXEC", "Unknown IO command type for command ID " + command.getId());
                return false;
            }
        } catch (Throwable t) {
            // Catch ALL exceptions to ensure IO failures don't crash the robot
            Logger.getInstance().error("ROBOT_EXEC", "IO command ID " + command.getId() + " failed with exception: " + t.getClass().getName() + " - " + t.getMessage());
            Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", t instanceof Exception ? (Exception)t : new Exception("Throwable wrapper", t));
            return false;
        }
    }
    
    /**
     * Executes an output command using IOList or ToolController.
     * 
     * @param ioPin The IO pin number
     * @param ioState The desired state (true/false)
     * @param commandId The command ID for logging
     * @return True if execution was successful, false otherwise
     */
    private boolean executeOutputCommand(int ioPin, boolean ioState, String commandId) {
        try {
            // First check if this is a special tool controller case (backward compatibility)
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
                    // Use IOList for all other cases
                    ioList.out.set(ioPin, ioState);
                    Logger.getInstance().log("ROBOT_EXEC", "Set output pin " + ioPin + " to " + ioState + " for command ID " + commandId);
                    return true;
            }
        } catch (IllegalArgumentException e) {
            Logger.getInstance().error("ROBOT_EXEC", "Invalid IO pin " + ioPin + " for command ID " + commandId + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Executes a digital input read command using IOList.
     * 
     * @param ioPin The IO pin number to read
     * @param commandId The command ID for logging
     * @return True if execution was successful, false otherwise
     */
    private boolean executeDigitalInputCommand(int ioPin, String commandId) {
        try {
            lastInputState = ioList.in.get(ioPin);
            Logger.getInstance().log("ROBOT_EXEC", "Read digital input pin " + ioPin + " = " + lastInputState + " for command ID " + commandId);
            return true;
        } catch (IllegalArgumentException e) {
            Logger.getInstance().error("ROBOT_EXEC", "Invalid input pin " + ioPin + " for command ID " + commandId + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Executes an analog input read command (placeholder - not yet implemented).
     * 
     * @param ioPin The IO pin number to read
     * @param commandId The command ID for logging
     * @return False (not implemented)
     */
    private boolean executeAnalogInputCommand(int ioPin, String commandId) {
        Logger.getInstance().warn("ROBOT_EXEC", "ANALOG_INPUT (command 13) is not yet implemented. Pin: " + ioPin + ", Command ID: " + commandId);
        // Set lastInputState to false as we don't have analog input support yet
        lastInputState = false;
        return false;
    }
}

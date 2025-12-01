package hartu.robot.executor.program;

import hartu.robot.commands.BaseCoordinateData;
import hartu.robot.commands.ParsedCommand;
import hartu.robot.communication.server.Logger;
import hartu.robot.executor.io.ToolController;

/**
 * Handles execution of program call commands.
 * Maps program IDs to specific operations like tool changing and gripper control.
 */
public class ProgramExecutor
{

    private final ToolController toolController;
    private final ProgramSubroutines programSubroutines;

    /**
     * Creates a new ProgramExecutor.
     *
     * @param toolController     The tool controller for executing tool operations
     * @param programSubroutines The program subroutines for pick/place operations
     */
    public ProgramExecutor(ToolController toolController, ProgramSubroutines programSubroutines)
    {
        this.toolController = toolController;
        this.programSubroutines = programSubroutines;
    }

    /**
     * Executes a program call command.
     * Program ID mapping:
     * - 1-3: Pick tool from T1Base-T3Base
     * - 11-13: Place tool to T1Base-T3Base
     * - 40+: Base data transmission (workpiece coordinates from ROS nodes)
     * - 101: Open tool (global)
     * - 102: Close tool (global)
     *
     * @param command The ParsedCommand containing program ID
     * @return True if execution was successful, false otherwise
     */
    public boolean executeProgramCall(ParsedCommand command)
    {
        Integer programId = command.getProgramId();
        if (programId == null)
        {
            Logger.getInstance().error("ROBOT_EXEC", "Program call command ID " + command.getId() + " has no program ID.");
            return false;
        }

        Logger.getInstance().log("ROBOT_EXEC", "Executing program call command ID " + command.getId() + " with program ID: " + programId);

        try
        {
            // Pick tool operations (program IDs 1-3)
            if (programId >= 1 && programId <= 3)
            {
                int toolId = programId; // Tool ID matches program ID (1-3)
                return programSubroutines.pickTool(toolId);
            }
            // Place tool operations (program IDs 11-13)
            else if (programId >= 11 && programId <= 13)
            {
                int toolId = programId - 10; // Tool ID is program ID minus 10 (11->1, 12->2, 13->3)
                return programSubroutines.placeTool(toolId);
            }
            // Base data transmission operations (program IDs 40+)
            // Used to transmit workpiece coordinates from ROS computer vision nodes
            else if (programId >= 40 && programId < 100)
            {
                return executeBaseDataTransmission(command);
            }
            // Tool gripper control operations
            else if (programId == 101)
            {
                return toolController.openTool(0);
            } else if (programId == 102)
            {
                return toolController.closeTool(0);
            } else if (programId == 20)
            {
                return programSubroutines.doParipe();
            }
            else if (programId == 21)
            {
                return programSubroutines.placeAxisPlaceholder();
            }
            else if (programId == 22)
            {
                return programSubroutines.pickAxis();
            }
            else if (programId == 23)
            {
                return programSubroutines.placeAxisBox();
            }
            else if (programId == 24)
            {
                return programSubroutines.placeDrum();
            }
            else if (programId == 25)
            {
                return programSubroutines.placeDisk();
            }
            // Invalid program ID
            else
            {
                Logger.getInstance().error("ROBOT_EXEC", "Invalid program ID: " + programId);
                return false;
            }
        } catch (Throwable t)
        {
            Logger.getInstance().error("ROBOT_EXEC", "Program call command ID " + command.getId() + " failed with exception: " + t.getClass().getName() + " - " + t.getMessage());
            Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", t instanceof Exception ? (Exception) t : new Exception("Throwable wrapper", t));
            return false;
        }
    }

    /**
     * Executes base data transmission from ROS nodes.
     * This stores the workpiece coordinates and type for subsequent kitting operations.
     * 
     * Program ID mapping for base data:
     * - 40: Store base data for current workpiece
     * 
     * @param command The ParsedCommand containing base coordinate data
     * @return True if execution was successful, false otherwise
     */
    private boolean executeBaseDataTransmission(ParsedCommand command)
    {
        if (!command.hasBaseCoordinateData())
        {
            Logger.getInstance().error("ROBOT_EXEC", "Base data transmission command ID " + command.getId() + " has no base coordinate data.");
            return false;
        }

        BaseCoordinateData baseData = command.getBaseCoordinateData();
        Logger.getInstance().log("ROBOT_EXEC", "Received base data transmission: " + baseData.toString());

        // Store the base coordinate data in ProgramSubroutines for use in kitting operations
        return programSubroutines.storeBaseCoordinateData(baseData);
    }
}

package hartu.robot.executor.program;

import com.kuka.roboticsAPI.geometricModel.Frame;
import hartu.protocols.constants.WorkpieceType;
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
     * - 101: Open tool (global)
     * - 102: Close tool (global)
     * <p>
     * Any program call can optionally include base coordinate data (workpiece position
     * from ROS computer vision nodes). If present, it will be stored before executing
     * the program routine.
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

        Logger.getInstance().low("ROBOT_EXEC", "Executing program call command ID " + command.getId() + " with program ID: " + programId);

        try
        {
            // If base coordinate data is present, store it before executing the program routine
            if (command.hasBaseCoordinateData())
            {
                if (!storeBaseCoordinateData(command))
                {
                    return false;
                }
            }

            // Extract frame and workpiece ID once for subroutines that need them (program IDs 21-25)
            Frame frame = getFrameFromCommand(command);
            int workpieceId = getWorkpieceIdFromCommand(command);

            // Pick tool operations (program IDs 1-3)
            if (programId >= 1 && programId <= 6)
            {
                int toolId = programId; // Tool ID matches program ID (1-3)
                return programSubroutines.pickTool(toolId);
            }
            // Place tool operations (program IDs 11-13)
            else if (programId >= 11 && programId <= 16)
            {
                int toolId = programId - 10; // Tool ID is program ID minus 10 (11->1, 12->2, 13->3)
                return programSubroutines.placeTool(toolId);
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
            } else if (programId == 21)
            {
                return programSubroutines.placeAxisPlaceholder();
            } else if (programId == 22)
            {
                return programSubroutines.pickAxis();
            } else if (programId == 23)
            {
                // Program 23: Place workpiece in box (auto-selects next free position)
                WorkpieceType workpieceType = WorkpieceType.fromId(workpieceId);
                return programSubroutines.placeWorkpieceInBox(frame, workpieceType);
            } else if (programId == 24)
            {
                // Legacy support for specific position placement
                return programSubroutines.placeAxisBox(frame, workpieceId, 2);
            } else if (programId == 25)
            {
                return programSubroutines.placeDisk(frame, workpieceId, 1);
            } else if (programId == 26)
            {
                return programSubroutines.placeDisk(frame, workpieceId, 2);
            } else if (programId == 27)
            {
                return programSubroutines.placeDrum(frame, workpieceId, 1);
            } else if (programId == 28)
            {
                return programSubroutines.placeDrum(frame, workpieceId, 2);
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
     * Stores base coordinate data from ROS nodes.
     * This stores the workpiece coordinates and type for subsequent kitting operations.
     *
     * @param command The ParsedCommand containing base coordinate data
     * @return True if storage was successful, false otherwise
     */
    private boolean storeBaseCoordinateData(ParsedCommand command)
    {
        BaseCoordinateData baseData = command.getBaseCoordinateData();
        Logger.getInstance().critical("ROBOT_EXEC", "Storing base coordinate data: " + baseData.toString());

        // Store the base coordinate data in ProgramSubroutines for use in kitting operations
        return programSubroutines.storeBaseCoordinateData(baseData);
    }

    /**
     * Extracts the Frame from a command's base coordinate data.
     *
     * @param command The ParsedCommand to extract Frame from
     * @return The Frame from base coordinate data, or null if not present
     */
    private Frame getFrameFromCommand(ParsedCommand command)
    {
        if (command.hasBaseCoordinateData())
        {
            BaseCoordinateData baseData = command.getBaseCoordinateData();
            return baseData.getCoordinateFrame();
        }
        return null;
    }

    /**
     * Extracts the workpiece ID from a command's base coordinate data.
     *
     * @param command The ParsedCommand to extract workpiece ID from
     * @return The workpiece ID, or 0 if not present
     */
    private int getWorkpieceIdFromCommand(ParsedCommand command)
    {
        if (command.hasBaseCoordinateData())
        {
            BaseCoordinateData baseData = command.getBaseCoordinateData();
            return baseData.getWorkpieceType().getId();
        }
        return 0;
    }
}

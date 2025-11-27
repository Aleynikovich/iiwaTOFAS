package hartu.robot.executor.io;

import com.kuka.generated.ioAccess.Ethercat_x44IOGroup;
import com.kuka.generated.ioAccess.IOFlangeIOGroup;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import hartu.robot.communication.server.Logger;

/**
 * Controls tool operations including pneumatic tool open/close,
 * Gimatic tool changer lock/unlock, and tool detection.
 */
public class ToolController
{

    private final IOFlangeIOGroup gimaticIO;
    private final Ethercat_x44IOGroup toolControlIO;
    private final MediaFlangeIOGroup mediaFlangeIO;

    /**
     * Creates a new ToolController.
     *
     * @param gimaticIO     The Gimatic IO group for tool changer control
     * @param toolControlIO The tool control IO group
     * @param mediaFlangeIO The media flange IO group for tool detection
     */
    public ToolController(IOFlangeIOGroup gimaticIO, Ethercat_x44IOGroup toolControlIO, MediaFlangeIOGroup mediaFlangeIO)
    {
        this.gimaticIO = gimaticIO;
        this.toolControlIO = toolControlIO;
        this.mediaFlangeIO = mediaFlangeIO;
    }

    /**
     * Detects the currently attached tool from MediaFlange digital inputs.
     * Digital inputs indicate the tool ID in binary format:
     * - InputX3Pin3 (bit 0): value 1
     * - InputX3Pin4 (bit 1): value 2
     * - InputX3Pin10 (bit 2): value 4
     * - InputX3Pin13 (bit 3): value 8
     * - InputX3Pin16 (bit 4): value 16
     * <p>
     * Examples:
     * - Tool 1: InputX3Pin3 = true (binary: 00001)
     * - Tool 3: InputX3Pin3 = true, InputX3Pin4 = true (binary: 00011, decimal: 1+2=3)
     * - No tool: All inputs = false (binary: 00000)
     *
     * @return The ID of the currently attached tool (0 if no tool attached)
     */
    public int getCurrentToolId()
    {
        int toolId = 0;

        if (mediaFlangeIO.getInputX3Pin10())
        {
            toolId += 1;
        }
        if (mediaFlangeIO.getInputX3Pin13())
        {
            toolId += 2;
        }
        if (mediaFlangeIO.getInputX3Pin16())
        {
            toolId += 4;
        }
        if (mediaFlangeIO.getInputX3Pin3())
        {
            toolId += 8;
        }
        if (mediaFlangeIO.getInputX3Pin4())
        {
            toolId += 16;
        }

        Logger.getInstance().log("ROBOT_EXEC", "Current tool ID detected from digital inputs: " + toolId);
        return toolId;
    }

    /**
     * Opens the tool by activating vacuum/suction (same for all tools).
     * Controls the IO outputs to activate suction.
     * This operation is global and works for all pneumatic tools.
     *
     * @param toolId The ID of the tool to open (0 for global operation)
     * @return True if the operation executed successfully, false otherwise.
     */
    public boolean openTool(int toolId)
    {
        try
        {
            String toolDesc = (toolId == 0) ? "(global)" : String.valueOf(toolId);
            Logger.getInstance().log("ROBOT_EXEC", "Opening tool " + toolDesc + " (blowing air)");

            toolControlIO.setOutput3(true);

            if (toolId > 3)
            {
                toolControlIO.setOutput2(true);
                toolControlIO.setOutput1(false);
            } else
            {
                toolControlIO.setOutput2(false);
                toolControlIO.setOutput1(true);
            }
            gimaticIO.setDO_Flange2(true);
            gimaticIO.setDO_Flange1(false);
            Thread.sleep(200);
            toolControlIO.setOutput1(false);
            toolControlIO.setOutput2(false);
            toolControlIO.setOutput3(false);
            Logger.getInstance().log("ROBOT_EXEC", "Tool " + toolDesc + " opened (blowing air)");
            return true;
        } catch (InterruptedException e)
        {
            Logger.getInstance().error("ROBOT_EXEC", "Open tool operation interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Closes the tool by blowing air (same for all tools).
     * Controls the IO outputs to blow air and release vacuum.
     * This operation is global and works for all pneumatic tools.
     *
     * @param toolId The ID of the tool to close (0 for global operation)
     * @return True if the operation executed successfully, false otherwise.
     */
    public boolean closeTool(int toolId)
    {
        try
        {
            String toolDesc = (toolId == 0) ? "(global)" : String.valueOf(toolId);
            Logger.getInstance().log("ROBOT_EXEC", "Closing tool " + toolDesc + " (vacuum on)");

            toolControlIO.setOutput3(true);
            toolControlIO.setOutput2(false);
            toolControlIO.setOutput1(true);
            gimaticIO.setDO_Flange2(false);
            gimaticIO.setDO_Flange1(true);
            Thread.sleep(300);
            toolControlIO.setOutput1(false);
            Logger.getInstance().log("ROBOT_EXEC", "Tool " + toolDesc + " closed (vacuum on)");
            return true;
        } catch (InterruptedException e)
        {
            Logger.getInstance().error("ROBOT_EXEC", "Close tool operation interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Locks the Gimatic tool changer.
     * IO operations to command the locking of the tool changer.
     *
     * @return True if the operation executed successfully, false otherwise.
     */
    public boolean lockGimatic()
    {
        try
        {
            Logger.getInstance().log("ROBOT_EXEC", "Locking Gimatic tool changer");
            gimaticIO.setDO_Flange7(false);
            Thread.sleep(300);
            Logger.getInstance().log("ROBOT_EXEC", "Gimatic tool changer locked");
            return true;
        } catch (InterruptedException e)
        {
            Logger.getInstance().error("ROBOT_EXEC", "Lock Gimatic operation interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Unlocks the Gimatic tool changer.
     * IO operations to command the unlocking of the tool changer.
     *
     * @return True if the operation executed successfully, false otherwise.
     */
    public boolean unlockGimatic()
    {
        try
        {
            Logger.getInstance().log("ROBOT_EXEC", "Unlocking Gimatic tool changer");
            toolControlIO.setOutput3(false);
            gimaticIO.setDO_Flange7(true);
            Thread.sleep(300);
            Logger.getInstance().log("ROBOT_EXEC", "Gimatic tool changer unlocked");
            return true;
        } catch (InterruptedException e)
        {
            Logger.getInstance().error("ROBOT_EXEC", "Unlock Gimatic operation interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }
}

package hartu.robot.executor;

import hartu.robot.communication.server.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps tool IDs to tool names for dynamic tool selection and attachment.
 * <p>
 * Tool ID 0 is reserved for the robot flange (no tool attached).
 * Other tool IDs correspond to specific tools defined in Object Templates.
 * <p>
 * Example mapping:
 * - Tool 0: Flange (no tool)
 * - Tool 1: GimaticCamera
 * - Tool 2: Vacuum1
 * - Tool 3: Vacuum2
 * - Tool 4: Gripper1
 */
public class ToolMapping
{

    private final Map<Integer, String> toolIdToNameMap;

    public ToolMapping()
    {
        toolIdToNameMap = new HashMap<>();
        initializeDefaultMappings();
    }

    /**
     * Initializes the default tool ID to name mappings.
     * Modify this method to match your tool configuration in Sunrise.Workbench.
     */
    private void initializeDefaultMappings()
    {
        // Tool IDs 0-N map to specific tool names
        // These names must match tools defined in Sunrise.Workbench Object Templates
        // Tool 0 is GimaticCamera (used for tool changing operations)
        toolIdToNameMap.put(0, "GimaticCamera");
        toolIdToNameMap.put(1, "GimaticVac1");
        toolIdToNameMap.put(2, "GimaticVac2");
        toolIdToNameMap.put(3, "GimaticVac3");
        Logger.getInstance().log("TOOL_MAPPING", "Initialized tool ID mappings: " + toolIdToNameMap.size() + " entries");
    }

    /**
     * Gets the tool name for a given tool ID.
     *
     * @param toolId The tool ID from the command
     * @return The tool name, or null if tool ID is 0 (flange) or not found
     */
    public String getToolName(int toolId)
    {
        return toolIdToNameMap.get(toolId);
    }

    /**
     * Checks if a tool ID is mapped.
     *
     * @param toolId The tool ID to check
     * @return true if the tool ID has a mapping, false otherwise
     */
    public boolean hasToolId(int toolId)
    {
        return toolIdToNameMap.containsKey(toolId);
    }

    /**
     * Gets the tool ID for a tool name.
     *
     * @param toolName The tool name to look up
     * @return The tool ID, or -1 if not found
     */
    public int getToolId(String toolName)
    {
        if (toolName == null)
        {
            return 0; // Flange
        }

        for (Map.Entry<Integer, String> entry : toolIdToNameMap.entrySet())
        {
            if (toolName.equals(entry.getValue()))
            {
                return entry.getKey();
            }
        }
        return -1; // Not found
    }

    /**
     * Adds or updates a tool mapping.
     *
     * @param toolId   The tool ID
     * @param toolName The tool name (null for flange)
     */
    public void setToolMapping(int toolId, String toolName)
    {
        toolIdToNameMap.put(toolId, toolName);
        Logger.getInstance().log("TOOL_MAPPING", "Mapped tool ID " + toolId + " to '" + toolName + "'");
    }

    /**
     * Gets all tool ID to name mappings.
     *
     * @return Unmodifiable map of tool IDs to names
     */
    public Map<Integer, String> getAllMappings()
    {
        return new HashMap<>(toolIdToNameMap);
    }
}

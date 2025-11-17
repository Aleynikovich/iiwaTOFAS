# Tool Configuration Guide

This guide explains how to configure and use tools with the iiwaTOFAS robot control system using tool ID-based dynamic attachment.

## Overview

The system supports KUKA Tool API integration with **dynamic tool attachment** based on command tool IDs. Each command can specify a tool ID, and the system will automatically attach the corresponding tool before executing the motion. This enables:
- Multiple tools per robot without manual switching
- Tool-specific TCP motion control
- Seamless tool changes between commands
- Tool ID 0 for flange-based operations

## Benefits of Tool ID System

- **Dynamic Tool Switching**: Automatically attach/detach tools based on command tool ID
- **Multiple Tools**: Configure multiple tools, each with unique ID
- **Accurate TCP Control**: Cartesian motions executed relative to tool TCP, not flange
- **Proper Load Compensation**: Robot controller accounts for tool mass and center of gravity
- **Command-Level Control**: Each command specifies which tool to use
- **Zero Manual Intervention**: Tool switching happens automatically

## Tool ID Mapping

The system uses a configurable mapping between tool IDs and tool names:

| Tool ID | Tool Name | Description |
|---------|-----------|-------------|
| 0 | *(none)* | Robot flange - no tool attached |
| 1 | GimaticCamera | Gimatic camera tool |
| 2 | Vacuum1 | First vacuum suction cup |
| 3 | Vacuum2 | Second vacuum suction cup |
| 4 | Gripper1 | First gripper |
| 5 | Gripper2 | Second gripper |

This mapping is defined in `ToolMapping.java` and can be customized for your application.

## Configuring Tools in Sunrise.Workbench

### Step 1: Create Tools in Object Templates

For each tool you want to use:

1. Open your project in KUKA Sunrise.Workbench
2. Navigate to the **Object Templates** view
3. Right-click and select **New > Tool**
4. Name the tool according to the mapping (e.g., "GimaticCamera", "Vacuum1", "Gripper1")

**Important**: Tool names must match exactly what's defined in `ToolMapping.java`

### Step 2: Define Tool Properties

Configure the following properties for your tool:

#### Load Data (Required)
- **Mass**: Total mass of the tool in kg
- **Center of Mass**: X, Y, Z coordinates relative to flange (in mm)
- **Moments of Inertia**: Ixx, Iyy, Izz, Ixy, Ixz, Iyz (in kg·mm²)

**Important**: Accurate load data is critical for safety and performance. Measure or calculate these values carefully.

#### Tool Frames (Required)
- Create at least one frame named **"/TCP"** (Tool Center Point)
- Define the TCP position relative to the tool flange mounting point
- Use X, Y, Z (mm) and A, B, C (degrees) to position the TCP

Example TCP definition for a gripper:
- X: 0 mm (centered on flange)
- Y: 0 mm (centered on flange)
- Z: 150 mm (150mm extension from flange)
- A, B, C: 0 degrees (aligned with flange orientation)

#### 3D Model (Optional)
- Import a 3D CAD model for visualization in Sunrise.Workbench
- Helps with path planning and collision avoidance

### Step 3: Configure Tool ID Mapping

Edit `src/hartu/robot/executor/ToolMapping.java` to match your tools:

```java
private void initializeDefaultMappings() {
    toolIdToNameMap.put(0, null);  // Flange
    toolIdToNameMap.put(1, "GimaticCamera");
    toolIdToNameMap.put(2, "Vacuum1");
    toolIdToNameMap.put(3, "Vacuum2");
    // Add more mappings as needed
}
```

### Step 4: Deploy Configuration

After defining tools and mapping:
1. Save your project
2. Deploy the application to the robot controller
3. All tools are loaded during initialization
4. Tools attach dynamically when commands specify their ID

## How Tool Integration Works

### Initialization Sequence

When CommandExecutor starts:

```java
// 1. Load tool ID to name mapping
toolMapping = new ToolMapping();

// 2. Load all tools from Object Templates into registry
for each tool in mapping:
    tool = getApplicationData().createFromTemplate(toolName);
    toolRegistry.put(toolName, tool);  // Store but don't attach yet

// 3. Tools are ready for dynamic attachment
```

### Command Execution Flow

When a command arrives with tool ID:

```java
// 1. Parse tool ID from command (MotionParameters.tool field)
int toolId = Integer.parseInt(command.getMotionParameters().getTool());

// 2. Get tool name from mapping
String toolName = toolMapping.getToolName(toolId);

// 3. Detach current tool if different
if (currentTool != requestedTool) {
    currentTool.detach();
}

// 4. Attach requested tool
tool = toolRegistry.get(toolName);
tool.attachTo(robot.getFlange());

// 5. Execute motion with tool
tool.moveAsync(motion);  // Motion relative to tool TCP
```

### Tool ID Examples

- **Tool ID 0**: `robot.moveAsync(motion)` - Uses flange, no tool attached
- **Tool ID 1**: `gimaticCamera.moveAsync(motion)` - Uses GimaticCamera TCP
- **Tool ID 2**: `vacuum1.moveAsync(motion)` - Uses Vacuum1 TCP

Joint motions (PTP_AXIS, LIN_AXIS, CIRC_AXIS) behave identically regardless of attached tool.

## Using the System Without Tools

The system supports flange-only operation:
- Send commands with **tool ID 0**
- All motions execute relative to robot flange
- No tool load compensation applied
- Any attached tool is automatically detached

This is suitable for:
- Testing and debugging
- Applications without end-effectors
- Direct flange-mounted operations

## Advanced Tool Configuration

### Multiple Tools

To support multiple tools:
1. Create additional tool templates (e.g., "Gripper1", "Gripper2")
2. Modify CommandExecutor to load specific tools based on command parameters
3. Use `tool.detach()` and alternate tool's `attachTo()` to switch tools programmatically

### Tool Frames

Define multiple frames on a tool for different operations:

```
DefaultTool/
  ├── /TCP            (Main tool center point)
  ├── /GripPoint1     (Left gripper finger)
  ├── /GripPoint2     (Right gripper finger)
  └── /SensorFrame    (Sensor mounting point)
```

Access specific frames in motion commands:
```java
tool.getFrame("/GripPoint1").move(lin(targetFrame));
```

### Safety-Oriented Tools

For safety-certified applications:
1. Mark tool as "Safety-Oriented" in Object Templates
2. Define maximum allowable loads
3. Configure safety monitoring parameters
4. Update safety workpiece when picking/placing objects

## Troubleshooting

### Tool Not Loading

**Symptom**: Log shows "No default tool configured..."

**Solutions**:
- Verify tool is named exactly "DefaultTool" (case-sensitive)
- Check that tool is deployed to the robot controller
- Review Sunrise.Workbench for configuration errors
- Check controller logs for tool loading errors

### Incorrect Motion Behavior

**Symptom**: Robot moves to wrong positions after adding tool

**Solutions**:
- Verify TCP coordinates are correct
- Check tool orientation (A, B, C angles)
- Ensure load data is accurate
- Test with joint motions first (PTP_AXIS) to verify basic functionality

### Load Data Warnings

**Symptom**: Controller shows load monitoring warnings

**Solutions**:
- Re-measure tool mass accurately
- Calculate center of mass position correctly
- Use manufacturer specifications if available
- Perform load identification procedure (KUKA SafeOperation app)

## Command Protocol with Tool ID

The command protocol includes a tool ID field (index 6):

```
ACTION_TYPE|NUM_POINTS|TARGET_POINTS|IO_POINT|IO_PIN|IO_STATE|TOOL_ID|BASE|SPEED_OVERRIDE|ID#
```

### Example Commands

**Flange motion (tool ID 0):**
```
1|1|400.0;0.0;600.0;180.0;0.0;180.0|||0||0.15|cmd_001#
```

**GimaticCamera motion (tool ID 1):**
```
1|1|400.0;0.0;600.0;180.0;0.0;180.0|||1||0.15|cmd_002#
```

**Vacuum1 motion (tool ID 2):**
```
3|1|450.0;50.0;550.0;180.0;0.0;180.0|||2||0.1|cmd_003#
```

**Tool switching in sequence:**
```python
# Use flange
client.send("0|1|0;10;-5;20;0;-15;0|||0||0.2|cmd_001#")

# Switch to Vacuum1, execute motion
client.send("1|1|400;0;600;180;0;180|||2||0.15|cmd_002#")

# Switch to GimaticCamera, execute motion  
client.send("1|1|450;50;550;180;0;180|||1||0.15|cmd_003#")

# Back to flange
client.send("0|1|0;0;0;0;0;0;0|||0||0.2|cmd_004#")
```

## Testing Tool Configuration

### Basic Verification

1. Deploy application with tools configured
2. Check logs for: `"Loaded N tool(s). Tools will be attached dynamically..."`
3. Send commands with different tool IDs
4. Verify tool switching and motion execution in logs

### Recommended Test Sequence

```python
import socket

SERVER_IP = "10.66.171.147"  # Your robot IP
TASK_PORT = 30001

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.connect((SERVER_IP, TASK_PORT))

# Test 1: Flange motion (tool ID 0)
cmd = "0|1|0;10;-5;20;0;-15;0|||0||0.2|test_001#"
sock.send(cmd.encode())
print("Sent flange motion")

# Test 2: Vacuum1 Cartesian motion (tool ID 2)
cmd = "1|1|400;0;600;180;0;180|||2||0.15|test_002#"
sock.send(cmd.encode())
print("Sent Vacuum1 motion - should see tool attachment")

# Test 3: GimaticCamera motion (tool ID 1)
cmd = "1|1|450;50;550;180;0;180|||1||0.15|test_003#"
sock.send(cmd.encode())
print("Sent GimaticCamera motion - should see tool switch")

# Test 4: Back to flange (tool ID 0)
cmd = "0|1|0;0;0;0;0;0;0|||0||0.2|test_004#"
sock.send(cmd.encode())
print("Sent flange motion - should see tool detachment")

sock.close()
```

## References

- **KUKA_PROGRAMMING_GUIDE.md**: Section "Tool and Workpiece Management"
- **README.md**: Main project documentation
- **KUKA Sunrise.OS Manual**: Chapter on Tool Definition and Calibration

## Support

For issues or questions about tool configuration:
1. Check this guide and KUKA_PROGRAMMING_GUIDE.md
2. Review KUKA Sunrise.OS documentation
3. Contact your KUKA system integrator
4. Open an issue on the project repository

---

**Last Updated**: 2024-11-17  
**Compatible with**: KUKA Sunrise.OS 1.11+

# Tool Configuration Guide

This guide explains how to configure and use tools with the iiwaTOFAS robot control system.

## Overview

The system now supports proper KUKA Tool API integration for accurate TCP (Tool Center Point) motion control. When a tool is configured, all Cartesian motion commands will be executed relative to the tool's TCP instead of the robot flange.

## Benefits of Using Tools

- **Accurate TCP Control**: Cartesian motions (LIN, PTP_FRAME, CIRC_FRAME) are executed relative to the tool tip, not the robot flange
- **Proper Load Compensation**: Robot controller accounts for tool mass and center of gravity
- **Multiple Tool Frames**: Define multiple reference frames on your tool (e.g., TCP, gripper fingers, etc.)
- **Safety Compliance**: Proper tool definitions are required for safety-certified applications

## Configuring a Tool in Sunrise.Workbench

### Step 1: Create Tool in Object Templates

1. Open your project in KUKA Sunrise.Workbench
2. Navigate to the **Object Templates** view
3. Right-click and select **New > Tool**
4. Name the tool **"DefaultTool"** (this name is hard-coded in CommandExecutor)

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

### Step 3: Deploy Configuration

After defining the tool:
1. Save your project
2. Deploy the application to the robot controller
3. The tool will be automatically loaded and attached during initialization

## How Tool Integration Works

### Initialization Sequence

When CommandExecutor starts:

```java
// 1. Try to load tool from Object Templates
defaultTool = getApplicationData().createFromTemplate("DefaultTool");

// 2. Attach tool to robot flange if found
if (defaultTool != null) {
    defaultTool.attachTo(iiwa.getFlange());
    Logger.log("Tool 'DefaultTool' attached to robot flange.");
}
```

### Motion Execution

For Cartesian motions (PTP_FRAME, LIN_FRAME, CIRC_FRAME):
- **With Tool**: `tool.moveAsync(motion)` - Motion relative to TCP
- **Without Tool**: `robot.moveAsync(motion)` - Motion relative to flange

For Joint motions (PTP_AXIS, LIN_AXIS, CIRC_AXIS):
- Behavior is identical with or without tool (joint space motion)

## Using the System Without a Tool

If no tool is configured, the system operates exactly as before:
- All motions are executed relative to the robot flange
- No tool load compensation is applied
- This is suitable for applications where the robot carries no end-effector

To disable tool usage:
1. Don't create a "DefaultTool" in Object Templates, or
2. Rename or delete the existing tool template

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

## Command Protocol Extensions

The existing command protocol already includes a tool field in MotionParameters:
```
ACTION_TYPE|NUM_POINTS|POINT_DATA|VELOCITY|ACCELERATION|JERK|ID#
```

While the protocol supports specifying tool names, the current implementation uses only the "DefaultTool". Future enhancements could parse tool names from commands and switch tools dynamically.

## Testing Tool Configuration

### Basic Verification

1. Deploy application with tool configured
2. Check logs for: `"Tool 'DefaultTool' attached to robot flange."`
3. Send a simple Cartesian motion command
4. Verify robot moves correctly relative to TCP

### Recommended Test Sequence

```python
# Test 1: Move to known Cartesian position (with tool)
cmd = "1|1|400.0;0.0;600.0;180.0;0.0;180.0|0.15|0.1|0.05|test_001#"

# Test 2: Joint motion (should be identical with/without tool)
cmd = "0|1|0.0;10.0;-5.0;20.0;0.0;-15.0;0.0|0.2|0.1|0.05|test_002#"

# Test 3: Linear motion (with tool)
cmd = "3|1|450.0;0.0;550.0;180.0;0.0;180.0|0.1|0.08|0.03|test_003#"
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

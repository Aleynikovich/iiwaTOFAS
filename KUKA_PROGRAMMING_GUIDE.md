# KUKA Sunrise.OS Programming Guide for AI Agents

This guide provides comprehensive programming instructions for developing robot applications using the KUKA Sunrise.OS
1.11 API. It is designed to help AI agents write correct, safe, and efficient robot code following KUKA best practices.

## Table of Contents

1. [Application Structure](#application-structure)
2. [Motion Programming](#motion-programming)
3. [Tool and Workpiece Management](#tool-and-workpiece-management)
4. [I/O Operations](#io-operations)
5. [Safety Considerations](#safety-considerations)
6. [Error Handling and Monitoring](#error-handling-and-monitoring)
7. [Best Practices](#best-practices)
8. [Common Patterns](#common-patterns)

---

## Application Structure

### Basic Robot Application Template

Every KUKA robot application extends `RoboticsAPIApplication` and implements two key methods:

```java
public class MyRobotApp extends RoboticsAPIApplication {
    @Inject
    private LBR robot;
    
    @Override
    public void initialize() {
        // Initialization code (executed once at startup)
        // - Attach tools to robot
        // - Configure motion parameters
        // - Initialize variables
    }
    
    @Override
    public void run() {
        // Main program logic (executed repeatedly or once)
        // - Motion commands
        // - I/O operations
        // - Process logic
    }
}
```

### Dependency Injection

KUKA uses dependency injection to provide access to robot resources. Use the `@Inject` annotation:

```java
@Inject
private LBR robot;                    // The robot controller

@Inject
private Controller controller;        // Robot controller

@Inject
@Named("ToolName")
private Tool myTool;                  // Tool defined in object templates

@Inject
@Named("WorkpieceName")
private Workpiece myWorkpiece;        // Workpiece defined in object templates
```

**Important:** The `@Named` annotation must match exactly the name defined in the Object Templates view in
Sunrise.Workbench.

---

## Motion Programming

### Motion Types Overview

| Motion Type | Description    | Use Case                                  |
|-------------|----------------|-------------------------------------------|
| PTP         | Point-to-point | Fastest path between points (joint space) |
| LIN         | Linear         | Straight line motion (Cartesian space)    |
| CIRC        | Circular       | Arc motion through auxiliary point        |
| Spline      | Smooth curve   | Continuous path through multiple points   |

### PTP (Point-to-Point) Motion

PTP motion moves the robot to a target position via the shortest path in joint space. The path is not necessarily a
straight line in Cartesian space.

#### PTP with Frame

```java
// Absolute PTP motion to a frame
robot.move(
    ptp(getApplicationData().getFrame("/TargetFrame"))
        .setJointVelocityRel(0.2)          // 20% of max velocity
        .setJointAccelerationRel(0.1)       // 10% of max acceleration
);
```

#### PTP with Joint Angles

```java
// PTP motion with explicit joint angles (in radians)
robot.move(
    ptp(0.0, Math.toRadians(10), Math.toRadians(-5), 
        Math.toRadians(20), 0.0, Math.toRadians(-15), 0.0)
        .setJointVelocityRel(0.2)
);
```

**Critical:** All 7 axis values must always be specified for joint angle motions.

#### PTP Relative Motion

```java
// Relative PTP motion (offset from current position)
robot.move(
    ptpRel(Transformation.ofDeg(0, 0, 50, 0, 0, 0))  // Move 50mm in Z
        .setJointVelocityRel(0.15)
);
```

### LIN (Linear) Motion

Linear motion moves the robot in a straight line in Cartesian space.

#### LIN with Frame

```java
// Absolute linear motion to a frame
robot.move(
    lin(getApplicationData().getFrame("/TargetFrame"))
        .setCartVelocity(100)              // 100 mm/s
        .setCartAcceleration(50)            // 50 mm/s²
);
```

#### LIN Relative Motion

```java
// Relative linear motion with offset values
robot.move(
    linRel(50, 0, 100)                     // x=50mm, y=0mm, z=100mm
        .setCartVelocity(80)
);

// Relative linear motion with rotation
robot.move(
    linRel(Transformation.ofDeg(10, 20, 30, 0, 0, 15))
        .setCartVelocity(60)
);
```

#### LIN with Reference Frame

```java
// Relative motion in a specific reference frame (e.g., base frame)
Frame baseFrame = getApplicationData().getFrame("/BaseFrame");
robot.move(
    linRel(100, 0, 0, baseFrame)           // Move 100mm in base X direction
        .setCartVelocity(80)
);
```

### CIRC (Circular) Motion

Circular motion creates an arc through an auxiliary point to the end point.

```java
// Circular motion through auxiliary point to end point
Frame auxPoint = getApplicationData().getFrame("/AuxFrame");
Frame endPoint = getApplicationData().getFrame("/EndFrame");

robot.move(
    circ(auxPoint, endPoint)
        .setCartVelocity(80)
);
```

**Important:** The auxiliary point defines the arc. The robot moves from current position through the auxiliary point to
the end point.

### Spline Motion

Spline motions create smooth paths through multiple points without stopping.

#### CP Spline (Cartesian Path)

```java
// Create a spline block with LIN and CIRC segments
SplineCP mySpline = new SplineCP(
    lin(getApplicationData().getFrame("/Point1"))
        .setCartVelocity(100),
    
    circ(getApplicationData().getFrame("/Aux1"),
         getApplicationData().getFrame("/Point2"))
        .setCartVelocity(80),
    
    lin(getApplicationData().getFrame("/Point3"))
        .setCartVelocity(100)
);

// Execute the spline
robot.move(mySpline);
```

#### JP Spline (Joint Path)

```java
// Create a spline block with PTP segments
SplineJP mySpline = new SplineJP(
    ptp(getApplicationData().getFrame("/Point1"))
        .setJointVelocityRel(0.2),
    
    ptp(getApplicationData().getFrame("/Point2"))
        .setJointVelocityRel(0.2),
    
    ptp(getApplicationData().getFrame("/Point3"))
        .setJointVelocityRel(0.2)
);

robot.move(mySpline);
```

#### Spline Best Practices

- **Segment Limit:** Program a maximum of 500 segments per spline block for optimal performance
- **Point Distance:** Maintain distances between points > 5 mm to avoid excessive planning time
- **Process Separation:** Use one spline block per process (e.g., one adhesive seam)
- **Segment Selection:**
    - Use LIN and CIRC for straight lines and arcs
    - Use SPL segments for short distances or when points are close together
- **Path Definition:**
    1. First teach/calculate characteristic points (e.g., direction changes)
    2. Add intermediate points as needed
    3. Optimize by removing unnecessary points

### Motion Parameters

Motion parameters control the speed, acceleration, and other characteristics of robot movements.

#### Velocity Settings

```java
// Joint velocity (relative to maximum)
.setJointVelocityRel(0.2)              // 20% of maximum joint velocity

// Cartesian velocity (absolute in mm/s)
.setCartVelocity(150)                  // 150 mm/s

// Axis-specific velocity (for individual axes)
.setJointVelocityRel(JointEnum.J1, 0.3)  // 30% for axis 1
```

#### Acceleration Settings

```java
// Joint acceleration (relative)
.setJointAccelerationRel(0.1)          // 10% of maximum

// Cartesian acceleration (absolute in mm/s²)
.setCartAcceleration(100)              // 100 mm/s²

// Axis-specific acceleration
.setJointAccelerationRel(JointEnum.J2, 0.15)
```

#### Jerk Settings

```java
// Joint jerk (relative)
.setJointJerkRel(0.05)                 // 5% of maximum

// Axis-specific jerk
.setJointJerkRel(JointEnum.J3, 0.08)
```

#### Blending

Blending allows smooth transitions between motions without stopping:

```java
// Set blending distance (in mm)
robot.move(
    lin(point1).setBlendingCart(5.0)   // Blend within 5mm of target
);

// Set blending for joint motion (relative)
robot.move(
    ptp(point2).setBlendingRel(0.1)    // Blend at 10% before target
);
```

---

## Tool and Workpiece Management

### Defining Tools and Workpieces

Tools and workpieces are defined in the **Object Templates** view in Sunrise.Workbench with:

- Load data (mass, center of mass, moments of inertia)
- Geometric data (3D model)
- Frames (TCP for tools, attachment points for workpieces)

### Integrating into Application

```java
@Inject
@Named("Gripper")
private Tool gripper;

@Inject
@Named("ComponentA")
private Workpiece component;
```

### Attaching Tools

#### Attach Tool to Robot Flange

```java
@Override
public void initialize() {
    // Attach tool to robot flange
    gripper.attachTo(robot.getFlange());
    
    // Set default motion frame (TCP)
    gripper.getFrame("/TCP").move(
        ptp(getApplicationData().getFrame("/TargetFrame"))
    );
}
```

**Important:** Tools must be attached in the `initialize()` method before use.

### Attaching Workpieces

#### Attach to Tool

```java
// Attach workpiece to gripper TCP
component.attachTo(gripper.getFrame("/TCP"));

// After manipulation, detach workpiece
component.detach();
```

#### Attach with Custom Frame

```java
// Attach workpiece using a custom grip frame
component.getFrame("/GripPoint").attachTo(gripper.getFrame("/TCP"));
```

### Safety-Oriented Load Management

When using safety-oriented tools/workpieces, you must inform the safety controller of load changes:

```java
@Override
public void run() {
    // Pick up component
    component.attachTo(gripper.getDefaultMotionFrame());
    robot.setSafetyWorkpiece(component);      // Notify safety controller
    
    // Perform operations...
    
    // Put down component
    component.detach();
    robot.setSafetyWorkpiece(null);           // Clear safety workpiece
}
```

**Critical:** Always update the safety controller when picking up or setting down safety-oriented workpieces.

### Default Motion Frame

Set a default motion frame for simplified motion programming:

```java
// Set TCP as default motion frame
gripper.getFrame("/TCP").move(
    lin(targetFrame).setCartVelocity(100)
);
```

---

## I/O Operations

### Accessing I/O Groups

I/O groups are automatically generated when exporting from WorkVisual. Each group has methods for reading inputs and
writing outputs.

```java
@Inject
private MediaFlangeIOGroup mediaFlange;

@Inject
private Ethercat_x44IOGroup fieldbus;
```

**Warning:** Never manually modify generated I/O classes in `com.kuka.generated.ioAccess`. To extend functionality,
create derived classes.

### Digital Outputs

```java
// Set digital output to HIGH
mediaFlange.setOutput_1(true);

// Set digital output to LOW
mediaFlange.setOutput_1(false);

// Set multiple outputs
mediaFlange.setOutput_1(true);
mediaFlange.setOutput_2(false);
mediaFlange.setOutput_3(true);
```

### Digital Inputs

```java
// Read digital input
boolean inputState = mediaFlange.getInput_1();

// Use in conditional logic
if (mediaFlange.getInput_1()) {
    // Input is HIGH
    robot.move(lin(targetFrame));
} else {
    // Input is LOW
    getLogger().info("Waiting for input signal");
}
```

### Analog Outputs

```java
// Set analog output voltage (in volts)
mediaFlange.setAnalogOutput_1(5.0);  // Set to 5V

// Set analog output current (in mA)
mediaFlange.setAnalogOutput_2(10.0); // Set to 10mA
```

### Analog Inputs

```java
// Read analog input voltage
double voltage = mediaFlange.getAnalogInput_1();

// Read analog input current
double current = mediaFlange.getAnalogInput_2();

// Use in control logic
if (voltage > 3.0) {
    // Perform action based on voltage level
}
```

### I/O in Motion Commands

Trigger I/O during motion using triggers:

```java
// Set output when reaching 50% of path
robot.move(
    lin(targetFrame)
        .setCartVelocity(100)
        .triggerWhen(
            new PathCondition(0.5),          // At 50% of path
            new SetOutput(mediaFlange, Output_1, true)
        )
);
```

---

## Safety Considerations

### Safety Controller Integration

The KUKA robot has two controller components:

1. **Motion Controller:** Controls robot movements
2. **Safety Controller:** Monitors and enforces safety limits

### Load Data Requirements

**Critical:** Incorrect load data can cause:

- Failed position/torque referencing
- Collision detection failures
- Robot damage or safety violations

Always ensure:

- Tool mass and center of mass are correctly configured
- Workpiece mass and center of mass are correctly configured
- Load changes are reported to the safety controller when using safety-oriented tools/workpieces

### Safety-Oriented Workpiece Management

```java
// When picking up safety-oriented workpiece
component.attachTo(gripper.getFrame("/TCP"));
robot.setSafetyWorkpiece(component);

// When setting down safety-oriented workpiece
component.detach();
robot.setSafetyWorkpiece(null);
```

### Operating Modes

The robot has different operating modes with different safety requirements:

| Mode | Description                    | Typical Use                     |
|------|--------------------------------|---------------------------------|
| T1   | Manual mode with reduced speed | Teaching, testing, verification |
| T2   | Manual mode with higher speed  | Testing, debugging              |
| AUT  | Automatic mode                 | Production operation            |

**Best Practice:** Always test new programs in T1 mode first.

### Emergency Stop

Always ensure:

- Emergency stop button is accessible
- Workspace is clear before running programs
- Proper safety fencing is in place for automatic operation

### Workspace Monitoring

```java
// Check if robot is in safe state before motion
if (robot.isReadyToMove()) {
    robot.move(ptp(targetFrame));
} else {
    getLogger().warn("Robot not ready to move");
}
```

---

## Error Handling and Monitoring

### Exception Handling

Always wrap motion commands in try-catch blocks for production code:

```java
@Override
public void run() {
    try {
        robot.move(ptp(targetFrame).setJointVelocityRel(0.2));
    } catch (CommandInvalidException e) {
        getLogger().error("Invalid command: " + e.getMessage());
    } catch (RoboticsException e) {
        getLogger().error("Robot error: " + e.getMessage());
    }
}
```

### Common Exception Types

- `CommandInvalidException`: Invalid motion command parameters
- `CollisionDetectedException`: Collision detected during motion
- `RoboticsException`: General robot error (base class)
- `RobotConnectionException`: Lost connection to robot

### Logging

Use the built-in logger for debugging and monitoring:

```java
// Information logging
getLogger().info("Starting motion sequence");

// Warning logging
getLogger().warn("Input signal not received within timeout");

// Error logging
getLogger().error("Failed to complete motion: " + errorMessage);

// Debug logging
getLogger().debug("Current position: " + robot.getCurrentCartesianPosition());
```

### Monitoring Conditions

Monitor robot state and external conditions during motion:

```java
// Create a force condition
ICondition forceCondition = ForceCondition.createNormalForceCondition(
    robot.getFlange(),
    CoordinateAxis.Z,
    50.0  // 50N threshold
);

// Use condition as break condition
IMotionContainer container = robot.moveAsync(
    lin(targetFrame)
        .setCartVelocity(100)
        .breakWhen(forceCondition)
);

// Wait for motion or condition
container.await();
```

### Listener Pattern for Monitoring

```java
// Create a listener for force monitoring
IConditionListener listener = new IConditionListener() {
    @Override
    public void onConditionChange(ICondition condition, boolean newValue) {
        if (newValue) {
            getLogger().info("Force threshold exceeded");
            // React to condition
        }
    }
};

// Register listener
forceCondition.addListener(listener);

// Perform motion with monitoring
robot.move(lin(targetFrame).setCartVelocity(100));

// Remove listener when done
forceCondition.removeListener(listener);
```

---

## Best Practices

### 1. Motion Planning

**Do:**

- Test motions in T1 mode first
- Use appropriate motion types (PTP for fast positioning, LIN for process paths)
- Set reasonable velocity and acceleration values
- Use blending for smooth continuous motion

**Don't:**

- Use maximum velocity for all motions
- Execute untested motions in automatic mode
- Ignore collision warnings
- Program very short motion segments without blending

### 2. Tool and Workpiece Management

**Do:**

- Attach tools in `initialize()` method
- Update safety controller for load changes
- Define accurate load data in Object Templates
- Use meaningful frame names

**Don't:**

- Forget to detach workpieces after use
- Modify load data during motion
- Use undefined or incorrect frame names
- Skip safety workpiece updates

### 3. Error Handling

**Do:**

- Wrap motion commands in try-catch blocks
- Log errors with meaningful messages
- Check robot state before motions
- Handle exceptions gracefully

**Don't:**

- Ignore exceptions
- Use empty catch blocks
- Continue program after critical errors
- Suppress safety warnings

### 4. Code Organization

**Do:**

- Use meaningful variable names
- Comment complex logic
- Organize code into logical sections
- Use dependency injection properly

**Don't:**

- Hardcode values (use constants or application data)
- Create monolithic run() methods
- Duplicate code
- Mix initialization and runtime code

### 5. Performance

**Do:**

- Use spline motions for continuous paths
- Optimize motion parameters for process
- Use relative motions when appropriate
- Profile and test performance

**Don't:**

- Create excessive small motions
- Use inefficient path planning
- Ignore planning time warnings
- Over-complicate motion sequences

---

## Common Patterns

### Pattern 1: Pick and Place

```java
@Override
public void run() {
    try {
        // Move to pick position
        robot.move(
            ptp(getApplicationData().getFrame("/PickApproach"))
                .setJointVelocityRel(0.3)
        );
        
        // Linear approach
        robot.move(
            lin(getApplicationData().getFrame("/PickPosition"))
                .setCartVelocity(50)
        );
        
        // Close gripper (via I/O)
        mediaFlange.setOutput_1(true);
        ThreadUtil.milliSleep(500);
        
        // Attach workpiece
        component.attachTo(gripper.getFrame("/TCP"));
        robot.setSafetyWorkpiece(component);
        
        // Retract
        robot.move(
            linRel(0, 0, 50)
                .setCartVelocity(50)
        );
        
        // Move to place position
        robot.move(
            ptp(getApplicationData().getFrame("/PlaceApproach"))
                .setJointVelocityRel(0.3)
        );
        
        // Linear approach
        robot.move(
            lin(getApplicationData().getFrame("/PlacePosition"))
                .setCartVelocity(50)
        );
        
        // Open gripper
        mediaFlange.setOutput_1(false);
        ThreadUtil.milliSleep(500);
        
        // Detach workpiece
        component.detach();
        robot.setSafetyWorkpiece(null);
        
        // Retract
        robot.move(
            linRel(0, 0, 50)
                .setCartVelocity(50)
        );
        
        // Return to home
        robot.move(
            ptp(getApplicationData().getFrame("/Home"))
                .setJointVelocityRel(0.4)
        );
        
    } catch (Exception e) {
        getLogger().error("Pick and place failed: " + e.getMessage());
    }
}
```

### Pattern 2: Continuous Path Processing

```java
@Override
public void run() {
    try {
        // Move to start position
        robot.move(
            ptp(getApplicationData().getFrame("/PathStart"))
                .setJointVelocityRel(0.3)
        );
        
        // Create continuous path with spline
        SplineCP processPath = new SplineCP(
            lin(getApplicationData().getFrame("/Point1"))
                .setCartVelocity(100)
                .setBlendingCart(2.0),
            
            lin(getApplicationData().getFrame("/Point2"))
                .setCartVelocity(100)
                .setBlendingCart(2.0),
            
            circ(getApplicationData().getFrame("/Aux1"),
                 getApplicationData().getFrame("/Point3"))
                .setCartVelocity(80)
                .setBlendingCart(2.0),
            
            lin(getApplicationData().getFrame("/Point4"))
                .setCartVelocity(100)
        );
        
        // Execute process path
        robot.move(processPath);
        
        // Return to home
        robot.move(
            ptp(getApplicationData().getFrame("/Home"))
                .setJointVelocityRel(0.4)
        );
        
    } catch (Exception e) {
        getLogger().error("Path processing failed: " + e.getMessage());
    }
}
```

### Pattern 3: Force-Controlled Assembly

```java
@Override
public void run() {
    try {
        // Approach assembly position
        robot.move(
            ptp(getApplicationData().getFrame("/AssemblyApproach"))
                .setJointVelocityRel(0.2)
        );
        
        // Create force condition for contact detection
        ICondition forceCondition = ForceCondition.createNormalForceCondition(
            robot.getFlange(),
            CoordinateAxis.Z,
            20.0  // 20N threshold
        );
        
        // Slow approach with force monitoring
        IMotionContainer container = robot.moveAsync(
            lin(getApplicationData().getFrame("/AssemblyPosition"))
                .setCartVelocity(10)
                .breakWhen(forceCondition)
        );
        
        // Wait for contact or target
        container.await();
        
        if (forceCondition.isTrue()) {
            getLogger().info("Contact detected, starting assembly");
            
            // Apply assembly force
            robot.move(
                linRel(0, 0, -5)  // Push down 5mm
                    .setCartVelocity(2)
            );
            
            // Hold for 2 seconds
            ThreadUtil.milliSleep(2000);
        }
        
        // Retract
        robot.move(
            linRel(0, 0, 50)
                .setCartVelocity(50)
        );
        
    } catch (Exception e) {
        getLogger().error("Assembly failed: " + e.getMessage());
    }
}
```

### Pattern 4: Waiting for External Signal

```java
@Override
public void run() {
    try {
        // Move to wait position
        robot.move(
            ptp(getApplicationData().getFrame("/WaitPosition"))
                .setJointVelocityRel(0.3)
        );
        
        getLogger().info("Waiting for start signal on Input 1");
        
        // Wait for digital input
        while (!mediaFlange.getInput_1()) {
            ThreadUtil.milliSleep(100);
        }
        
        getLogger().info("Start signal received, beginning process");
        
        // Perform process
        robot.move(
            lin(getApplicationData().getFrame("/ProcessPosition"))
                .setCartVelocity(100)
        );
        
        // Signal completion
        mediaFlange.setOutput_1(true);
        ThreadUtil.milliSleep(500);
        mediaFlange.setOutput_1(false);
        
    } catch (Exception e) {
        getLogger().error("Process failed: " + e.getMessage());
    }
}
```

### Pattern 5: Repeating Motion Sequence

```java
@Override
public void run() {
    try {
        int cycleCount = 10;
        
        for (int i = 0; i < cycleCount; i++) {
            getLogger().info("Starting cycle " + (i + 1) + " of " + cycleCount);
            
            // Move to position A
            robot.move(
                ptp(getApplicationData().getFrame("/PositionA"))
                    .setJointVelocityRel(0.3)
            );
            
            // Perform operation at A
            ThreadUtil.milliSleep(1000);
            
            // Move to position B
            robot.move(
                ptp(getApplicationData().getFrame("/PositionB"))
                    .setJointVelocityRel(0.3)
            );
            
            // Perform operation at B
            ThreadUtil.milliSleep(1000);
            
            // Check for abort condition
            if (mediaFlange.getInput_1()) {
                getLogger().warn("Abort signal received, stopping cycle");
                break;
            }
        }
        
        // Return to home
        robot.move(
            ptp(getApplicationData().getFrame("/Home"))
                .setJointVelocityRel(0.4)
        );
        
        getLogger().info("Sequence completed successfully");
        
    } catch (Exception e) {
        getLogger().error("Sequence failed: " + e.getMessage());
    }
}
```

---

## Quick Reference

### Essential Imports

```java
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.Workpiece;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;
import com.kuka.roboticsAPI.motionModel.PTP;
import com.kuka.roboticsAPI.motionModel.Spline;
import com.kuka.roboticsAPI.motionModel.SplineCP;
import com.kuka.roboticsAPI.motionModel.SplineJP;
import com.kuka.roboticsAPI.conditionModel.ICondition;
import com.kuka.roboticsAPI.conditionModel.ForceCondition;
import javax.inject.Inject;
import javax.inject.Named;
```

### Motion Commands Quick Reference

```java
// PTP motion
ptp(frame).setJointVelocityRel(0.2)

// Linear motion
lin(frame).setCartVelocity(100)

// Circular motion
circ(auxFrame, endFrame).setCartVelocity(80)

// Relative motions
ptpRel(Transformation.ofDeg(x, y, z, a, b, c))
linRel(x, y, z)

// Blending
.setBlendingCart(5.0)         // 5mm blending
.setBlendingRel(0.1)          // 10% blending
```

### Common Method Calls

```java
// Attach/detach
tool.attachTo(robot.getFlange())
workpiece.attachTo(tool.getFrame("/TCP"))
workpiece.detach()

// Safety workpiece
robot.setSafetyWorkpiece(workpiece)
robot.setSafetyWorkpiece(null)

// Get frames
getApplicationData().getFrame("/FrameName")
tool.getFrame("/TCP")

// I/O operations
ioGroup.setOutput_1(true)
boolean state = ioGroup.getInput_1()

// Logging
getLogger().info("message")
getLogger().warn("message")
getLogger().error("message")
```

---

## Summary

This guide provides the essential information for programming KUKA robots using the Sunrise.OS API. Key takeaways:

1. **Always prioritize safety:** Test in T1 mode, use correct load data, handle exceptions
2. **Use appropriate motion types:** PTP for positioning, LIN for paths, spline for continuous motion
3. **Manage tools and workpieces properly:** Attach/detach correctly, update safety controller
4. **Handle errors gracefully:** Use try-catch blocks, log meaningful messages
5. **Follow best practices:** Organize code well, use meaningful names, avoid hardcoding

For more detailed information, consult the KUKA Sunrise.OS 1.11 Operating and Programming Instructions manual.

---

**Document Version:** 1.0  
**Based on:** KUKA Sunrise.OS 1.11 SI V1  
**Last Updated:** 2025-01-12

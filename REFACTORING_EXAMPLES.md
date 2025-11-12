# Refactoring Examples - Before and After

**Practical examples showing how to apply refactoring patterns to iiwaTOFAS codebase**

---

## Example 1: CommandExecutor - Extract Motion Executor

### BEFORE (CommandExecutor.java - 596 lines)

```java
public class CommandExecutor extends RoboticsAPIApplication {
    
    @Inject
    private LBR iiwa;
    @Inject
    private IOFlangeIOGroup gimaticIO;
    @Inject
    private Ethercat_x44IOGroup toolControlIO;
    
    @Override
    public void run() {
        while (true) {
            CommandResultHolder resultHolder = CommandQueue.pollCommand(100, TimeUnit.MILLISECONDS);
            
            if (resultHolder != null) {
                ParsedCommand command = resultHolder.getCommand();
                boolean executionSuccess = false;
                
                try {
                    switch (command.getCommandCategory()) {
                        case MOVEMENT:
                            executionSuccess = executeMovementCommand(command);
                            break;
                        case IO:
                            executionSuccess = executeIO(command);
                            break;
                        case PROGRAM_CALL:
                            executionSuccess = executeProgramCallCommand(command);
                            break;
                    }
                } catch (Exception e) {
                    Logger.getInstance().error("ROBOT_EXEC", "Error: " + e.getMessage());
                    executionSuccess = false;
                } finally {
                    resultHolder.setSuccess(executionSuccess);
                    resultHolder.getLatch().countDown();
                }
            }
        }
    }
    
    // 100+ lines of motion execution logic
    private boolean executeMovementCommand(ParsedCommand command) {
        ActionTypes actionType = command.getActionType();
        List<IMotion> motions;
        
        MovementType movementType = actionType.getMovementType();
        if (movementType == MovementType.PTP) {
            if (actionType.isJointMotion()) {
                motions = createPtpJointMotions(command);
            } else {
                motions = createPtpCartesianMotions(command);
            }
        } else if (movementType == MovementType.LIN) {
            motions = createLinMotions(command);
        } else if (movementType == MovementType.CIRC) {
            motions = createCircMotions(command);
        } else {
            Logger.getInstance().error("ROBOT_EXEC", "Unsupported ActionType");
            return false;
        }
        
        // ... 50+ more lines of execution logic
    }
    
    // 30+ lines for each motion type
    private List<IMotion> createPtpJointMotions(ParsedCommand command) { }
    private List<IMotion> createPtpCartesianMotions(ParsedCommand command) { }
    private List<IMotion> createLinMotions(ParsedCommand command) { }
    private List<IMotion> createCircMotions(ParsedCommand command) { }
    
    // 80+ lines of IO logic
    private boolean executeIO(ParsedCommand command) { }
    private boolean lockGimatic() { }
    private boolean unlockGimatic() { }
    private boolean openTool(int toolId) { }
    private boolean closeTool(int toolId) { }
    
    // 100+ lines of program call logic
    private boolean executeProgramCallCommand(ParsedCommand command) { }
    private boolean pickTool(int toolId) { }
    private boolean placeTool() { }
}
```

**Problems:**
- 596 lines in one file
- Mixed responsibilities (motion, IO, programs)
- Hard to test individual components
- Hard to find specific functionality

---

### AFTER - Refactored Structure

#### CommandExecutor.java (~150 lines)
```java
public class CommandExecutor extends RoboticsAPIApplication {
    
    @Inject
    private LBR iiwa;
    @Inject
    private IOFlangeIOGroup gimaticIO;
    @Inject
    private Ethercat_x44IOGroup toolControlIO;
    @Inject
    private MediaFlangeIOGroup mediaFlangeIO;
    
    private MotionExecutor motionExecutor;
    private IoExecutor ioExecutor;
    private ProgramExecutor programExecutor;
    
    @Override
    public void initialize() {
        // Delegate to specialized executors
        this.motionExecutor = new MotionExecutor(iiwa);
        this.ioExecutor = new IoExecutor(gimaticIO, toolControlIO);
        this.programExecutor = new ProgramExecutor(iiwa, gimaticIO, toolControlIO);
        
        Logger.getInstance().log("ROBOT_EXEC", "CommandExecutor initialized");
    }
    
    @Override
    public void run() {
        try {
            while (true) {
                CommandResultHolder resultHolder = CommandQueue.pollCommand(100, TimeUnit.MILLISECONDS);
                
                if (resultHolder != null) {
                    boolean success = executeCommand(resultHolder.getCommand());
                    resultHolder.setSuccess(success);
                    resultHolder.getLatch().countDown();
                }
            }
        } catch (Exception e) {
            Logger.getInstance().error("ROBOT_EXEC", "Critical error in run loop", e);
        }
    }
    
    /**
     * Executes a parsed command by delegating to the appropriate executor.
     * 
     * @param command The parsed command to execute
     * @return true if execution succeeded, false otherwise
     */
    private boolean executeCommand(ParsedCommand command) {
        try {
            switch (command.getCommandCategory()) {
                case MOVEMENT:
                    return motionExecutor.executeMotion(command);
                case IO:
                    return ioExecutor.executeIoCommand(command);
                case PROGRAM_CALL:
                    return programExecutor.executeProgramCall(command);
                default:
                    Logger.getInstance().warn("ROBOT_EXEC", "Unknown command category: " + 
                        command.getCommandCategory());
                    return false;
            }
        } catch (Exception e) {
            Logger.getInstance().error("ROBOT_EXEC", "Error executing command ID " + 
                command.getId() + ": " + e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public void dispose() {
        Logger.getInstance().log("ROBOT_EXEC", "CommandExecutor disposing");
        super.dispose();
    }
}
```

#### MotionExecutor.java (~180 lines)
```java
package hartu.robot.executor.motion;

import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.executionModel.*;
import com.kuka.roboticsAPI.motionModel.*;
import hartu.robot.commands.ParsedCommand;
import hartu.robot.communication.server.Logger;
import java.util.List;

/**
 * Executes motion commands for the robot.
 * Handles PTP, LIN, and CIRC motion types in both joint and Cartesian space.
 */
public class MotionExecutor {
    
    private final LBR robot;
    private final MotionFactory motionFactory;
    
    public MotionExecutor(LBR robot) {
        this.robot = robot;
        this.motionFactory = new MotionFactory();
    }
    
    /**
     * Executes a motion command.
     * 
     * @param command The parsed motion command
     * @return true if motion executed successfully, false otherwise
     */
    public boolean executeMotion(ParsedCommand command) {
        Logger.getInstance().log("MOTION", "Executing " + command.getActionType().name() + 
            " command ID " + command.getId());
        
        List<IMotion> motions = motionFactory.createMotions(command);
        
        if (motions.isEmpty()) {
            Logger.getInstance().error("MOTION", "Failed to create motions for command ID " + 
                command.getId());
            return false;
        }
        
        return executeMotionSequence(motions, command.getId());
    }
    
    /**
     * Executes a sequence of motions, handling them as a batch if multiple motions exist.
     */
    private boolean executeMotionSequence(List<IMotion> motions, String commandId) {
        try {
            IMotion motionToExecute = createMotionOrBatch(motions);
            
            Logger.getInstance().log("MOTION", "Executing motion for command ID " + commandId);
            IMotionContainer container = robot.moveAsync(motionToExecute);
            container.await();
            
            Logger.getInstance().log("MOTION", "Motion completed successfully for command ID " + commandId);
            return true;
            
        } catch (CommandInvalidException e) {
            Logger.getInstance().error("MOTION", "Invalid motion parameters: " + e.getMessage());
            return false;
        } catch (CancelledException e) {
            Logger.getInstance().warn("MOTION", "Motion cancelled: " + e.getMessage());
            return false;
        } catch (ExternalStopException e) {
            Logger.getInstance().warn("MOTION", "External stop triggered: " + e.getMessage());
            return false;
        } catch (Exception e) {
            Logger.getInstance().error("MOTION", "Unexpected error during motion execution", e);
            return false;
        }
    }
    
    /**
     * Creates a single motion or MotionBatch depending on the number of motions.
     */
    private IMotion createMotionOrBatch(List<IMotion> motions) {
        if (motions.size() == 1) {
            return motions.get(0);
        } else {
            return new MotionBatch(motions.toArray(new RobotMotion[0]));
        }
    }
}
```

#### IoExecutor.java (~120 lines)
```java
package hartu.robot.executor.io;

import com.kuka.generated.ioAccess.*;
import hartu.robot.commands.ParsedCommand;
import hartu.robot.commands.io.IoCommandData;
import hartu.robot.communication.server.Logger;

/**
 * Executes IO commands for digital outputs and tool control.
 */
public class IoExecutor {
    
    private final IOFlangeIOGroup flangeIO;
    private final Ethercat_x44IOGroup ethernetIO;
    private final GimaticToolChanger toolChanger;
    
    public IoExecutor(IOFlangeIOGroup flangeIO, Ethercat_x44IOGroup ethernetIO) {
        this.flangeIO = flangeIO;
        this.ethernetIO = ethernetIO;
        this.toolChanger = new GimaticToolChanger(flangeIO);
    }
    
    /**
     * Executes an IO command.
     * 
     * @param command The parsed IO command
     * @return true if execution succeeded, false otherwise
     */
    public boolean executeIoCommand(ParsedCommand command) {
        IoCommandData ioData = command.getIoCommandData();
        if (ioData == null) {
            Logger.getInstance().error("IO", "Command ID " + command.getId() + 
                " has no IO data");
            return false;
        }
        
        int pin = ioData.getIoPin();
        boolean state = ioData.getIoState();
        
        Logger.getInstance().log("IO", "Executing IO command. Pin: " + pin + 
            ", State: " + state);
        
        return executeIoOperation(pin, state);
    }
    
    /**
     * Routes IO operation to the appropriate handler based on pin number.
     */
    private boolean executeIoOperation(int pin, boolean state) {
        try {
            switch (pin) {
                case IoPinConfiguration.FLANGE_IO_PIN_7:
                    return setFlangeOutput(7, state);
                    
                case IoPinConfiguration.ETHERCAT_OUTPUT_1:
                    return setEthercatOutput(1, state);
                    
                case IoPinConfiguration.ETHERCAT_OUTPUT_2:
                    return setEthercatOutput(2, state);
                    
                case IoPinConfiguration.GIMATIC_LOCK_PIN:
                    return toolChanger.lock();
                    
                case IoPinConfiguration.GIMATIC_UNLOCK_PIN:
                    return toolChanger.unlock();
                    
                default:
                    Logger.getInstance().error("IO", "Invalid IO pin: " + pin);
                    return false;
            }
        } catch (Exception e) {
            Logger.getInstance().error("IO", "IO operation failed: " + e.getMessage(), e);
            return false;
        }
    }
    
    private boolean setFlangeOutput(int outputNumber, boolean state) {
        flangeIO.setDO_Flange7(state);
        Logger.getInstance().log("IO", "Set flange output " + outputNumber + " to " + state);
        return true;
    }
    
    private boolean setEthercatOutput(int outputNumber, boolean state) {
        if (outputNumber == 1) {
            ethernetIO.setOutput1(state);
        } else if (outputNumber == 2) {
            ethernetIO.setOutput2(state);
        }
        Logger.getInstance().log("IO", "Set Ethercat output " + outputNumber + " to " + state);
        return true;
    }
}
```

#### IoPinConfiguration.java (~40 lines)
```java
package hartu.robot.executor.io;

/**
 * Configuration constants for IO pin mappings.
 * Centralizes all pin number definitions for easy maintenance.
 */
public class IoPinConfiguration {
    
    // Digital Output Pins - Direct IO Control
    /** Flange IO pin 7 - General purpose output */
    public static final int FLANGE_IO_PIN_7 = 1;
    
    /** Ethercat IO output 2 */
    public static final int ETHERCAT_OUTPUT_2 = 2;
    
    /** Ethercat IO output 1 */
    public static final int ETHERCAT_OUTPUT_1 = 3;
    
    // Special Function Pins - Tool Control
    /** Lock the Gimatic tool changer */
    public static final int GIMATIC_LOCK_PIN = 10;
    
    /** Unlock the Gimatic tool changer */
    public static final int GIMATIC_UNLOCK_PIN = 11;
    
    /** Open tool (activate vacuum/suction) */
    public static final int TOOL_OPEN_PIN = 12;
    
    /** Close tool (blow air to release) */
    public static final int TOOL_CLOSE_PIN = 13;
    
    // Private constructor to prevent instantiation
    private IoPinConfiguration() {
        throw new AssertionError("Utility class - do not instantiate");
    }
}
```

**Benefits:**
- ✅ CommandExecutor reduced from 596 to ~150 lines
- ✅ Clear separation of concerns (motion/IO/programs)
- ✅ Each class < 200 lines
- ✅ Easy to test individual executors
- ✅ Easy to find and modify specific functionality
- ✅ No magic numbers (IoPinConfiguration)
- ✅ Better error handling and logging
- ✅ Maintainable and extensible

---

## Example 2: Replace Magic Numbers with Constants

### BEFORE
```java
private boolean executeIO(ParsedCommand command) {
    int ioPin = command.getIoCommandData().getIoPin();
    boolean ioState = command.getIoCommandData().getIoState();
    
    switch (ioPin) {
        case 1:
            gimaticIO.setDO_Flange7(ioState);
            return true;
        case 2:
            toolControlIO.setOutput2(ioState);
            return true;
        case 10:
            return lockGimatic();
        case 11:
            return unlockGimatic();
        default:
            return false;
    }
}
```

**Problem:** What do pins 1, 2, 10, 11 mean? Hard to understand and maintain.

---

### AFTER
```java
// IoPinConfiguration.java
public class IoPinConfiguration {
    public static final int FLANGE_IO_PIN_7 = 1;
    public static final int ETHERCAT_OUTPUT_2 = 2;
    public static final int GIMATIC_LOCK_PIN = 10;
    public static final int GIMATIC_UNLOCK_PIN = 11;
}

// IoExecutor.java
private boolean executeIO(ParsedCommand command) {
    int ioPin = command.getIoCommandData().getIoPin();
    boolean ioState = command.getIoCommandData().getIoState();
    
    switch (ioPin) {
        case IoPinConfiguration.FLANGE_IO_PIN_7:
            gimaticIO.setDO_Flange7(ioState);
            return true;
        case IoPinConfiguration.ETHERCAT_OUTPUT_2:
            toolControlIO.setOutput2(ioState);
            return true;
        case IoPinConfiguration.GIMATIC_LOCK_PIN:
            return toolChanger.lock();
        case IoPinConfiguration.GIMATIC_UNLOCK_PIN:
            return toolChanger.unlock();
        default:
            Logger.getInstance().error("IO", "Invalid IO pin: " + ioPin);
            return false;
    }
}
```

**Benefits:**
- ✅ Self-documenting code
- ✅ Easy to change pin mappings
- ✅ Single source of truth
- ✅ IDE can find all usages

---

## Example 3: Builder Pattern for Complex Objects

### BEFORE
```java
MotionParameters params = new MotionParameters(
    speedOverride,  // What's this?
    tool,          // Is this required?
    base,          // Can be null?
    isContinuous,  // Boolean meaning?
    numPoints,     // Count of what?
    null,          // What parameter is this?
    null,          // And this?
    null           // Confusing!
);
```

**Problem:** Hard to understand what each parameter means, easy to mix up order.

---

### AFTER
```java
// MotionParameters.java - Add Builder
public static class Builder {
    // Required parameters
    private final double speedOverride;
    private final boolean isContinuous;
    private final int numPoints;
    
    // Optional parameters with defaults
    private String tool = "";
    private String base = "";
    private Map<JointEnum, Double> jointVelocityRel = null;
    private Map<JointEnum, Double> jointAccelerationRel = null;
    private Double blendingRel = null;
    
    public Builder(double speedOverride, boolean isContinuous, int numPoints) {
        this.speedOverride = speedOverride;
        this.isContinuous = isContinuous;
        this.numPoints = numPoints;
    }
    
    public Builder setTool(String tool) {
        this.tool = tool;
        return this;
    }
    
    public Builder setBase(String base) {
        this.base = base;
        return this;
    }
    
    public Builder setJointVelocityRel(Map<JointEnum, Double> velocityRel) {
        this.jointVelocityRel = velocityRel;
        return this;
    }
    
    public Builder setBlendingRel(Double blendingRel) {
        this.blendingRel = blendingRel;
        return this;
    }
    
    public MotionParameters build() {
        return new MotionParameters(
            speedOverride, tool, base, isContinuous, numPoints,
            jointVelocityRel, jointAccelerationRel, blendingRel
        );
    }
}

// Usage
MotionParameters params = new MotionParameters.Builder(0.2, true, 5)
    .setTool("Gripper")
    .setBase("World")
    .setBlendingRel(0.05)
    .build();
```

**Benefits:**
- ✅ Clear what each parameter is
- ✅ Required vs optional parameters obvious
- ✅ Can't mix up parameter order
- ✅ Easy to add new optional parameters
- ✅ Fluent, readable API

---

## Example 4: Extract Method for Readability

### BEFORE
```java
public boolean executeMovementCommand(ParsedCommand command) {
    ActionTypes actionType = command.getActionType();
    Logger.getInstance().log("ROBOT_EXEC", "Executing " + actionType.name());
    
    List<IMotion> motions;
    
    MovementType movementType = actionType.getMovementType();
    if (movementType == MovementType.PTP) {
        if (actionType.isJointMotion()) {
            motions = new ArrayList<IMotion>();
            MotionParameters params = command.getMotionParameters();
            for (JointPosition axPos : command.getAxisTargetPoints()) {
                motions.add(params.createPTPJointMotion(axPos));
            }
        } else {
            motions = new ArrayList<IMotion>();
            MotionParameters params = command.getMotionParameters();
            for (Frame cartPos : command.getCartesianTargetPoints()) {
                motions.add(params.createPTPMotion(cartPos));
            }
        }
    } else if (movementType == MovementType.LIN) {
        motions = new ArrayList<IMotion>();
        MotionParameters params = command.getMotionParameters();
        for (Frame cartPos : command.getCartesianTargetPoints()) {
            motions.add(params.createLINMotion(cartPos));
        }
    } else {
        Logger.getInstance().error("ROBOT_EXEC", "Unsupported type");
        return false;
    }
    
    if (motions.isEmpty()) {
        return false;
    }
    
    try {
        IMotion motionToExecute;
        if (motions.size() > 1) {
            motionToExecute = new MotionBatch(motions.toArray(new RobotMotion[0]));
        } else {
            motionToExecute = motions.get(0);
        }
        
        IMotionContainer container = iiwa.moveAsync(motionToExecute);
        container.await();
        return true;
    } catch (Exception e) {
        Logger.getInstance().error("ROBOT_EXEC", "Error: " + e.getMessage());
        return false;
    }
}
```

**Problem:** Method is too long (50+ lines), does too many things, hard to follow.

---

### AFTER
```java
public boolean executeMotion(ParsedCommand command) {
    Logger.getInstance().log("MOTION", "Executing " + command.getActionType().name());
    
    List<IMotion> motions = createMotions(command);
    
    if (motions.isEmpty()) {
        Logger.getInstance().error("MOTION", "Failed to create motions");
        return false;
    }
    
    return executeMotionSequence(motions, command.getId());
}

private List<IMotion> createMotions(ParsedCommand command) {
    MovementType type = command.getActionType().getMovementType();
    
    if (type == MovementType.PTP) {
        return createPtpMotions(command);
    } else if (type == MovementType.LIN) {
        return createLinMotions(command);
    } else if (type == MovementType.CIRC) {
        return createCircMotions(command);
    }
    
    Logger.getInstance().error("MOTION", "Unsupported movement type: " + type);
    return Collections.emptyList();
}

private List<IMotion> createPtpMotions(ParsedCommand command) {
    if (command.getActionType().isJointMotion()) {
        return createPtpJointMotions(command);
    } else {
        return createPtpCartesianMotions(command);
    }
}

private List<IMotion> createPtpJointMotions(ParsedCommand command) {
    List<IMotion> motions = new ArrayList<IMotion>();
    MotionParameters params = command.getMotionParameters();
    
    for (JointPosition position : command.getAxisTargetPoints()) {
        motions.add(params.createPTPJointMotion(position));
    }
    
    return motions;
}

private boolean executeMotionSequence(List<IMotion> motions, String commandId) {
    try {
        IMotion motion = (motions.size() == 1) ? 
            motions.get(0) : 
            new MotionBatch(motions.toArray(new RobotMotion[0]));
        
        robot.moveAsync(motion).await();
        Logger.getInstance().log("MOTION", "Completed command " + commandId);
        return true;
        
    } catch (CommandInvalidException e) {
        Logger.getInstance().error("MOTION", "Invalid motion: " + e.getMessage());
        return false;
    } catch (Exception e) {
        Logger.getInstance().error("MOTION", "Execution error", e);
        return false;
    }
}
```

**Benefits:**
- ✅ Each method < 20 lines
- ✅ Single responsibility per method
- ✅ Easy to understand flow
- ✅ Easy to test individual steps
- ✅ Better error handling

---

## Example 5: Proper Exception Handling

### BEFORE
```java
try {
    robot.move(motion);
} catch (Exception e) {
    e.printStackTrace();
    return false;
}
```

**Problems:**
- Catches too broad (Exception)
- printStackTrace instead of logging
- No context about what failed

---

### AFTER
```java
try {
    IMotionContainer container = robot.moveAsync(motion);
    container.await();
    Logger.getInstance().log("MOTION", "Motion completed for command " + commandId);
    return true;
    
} catch (CommandInvalidException e) {
    // Motion parameters are invalid (e.g., unreachable position)
    Logger.getInstance().error("MOTION", 
        "Invalid motion parameters for command " + commandId + ": " + e.getMessage());
    return false;
    
} catch (CancelledException e) {
    // User or system cancelled the motion
    Logger.getInstance().warn("MOTION", 
        "Motion cancelled for command " + commandId + ": " + e.getMessage());
    return false;
    
} catch (ExternalStopException e) {
    // E-stop or external safety stop triggered
    Logger.getInstance().warn("MOTION", 
        "External stop during command " + commandId + ": " + e.getMessage());
    return false;
    
} catch (ExecutionException e) {
    // General execution error
    Logger.getInstance().error("MOTION", 
        "Execution error for command " + commandId, e);
    return false;
}
```

**Benefits:**
- ✅ Specific exception handling
- ✅ Proper logging with context
- ✅ Clear error messages
- ✅ Different handling for different errors
- ✅ Helps with debugging

---

## Summary

These examples show common refactoring patterns:

1. **Extract Class** - Split large classes by responsibility
2. **Extract Method** - Break long methods into focused ones
3. **Replace Magic Numbers** - Use named constants
4. **Builder Pattern** - Simplify complex object creation
5. **Specific Exceptions** - Catch and handle specific errors

**Key Takeaway:** Small, incremental improvements lead to much better code quality and maintainability.

---

**See also:**
- REFACTORING_GUIDELINES.md - Full coding standards
- REFACTORING_PLAN.md - Detailed refactoring roadmap
- REFACTORING_QUICK_REFERENCE.md - Quick lookup guide

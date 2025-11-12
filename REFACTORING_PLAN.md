# Refactoring Plan for iiwaTOFAS Codebase

## Document Purpose
This document provides a concrete, actionable plan for refactoring the iiwaTOFAS codebase. It identifies specific problems, proposes solutions, and prioritizes work to improve code maintainability while preserving functionality.

**Related Document:** See `REFACTORING_GUIDELINES.md` for coding standards and principles.

## Version: 1.0
**Last Updated:** 2025-11-12  
**Status:** Planning Phase

---

## Executive Summary

### Current State
- Working system with TCP/IP control for KUKA iiwa robots
- 36 Java files (excluding generated code and FRI SDK examples)
- Several large files (500+ lines) with mixed responsibilities
- Limited separation of concerns
- Hard-coded business logic in execution layer

### Goals
1. Reduce file sizes to < 300 lines each
2. Implement clear separation of concerns
3. Improve testability and maintainability
4. Maintain 100% backward compatibility
5. Enable easier future modifications

### Timeline
Phased approach over multiple iterations, each preserving functionality.

---

## KUKA RoboticsAPI Version Information

**API Version:** 1.15.1.7  
**Sunrise Version:** 1.11  
**Key Libraries Used:**
- `com.kuka.roboticsAPI.motionModel.*` - Motion commands (PTP, LIN, CIRC, MotionBatch)
- `com.kuka.roboticsAPI.deviceModel.*` - Robot device (LBR, JointPosition)
- `com.kuka.roboticsAPI.geometricModel.*` - Frames and transformations
- `com.kuka.roboticsAPI.executionModel.*` - Execution containers and exceptions
- `com.kuka.roboticsAPI.applicationModel.*` - Application base classes
- `com.kuka.generated.ioAccess.*` - Project-specific generated IO classes

**Important API Constraints:**
- All motion classes (PTP, LIN, CIRC) implement `IMotion` interface
- Robot execution via `LBR.move()` (sync) or `LBR.moveAsync()` (async)
- Exception handling required: `CommandInvalidException`, `CancelledException`, `ExternalStopException`
- `MotionBatch` used for combining multiple motions into smooth trajectories
- Blending parameter controls motion smoothness between waypoints

---

## Current Code Analysis

### Large Files Requiring Refactoring

| File | Lines | Issues | Priority |
|------|-------|--------|----------|
| CommandExecutor.java | 596 | Mixed motion, IO, and program execution | HIGH |
| ClientHandler.java | 243 | Protocol + business logic mixed | MEDIUM |
| CommandParser.java | 212 | Monolithic parsing logic | MEDIUM |
| ParsedCommand.java | 200 | Could benefit from builders | LOW |

### File Dependencies (High-Level)

```
ServerClass
    ↓
ClientHandler
    ↓
CommandParser → ParsedCommand
    ↓
CommandQueue
    ↓
CommandExecutor
    ↓
MotionParameters, IoCommandData
```

---

## Phase 1: CommandExecutor Refactoring (HIGH PRIORITY)

### Problem Statement
`CommandExecutor.java` (596 lines) has multiple responsibilities:
1. Motion command execution (PTP, LIN, CIRC)
2. IO operations (digital I/O, tool control)
3. Program calls (tool changing, gripper control)
4. Motion strategy creation
5. Gimatic tool changer operations

### Proposed Structure

```
CommandExecutor (Main coordinator, ~150 lines)
    ├── MotionExecutor (~200 lines)
    │   ├── PtpMotionStrategy
    │   ├── LinMotionStrategy
    │   └── CircMotionStrategy
    ├── IoExecutor (~150 lines)
    │   ├── IoPinConfiguration (constants)
    │   └── IoOperations (lock, unlock, etc.)
    └── ProgramExecutor (~100 lines)
        └── ToolChangerOperations
```

### Refactoring Steps

#### Step 1.1: Extract Motion Executor
**Target Files:**
- Create: `src/hartu/robot/executor/motion/MotionExecutor.java`
- Create: `src/hartu/robot/executor/motion/MotionStrategyFactory.java`
- Modify: `src/hartu/robot/executor/CommandExecutor.java`

**Extracted Methods:**
```java
// From CommandExecutor to MotionExecutor
- executeMovementCommand()
- createPtpJointMotions()
- createPtpCartesianMotions()
- createLinMotions()
- createCircMotions()
```

**New MotionExecutor.java Structure:**
```java
package hartu.robot.executor.motion;

public class MotionExecutor {
    private final LBR robot;
    
    public MotionExecutor(LBR robot) {
        this.robot = robot;
    }
    
    public boolean executeMotion(ParsedCommand command) {
        // Motion execution logic
    }
    
    private List<IMotion> createMotions(ParsedCommand command) {
        // Delegate to strategy factory
    }
}
```

**Estimated Lines:**
- MotionExecutor.java: ~180 lines
- MotionStrategyFactory.java: ~60 lines

#### Step 1.2: Extract IO Executor
**Target Files:**
- Create: `src/hartu/robot/executor/io/IoExecutor.java`
- Create: `src/hartu/robot/executor/io/IoPinConfiguration.java`
- Create: `src/hartu/robot/executor/io/GimaticToolChanger.java`
- Modify: `src/hartu/robot/executor/CommandExecutor.java`

**Extracted Methods:**
```java
// From CommandExecutor to IoExecutor
- executeIO()
- lockGimatic()
- unlockGimatic()
- openTool()
- closeTool()
```

**New IoExecutor.java Structure:**
```java
package hartu.robot.executor.io;

public class IoExecutor {
    private final IOFlangeIOGroup gimaticIO;
    private final Ethercat_x44IOGroup toolControlIO;
    private final GimaticToolChanger toolChanger;
    
    public IoExecutor(IOFlangeIOGroup gimaticIO, Ethercat_x44IOGroup toolControlIO) {
        this.gimaticIO = gimaticIO;
        this.toolControlIO = toolControlIO;
        this.toolChanger = new GimaticToolChanger(gimaticIO);
    }
    
    public boolean executeIoCommand(ParsedCommand command) {
        // IO execution logic
    }
    
    private boolean setDigitalOutput(int pin, boolean state) {
        // Pin mapping logic
    }
}
```

**IoPinConfiguration.java (Constants):**
```java
package hartu.robot.executor.io;

public class IoPinConfiguration {
    // Digital Output Pins
    public static final int FLANGE_IO_PIN_7 = 1;
    public static final int ETHERCAT_OUTPUT_2 = 2;
    public static final int ETHERCAT_OUTPUT_1 = 3;
    
    // Special Function Pins
    public static final int GIMATIC_LOCK_PIN = 10;
    public static final int GIMATIC_UNLOCK_PIN = 11;
    public static final int TOOL_OPEN_PIN = 12;
    public static final int TOOL_CLOSE_PIN = 13;
    
    private IoPinConfiguration() {} // Utility class
}
```

**Estimated Lines:**
- IoExecutor.java: ~120 lines
- IoPinConfiguration.java: ~40 lines
- GimaticToolChanger.java: ~80 lines

#### Step 1.3: Extract Program Executor
**Target Files:**
- Create: `src/hartu/robot/executor/program/ProgramExecutor.java`
- Create: `src/hartu/robot/executor/program/ProgramIdMapping.java`
- Modify: `src/hartu/robot/executor/CommandExecutor.java`

**Extracted Methods:**
```java
// From CommandExecutor to ProgramExecutor
- executeProgramCallCommand()
- pickTool()
- placeTool()
- detectCurrentTool()
```

**Estimated Lines:**
- ProgramExecutor.java: ~100 lines
- ProgramIdMapping.java: ~30 lines

#### Step 1.4: Refactored CommandExecutor
**Final CommandExecutor.java (~150 lines):**
```java
package hartu.robot.executor;

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
        this.motionExecutor = new MotionExecutor(iiwa);
        this.ioExecutor = new IoExecutor(gimaticIO, toolControlIO);
        this.programExecutor = new ProgramExecutor(iiwa, gimaticIO, toolControlIO);
        
        Logger.getInstance().log("ROBOT_EXEC", "Initializing. Ready to take commands from queue.");
    }
    
    @Override
    public void run() {
        while (true) {
            CommandResultHolder resultHolder = CommandQueue.pollCommand(100, TimeUnit.MILLISECONDS);
            
            if (resultHolder != null) {
                boolean success = executeCommand(resultHolder.getCommand());
                resultHolder.setSuccess(success);
                resultHolder.getLatch().countDown();
            }
        }
    }
    
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
                    Logger.getInstance().warn("ROBOT_EXEC", "Unknown command category: " + command.getCommandCategory());
                    return false;
            }
        } catch (Exception e) {
            Logger.getInstance().error("ROBOT_EXEC", "Error executing command: " + e.getMessage(), e);
            return false;
        }
    }
}
```

### Testing Strategy for Phase 1
1. Create test commands for each motion type
2. Verify IO operations still work
3. Test tool changer operations
4. Regression test with existing client scripts
5. Compare logs before and after refactoring

### Success Criteria
- [ ] CommandExecutor.java < 200 lines
- [ ] All motion types work identically
- [ ] All IO operations work identically
- [ ] No behavioral changes
- [ ] All new classes < 250 lines

---

## Phase 2: CommandParser Refactoring (MEDIUM PRIORITY)

### Problem Statement
`CommandParser.java` (212 lines) is a monolithic utility class with complex parsing logic for different command types.

### Proposed Structure

```
CommandParser (Facade, ~80 lines)
    ├── MovementCommandParser (~80 lines)
    ├── IoCommandParser (~50 lines)
    ├── ProgramCommandParser (~40 lines)
    └── ParameterParser (~60 lines)
```

### Refactoring Steps

#### Step 2.1: Extract Parameter Parser
**Target Files:**
- Create: `src/hartu/robot/utils/parser/ParameterParser.java`
- Modify: `src/hartu/robot/utils/CommandParser.java`

**Extracted Logic:**
```java
// From CommandParser to ParameterParser
- Parse motion parameters (velocity, acceleration, jerk)
- Parse tool and base frame names
- Parse speed override
- Parse joint velocity/acceleration/blending parameters
```

#### Step 2.2: Extract Movement Command Parser
**Target Files:**
- Create: `src/hartu/robot/utils/parser/MovementCommandParser.java`

**Responsibilities:**
- Parse joint positions
- Parse cartesian frames
- Create JointPosition and Frame objects
- Validate movement command structure

#### Step 2.3: Extract IO Command Parser
**Target Files:**
- Create: `src/hartu/robot/utils/parser/IoCommandParser.java`

**Responsibilities:**
- Parse IO pin numbers
- Parse IO states
- Validate IO command parameters

#### Step 2.4: Extract Program Command Parser
**Target Files:**
- Create: `src/hartu/robot/utils/parser/ProgramCommandParser.java`

**Responsibilities:**
- Parse program IDs
- Validate program command structure

#### Step 2.5: Refactored CommandParser (Facade)
**Final Structure:**
```java
public class CommandParser {
    private static final ParameterParser parameterParser = new ParameterParser();
    private static final MovementCommandParser movementParser = new MovementCommandParser();
    private static final IoCommandParser ioParser = new IoCommandParser();
    private static final ProgramCommandParser programParser = new ProgramCommandParser();
    
    private CommandParser() {}
    
    public static ParsedCommand parseCommand(String commandString) {
        // 1. Basic validation and splitting
        // 2. Determine command category
        // 3. Delegate to appropriate parser
        // 4. Return ParsedCommand
    }
}
```

### Success Criteria
- [ ] CommandParser.java < 100 lines
- [ ] All parser classes < 100 lines each
- [ ] All existing commands parse identically
- [ ] No behavioral changes

---

## Phase 3: ClientHandler Refactoring (MEDIUM PRIORITY)

### Problem Statement
`ClientHandler.java` (243 lines) mixes protocol handling with command execution coordination.

### Proposed Structure

```
ClientHandler (Protocol handling, ~100 lines)
    ├── TaskClientHandler (~80 lines)
    ├── LogClientHandler (~40 lines)
    └── CommandCoordinator (~60 lines)
```

### Refactoring Steps

#### Step 3.1: Extract Command Coordinator
**Target Files:**
- Create: `src/hartu/robot/communication/server/CommandCoordinator.java`
- Modify: `src/hartu/robot/communication/server/ClientHandler.java`

**Responsibilities:**
- Parse commands
- Enqueue to CommandQueue
- Wait for execution results
- Send responses

#### Step 3.2: Split by Client Type
**Target Files:**
- Create: `src/hartu/robot/communication/server/TaskClientHandler.java`
- Create: `src/hartu/robot/communication/server/LogClientHandler.java`
- Refactor: `src/hartu/robot/communication/server/ClientHandler.java` (make abstract or interface)

### Success Criteria
- [ ] All handler classes < 150 lines
- [ ] Clear separation between protocol and coordination
- [ ] No behavioral changes

---

## Phase 4: Enhance MotionParameters (LOW PRIORITY)

### Problem Statement
`MotionParameters` constructor has 12+ parameters, making it hard to use and maintain.

### Proposed Solution
Implement Builder Pattern for MotionParameters.

### Refactoring Steps

#### Step 4.1: Create Builder
**Target Files:**
- Modify: `src/hartu/robot/commands/MotionParameters.java`

**New Structure:**
```java
public class MotionParameters {
    // Existing fields
    
    private MotionParameters(Builder builder) {
        // Initialize from builder
    }
    
    public static class Builder {
        // Required parameters
        private double speedOverride;
        private boolean isContinuous;
        private int numPoints;
        
        // Optional parameters with defaults
        private String tool = "";
        private String base = "";
        private Double jointVelocityRel = null;
        // ... other optional parameters
        
        public Builder(double speedOverride, boolean isContinuous, int numPoints) {
            this.speedOverride = speedOverride;
            this.isContinuous = isContinuous;
            this.numPoints = numPoints;
        }
        
        public Builder setTool(String tool) {
            this.tool = tool;
            return this;
        }
        
        // ... other setters
        
        public MotionParameters build() {
            return new MotionParameters(this);
        }
    }
    
    // Keep old constructor for backward compatibility (mark @Deprecated)
    @Deprecated
    public MotionParameters(double speedOverride, String tool, ...) {
        // Old constructor
    }
}
```

### Success Criteria
- [ ] Builder pattern implemented
- [ ] Old constructor still works (deprecated)
- [ ] Update CommandParser to use builder
- [ ] All motion commands work identically

---

## Phase 5: Test Cleanup and Organization (LOW PRIORITY)

### Problem Statement
Test files are large and some appear unused or incomplete.

### Analysis
| File | Lines | Status |
|------|-------|--------|
| lemao.java | 543 | Review if still needed |
| TestExecutingServer.java | 454 | Appears to be old server implementation |
| testin.java | 130 | Review if still needed |
| SPS.java | 130 | Review if still needed |
| TestServer.java | 83 | Commented out, review |
| RobotApplicationTemplate.java | 610 | Template, keep |

### Refactoring Steps

#### Step 5.1: Audit Test Files
1. Review each test file with team
2. Identify actively used tests
3. Mark obsolete tests for removal
4. Extract reusable test utilities

#### Step 5.2: Organize Tests
**Proposed Structure:**
```
src/hartu/tests/
    ├── integration/          # Full system tests
    ├── unit/                 # Unit tests
    ├── templates/            # Template applications
    └── utils/                # Test utilities
```

### Success Criteria
- [ ] All test files reviewed and documented
- [ ] Obsolete tests removed
- [ ] Active tests organized by type
- [ ] Test utilities extracted

---

## Phase 6: Constants and Configuration Consolidation (LOW PRIORITY)

### Problem Statement
Constants and configuration values scattered across codebase.

### Proposed Changes

#### Step 6.1: Consolidate Protocol Constants
**Review and organize:**
- `hartu.protocols.constants.*`
- Ensure all protocol values are defined in one place
- Add documentation for each constant

#### Step 6.2: Create Configuration Classes
**New Files:**
- `src/hartu/robot/config/ServerConfiguration.java` (ports, timeouts)
- `src/hartu/robot/config/RobotConfiguration.java` (robot-specific settings)
- `src/hartu/robot/config/IoConfiguration.java` (IO pin mappings)

### Success Criteria
- [ ] All magic numbers replaced with named constants
- [ ] Configuration values in dedicated classes
- [ ] Documentation for all constants

---

## Implementation Strategy

### General Principles
1. **One phase at a time** - Complete and test each phase before moving to next
2. **Backward compatibility** - Never break existing functionality
3. **Incremental changes** - Small, tested commits
4. **Parallel development** - Some phases can be done in parallel
5. **Continuous testing** - Test after each significant change

### Phase Dependencies
```
Phase 1 (CommandExecutor) ← Independent, start here
    ↓
Phase 2 (CommandParser) ← Can start after Phase 1 or in parallel
    ↓
Phase 3 (ClientHandler) ← Can start after Phase 1
    ↓
Phase 4 (MotionParameters) ← Can be done anytime
    ↓
Phase 5 (Tests) ← After main refactoring complete
    ↓
Phase 6 (Constants) ← Ongoing, as needed
```

### Recommended Order
1. **Start:** Phase 1 (CommandExecutor) - Highest priority, biggest impact
2. **Then:** Phase 2 (CommandParser) - Natural follow-up
3. **Next:** Phase 3 (ClientHandler) - Complete the major refactorings
4. **Later:** Phase 4 (MotionParameters) - Enhancement, not critical
5. **Finally:** Phase 5 & 6 (Cleanup) - Polish and maintainability

---

## Risk Assessment

### Risks and Mitigations

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Breaking robot control | Critical | Low | Extensive testing, small changes |
| Performance degradation | High | Low | Profile before/after, optimize if needed |
| Introducing bugs | High | Medium | Comprehensive testing, code review |
| Scope creep | Medium | High | Strict adherence to plan, phases |
| Team resistance | Medium | Low | Clear documentation, gradual changes |

### Testing Requirements
- Unit tests for new components
- Integration tests for command flow
- Regression tests with real robot (or simulator)
- Performance benchmarks
- Code review for all changes

---

## Success Metrics

### Quantitative Goals
- [ ] No files > 400 lines (except generated)
- [ ] Average file size < 200 lines
- [ ] No methods > 50 lines
- [ ] Test coverage > 60% (if unit tests added)
- [ ] Zero behavioral changes (100% compatibility)

### Qualitative Goals
- [ ] Easier to understand for new developers
- [ ] Faster to add new features
- [ ] Easier to debug issues
- [ ] Better code organization
- [ ] Clearer responsibilities

---

## Phase Tracking

### Phase 1: CommandExecutor Refactoring
**Status:** Not Started  
**Assigned:** TBD  
**Target Date:** TBD  
**Completion:** 0%

**Tasks:**
- [ ] Step 1.1: Extract MotionExecutor
- [ ] Step 1.2: Extract IoExecutor
- [ ] Step 1.3: Extract ProgramExecutor
- [ ] Step 1.4: Refactor CommandExecutor
- [ ] Testing and validation

### Phase 2: CommandParser Refactoring
**Status:** Not Started  
**Assigned:** TBD  
**Target Date:** TBD  
**Completion:** 0%

**Tasks:**
- [ ] Step 2.1: Extract ParameterParser
- [ ] Step 2.2: Extract MovementCommandParser
- [ ] Step 2.3: Extract IoCommandParser
- [ ] Step 2.4: Extract ProgramCommandParser
- [ ] Step 2.5: Refactor CommandParser facade
- [ ] Testing and validation

### Phase 3: ClientHandler Refactoring
**Status:** Not Started  
**Assigned:** TBD  
**Target Date:** TBD  
**Completion:** 0%

**Tasks:**
- [ ] Step 3.1: Extract CommandCoordinator
- [ ] Step 3.2: Split by client type
- [ ] Testing and validation

### Phase 4: MotionParameters Enhancement
**Status:** Not Started  
**Assigned:** TBD  
**Target Date:** TBD  
**Completion:** 0%

**Tasks:**
- [ ] Step 4.1: Implement Builder pattern
- [ ] Update all usages
- [ ] Testing and validation

### Phase 5: Test Cleanup
**Status:** Not Started  
**Assigned:** TBD  
**Target Date:** TBD  
**Completion:** 0%

**Tasks:**
- [ ] Step 5.1: Audit test files
- [ ] Step 5.2: Organize tests
- [ ] Documentation

### Phase 6: Constants Consolidation
**Status:** Not Started  
**Assigned:** TBD  
**Target Date:** TBD  
**Completion:** 0%

**Tasks:**
- [ ] Step 6.1: Consolidate protocol constants
- [ ] Step 6.2: Create configuration classes
- [ ] Documentation

---

## Resource Requirements

### Development Environment
- Sunrise Workbench 1.11
- KUKA iiwa robot or simulator
- Git for version control
- Testing environment

### Time Estimates
- Phase 1: 2-3 days (including testing)
- Phase 2: 1-2 days
- Phase 3: 1-2 days
- Phase 4: 1 day
- Phase 5: 1-2 days
- Phase 6: 1 day

**Total Estimated Time:** 7-11 days of focused development

### Team Requirements
- Minimum 1 developer familiar with KUKA APIs
- Access to robot for testing
- Code reviewer

---

## Approval and Sign-Off

### Review Checklist
- [ ] Plan reviewed by technical lead
- [ ] Timeline approved
- [ ] Resources allocated
- [ ] Risk assessment accepted
- [ ] Testing strategy agreed

### Sign-Off
- **Created By:** GitHub Copilot
- **Date:** 2025-11-12
- **Status:** Awaiting Review

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-11-12 | GitHub Copilot | Initial refactoring plan created |

---

## Appendix A: File Size Targets

### Current State
| Component | Current Lines | Target Lines | Status |
|-----------|--------------|--------------|--------|
| CommandExecutor | 596 | 150 | ❌ Needs refactoring |
| ClientHandler | 243 | 100 | ⚠️ Could improve |
| CommandParser | 212 | 80 | ⚠️ Could improve |
| ParsedCommand | 200 | 200 | ✅ Acceptable |
| Others | < 200 | < 200 | ✅ Good |

### After Refactoring
All files should be < 250 lines, with most < 200 lines.

---

## Appendix B: Code Examples

### Example 1: Before and After - Motion Execution

**Before (CommandExecutor - 596 lines):**
```java
public class CommandExecutor extends RoboticsAPIApplication {
    public boolean executeMovementCommand(ParsedCommand command) {
        // 100+ lines of motion logic
    }
    
    private List<IMotion> createPtpJointMotions(ParsedCommand command) {
        // 30 lines
    }
    
    public boolean executeIO(ParsedCommand command) {
        // 80+ lines of IO logic
    }
    
    private boolean lockGimatic() {
        // 40 lines
    }
    
    // ... 20+ more methods
}
```

**After (CommandExecutor - 150 lines):**
```java
public class CommandExecutor extends RoboticsAPIApplication {
    private MotionExecutor motionExecutor;
    private IoExecutor ioExecutor;
    private ProgramExecutor programExecutor;
    
    private boolean executeCommand(ParsedCommand command) {
        switch (command.getCommandCategory()) {
            case MOVEMENT:
                return motionExecutor.executeMotion(command);
            case IO:
                return ioExecutor.executeIoCommand(command);
            case PROGRAM_CALL:
                return programExecutor.executeProgramCall(command);
        }
    }
}
```

**MotionExecutor (180 lines):**
```java
public class MotionExecutor {
    private final LBR robot;
    
    public boolean executeMotion(ParsedCommand command) {
        List<IMotion> motions = createMotions(command);
        return executeMotionSequence(motions);
    }
    
    private List<IMotion> createMotions(ParsedCommand command) {
        // Delegate to strategy factory
    }
}
```

---

## Appendix C: Testing Checklist

### Regression Test Checklist
- [ ] PTP joint motion commands
- [ ] PTP cartesian motion commands
- [ ] LIN motion commands
- [ ] CIRC motion commands
- [ ] Continuous motion commands
- [ ] Digital IO set operations
- [ ] Digital IO read operations
- [ ] Tool changer lock/unlock
- [ ] Tool open/close operations
- [ ] Program call commands
- [ ] Multi-point trajectories
- [ ] Error handling and recovery
- [ ] Concurrent command handling
- [ ] Log client connectivity
- [ ] Joint state broadcasting

---

**Note:** This plan is a living document. Update it as phases complete and new requirements emerge.

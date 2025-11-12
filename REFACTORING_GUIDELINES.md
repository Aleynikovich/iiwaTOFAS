# Refactoring Guidelines for iiwaTOFAS

## Document Purpose
This document establishes coding standards, architectural principles, and refactoring guidelines for the iiwaTOFAS codebase. **All developers and future code changes must follow these guidelines** to maintain code quality, readability, and maintainability.

## Version: 1.0
**Last Updated:** 2025-11-12  
**Status:** Active - Must be consulted for all code changes

---

## 1. Project Constraints

### Technical Requirements
- **Java Version:** 1.7 (Java 7) - No Java 8+ features allowed
- **KUKA Framework:** Sunrise Workbench 1.11
- **KUKA RoboticsAPI:** Version 1.15.1.7
- **IDE:** Eclipse-based (Sunrise Workbench)
- **Build System:** Eclipse project with .classpath configuration
- **No External Build Tools:** Maven, Gradle not available

### Compatibility Requirements
- Code must compile and run on KUKA Sunrise 1.11
- Must work with LBR iiwa robots
- Thread-safe operations required for multi-client server
- Real-time performance considerations for robot control

### KUKA API Constraints
- **Motion API:** Use `com.kuka.roboticsAPI.motionModel.*` classes
  - `PTP` for point-to-point joint motions
  - `CartesianPTP` for point-to-point Cartesian motions
  - `LIN` for linear motions
  - `CIRC` for circular motions
  - `MotionBatch` for combining multiple motions
- **Device API:** Use `LBR` class for robot control
  - `move(IMotion)` - synchronous execution
  - `moveAsync(IMotion)` - asynchronous execution with `IMotionContainer`
- **Execution Exceptions:** Must handle:
  - `CommandInvalidException` - Invalid motion parameters
  - `CancelledException` - Motion cancelled by user/system
  - `ExternalStopException` - External stop triggered
  - `ExecutionException` - General execution failures
- **Application Model:** Extend `RoboticsAPIApplication` for main applications
- **IO Model:** Use generated IO access classes from `com.kuka.generated.ioAccess.*`

---

## 2. Core Architectural Principles

### Separation of Concerns
**Each class should have ONE clear responsibility.**

#### Layer Architecture
```
┌─────────────────────────────────────────────┐
│  Application Layer (RoboticsAPIApplication)  │
│  - CommandExecutor                           │
│  - Main application entry points             │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│  Domain Layer                                │
│  - ParsedCommand, MotionParameters           │
│  - Business logic and validation             │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│  Communication Layer                         │
│  - ServerClass, ClientHandler                │
│  - Network I/O and session management        │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│  Protocol Layer                              │
│  - CommandParser, Constants                  │
│  - Message format and parsing                │
└─────────────────────────────────────────────┘
```

### File Size Guidelines
- **Target:** < 300 lines per class
- **Warning Threshold:** 300-400 lines
- **Refactor Required:** > 400 lines
- **Exception:** Generated code (com.kuka.generated.*)

### Package Organization
```
hartu/
├── protocols/           # Protocol definitions and constants
│   └── constants/       # All protocol constants
├── robot/
│   ├── commands/        # Command data structures and validation
│   │   └── io/          # IO-specific commands
│   ├── communication/   # Network layer
│   │   ├── server/      # Server components
│   │   └── client/      # Client components (if needed)
│   ├── executor/        # Robot motion execution
│   │   ├── motion/      # Motion-specific executors (CREATE THIS)
│   │   └── io/          # IO-specific executors (CREATE THIS)
│   └── utils/           # Utility classes (parsing, formatting)
└── tests/               # Test applications
```

---

## 3. Coding Standards

### Naming Conventions

#### Classes
- **Format:** PascalCase
- **Suffixes:**
  - `*Handler` - Handles events or requests
  - `*Manager` - Manages resources or components
  - `*Executor` - Executes commands or operations
  - `*Parser` - Parses input data
  - `*Builder` - Constructs complex objects
  - `*Factory` - Creates instances
  - `*Config` - Configuration data
  - `*Constants` - Constant values

#### Methods
- **Format:** camelCase
- **Prefixes:**
  - `create*` - Factory methods
  - `build*` - Builder methods
  - `execute*` - Execution methods
  - `parse*` - Parsing methods
  - `validate*` - Validation methods
  - `is*` / `has*` - Boolean getters
  - `get*` / `set*` - Property accessors

#### Variables
- **Format:** camelCase
- **Constants:** UPPER_SNAKE_CASE
- **Meaningful names:** No single-letter variables except loop counters
- **Boolean variables:** Should read like questions (e.g., `isConnected`, `hasPermission`)

### Code Organization

#### Class Structure Order
```java
public class ExampleClass {
    // 1. Static constants
    private static final int MAX_RETRIES = 3;
    
    // 2. Static variables
    private static ExampleClass instance;
    
    // 3. Instance variables
    private final String name;
    private int retryCount;
    
    // 4. Constructors
    public ExampleClass(String name) {
        this.name = name;
    }
    
    // 5. Static factory methods
    public static ExampleClass create(String name) {
        return new ExampleClass(name);
    }
    
    // 6. Public methods
    public void execute() { }
    
    // 7. Protected methods
    protected void validate() { }
    
    // 8. Private methods
    private void doSomething() { }
    
    // 9. Getters and setters (at the end)
    public String getName() { return name; }
}
```

#### Method Length
- **Target:** < 30 lines
- **Maximum:** 50 lines
- **Extract methods** when logic becomes complex

#### Indentation and Formatting
- **Indentation:** 4 spaces (no tabs)
- **Line length:** Maximum 120 characters
- **Braces:** Opening brace on same line (K&R style)
- **Blank lines:** One between methods, logical sections

### Comments and Documentation

#### When to Comment
✅ **DO Comment:**
- Complex algorithms or business logic
- Non-obvious design decisions
- Thread-safety considerations
- Hardware-specific constraints
- Protocol specifications
- Workarounds for KUKA API limitations

❌ **DON'T Comment:**
- Obvious code (e.g., `// Set name`)
- What code does (the code should be self-explanatory)
- Outdated information

#### JavaDoc Requirements
- All public classes and interfaces
- All public methods (except simple getters/setters)
- Complex private methods
- Format:
```java
/**
 * Brief description of what the class/method does.
 * 
 * <p>Additional details if needed, including usage examples
 * or important constraints.
 * 
 * @param paramName Description of parameter
 * @return Description of return value
 * @throws ExceptionType When and why this exception is thrown
 */
```

---

## 4. Refactoring Patterns

### Pattern 1: Extract Method
**When to use:** Method > 30 lines or has multiple responsibilities

**Example:**
```java
// BEFORE
public boolean executeCommand(ParsedCommand cmd) {
    if (cmd.getType() == MOTION) {
        // 20 lines of motion logic
    } else if (cmd.getType() == IO) {
        // 15 lines of IO logic
    }
}

// AFTER
public boolean executeCommand(ParsedCommand cmd) {
    if (cmd.getType() == MOTION) {
        return executeMotion(cmd);
    } else if (cmd.getType() == IO) {
        return executeIO(cmd);
    }
}

private boolean executeMotion(ParsedCommand cmd) {
    // 20 lines of motion logic
}

private boolean executeIO(ParsedCommand cmd) {
    // 15 lines of IO logic
}
```

### Pattern 2: Extract Class
**When to use:** Class > 400 lines or has multiple responsibilities

**Example:**
```java
// BEFORE: CommandExecutor (596 lines)
public class CommandExecutor {
    public boolean execute() { }
    private boolean executeMotion() { }
    private boolean executeIO() { }
    private boolean lockGimatic() { }
    private boolean unlockGimatic() { }
    // ... 30+ more methods
}

// AFTER: Separate concerns
public class CommandExecutor {
    private final MotionExecutor motionExecutor;
    private final IoExecutor ioExecutor;
    
    public boolean execute() {
        // Delegate to specialized executors
    }
}

public class MotionExecutor {
    public boolean executeMotion(ParsedCommand cmd) { }
}

public class IoExecutor {
    public boolean executeIO(ParsedCommand cmd) { }
    private boolean lockGimatic() { }
    private boolean unlockGimatic() { }
}
```

### Pattern 3: Replace Magic Numbers with Constants
**When to use:** Any literal number with business meaning

**Example:**
```java
// BEFORE
if (ioPin == 10) {
    return lockGimatic();
}

// AFTER
public class IoPinConstants {
    public static final int GIMATIC_LOCK_PIN = 10;
    public static final int GIMATIC_UNLOCK_PIN = 11;
}

if (ioPin == IoPinConstants.GIMATIC_LOCK_PIN) {
    return lockGimatic();
}
```

### Pattern 4: Builder Pattern for Complex Objects
**When to use:** Constructor with > 4 parameters

**Example:**
```java
// BEFORE
MotionParameters params = new MotionParameters(
    speedOverride, tool, base, isContinuous, numPoints,
    null, null, null, null, null, null, null
);

// AFTER
MotionParameters params = new MotionParameters.Builder()
    .setSpeedOverride(speedOverride)
    .setTool(tool)
    .setBase(base)
    .setContinuous(isContinuous)
    .setNumPoints(numPoints)
    .build();
```

### Pattern 5: Strategy Pattern for Variations
**When to use:** Multiple if-else or switch statements doing similar operations

**Example:**
```java
// BEFORE
public IMotion createMotion(ActionType type) {
    if (type == PTP_JOINT) {
        // PTP joint logic
    } else if (type == PTP_CARTESIAN) {
        // PTP cartesian logic
    } else if (type == LIN) {
        // LIN logic
    }
}

// AFTER
public interface MotionStrategy {
    IMotion createMotion(ParsedCommand cmd);
}

public class PtpJointMotionStrategy implements MotionStrategy { }
public class PtpCartesianMotionStrategy implements MotionStrategy { }
public class LinMotionStrategy implements MotionStrategy { }

public class MotionFactory {
    private Map<ActionType, MotionStrategy> strategies;
    
    public IMotion createMotion(ParsedCommand cmd) {
        return strategies.get(cmd.getActionType()).createMotion(cmd);
    }
}
```

---

## 5. Error Handling

### Logging Guidelines
- **Use Logger singleton:** `Logger.getInstance().log(tag, message)`
- **Tag format:** Component name in uppercase (e.g., "COMM", "PARSER", "ROBOT_EXEC")
- **Log levels:**
  - `log()` - Normal operation, info
  - `warn()` - Recoverable issues
  - `error()` - Failures, exceptions

### Error Handling Best Practices
```java
// ✅ GOOD
try {
    result = executeOperation();
} catch (SpecificException e) {
    Logger.getInstance().error("TAG", "Operation failed: " + e.getMessage(), e);
    return false;
} catch (Exception e) {
    Logger.getInstance().error("TAG", "Unexpected error: " + e.getMessage(), e);
    throw new RuntimeException("Critical failure", e);
}

// ❌ BAD
try {
    result = executeOperation();
} catch (Exception e) {
    // Silent failure
}

// ❌ BAD
try {
    result = executeOperation();
} catch (Exception e) {
    e.printStackTrace(); // Use Logger instead
}
```

---

## 6. Concurrency and Thread Safety

### Thread Safety Guidelines
- **Immutable objects:** Prefer immutable designs when possible
- **Synchronization:** Use `synchronized` keyword for Java 7
- **Volatile:** Use for flags checked by multiple threads
- **Concurrent collections:** Use `ConcurrentHashMap`, `BlockingQueue` from java.util.concurrent

### Thread Naming
```java
Thread thread = new Thread(runnable, "DescriptiveName-" + counter);
thread.setDaemon(true); // If appropriate
thread.start();
```

---

## 7. KUKA RoboticsAPI Usage Patterns

### Motion Creation Pattern
```java
// ✅ GOOD - Clear, parameterized motion creation
public PTP createPtpMotion(JointPosition position, double velocity) {
    PTP motion = new PTP(position);
    motion.setJointVelocityRel(velocity);
    motion.setBlendingRel(0.05);
    return motion;
}

// ❌ BAD - Creating motion inline without parameters
robot.move(new PTP(somePosition));
```

### Motion Execution Pattern
```java
// ✅ GOOD - Proper exception handling for robot motions
try {
    IMotionContainer container = robot.moveAsync(motion);
    container.await();
    Logger.getInstance().log("MOTION", "Motion completed successfully");
} catch (CommandInvalidException e) {
    Logger.getInstance().error("MOTION", "Invalid motion parameters: " + e.getMessage());
    return false;
} catch (CancelledException e) {
    Logger.getInstance().warn("MOTION", "Motion cancelled: " + e.getMessage());
    return false;
} catch (ExternalStopException e) {
    Logger.getInstance().warn("MOTION", "External stop triggered: " + e.getMessage());
    return false;
}

// ❌ BAD - Catching generic Exception
try {
    robot.move(motion);
} catch (Exception e) {
    // Too broad, masks specific errors
}
```

### MotionBatch Usage
```java
// ✅ GOOD - Batch multiple motions for smooth execution
List<IMotion> motions = new ArrayList<IMotion>();
for (Frame waypoint : waypoints) {
    LIN linMotion = new LIN(waypoint);
    linMotion.setJointVelocityRel(speed);
    linMotion.setBlendingRel(0.05);
    motions.add(linMotion);
}
MotionBatch batch = new MotionBatch(motions.toArray(new RobotMotion[0]));
robot.moveAsync(batch).await();

// ❌ BAD - Individual moves without batching (robot stops between waypoints)
for (Frame waypoint : waypoints) {
    robot.move(new LIN(waypoint));
}
```

### Frame and Position Handling
```java
// ✅ GOOD - Explicit frame/position creation
Frame targetFrame = new Frame(x, y, z, alpha, beta, gamma);
JointPosition jointPos = new JointPosition(j1, j2, j3, j4, j5, j6, j7);

// Frame coordinates in mm, angles in radians
// Joint angles in radians (convert from degrees if needed)
```

### IO Access Pattern
```java
// ✅ GOOD - Use generated IO classes with descriptive methods
@Inject
private IOFlangeIOGroup flangeIO;

public boolean setGripperState(boolean open) {
    try {
        flangeIO.setDO_Flange7(open);
        return true;
    } catch (Exception e) {
        Logger.getInstance().error("IO", "Failed to set gripper: " + e.getMessage());
        return false;
    }
}

// ❌ BAD - Hardcoded pin numbers without constants
flangeIO.setOutput(7, true); // Which output is this?
```

### RoboticsAPIApplication Lifecycle
```java
// ✅ GOOD - Proper lifecycle implementation
public class MyApplication extends RoboticsAPIApplication {
    
    @Inject
    private LBR robot;
    
    @Override
    public void initialize() {
        // Setup: Initialize resources, create objects
        Logger.getInstance().log("APP", "Initializing application");
    }
    
    @Override
    public void run() {
        // Main logic: Command execution loop
        while (true) {
            // Process commands
        }
    }
    
    @Override
    public void dispose() {
        // Cleanup: Release resources, close connections
        Logger.getInstance().log("APP", "Disposing application");
        super.dispose();
    }
}
```

### Key KUKA API Classes Reference

| Class | Package | Purpose |
|-------|---------|---------|
| `LBR` | deviceModel | Robot control interface |
| `PTP` | motionModel | Point-to-point joint motion |
| `CartesianPTP` | motionModel | Point-to-point Cartesian motion |
| `LIN` | motionModel | Linear motion |
| `CIRC` | motionModel | Circular motion |
| `MotionBatch` | motionModel | Batch multiple motions |
| `Frame` | geometricModel | Cartesian position and orientation |
| `JointPosition` | deviceModel | Joint angle positions |
| `IMotionContainer` | executionModel | Async motion execution handle |
| `RoboticsAPIApplication` | applicationModel | Base class for applications |

---

## 8. Testing Guidelines

### Test Organization
- Keep tests in `hartu.tests` package
- Use descriptive test class names: `Test*` or `*Test`
- Clean up test classes that are no longer used
- Test files should also follow the < 300 line guideline

### What to Test
- Command parsing logic
- Motion parameter validation
- Protocol message formatting
- Error handling paths
- Edge cases and boundary conditions

---

## 8. Refactoring Checklist

Before committing code changes, verify:

- [ ] Each class has a single, clear responsibility
- [ ] No class exceeds 400 lines (excluding generated code)
- [ ] No method exceeds 50 lines
- [ ] All magic numbers replaced with named constants
- [ ] Proper error handling and logging
- [ ] Code is formatted consistently
- [ ] Public APIs have JavaDoc comments
- [ ] No compiler warnings
- [ ] Thread safety considered for shared state
- [ ] Backward compatibility maintained

---

## 9. Common Anti-Patterns to Avoid

### ❌ God Class
A class that does too much. **Solution:** Extract responsibilities into separate classes.

### ❌ Long Parameter List
Methods with > 4 parameters. **Solution:** Create parameter objects or use builder pattern.

### ❌ Duplicated Code
Same logic in multiple places. **Solution:** Extract to a common method or utility class.

### ❌ Primitive Obsession
Using primitive types instead of domain objects. **Solution:** Create value objects.

### ❌ Inappropriate Intimacy
Classes that know too much about each other's internals. **Solution:** Define clear interfaces.

### ❌ Shotgun Surgery
Single change requires modifications in many classes. **Solution:** Improve encapsulation and responsibility assignment.

---

## 10. Migration Strategy

### Incremental Refactoring
- **Never rewrite everything at once**
- Refactor one component at a time
- Maintain backward compatibility during migration
- Use adapter pattern if needed for legacy compatibility
- Test after each refactoring step

### Priority Order
1. **High Priority:** Classes > 400 lines with multiple responsibilities
2. **Medium Priority:** Classes 300-400 lines
3. **Low Priority:** Code cleanup, naming improvements

### Backward Compatibility
- Maintain existing public APIs during refactoring
- Mark deprecated methods with `@Deprecated` annotation
- Provide migration path in deprecation comments
- Remove deprecated code only after migration period

---

## 11. Documentation Requirements

### Code Documentation
- README.md for project overview
- This REFACTORING_GUIDELINES.md for standards
- Protocol documentation in protocols/ package
- Architecture diagrams in README.md

### Change Documentation
- Update README.md when adding major features
- Update architecture diagrams when structure changes
- Add migration notes for breaking changes
- Keep REFACTORING_GUIDELINES.md current

---

## 12. Tool-Specific Guidelines

### Eclipse/Sunrise Workbench
- Use Eclipse formatter settings (4 spaces, 120 char line length)
- Keep .classpath and .project files in repository
- Don't commit bin/ directory (in .gitignore)
- Use Eclipse refactoring tools when possible

### Git Practices
- Commit small, focused changes
- Write descriptive commit messages
- Don't commit generated files (*.class, bin/)
- Review .gitignore before committing

---

## 13. Performance Considerations

### Robot Control Constraints
- Motion commands must complete within cycle time
- Avoid blocking operations in robot control thread
- Use asynchronous operations for I/O where possible
- Minimize object allocation in real-time paths
- **KUKA Best Practices:**
  - Use `moveAsync()` instead of `move()` when you need to monitor motion progress
  - Avoid creating new motion objects in tight loops
  - Reuse `MotionParameters` objects when possible
  - Use `MotionBatch` for sequences instead of individual moves

### Network Performance
- Buffer log messages if log client is slow
- Use non-blocking I/O for client communications
- Set appropriate socket timeouts
- Handle disconnections gracefully

### Memory Management (Java 7)
- Be mindful of object creation in loops
- Reuse objects where appropriate
- No try-with-resources (Java 7 limitation)
- Manual resource cleanup in finally blocks

---

## 14. Security Considerations

### Input Validation
- Validate all command parameters
- Check numeric ranges (joint angles, speeds)
- Prevent malformed commands from crashing server
- Log suspicious input patterns

### Access Control
- No authentication in current version (document this)
- Consider adding authentication in future
- Log all command sources (IP addresses)
- Rate limiting for command input (future consideration)

---

## 15. Review and Approval Process

### Code Review Requirements
- All changes should be reviewed by at least one other developer
- Focus review on:
  - Adherence to these guidelines
  - Correct separation of concerns
  - Appropriate error handling
  - Thread safety
  - Performance impact

### Guideline Updates
- These guidelines are living documents
- Propose changes via pull request
- Discuss significant changes with team
- Update version number and date when changed

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-11-12 | GitHub Copilot | Initial document creation |

---

## Quick Reference Card

### File Size Targets
- Target: < 300 lines
- Warning: 300-400 lines
- Refactor: > 400 lines

### Method Size
- Target: < 30 lines
- Maximum: 50 lines

### Naming
- Classes: PascalCase
- Methods: camelCase
- Constants: UPPER_SNAKE_CASE

### Always Ask
1. Does this class have one responsibility?
2. Can I understand what this does in < 1 minute?
3. Would a new developer understand this?
4. Is this the simplest solution that works?
5. Have I followed the guidelines?

---

**Remember:** Good code is code that is easy to understand, easy to change, and hard to break. When in doubt, prioritize clarity over cleverness.

# GitHub Copilot Instructions for iiwaTOFAS

## Project Overview

iiwaTOFAS is a TCP/IP-based control system for KUKA iiwa robots running on the KUKA Sunrise controller. This project
provides a robust communication framework that allows external clients (like ROS2 drivers) to send motion commands and
receive real-time logging and joint state information from the robot.

**Key Purpose**: Bridge between robot control software and KUKA hardware using simple string-based TCP commands.

## Technology Stack

- **Language**: Java (KUKA Sunrise.OS 1.11 API)
- **Framework**: KUKA Robotics API (RoboticsAPIApplication)
- **Communication**: TCP/IP sockets (multi-port architecture)
- **Client Utilities**: Python 3.x
- **Development Environment**: KUKA Sunrise.Workbench

## Architecture

### Multi-Port Server Architecture

The system uses three dedicated TCP server managers following the single responsibility principle:

- **Port 30001**: Task commands (Ros2ServerManager) - receives motion commands and control instructions
- **Port 30002**: Log data broadcasting (LoggingServerManager) - broadcasts JSON-formatted logs
- **Port 30003**: Joint state data (JointStateServerManager) - broadcasts real-time joint positions at 100Hz

### Logging System Architecture

The logging system uses a centralized hub-and-spoke pattern:

```
Background Task → Logger (Singleton) → LoggingServerManager → Network Clients (Python)
                                              ↓
                                      RobotConsoleClient → Robot Console (println)
```

**Key Benefits:**

- All logs from all tasks (foreground and background) appear on robot console
- Multiple Python clients can simultaneously receive logs
- Single responsibility: one server, one port, one function
- Configurable log verbosity (INFO, WARN, ERROR levels) to control output volume

## Project Structure

```
iiwaTOFAS/
├── src/
│   └── hartu/
│       ├── protocols/              # Protocol definitions and constants
│       │   └── constants/          # ActionTypes, message formats
│       └── robot/
│           ├── commands/           # Command data structures (ParsedCommand)
│           ├── communication/      # TCP server and client handlers
│           │   ├── server/         # ServerClass, ServerManagers, Logger
│           │   └── client/         # Client connection utilities
│           ├── executor/           # Robot motion execution (CommandExecutor)
│           └── utils/              # CommandParser, formatters
├── pythonUtils/                    # Python client utilities
├── parsedData/                     # Command history logs (JSON)
└── FastRobotInterface_Client_Source/  # KUKA FRI SDK examples
```

## Code Conventions

### Java/KUKA Sunrise.OS Patterns

1. **Application Structure**: All robot applications extend `RoboticsAPIApplication`
   ```java
   public class MyApp extends RoboticsAPIApplication {
       @Inject
       private LBR robot;
       
       @Override
       public void initialize() { /* setup */ }
       
       @Override
       public void run() { /* main logic */ }
   }
   ```

2. **Dependency Injection**: Use `@Inject` for robot resources
   ```java
   @Inject
   private LBR robot;
   
   @Inject
   @Named("ToolName")
   private Tool myTool;
   ```

3. **Thread Safety**: Use proper synchronization for shared resources
    - Logger is a singleton with synchronized methods
    - ClientHandler instances manage their own state
    - Use CopyOnWriteArrayList for client collections

4. **Exception Handling**: Never let exceptions crash the server
    - Catch and log all exceptions in client handlers
    - Use try-with-resources for socket operations
    - Continue operation after recoverable errors

### Naming Conventions

- **Classes**: PascalCase (e.g., `CommandParser`, `ServerClass`)
- **Methods**: camelCase (e.g., `parseCommand()`, `executeMotion()`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `TASK_PORT`, `MAX_CONNECTIONS`)
- **Packages**: lowercase (e.g., `hartu.robot.commands`)

### Motion Programming

- Joint angles are in **radians** in KUKA API (convert from degrees in protocol)
- Cartesian positions: X,Y,Z in **millimeters**, A,B,C in **degrees**
- Always validate motion parameters before execution
- Use relative velocity/acceleration (0.0-1.0 range)

## Command Protocol

Commands are pipe-delimited strings ending with `#`:

```
ACTION_TYPE|NUM_POINTS|POINT_DATA|VELOCITY|ACCELERATION|JERK|ID#
```

### Action Types (see protocols/constants/ActionTypes.java)

- 0: PTP_AXIS - Point-to-point in joint space
- 1: PTP_FRAME - Point-to-point in Cartesian space
- 2: LIN_AXIS - Linear motion in joint space
- 3: LIN_FRAME - Linear motion in Cartesian space
- 4: CIRC_AXIS - Circular motion in joint space
- 5: CIRC_FRAME - Circular motion in Cartesian space
- 6-8: Continuous motion variants (PTP_AXIS_C, PTP_FRAME_C, LIN_FRAME_C)
- 9-12: Digital I/O operations

### Response Format

- Success: `FREE|ID|success#`
- Failure: `FREE|ID|failure#`
- Initial ready: `FREE|0#`

## Testing and Safety

### Safety Requirements

1. **Log Client Requirement**: A log client MUST be connected before task clients can connect (safety feature)
2. **Emergency Stop**: Always keep E-stop accessible during testing
3. **Manual Mode First**: Test new commands in manual mode before automation
4. **Workspace Clear**: Ensure workspace is clear before running sequences
5. **Command Validation**: All commands are validated before execution

### Testing Approach

- Test motion commands with actual hardware or simulator
- Validate command parsing with unit tests
- Test socket communication with Python clients
- Verify logging output format and content
- Check thread safety under concurrent connections

### No Automated Test Infrastructure

This project does not have automated unit tests or CI/CD. Testing is done manually:

- Deploy to KUKA controller via Sunrise.Workbench
- Test with Python clients in `pythonUtils/`
- Monitor logs via log client
- Verify robot behavior visually

## Documentation References

When working on this project, refer to:

- **[KUKA_PROGRAMMING_GUIDE.md](../KUKA_PROGRAMMING_GUIDE.md)**: Comprehensive guide for KUKA Sunrise.OS API programming
- **[README.md](../README.md)**: Project overview, getting started, command protocol
- **[LOG_FORMAT.md](../LOG_FORMAT.md)**: JSON logging format specification
- **[LOG_VERBOSITY_GUIDE.md](../LOG_VERBOSITY_GUIDE.md)**: How to control log verbosity levels
- **[REFACTORING_GUIDELINES.md](../REFACTORING_GUIDELINES.md)**: Code organization and refactoring best practices
- **[REFACTORING_PLAN.md](../REFACTORING_PLAN.md)**: Current refactoring status and future plans

## Common Patterns

### Adding New Command Types

1. Add enum to `protocols/constants/ActionTypes.java`
2. Update parser logic in `utils/CommandParser.java`
3. Handle execution in `executor/CommandExecutor.java`
4. Update command protocol documentation in README.md
5. Test thoroughly with Python client

### Logging Best Practices

```java
// Use Logger singleton for all logging
Logger.getInstance().log("TAG", "Message");  // INFO level
Logger.getInstance().warn("TAG", "Warning");  // WARN level
Logger.getInstance().error("TAG", "Error");   // ERROR level

// Control log verbosity (set once during initialization)
Logger.getInstance().setMinimumLogLevel(LogLevel.WARN);  // Filter out INFO logs

// Log levels: INFO, WARN, ERROR
// Tags help categorize log messages (e.g., "PARSER", "MOTION", "CONNECTION")
```

### Socket Communication Pattern

```java
// Reading commands (ends with #)
StringBuilder buffer = new StringBuilder();
int c;
while ((c = input.read()) != -1) {
    char ch = (char) c;
    if (ch == '#') {
        processCommand(buffer.toString());
        buffer.setLength(0);
    } else {
        buffer.append(ch);
    }
}
```

### Thread Safety for Broadcast

```java
// Use CopyOnWriteArrayList for client collections
private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

// Broadcast to all clients safely
for (ClientHandler client : clients) {
    try {
        client.sendMessage(message);
    } catch (Exception e) {
        clients.remove(client);
    }
}
```

## Anti-Patterns to Avoid

1. **DON'T** block the robot's main thread with long-running operations
2. **DON'T** use `System.out.println()` directly - use Logger instead
3. **DON'T** create multiple Logger instances - it's a singleton
4. **DON'T** forget to close socket resources - use try-with-resources
5. **DON'T** allow parse errors to crash the server - catch and log
6. **DON'T** mix degrees and radians without proper conversion
7. **DON'T** assume clients are always connected - handle disconnections gracefully

## Build and Deployment

### Development Workflow

1. Import project into KUKA Sunrise.Workbench
2. Configure robot IP in network settings
3. Deploy application to KUKA controller
4. Application starts automatically on controller boot

### No External Build Tools

- No Maven, Gradle, or Ant - KUKA uses Sunrise.Workbench for builds
- IDE handles compilation and packaging
- Deployment is done through Sunrise.Workbench interface

## Key Classes to Know

- **ServerClass**: Main TCP server managing all three ports
- **Ros2ServerManager**: Manages task command connections (port 30001)
- **LoggingServerManager**: Manages log broadcasting (port 30002)
- **JointStateServerManager**: Manages joint state broadcasting (port 30003)
- **ClientHandler**: Handles individual client connections
- **CommandParser**: Converts string commands to ParsedCommand objects
- **CommandExecutor**: Executes robot motions based on parsed commands
- **Logger**: Singleton for centralized logging
- **ParsedCommand**: Validated, structured command data object
- **ActionTypes**: Enum defining all supported command types

## Integration Points

### External Systems

- **ROS2 Drivers**: Primary use case - connect via task port
- **Python Clients**: Utilities for testing and monitoring
- **Custom Control Software**: Any system that can send TCP commands

### Data Flow

1. External client → Task Port (30001) → Command Parser → Command Executor → Robot Motion
2. Robot/Server Events → Logger → Log Port (30002) → Python/ROS2 Log Clients
3. Robot Joint State → Joint State Port (30003) → Monitoring Clients

## Performance Considerations

- **Joint State Broadcasting**: 100Hz (10ms intervals) - optimize for minimal latency
- **Command Processing**: Parse and validate before execution to avoid delays
- **Logging**: Asynchronous to prevent blocking robot operations
- **Socket I/O**: Non-blocking where possible, with proper timeout handling

## Security Considerations

- Industrial network environment (isolated from public internet)
- No authentication/encryption in current implementation
- Trust model: trusted clients on trusted network
- Safety features: E-stop, workspace monitoring, command validation

---

**Last Updated**: 2024
**Maintained by**: Repository contributors
**Questions**: Refer to existing documentation or open an issue

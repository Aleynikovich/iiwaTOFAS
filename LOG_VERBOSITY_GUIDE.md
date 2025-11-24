# Log Verbosity Control Guide

## Overview

The logging system now supports configurable log verbosity levels to help you control the amount of log output based on your operational needs. This is especially useful for reducing log noise during normal operations while maintaining detailed logging for debugging.

## Log Levels

The system supports three log levels in order of increasing severity:

| Level | Description | Use Case |
|-------|-------------|----------|
| **INFO** | Detailed operational messages | Development and debugging |
| **WARN** | Warnings and recoverable issues | Production monitoring |
| **ERROR** | Critical errors and failures | Critical issue tracking |

## Default Behavior

By default, the logger is set to **INFO** level, which shows all log messages (INFO, WARN, and ERROR). This ensures backward compatibility with existing deployments.

## How to Configure Log Level

### In Your Robot Application

You can set the log level in any of your robot application classes that extend `RoboticsAPIApplication` or `RoboticsAPICyclicBackgroundTask`. The best place is typically in the `initialize()` method of your main application or server manager.

#### Example 1: In a Background Task

```java
package hartu.robot.communication.server;

import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;

public class MyServerManager extends RoboticsAPICyclicBackgroundTask
{
    @Override
    public void initialize()
    {
        // Set log level to WARN - shows only warnings and errors
        Logger.getInstance().setMinimumLogLevel(LogLevel.WARN);
        
        // Rest of your initialization code...
    }
}
```

#### Example 2: In CommandExecutor

If you want to reduce log verbosity for the robot executor, you can add this to `CommandExecutor.initialize()`:

```java
@Override
public void initialize() {
    // Set log level before other initialization
    // This will filter out routine INFO logs during motion execution
    Logger.getInstance().setMinimumLogLevel(LogLevel.WARN);
    
    // Start robot console client...
    startRobotConsoleClient();
    
    // Rest of initialization...
}
```

#### Example 3: In Ros2ServerManager

To reduce verbosity for the task server:

```java
@Override
public void initialize()
{
    // Reduce verbosity - show only important events
    Logger.getInstance().setMinimumLogLevel(LogLevel.WARN);
    
    initializeCyclic(0, 1000, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);
    // ...
}
```

### Dynamic Log Level Changes

You can change the log level at any time during runtime:

```java
// Enable verbose logging for debugging
Logger.getInstance().setMinimumLogLevel(LogLevel.INFO);

// Perform some operation...

// Reduce verbosity again
Logger.getInstance().setMinimumLogLevel(LogLevel.WARN);
```

### Checking Current Log Level

```java
LogLevel currentLevel = Logger.getInstance().getMinimumLogLevel();
System.out.println("Current log level: " + currentLevel);
```

## Recommended Settings

### Development and Testing
```java
Logger.getInstance().setMinimumLogLevel(LogLevel.INFO);
```
- Shows all logs including detailed operation traces
- Helps debug issues and understand system behavior
- Useful when testing new commands or troubleshooting

### Production / Normal Operation
```java
Logger.getInstance().setMinimumLogLevel(LogLevel.WARN);
```
- Reduces log noise significantly (filters out ~64% of logs)
- Still shows important warnings and all errors
- Recommended for day-to-day operations
- Makes it easier to spot actual issues

### Critical Monitoring Only
```java
Logger.getInstance().setMinimumLogLevel(LogLevel.ERROR);
```
- Shows only critical failures
- Useful when you want minimal log output
- Good for stable production systems where you only care about failures

## What Gets Filtered

### At INFO Level (Default)
- ✅ All INFO messages (132 total in codebase)
- ✅ All WARN messages (19 total in codebase)
- ✅ All ERROR messages (55 total in codebase)

### At WARN Level
- ❌ INFO messages filtered out (e.g., "Tool loaded", "Motion completed")
- ✅ All WARN messages (e.g., "Tool not found", "Motion cancelled")
- ✅ All ERROR messages (e.g., "Motion failed", "Parse error")

### At ERROR Level
- ❌ INFO messages filtered out
- ❌ WARN messages filtered out
- ✅ All ERROR messages only

## Examples of Filtered Messages

### INFO Messages (Filtered at WARN/ERROR)
- "Tool ID 1 ('GimaticCamera') loaded successfully."
- "Attached tool 'Vacuum1' (ID 2) to robot flange."
- "Motion for command ID cmd_001 completed successfully."
- "Joint State Server started on port 30003"
- "New client connected: 192.168.1.100"

### WARN Messages (Filtered at ERROR)
- "Tool ID 5 not found in mapping. Cannot attach tool."
- "Motion was cancelled: User pressed stop button"
- "Error sending to client 192.168.1.100: Connection reset"

### ERROR Messages (Always Shown)
- "Failed to attach tool 'Vacuum1': Device not responding"
- "Asynchronous motion failed: Unreachable pose detected"
- "Error parsing command: Invalid format"
- "Motion for command ID cmd_123 failed"

## Performance Impact

The log level filtering is very efficient:
- Filtering happens before message formatting
- No performance overhead for filtered messages
- Thread-safe implementation using volatile field
- No locking required for level checks

## Python Log Client

The Python log client (`pythonUtils/log_client.py`) will automatically receive filtered logs based on the server's configured level. No changes needed on the client side.

The color coding will still work correctly:
- 🟢 Green for INFO (if not filtered)
- 🟡 Yellow for WARN (if not filtered)
- 🔴 Red for ERROR (always shown unless filtered)

## Backward Compatibility

This feature is fully backward compatible:
- Default behavior unchanged (INFO level shows all logs)
- Existing code continues to work without modification
- All existing log statements remain in the code
- Only the broadcasting is filtered, not the logging calls themselves

## Implementation Details

For developers interested in the implementation:

- Log levels are defined in `LogLevel.java` enum
- Filtering is implemented in `Logger.java` via `shouldLog()` method
- The minimum log level is stored as a volatile field for thread-safety
- Filtering happens in `logWithLevel()` before message formatting

## See Also

- [LOG_FORMAT.md](LOG_FORMAT.md) - Details about log message format
- [README.md](README.md) - General project documentation
- `LogLevelExample.java` - Code examples demonstrating log level usage

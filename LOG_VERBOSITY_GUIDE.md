# Log Verbosity Control Guide

## Overview

The logging system uses a priority-based verbosity control to help you manage the amount of log output. This system allows you to filter logs based on their importance, reducing noise during normal operations while maintaining critical information visibility.

## Priority Levels

The system uses 5 priority levels, where lower numbers indicate higher priority (more important logs):

| Priority | Level Name | Description | Examples |
|----------|------------|-------------|----------|
| **0** | **CRITICAL** | Must-have logs that should ALWAYS be shown | Parsed commands, program calls, base coordinate data, picking operations |
| **1** | **HIGH** | High-priority logs including errors and important status | Errors, command responses (FREE\|ID\|success#), failures |
| **2** | **MEDIUM** | Medium-priority logs for warnings and state changes | Warnings, parameter clamping, motion cancellations |
| **3** | **LOW** | Low-priority logs for execution progress | Executing commands, motion operations, moving to positions |
| **4** | **DEBUG** | Debug/trace logs for verbose operations | Queue operations, connections, initializations, tool loading |

## Default Behavior

By default, the logger is set to **MEDIUM** (priority 2), which shows priorities 0, 1, and 2. This means:
- ✅ All CRITICAL logs (priority 0) - parsed commands, critical operations
- ✅ All HIGH logs (priority 1) - errors, command responses  
- ✅ All MEDIUM logs (priority 2) - warnings, state changes
- ❌ LOW logs (priority 3) filtered out - execution progress
- ❌ DEBUG logs (priority 4) filtered out - verbose operations

This default setting provides a good balance between visibility and noise reduction for day-to-day operations.

## How to Configure Log Level

### In Your Robot Application

You can set the minimum priority level in any robot application class. The best place is typically in the `initialize()` method.

#### Example 1: Default Configuration (Recommended for Production)

```java
package hartu.robot.communication.server;

import hartu.robot.communication.server.Logger;
import hartu.robot.communication.server.LogLevel;

public class MyServerManager extends RoboticsAPICyclicBackgroundTask
{
    @Override
    public void initialize()
    {
        // Default: Show priorities 0, 1, 2 (CRITICAL, HIGH, MEDIUM)
        Logger.getInstance().setMinimumLogLevel(LogLevel.MEDIUM);
        
        // Rest of your initialization code...
    }
}
```

#### Example 2: Verbose Logging for Development/Debugging

```java
@Override
public void initialize()
{
    // Show all logs including LOW and DEBUG
    Logger.getInstance().setMinimumLogLevel(LogLevel.DEBUG);
    
    // This will show:
    // - All parsed commands (CRITICAL)
    // - All errors and responses (HIGH)
    // - All warnings (MEDIUM)
    // - Execution progress and motion operations (LOW)
    // - Queue operations, connections, tool loading (DEBUG)
    
    // ...
}
```

#### Example 3: Minimal Logging for Stable Production

```java
@Override
public void initialize()
{
    // Show only CRITICAL and HIGH priority logs
    Logger.getInstance().setMinimumLogLevel(LogLevel.HIGH);
    
    // This will show:
    // - All parsed commands (CRITICAL)
    // - All errors and responses (HIGH)
    // But NOT warnings (MEDIUM), progress (LOW), or debug info (DEBUG)
    
    // ...
}
```

#### Example 4: Critical Information Only

```java
@Override
public void initialize()
{
    // Show only CRITICAL priority logs
    Logger.getInstance().setMinimumLogLevel(LogLevel.CRITICAL);
    
    // This will show ONLY:
    // - Parsed commands
    // - Program calls
    // - Base coordinate data
    // - Picking/placing operations
    
    // ...
}
```

### Dynamic Log Level Changes

You can change the log level at any time during runtime:

```java
// Enable verbose logging temporarily for debugging
Logger.getInstance().setMinimumLogLevel(LogLevel.DEBUG);

// Perform some operation...

// Return to production level
Logger.getInstance().setMinimumLogLevel(LogLevel.MEDIUM);
```

### Checking Current Log Level

```java
LogLevel currentLevel = Logger.getInstance().getMinimumLogLevel();
System.out.println("Current minimum log level: " + currentLevel);
```

## Recommended Settings by Use Case

### Development and Testing
```java
Logger.getInstance().setMinimumLogLevel(LogLevel.DEBUG);
```
- **Shows**: Everything (priorities 0-4)
- **Use when**: Developing new features, debugging issues, understanding system behavior
- **Noise level**: High, but comprehensive

### Production / Normal Operation (DEFAULT)
```java
Logger.getInstance().setMinimumLogLevel(LogLevel.MEDIUM);
```
- **Shows**: Priorities 0, 1, 2 (CRITICAL, HIGH, MEDIUM)
- **Use when**: Day-to-day robot operations
- **Noise level**: Moderate, good balance
- **Filters out**: ~60% of logs (execution progress and debug info)

### Stable Production / Monitoring
```java
Logger.getInstance().setMinimumLogLevel(LogLevel.HIGH);
```
- **Shows**: Priorities 0, 1 (CRITICAL, HIGH)
- **Use when**: Stable system, only want to see critical operations and errors
- **Noise level**: Low
- **Filters out**: ~75% of logs

### Critical Operations Only
```java
Logger.getInstance().setMinimumLogLevel(LogLevel.CRITICAL);
```
- **Shows**: Priority 0 only (CRITICAL)
- **Use when**: Monitoring only the most important operations
- **Noise level**: Minimal
- **Filters out**: ~90% of logs

## Log Level Breakdown by Component

### What Gets Logged at Each Priority

#### CRITICAL (Priority 0)
- Parsed command details (ActionType, ID, parameters)
- Program call execution (Program ID, base coordinate data)
- Picking/placing operations (workpiece ID, frame coordinates)
- Stored base coordinate data (workpiece type, position, orientation)

#### HIGH (Priority 1)
- All errors and exceptions
- Command response messages (`FREE|ID|success#` or `FREE|ID|failure#`)
- Motion failures and timeouts
- Parse errors
- Tool attachment failures

#### MEDIUM (Priority 2)
- Warnings (tool not found, invalid parameters)
- Motion cancellations
- Parameter clamping notifications
- Interrupted operations
- Error closing connections

#### LOW (Priority 3)
- Executing motion commands
- Executing IO commands  
- Executing program calls
- Moving to specific positions
- Attached/detached tool messages

#### DEBUG (Priority 4)
- Command added to queue, queue size
- Waiting for command to execute
- Client connected/disconnected
- Server started/initialized
- Tool loaded successfully
- Signaled completion
- Motion completed successfully
- Session closed
- Received command from client

## Examples of Filtered Messages

### At MEDIUM Level (Default - Priorities 0, 1, 2)

✅ **Shown:**
```
[03:47:46.385] [CRITICAL] [ROBOT_EXEC] Picking axis workID: 1
[03:47:46.385] [CRITICAL] [ROBOT_EXEC] Picking axis frame: [X=1000.00 Y=1000.00 Z=1000.00]
[03:57:55.151] [HIGH] [COMM] ClientHandler: Sent response: FREE|cmd_001|success#
[03:57:46.253] [MEDIUM] [CMD_PARAM] Warning: speedOverride (20.0) outside range. Clamping by 0.01
```

❌ **Filtered:**
```
[03:27:04.284] [DEBUG] [QUEUE] Added command ID ec599f9a to queue. Queue size: 1
[03:27:04.284] [DEBUG] [COMM] ClientHandler: Waiting for command ID ec599f9a to execute...
[03:45:36.826] [DEBUG] [ROBOT_EXEC] Tool ID 1 ('GimaticVac1') loaded successfully.
[03:57:55.150] [DEBUG] [ROBOT_EXEC] Motion for command ID 48a7916c completed successfully.
[03:47:29.873] [LOW] [ROBOT_EXEC] Moving to T1Base/P7
```

### At HIGH Level (Priorities 0, 1)

✅ **Shown:**
```
[03:47:46.385] [CRITICAL] [ROBOT_EXEC] Picking axis workID: 1  
[03:57:55.151] [HIGH] [COMM] ClientHandler: Sent response: FREE|cmd_001|success#
[03:45:35.625] [HIGH] [ROBOT_EXEC] Unexpected error: ApplicationExitException
```

❌ **Additional Filtered:**
```
[03:57:46.253] [MEDIUM] [CMD_PARAM] Warning: speedOverride outside range
```

### At CRITICAL Level (Priority 0 only)

✅ **Shown:**
```
[03:47:46.385] [CRITICAL] [PARSER] Parsed program call: 100 - 100 = 0
[03:47:46.385] [CRITICAL] [ROBOT_EXEC] Picking axis workID: 1
[03:47:46.385] [CRITICAL] [ROBOT_EXEC] Stored base coordinate data: Workpiece Type: Axis
```

❌ **Additional Filtered:**
```
[03:57:55.151] [HIGH] [COMM] ClientHandler: Sent response: FREE|cmd_001|success#
```

## Logging in Your Code

When adding new logging calls, use the appropriate method for the priority:

```java
// Priority 0 - Critical operations
Logger.getInstance().critical("TAG", "Parsed command: " + command);

// Priority 1 - Errors and important status
Logger.getInstance().high("TAG", "Command response sent: " + response);
Logger.getInstance().error("TAG", "Operation failed: " + error);

// Priority 2 - Warnings
Logger.getInstance().medium("TAG", "Parameter adjusted");
Logger.getInstance().warn("TAG", "Tool not found, using default");

// Priority 3 - Execution progress
Logger.getInstance().low("TAG", "Executing motion command");

// Priority 4 - Debug/trace info
Logger.getInstance().debug("TAG", "Queue size: " + queueSize);
```

### Legacy Method (Deprecated)

The old `log(String, String)` method still works but defaults to DEBUG priority:

```java
Logger.getInstance().log("TAG", "Message");  // Equivalent to debug()
```

It's recommended to use explicit priority methods instead.

## Performance Impact

The priority-based filtering is very efficient:
- Filtering happens before message formatting
- No string concatenation overhead for filtered messages
- No performance impact for filtered logs
- Thread-safe implementation using volatile field
- No locking required for priority checks

## Python Log Client Compatibility

The Python log client (`pythonUtils/log_client.py`) automatically receives filtered logs based on the server's configured minimum level. No changes needed on the client side.

Color coding works as expected:
- 🟦 Blue for CRITICAL
- 🟢 Green for HIGH
- 🟡 Yellow for MEDIUM  
- 🟠 Orange for LOW
- ⚪ White for DEBUG

Note: The actual color implementation may vary based on the log format parser.

## Backward Compatibility

This feature maintains backward compatibility:
- Default level is MEDIUM (shows CRITICAL, HIGH, MEDIUM)
- Existing code using `error()` and `warn()` continues to work correctly
- All existing log statements remain in the code
- Only the broadcasting is filtered, not the logging calls themselves
- Old `log()` method still works (maps to DEBUG priority)

## Troubleshooting

### "I don't see any logs!"

Check your minimum log level:
```java
LogLevel level = Logger.getInstance().getMinimumLogLevel();
System.out.println("Current level: " + level);
```

If it's set too high (e.g., CRITICAL), you won't see most logs. Change it:
```java
Logger.getInstance().setMinimumLogLevel(LogLevel.MEDIUM);  // or DEBUG
```

### "I'm seeing too many logs!"

Increase the minimum priority to filter more:
```java
Logger.getInstance().setMinimumLogLevel(LogLevel.HIGH);  // Shows only 0, 1
```

### "I need to see specific DEBUG logs temporarily"

Enable DEBUG level temporarily:
```java
Logger.getInstance().setMinimumLogLevel(LogLevel.DEBUG);
// ... do your debugging ...
Logger.getInstance().setMinimumLogLevel(LogLevel.MEDIUM);  // Restore
```

## See Also

- [LOG_FORMAT.md](LOG_FORMAT.md) - Details about log message format  
- [README.md](README.md) - General project documentation
- `LogLevelExample.java` - Code examples demonstrating priority level usage

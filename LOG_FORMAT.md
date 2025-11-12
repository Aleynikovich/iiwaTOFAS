# Log Message Format

## Overview

The logging system broadcasts messages in a structured format that makes it easy to parse and color-code in client applications.

## Message Format

```
[timestamp] [LEVEL] [tag] message\n
```

### Format Components

1. **Timestamp**: `HH:mm:ss.SSS` format (24-hour time with milliseconds)
   - Example: `14:23:45.123`

2. **Level**: One of three severity levels
   - `INFO` - Normal informational messages
   - `WARN` - Warning messages (recoverable issues)
   - `ERROR` - Error messages (failures, exceptions)

3. **Tag**: Component identifier (uppercase)
   - Examples: `ROBOT_EXEC`, `COMM`, `SERVER`, `JOINT_STATE_SRV`

4. **Message**: The actual log message content

5. **Line terminator**: `\n` (newline)

## Examples

```
[10:15:30.456] [INFO] [ROBOT_EXEC] Initializing CommandExecutor with console logging enabled.
[10:15:35.789] [WARN] [ROBOT_EXEC] Motion was cancelled: User pressed stop button
[10:15:40.123] [ERROR] [ROBOT_EXEC] Invalid motion parameters: Unreachable pose detected
```

## Python Client Parsing Example

```python
import re
import socket

# Color codes for terminal output
COLORS = {
    'INFO': '\033[92m',    # Green
    'WARN': '\033[93m',    # Yellow
    'ERROR': '\033[91m',   # Red
    'RESET': '\033[0m'     # Reset
}

# Regex pattern to parse log messages
LOG_PATTERN = re.compile(r'\[([\d:\.]+)\] \[(\w+)\] \[([\w_]+)\] (.+)')

def parse_and_color_log(log_message):
    """Parse a log message and return it with color codes."""
    match = LOG_PATTERN.match(log_message.strip())
    if match:
        timestamp, level, tag, message = match.groups()
        color = COLORS.get(level, COLORS['RESET'])
        return f"{color}[{timestamp}] [{level}] [{tag}] {message}{COLORS['RESET']}"
    return log_message

# Example usage with socket
SERVER_IP = "192.168.0.2"
LOG_PORT = 30002

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.connect((SERVER_IP, LOG_PORT))

buffer = ""
while True:
    data = sock.recv(1024).decode('utf-8')
    if not data:
        break
    
    buffer += data
    while '\n' in buffer:
        line, buffer = buffer.split('\n', 1)
        colored_line = parse_and_color_log(line)
        print(colored_line)
```

## Log Levels Usage Guide

### INFO
Use for:
- Normal operation status
- Successful command execution
- State transitions
- Connection events

### WARN
Use for:
- Recoverable errors
- Cancelled operations
- External stop events
- Timeout warnings

### ERROR
Use for:
- Command failures
- Invalid parameters
- Unreachable poses
- IK failures
- Network errors
- Unexpected exceptions

## Network Broadcasting

- **Port**: 30002
- **Protocol**: TCP
- **Clients**: Multiple simultaneous clients supported
- **Encoding**: UTF-8
- **Reliability**: Messages are broadcast to all connected clients; if a client disconnects, it's automatically removed

## Console Output

Foreground tasks (like CommandExecutor) also write logs to the robot's SmartPad console using the same format. This allows operators to see logs directly on the robot without needing a network connection.

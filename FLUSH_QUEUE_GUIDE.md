# Flush Queue Command - Quick Reference

## Overview
The flush queue command allows you to clear all pending commands from the robot's command queue and reset the robot to its home position.

## When to Use
- After starting CommandExecutor from scratch
- When recovering from an error state
- After an emergency stop
- When you need to clear stale commands
- Before starting a new task sequence

## Automatic Behavior
The queue is automatically flushed when `CommandExecutor` initializes, ensuring a clean start every time the application starts.

## Manual Flush Command

### Command Format
```
14|0||0.0|0.0|0.0|flush_cmd#
```

### Command Breakdown
- `14` - Action type (FLUSH_QUEUE)
- `0` - Number of points (not used for this command)
- Empty fields for point data
- `0.0|0.0|0.0` - Velocity, acceleration, jerk (not used for this command)
- `flush_cmd` - Command ID (can be any identifier)
- `#` - Message terminator

## What Happens
1. **Queue Clearing**: All pending commands are removed from the queue
2. **Thread Notification**: Any threads waiting for those commands are notified (commands marked as failed)
3. **Home Movement**: Robot moves to home position with all joints at 0 degrees
4. **Response**: Server sends `FREE|flush_cmd|success#` on success or `FREE|flush_cmd|failure#` on failure

## Home Position
The home position is defined as all 7 joints at 0 degrees:
- Joint 1: 0°
- Joint 2: 0°
- Joint 3: 0°
- Joint 4: 0°
- Joint 5: 0°
- Joint 6: 0°
- Joint 7: 0°

## Example Usage

### Using Python (provided example script)
```bash
python pythonUtils/flush_queue_example.py
```

### Using Raw TCP Socket (Python)
```python
import socket

ROBOT_IP = "10.66.171.147"  # Your robot's IP
TASK_PORT = 30001

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.connect((ROBOT_IP, TASK_PORT))

# Wait for initial response
data = sock.recv(1024)
print(data.decode())  # Should be "FREE|0#"

# Send flush command
sock.sendall(b"14|0||0.0|0.0|0.0|flush_cmd#")

# Wait for response
response = sock.recv(1024)
print(response.decode())  # Should be "FREE|flush_cmd|success#"

sock.close()
```

### Using the sunriseBench.py Tool
Add this function to your client:
```python
def flush_queue(sock):
    id = generate_command_id()
    command = f"14|0||0.0|0.0|0.0|{id}"
    send_command(sock, command)
    print("< Response:", receive_response(sock))
```

## Safety Notes
- The robot will move to home position after flushing
- Ensure the workspace is clear before sending this command
- The movement uses conservative speed (20% of maximum) for safety
- Any in-progress motion will be cancelled

## Interrupt Loop Fix
This update also fixes an issue where interrupted threads would flood the logs with error messages. The system now checks if a thread is interrupted before attempting to poll commands, preventing the infinite loop of "Error: Interrupted while trying to poll command" messages.

## Related Files
- `src/hartu/robot/communication/server/CommandQueue.java` - Queue management
- `src/hartu/robot/executor/CommandExecutor.java` - Command execution
- `src/hartu/protocols/constants/ActionTypes.java` - Action type definitions
- `pythonUtils/flush_queue_example.py` - Example implementation

## Troubleshooting

### Command Times Out
- Check that the robot is connected and CommandExecutor is running
- Verify that the log client is connected (required for task commands)
- Ensure no other commands are blocking execution

### Robot Doesn't Move to Home
- Check robot logs for error messages
- Verify that home position (all joints 0°) is reachable
- Check for joint limits or singularity issues

### Queue Not Clearing
- Check that command format is correct
- Verify the action type is 14
- Ensure message ends with '#' terminator

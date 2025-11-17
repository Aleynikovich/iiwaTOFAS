# Tool System Architecture Diagram

## System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     Client Application                           │
│  (ROS2 Driver, Python Script, Custom Controller)                │
└───────────────────────────┬─────────────────────────────────────┘
                            │ TCP Command
                            │ Tool ID in field 6
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    ServerClass / ClientHandler                   │
│                         (Port 30001)                             │
└───────────────────────────┬─────────────────────────────────────┘
                            │ Parse Command
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                       CommandParser                              │
│  Extracts: ActionType, Points, ToolID, Speed, etc.             │
└───────────────────────────┬─────────────────────────────────────┘
                            │ ParsedCommand
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                      CommandExecutor                             │
│  ┌────────────────────────────────────────────────────────┐    │
│  │ Tool Registry (Map<String, Tool>)                      │    │
│  │  "GimaticCamera" → Tool Object (loaded, not attached)  │    │
│  │  "Vacuum1"       → Tool Object (loaded, not attached)  │    │
│  │  "Vacuum2"       → Tool Object (loaded, not attached)  │    │
│  │  "Gripper1"      → Tool Object (loaded, not attached)  │    │
│  └────────────────────────────────────────────────────────┘    │
│  ┌────────────────────────────────────────────────────────┐    │
│  │ ToolMapping                                             │    │
│  │  Tool ID 0 → null (Flange)                             │    │
│  │  Tool ID 1 → "GimaticCamera"                           │    │
│  │  Tool ID 2 → "Vacuum1"                                 │    │
│  │  Tool ID 3 → "Vacuum2"                                 │    │
│  │  Tool ID 4 → "Gripper1"                                │    │
│  └────────────────────────────────────────────────────────┘    │
│  ┌────────────────────────────────────────────────────────┐    │
│  │ Currently Attached:                                     │    │
│  │  currentlyAttachedTool → Tool Object or null           │    │
│  │  currentlyAttachedToolName → "Vacuum1" or null         │    │
│  └────────────────────────────────────────────────────────┘    │
└───────────────────────────┬─────────────────────────────────────┘
                            │ Queue Command
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                     MotionExecutor                               │
│                                                                  │
│  executeMotion(ParsedCommand):                                  │
│    1. Get tool ID from command.getMotionParameters().getTool()  │
│    2. Call commandExecutor.getAndAttachToolForId(toolId)        │
│    3. CommandExecutor handles attach/detach                     │
│    4. Execute motion with attached tool or flange               │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                  Tool Attachment Logic                           │
│                (in CommandExecutor)                              │
│                                                                  │
│  if (toolId == 0):                                              │
│    • Detach current tool if any                                 │
│    • Return null (use flange)                                   │
│                                                                  │
│  else:                                                           │
│    • Look up tool name from ToolMapping                         │
│    • Get Tool object from registry                              │
│    • If different from current tool:                            │
│      - Detach current tool                                      │
│      - Attach new tool to robot flange                          │
│    • Return Tool object                                         │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Robot Motion Execution                        │
│                                                                  │
│  if (tool != null):                                             │
│    tool.moveAsync(motion)     ← Motion relative to TCP          │
│  else:                                                           │
│    robot.moveAsync(motion)    ← Motion relative to flange       │
└─────────────────────────────────────────────────────────────────┘
```

## Command Sequence Example

### Scenario: Switch from flange to Vacuum1 to GimaticCamera

```
Time  │ Command                                    │ Action
──────┼────────────────────────────────────────────┼────────────────────────
T0    │ App starts                                 │ Load all tools to registry
      │                                            │ No tools attached yet
──────┼────────────────────────────────────────────┼────────────────────────
T1    │ 0|1|0;10;-5;20;0;-15;0|||0||0.2|cmd001#   │ Tool ID = 0 (Flange)
      │                                            │ • No attachment needed
      │                                            │ • Execute with robot.moveAsync()
──────┼────────────────────────────────────────────┼────────────────────────
T2    │ 1|1|400;0;600;180;0;180|||2||0.15|cmd002# │ Tool ID = 2 (Vacuum1)
      │                                            │ • Look up: 2 → "Vacuum1"
      │                                            │ • Get Vacuum1 from registry
      │                                            │ • Attach Vacuum1 to flange
      │                                            │ • currentlyAttachedTool = Vacuum1
      │                                            │ • Execute with vacuum1.moveAsync()
──────┼────────────────────────────────────────────┼────────────────────────
T3    │ 1|1|450;50;550;180;0;180|||1||0.15|cmd003#│ Tool ID = 1 (GimaticCamera)
      │                                            │ • Look up: 1 → "GimaticCamera"
      │                                            │ • Get GimaticCamera from registry
      │                                            │ • Detach Vacuum1
      │                                            │ • Attach GimaticCamera to flange
      │                                            │ • currentlyAttachedTool = GimaticCamera
      │                                            │ • Execute with gimaticCamera.moveAsync()
──────┼────────────────────────────────────────────┼────────────────────────
T4    │ 0|1|0;0;0;0;0;0;0|||0||0.2|cmd004#        │ Tool ID = 0 (Flange)
      │                                            │ • Detach GimaticCamera
      │                                            │ • currentlyAttachedTool = null
      │                                            │ • Execute with robot.moveAsync()
```

## State Transitions

```
┌──────────────┐
│   Initial    │
│  No Tool     │
│  Attached    │
└──────┬───────┘
       │
       │ Command with Tool ID > 0
       ▼
┌──────────────┐
│   Tool N     │◄────────┐
│   Attached   │         │
└──────┬───────┘         │
       │                 │
       │ Command with    │ Command with
       │ different       │ same Tool ID
       │ Tool ID         │ (no change)
       │                 │
       ▼                 │
┌──────────────┐         │
│  Detach Old  │         │
│  Attach New  │─────────┘
└──────┬───────┘
       │
       │ Command with Tool ID = 0
       ▼
┌──────────────┐
│ Detach Tool  │
│ Use Flange   │
└──────────────┘
```

## Key Design Principles

1. **Lazy Attachment**: Tools loaded at startup but not attached until needed
2. **Automatic Switching**: System handles tool changes transparently
3. **Command-Level Control**: Each command specifies its tool requirement
4. **No Manual Intervention**: Client doesn't manage tool state
5. **Graceful Degradation**: Missing tools fall back to flange
6. **State Tracking**: System knows which tool is currently attached

## Performance Considerations

- **Tool Attachment Time**: ~200-300ms per attach/detach operation
- **Optimization**: Same tool ID → no attach/detach overhead
- **Best Practice**: Group commands by tool to minimize switching
- **Example**: Do all Vacuum1 operations, then all Camera operations

## Error Handling

```
Error Condition                     │ System Response
────────────────────────────────────┼──────────────────────────────────
Tool ID not in mapping              │ Warn, use flange (tool ID 0)
Tool name not in registry           │ Warn, use flange
Tool attachment fails               │ Error, use flange, motion may fail
Tool detachment fails               │ Error, attempt new attachment anyway
Invalid tool ID format (non-int)    │ Warn, default to flange (ID 0)
```

## Logging Output Example

```
[ROBOT_EXEC] Initializing CommandExecutor.
[TOOL_MAPPING] Initialized tool ID mappings: 6 entries
[ROBOT_EXEC] Tool ID 1 ('GimaticCamera') loaded successfully.
[ROBOT_EXEC] Tool ID 2 ('Vacuum1') loaded successfully.
[ROBOT_EXEC] Loaded 2 tool(s). Tools will be attached dynamically...

[ROBOT_EXEC] Received command ID cmd001 from queue for execution.
[ROBOT_EXEC] Executing PTP_AXIS command ID cmd001
[ROBOT_EXEC] Executing motion with robot flange (tool ID 0).

[ROBOT_EXEC] Received command ID cmd002 from queue for execution.
[ROBOT_EXEC] Attached tool 'Vacuum1' (ID 2) to robot flange.
[ROBOT_EXEC] Executing PTP_FRAME command ID cmd002
[ROBOT_EXEC] Executing motion with tool ID 2 TCP.

[ROBOT_EXEC] Received command ID cmd003 from queue for execution.
[ROBOT_EXEC] Detached previous tool 'Vacuum1'.
[ROBOT_EXEC] Attached tool 'GimaticCamera' (ID 1) to robot flange.
[ROBOT_EXEC] Executing PTP_FRAME command ID cmd003
[ROBOT_EXEC] Executing motion with tool ID 1 TCP.
```

---

**Note**: This diagram shows the complete tool attachment flow as implemented in the system. The design supports unlimited tools via the ToolMapping configuration.

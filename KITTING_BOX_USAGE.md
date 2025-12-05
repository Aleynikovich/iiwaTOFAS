# Kitting Box Usage Guide

This document explains how to use the new kitting box abstraction for workpiece placement.

## Overview

The kitting box system automatically manages workpiece placement positions to avoid collisions and optimize the kitting process. When the CommandExecutor restarts, all positions are reset to available.

## Architecture

### Classes

1. **KittingBox**: Manages the entire kitting box and tracks position occupancy
2. **KittingPosition**: Represents a single position in the box with approach and place frames
3. **BoxType**: Enum defining different box types (currently only STANDARD is supported)

### Position Configuration

The standard kitting box has 6 positions:
- **2 positions for Axis workpieces**: PlaceAxis1_1/1_2, PlaceAxis2_1/2_2
- **2 positions for Drum workpieces**: PlaceDrum1_1/1_2, PlaceDrum2_1/2_2
- **2 positions for Disk workpieces**: PlaceDisk1_1/1_2, PlaceDisk2_1/2_2

Each position has two frames:
- **Approach frame** (e.g., PlaceAxis1_1): Safe approach position above the placement location
- **Place frame** (e.g., PlaceAxis1_2): Final drop position where the workpiece is released

## Using Program ID 23

### Command Format

To place a workpiece in the kitting box, send a program call command with:
- **Program ID**: 123 (action type 140 + program ID 23)
- **Frame data**: X, Y, Z, Roll, Pitch, Yaw of the kitting box base from camera
- **Workpiece ID**: 1 (Axis), 2 (Drum), or 3 (Disk)

### Example Command

```
140|1|x;y;z;roll;pitch;yaw|0.0|0.0|0.0|workpieceId|baseName|0.0|commandId#
```

Where:
- `x;y;z;roll;pitch;yaw`: Camera-detected position of kitting box base
- `workpieceId`: 1 for Axis, 2 for Drum, 3 for Disk
- The system will automatically select the next available position

### Workflow

1. **Pick workpiece**: Use existing pick methods (e.g., program 22 for picking axis)
2. **Move to kitting area**: Robot moves to kitting station
3. **Camera captures box position**: 3D camera detects kitting box base frame
4. **Send program 23 command**: ROS driver sends command with frame and workpiece ID
5. **Automatic placement**: System finds next free position and places workpiece
6. **Position tracking**: Position is marked as occupied for future placements

## Position Tracking

### Automatic Position Selection

The system automatically:
1. Searches for the first available position matching the workpiece type
2. Executes the placement motion sequence
3. Marks the position as occupied on success
4. Rejects placement if no positions are available

### Position Reset

All positions are reset to available when:
- CommandExecutor restarts (application initialization)
- The physical box is emptied before restart

### Capacity Management

The system will refuse placement requests when:
- All positions for that workpiece type are occupied (e.g., 2 axis already placed)
- This prevents collisions and maintains safety

Example: If 2 axis are already in the box and you try to place a third axis, the command will fail with an error message.

## Tool Selection

The system automatically selects the correct tool based on workpiece type:
- **Axis**: GimaticGripperV (Gripper)
- **Drum**: GimaticIxtur (CircMagnet)
- **Disk**: GimaticIxtur (CircMagnet)

## Error Handling

The system provides detailed error messages for:
- No available positions for workpiece type
- Missing taught frames in RoboticsAPI.data.xml
- Tool attachment failures
- Motion execution errors

All errors are logged through the Logger system and broadcast to connected clients.

## Legacy Support

The old methods are still available but deprecated:
- `placeAxisBox(frame, workpieceId, positionId)` - Use `placeWorkpieceInBox(frame, workpieceType)` instead
- `placeDrum(frame, workpieceId, positionId)` - Use `placeWorkpieceInBox(frame, workpieceType)` instead
- `placeDisk(frame, workpieceId, positionId)` - Use `placeWorkpieceInBox(frame, workpieceType)` instead

Program IDs 24-28 still work for backward compatibility but use the new abstraction internally:
- **Program 24**: Place Axis at position 2 (legacy)
- **Program 25**: Place Drum at position 1 (legacy)
- **Program 26**: Place Drum at position 2 (legacy)
- **Program 27**: Place Disk at position 1 (legacy)
- **Program 28**: Place Disk at position 2 (legacy)

## Future Extensions

The system is designed to be scalable for:
- Additional box types (BoxType enum can be extended)
- More positions per box (modify KittingBox initialization)
- Different workpiece types (extend WorkpieceType enum)
- Custom placement trajectories (override motion sequences)

## Frame Structure in RoboticsAPI.data.xml

The system expects the following frame hierarchy:

```
/basekitting (base frame)
├── PlaceAxis1_1 (approach)
├── PlaceAxis1_2 (place)
├── PlaceAxis2_1 (approach)
├── PlaceAxis2_2 (place)
├── PlaceDrum1_1 (approach)
├── PlaceDrum1_2 (place)
├── PlaceDrum2_1 (approach)
├── PlaceDrum2_2 (place)
├── PlaceDisk1_1 (approach)
├── PlaceDisk1_2 (place)
├── PlaceDisk2_1 (approach)
└── PlaceDisk2_2 (place)
```

All frames must be taught relative to the `/basekitting` base frame with the appropriate tool attached (GimaticGripperV for Axis, GimaticIxtur for Drum/Disk).

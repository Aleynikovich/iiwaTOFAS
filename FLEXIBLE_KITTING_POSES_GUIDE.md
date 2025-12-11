# Flexible Kitting Poses Implementation Guide

## Overview

This guide explains the new flexible kitting pose system that allows each workpiece type to have its own custom placement trajectory with any number of frames.

## What Changed?

### Before (Fixed 2-Frame System)
Previously, every workpiece position was hardcoded with exactly 2 frames:
- Frame 1: Approach position
- Frame 2: Place position

This was inflexible and couldn't handle complex placement requirements.

### After (Flexible Multi-Frame System)
Now, each workpiece type can define its own trajectory with any number of frames:
- **Axis**: 2 frames (approach, place)
- **Drum**: 3 frames (approach, intermediate, place)
- **Disk**: 2 frames (approach, place)
- **Future workpieces**: Can use 4, 5, or more frames as needed

## How It Works

### 1. Frame Trajectory Execution

The robot executes frames in sequence:
1. **First frame**: Executed with PTP (Point-to-Point) motion at 70% speed
2. **Middle frames**: Executed with LIN (Linear) motion at 20% speed
3. **Last frame**: Final placement position where workpiece is released
4. **Return**: Robot returns to first frame (approach) to clear the area

### 2. Example: Drum Placement (3 Frames)

```
Frame 1 (PlaceDrum1_1) - Approach
    ↓ PTP motion (70% speed)
Frame 2 (PlaceDrum1_2) - Intermediate position
    ↓ LIN motion (20% speed)
Frame 3 (PlaceDrum1_3) - Final place position
    ↓ Release workpiece
    ↓ LIN motion (70% speed) back
Frame 1 (PlaceDrum1_1) - Clear the area
```

## Configuration

### How to Add/Modify Trajectories

Edit `KittingBox.java` in the `initializeStandardBox()` method:

```java
// Example: Add a 4-frame trajectory for a new workpiece type
positions.add(new KittingPosition(
    WorkpieceType.NEW_TYPE, 
    "Approach1", 
    "Intermediate1", 
    "Intermediate2", 
    "Place1"
));
```

### Required Frames in RoboticsAPI.data.xml

For the current configuration, you need these frames under `/basekitting`:

**Axis Positions (2 frames each):**
- `/basekitting/PlaceAxis1_1` (approach)
- `/basekitting/PlaceAxis1_2` (place)
- `/basekitting/PlaceAxis2_1` (approach)
- `/basekitting/PlaceAxis2_2` (place)

**Drum Positions (3 frames each):**
- `/basekitting/PlaceDrum1_1` (approach)
- `/basekitting/PlaceDrum1_2` (intermediate)
- `/basekitting/PlaceDrum1_3` (place)
- `/basekitting/PlaceDrum2_1` (approach)
- `/basekitting/PlaceDrum2_2` (intermediate)
- `/basekitting/PlaceDrum2_3` (place)

**Disk Positions (2 frames each):**
- `/basekitting/PlaceDisk1_1` (approach)
- `/basekitting/PlaceDisk1_2` (place)
- `/basekitting/PlaceDisk2_1` (approach)
- `/basekitting/PlaceDisk2_2` (place)

## Teaching New Frames

When adding new intermediate frames:

1. **Attach the correct tool** for the workpiece type:
   - Axis: GimaticGripperV
   - Drum: GimaticIxtur
   - Disk: GimaticIxtur

2. **Move robot to position** manually

3. **Teach frame** in Sunrise.Workbench under `/basekitting/`

4. **Verify frame order** matches the trajectory sequence

5. **Test trajectory** by running the placement command

## Benefits of Flexible System

### 1. Complex Placements
Drums may require careful intermediate positioning to avoid collision or ensure proper alignment.

### 2. Safety
Additional frames allow the robot to navigate around obstacles or through safe zones.

### 3. Precision
More control points mean more precise placement trajectories.

### 4. Easy Extensibility
Add new workpiece types with different trajectory requirements without changing core logic.

## Code Structure

### KittingPosition Class
```java
// Flexible constructor with varargs
public KittingPosition(WorkpieceType type, String... frameNames)

// Get all frames
public List<String> getFrameNames()
```

### KittingBox Class
```java
// Initialize positions with flexible frame counts
positions.add(new KittingPosition(WorkpieceType.DRUM, 
    "PlaceDrum1_1", "PlaceDrum1_2", "PlaceDrum1_3"));
```

### ProgramSubroutines Class
```java
// Execute trajectory dynamically
List<String> frameNames = position.getFrameNames();
for (int i = 0; i < frameNames.size(); i++) {
    if (i == 0) {
        // First frame: PTP
        toolToUse.move(ptp(targetFrame).setJointVelocityRel(0.7));
    } else {
        // Subsequent frames: LIN
        toolToUse.move(lin(targetFrame).setJointVelocityRel(0.2));
    }
}
```

## Testing Checklist

- [ ] Teach all required frames in RoboticsAPI.data.xml
- [ ] Verify frame hierarchy under `/basekitting/`
- [ ] Test axis placement (2 frames)
- [ ] Test drum placement (3 frames) - **NEW!**
- [ ] Test disk placement (2 frames)
- [ ] Verify robot clears the area after placement
- [ ] Check error messages if frames are missing

## Troubleshooting

### Error: "Taught frame not found"
- Check frame names in `KittingBox.java` match frames in RoboticsAPI.data.xml
- Verify frame hierarchy (must be under `/basekitting/`)

### Robot motion is jerky
- Ensure velocity settings are appropriate
- Check frame positions don't require sudden direction changes

### Workpiece not released properly
- Verify final frame position is at correct height
- Check tool grip/release timing

## Future Enhancements

Possible improvements (not implemented):
- Configurable motion speeds per frame
- Different motion types (PTP/LIN) per frame
- Dynamic frame addition based on workpiece size
- Collision avoidance waypoints

## Migration from Old System

The old fixed 2-frame system is still supported through deprecated methods:
- `getFrameNameApproach()` - returns first frame
- `getFrameNamePlace()` - returns last frame

However, new code should use `getFrameNames()` to access the full trajectory.

## Summary

This flexible system allows each workpiece type to have its own custom placement trajectory with any number of frames, solving the original issue where drums needed 3 poses instead of just 2. The implementation is backward compatible, easy to extend, and maintains the same safety and error handling as the original system.

# GitHub Copilot Instructions for iiwaTOFAS

## Project Overview

This is a KUKA iiwa robot control system that implements communication interfaces for robot motion control and I/O operations. The project uses the KUKA Robotics API (Sunrise) and implements server-client communication for robot commands.

## Technology Stack

- **Language**: Java
- **Framework**: KUKA Robotics API (Sunrise Workbench)
- **Build System**: IntelliJ IDEA project with external KUKAJavaLib
- **Communication**: TCP/IP socket-based command protocol
- **Robot Interface**: Fast Robot Interface (FRI) for real-time control

## Project Structure

- `src/com/kuka/` - KUKA-generated I/O access classes
- `src/hartu/robot/` - Main robot control implementation
  - `commands/` - Command data structures and parsing
  - `communication/` - Server and client implementations for robot communication
  - `utils/` - Utility classes for parsing, units conversion
  - `logging/` - Remote logging functionality
- `src/hartu/protocols/` - Protocol constants and message definitions
- `KUKAJavaLib/` - KUKA Robotics API library dependencies
- `FastRobotInterface_Client_Source/` - FRI client SDK

## Coding Guidelines

### Java Conventions
- Follow standard Java naming conventions (camelCase for methods/variables, PascalCase for classes)
- Use meaningful variable and method names that reflect the robotics domain
- Handle exceptions appropriately, especially for network I/O operations
- Add proper error logging using System.err for error messages

### Robotics-Specific Considerations
- Be cautious with robot motion commands - incorrect parameters can cause safety issues
- Always validate motion parameters (positions, velocities, accelerations)
- Respect real-time constraints in cyclic tasks
- Handle socket disconnections gracefully to avoid robot stops
- Units matter: distinguish between radians/degrees and mm/meters (use AngularUnit and LinearUnit classes)

### Communication Protocol
- Server listens on port 30001 for commands
- Commands are parsed using the CommandParser utility
- Responses should be sent back to clients for acknowledgment
- Handle socket timeouts and reconnection scenarios

## Development Workflow

### Building
This project is built using IntelliJ IDEA or KUKA Sunrise Workbench:
1. Open the project in IntelliJ IDEA
2. Ensure KUKAJavaLib is properly linked in project libraries
3. Build the project using IDE build tools

### Testing
- Test on KUKA Sunrise.Workbench simulation before deploying to real hardware
- Verify communication protocol changes with test clients
- Always validate safety-critical motion commands in simulation first

### Deployment
- Deploy to KUKA controller using Sunrise Workbench
- Verify safety configuration before enabling on real robot
- Test communication endpoints before full integration

## Important Notes

- **Safety First**: This code controls physical robot hardware. Always validate changes in simulation before real deployment.
- **API Dependencies**: Code depends on KUKA Robotics API which may not be available in all environments
- **Network Security**: Server sockets are exposed - ensure appropriate network security in production
- **Real-time Constraints**: Cyclic tasks must complete within their time budget to avoid cycle overruns

## Common Tasks

When adding new robot commands:
1. Define command structure in `hartu.robot.commands`
2. Update CommandParser to handle new command types
3. Implement command execution logic in appropriate task classes
4. Add proper error handling and validation
5. Test thoroughly in simulation

When modifying communication protocol:
1. Update both server and client implementations
2. Maintain backward compatibility where possible
3. Document protocol changes
4. Test with various client scenarios (disconnect, reconnect, invalid commands)

## Questions to Ask

Before making changes, consider:
- Does this change affect robot safety?
- Are all motion parameters properly validated?
- Is the communication protocol backward compatible?
- Have units been properly converted (degrees/radians, mm/meters)?
- Will this work in both simulation and real hardware?

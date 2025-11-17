# IO List - Centralized I/O Access

## Overview

The `IOList` class provides centralized access to all available I/Os on the robot. Instead of working with three separate I/O group files, you can now access all I/Os through a single, unified interface.

## Available I/O Groups

### 1. Ethercat_x44 I/O Group
- **8 Digital Inputs**: Input1 through Input8
- **8 Digital Outputs**: Output1 through Output8

### 2. IO Flange I/O Group
- **8 Digital Inputs**: DI_Flange1 through DI_Flange8
- **8 Digital Outputs**: DO_Flange1 through DO_Flange8

### 3. Media Flange I/O Group
- **6 Digital Inputs**: 
  - InputX3Pin3, InputX3Pin4, InputX3Pin10, InputX3Pin13, InputX3Pin16
  - UserButton
- **8 Digital Outputs**: 
  - LEDBlue, SwitchOffX3Voltage
  - OutputX3Pin1, OutputX3Pin2, OutputX3Pin11, OutputX3Pin12
  - LedRed, LedGreen

## Usage Examples

### Basic Setup

```java
import hartu.robot.io.IOList;
import com.kuka.generated.ioAccess.*;

// Initialize the IO groups (typically done via dependency injection)
Ethercat_x44IOGroup ethercat = new Ethercat_x44IOGroup(controller);
IOFlangeIOGroup ioFlange = new IOFlangeIOGroup(controller);
MediaFlangeIOGroup mediaFlange = new MediaFlangeIOGroup(controller);

// Create the IOList
IOList ioList = new IOList(ethercat, ioFlange, mediaFlange);
```

### Reading Inputs

```java
// Read Ethercat inputs
boolean input1 = ioList.getEthercat_Input1();
boolean input2 = ioList.getEthercat_Input2();

// Read IO Flange inputs
boolean flangeInput1 = ioList.getIOFlange_DI_Flange1();
boolean flangeInput2 = ioList.getIOFlange_DI_Flange2();

// Read Media Flange inputs
boolean userButton = ioList.getMediaFlange_UserButton();
boolean pin3 = ioList.getMediaFlange_InputX3Pin3();
```

### Setting Outputs

```java
// Set Ethercat outputs
ioList.setEthercat_Output1(true);
ioList.setEthercat_Output2(false);

// Set IO Flange outputs
ioList.setIOFlange_DO_Flange1(true);
ioList.setIOFlange_DO_Flange2(false);

// Set Media Flange outputs
ioList.setMediaFlange_LEDBlue(true);
ioList.setMediaFlange_LedRed(false);
ioList.setMediaFlange_LedGreen(true);
```

### Accessing Original IO Groups

If you need direct access to the original IO group objects:

```java
Ethercat_x44IOGroup ethercat = ioList.getEthercat();
IOFlangeIOGroup ioFlange = ioList.getIOFlange();
MediaFlangeIOGroup mediaFlange = ioList.getMediaFlange();
```

### Getting I/O Information

```java
// Print a summary of all available I/Os
String summary = ioList.getIOSummary();
System.out.println(summary);

// Get current state of all I/Os
String states = ioList.getCurrentIOStates();
System.out.println(states);
```

## Benefits

1. **Single Point of Access**: All I/Os accessible from one object
2. **Clear Naming**: Method names indicate which I/O group they belong to
3. **Type Safety**: Full compile-time type checking
4. **Documentation**: Built-in summary and state reporting methods
5. **Backwards Compatible**: Original IO group objects still accessible

## Integration with Existing Code

The IOList class is designed to work alongside your existing code. You can continue using the individual IO group classes directly, or you can migrate to using IOList for a more centralized approach.

For example, in `ToolController`:
```java
// Instead of:
toolControlIO.setOutput3(true);

// You can use:
ioList.setEthercat_Output3(true);
```

## Notes

- All I/O operations are pass-through to the underlying IO group objects
- The IOList does not maintain any state - it simply provides a unified interface
- Thread safety is determined by the underlying KUKA IO group implementations

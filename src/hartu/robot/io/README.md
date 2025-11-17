# IO List - Centralized I/O Access

## Overview

The `IOList` class provides centralized access to all available I/Os on the robot, including 64 virtual marks for custom use. Instead of working with three separate I/O group files, you can now access all I/Os through a single, unified interface.

## Available I/Os

### 0. Virtual Marks (Software Flags)
- **64 Virtual Marks**: Mark1 through Mark64
- **Purpose**: These are NOT physical I/Os. They are software flags that you can use for any custom purpose (state tracking, flow control, debugging, etc.)
- **Features**:
  - Stored in memory only (not connected to hardware)
  - Can be set/get like any other I/O
  - Persist for the lifetime of the IOList object
  - Useful for marking program states, conditions, or custom logic

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

## I/O Index Map

### Simple Array Access (Recommended)

The easiest way to access I/Os is using simple array indexing:

**INPUTS:** `ioList.in.get(index)`
- **1-64**: Virtual Marks (Mark1-Mark64)
- **65-72**: Ethercat Inputs (Input1-Input8)
- **73-80**: IOFlange Inputs (DI_Flange1-DI_Flange8)
- **81-86**: MediaFlange Inputs (InputX3Pin3, InputX3Pin4, InputX3Pin10, InputX3Pin13, InputX3Pin16, UserButton)

**OUTPUTS:** `ioList.out.get(index)` / `ioList.out.set(index, value)`
- **1-64**: Virtual Marks (Mark1-Mark64)
- **65-72**: Ethercat Outputs (Output1-Output8)
- **73-80**: IOFlange Outputs (DO_Flange1-DO_Flange8)
- **81-88**: MediaFlange Outputs (LEDBlue, SwitchOffX3Voltage, OutputX3Pin1, OutputX3Pin2, OutputX3Pin11, OutputX3Pin12, LedRed, LedGreen)

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

### Array-Based Access (Simplest Method)

```java
// Read inputs using simple indexing
boolean mark1 = ioList.in.get(1);          // Virtual mark 1
boolean ethInput1 = ioList.in.get(65);     // Ethercat Input1
boolean flangeInput1 = ioList.in.get(73);  // IOFlange DI_Flange1
boolean userButton = ioList.in.get(86);    // MediaFlange UserButton

// Set outputs using simple indexing
ioList.out.set(1, true);    // Set virtual mark 1
ioList.out.set(65, true);   // Set Ethercat Output1
ioList.out.set(73, false);  // Set IOFlange DO_Flange1
ioList.out.set(81, true);   // Set MediaFlange LEDBlue

// Read output states
boolean output65State = ioList.out.get(65);

// Loop through inputs
for (int i = 65; i <= 72; i++) {
    boolean value = ioList.in.get(i);
    System.out.println("Ethercat Input " + (i-64) + ": " + value);
}
```

### Working with Virtual Marks

Virtual marks are software flags (not physical I/Os) that you can use for custom purposes:

```java
// Set marks to track program states
ioList.setMark(1, true);   // Mark that initialization is complete
ioList.setMark(5, true);   // Mark that calibration was done
ioList.setMark(10, true);  // Mark a specific condition

// Read marks to check states
if (ioList.getMark(1)) {
    // Initialization is complete, proceed
}

if (ioList.getMark(5) && ioList.getMark(10)) {
    // Both conditions are met
}

// Reset all marks
ioList.resetAllMarks();

// Check how many marks are available
int totalMarks = ioList.getTotalMarks(); // Returns 64
```

**Use Cases for Virtual Marks:**
- Track workflow states (e.g., Mark1 = "initialized", Mark2 = "calibrated")
- Implement custom interlocks or safety conditions
- Debug program flow by setting marks at key points
- Store temporary flags during complex operations
- Create custom state machines

### Alternative: Named Method Access

You can also use descriptive method names instead of array indexing:

```java
// Read Ethercat inputs
boolean input1 = ioList.getEthercat_Input1();
boolean input2 = ioList.getEthercat_Input2();

// Or use array access (simpler):
boolean input1 = ioList.in.get(65);
boolean input2 = ioList.in.get(66);

// Read IO Flange inputs
boolean flangeInput1 = ioList.getIOFlange_DI_Flange1();
// Or: ioList.in.get(73);

// Read Media Flange inputs
boolean userButton = ioList.getMediaFlange_UserButton();
// Or: ioList.in.get(86);
```

### Alternative: Setting Outputs with Named Methods

```java
// Set Ethercat outputs
ioList.setEthercat_Output1(true);
ioList.setEthercat_Output2(false);

// Or use array access (simpler):
ioList.out.set(65, true);
ioList.out.set(66, false);

// Set IO Flange outputs
ioList.setIOFlange_DO_Flange1(true);
// Or: ioList.out.set(73, true);

// Set Media Flange outputs
ioList.setMediaFlange_LEDBlue(true);
// Or: ioList.out.set(81, true);
```

### Comparison of Access Methods

```java
// Three ways to read Ethercat Input 1 (all equivalent):
boolean val1 = ioList.in.get(65);                    // SIMPLEST - Array access
boolean val2 = ioList.getEthercat_Input1();          // Named method
boolean val3 = ioList.getEthercat().getInput1();     // Direct group access

// Three ways to set Ethercat Output 1 (all equivalent):
ioList.out.set(65, true);                            // SIMPLEST - Array access
ioList.setEthercat_Output1(true);                    // Named method
ioList.getEthercat().setOutput1(true);               // Direct group access
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

# I/O Index Map - Quick Reference

## How to Use

```java
// Read any input
boolean value = ioList.in.get(INDEX);

// Set any output
ioList.out.

set(INDEX, true/false);
```

## Complete Index Map

### Virtual Marks (1-64)

Software flags for custom use - NOT physical I/Os

| Index | Name         | Type    | Description                                           |
|-------|--------------|---------|-------------------------------------------------------|
| 1-64  | Mark1-Mark64 | Virtual | Software flags for state tracking, flow control, etc. |

**Access:**

```java
ioList.in.get(1);        // Read mark 1
ioList.out.

set(1,true); // Set mark 1
```

---

### Ethercat_x44 I/O Group (65-72)

#### Inputs (65-72)

| Index | Name   | Type          | Physical I/O     |
|-------|--------|---------------|------------------|
| 65    | Input1 | Digital Input | Ethercat Input 1 |
| 66    | Input2 | Digital Input | Ethercat Input 2 |
| 67    | Input3 | Digital Input | Ethercat Input 3 |
| 68    | Input4 | Digital Input | Ethercat Input 4 |
| 69    | Input5 | Digital Input | Ethercat Input 5 |
| 70    | Input6 | Digital Input | Ethercat Input 6 |
| 71    | Input7 | Digital Input | Ethercat Input 7 |
| 72    | Input8 | Digital Input | Ethercat Input 8 |

**Access:**

```java
boolean val = ioList.in.get(65);  // Read Ethercat Input1
```

#### Outputs (65-72)

| Index | Name    | Type           | Physical I/O      |
|-------|---------|----------------|-------------------|
| 65    | Output1 | Digital Output | Ethercat Output 1 |
| 66    | Output2 | Digital Output | Ethercat Output 2 |
| 67    | Output3 | Digital Output | Ethercat Output 3 |
| 68    | Output4 | Digital Output | Ethercat Output 4 |
| 69    | Output5 | Digital Output | Ethercat Output 5 |
| 70    | Output6 | Digital Output | Ethercat Output 6 |
| 71    | Output7 | Digital Output | Ethercat Output 7 |
| 72    | Output8 | Digital Output | Ethercat Output 8 |

**Access:**

```java
ioList.out.set(65,true);  // Set Ethercat Output1
```

---

### IOFlange I/O Group (73-80)

#### Inputs (73-80)

| Index | Name       | Type          | Physical I/O             |
|-------|------------|---------------|--------------------------|
| 73    | DI_Flange1 | Digital Input | IOFlange Digital Input 1 |
| 74    | DI_Flange2 | Digital Input | IOFlange Digital Input 2 |
| 75    | DI_Flange3 | Digital Input | IOFlange Digital Input 3 |
| 76    | DI_Flange4 | Digital Input | IOFlange Digital Input 4 |
| 77    | DI_Flange5 | Digital Input | IOFlange Digital Input 5 |
| 78    | DI_Flange6 | Digital Input | IOFlange Digital Input 6 |
| 79    | DI_Flange7 | Digital Input | IOFlange Digital Input 7 |
| 80    | DI_Flange8 | Digital Input | IOFlange Digital Input 8 |

**Access:**

```java
boolean val = ioList.in.get(73);  // Read IOFlange DI_Flange1
```

#### Outputs (73-80)

| Index | Name       | Type           | Physical I/O              |
|-------|------------|----------------|---------------------------|
| 73    | DO_Flange1 | Digital Output | IOFlange Digital Output 1 |
| 74    | DO_Flange2 | Digital Output | IOFlange Digital Output 2 |
| 75    | DO_Flange3 | Digital Output | IOFlange Digital Output 3 |
| 76    | DO_Flange4 | Digital Output | IOFlange Digital Output 4 |
| 77    | DO_Flange5 | Digital Output | IOFlange Digital Output 5 |
| 78    | DO_Flange6 | Digital Output | IOFlange Digital Output 6 |
| 79    | DO_Flange7 | Digital Output | IOFlange Digital Output 7 |
| 80    | DO_Flange8 | Digital Output | IOFlange Digital Output 8 |

**Access:**

```java
ioList.out.set(73,true);  // Set IOFlange DO_Flange1
```

---

### MediaFlange I/O Group (81-88)

#### Inputs (81-86)

| Index | Name         | Type          | Physical I/O            |
|-------|--------------|---------------|-------------------------|
| 81    | InputX3Pin3  | Digital Input | MediaFlange X3 Pin 3    |
| 82    | InputX3Pin4  | Digital Input | MediaFlange X3 Pin 4    |
| 83    | InputX3Pin10 | Digital Input | MediaFlange X3 Pin 10   |
| 84    | InputX3Pin13 | Digital Input | MediaFlange X3 Pin 13   |
| 85    | InputX3Pin16 | Digital Input | MediaFlange X3 Pin 16   |
| 86    | UserButton   | Digital Input | MediaFlange User Button |

**Access:**

```java
boolean val = ioList.in.get(81);  // Read MediaFlange InputX3Pin3
boolean btn = ioList.in.get(86);  // Read User Button
```

#### Outputs (81-88)

| Index | Name               | Type           | Physical I/O                  |
|-------|--------------------|----------------|-------------------------------|
| 81    | LEDBlue            | Digital Output | MediaFlange Blue LED          |
| 82    | SwitchOffX3Voltage | Digital Output | MediaFlange X3 Voltage Switch |
| 83    | OutputX3Pin1       | Digital Output | MediaFlange X3 Pin 1          |
| 84    | OutputX3Pin2       | Digital Output | MediaFlange X3 Pin 2          |
| 85    | OutputX3Pin11      | Digital Output | MediaFlange X3 Pin 11         |
| 86    | OutputX3Pin12      | Digital Output | MediaFlange X3 Pin 12         |
| 87    | LedRed             | Digital Output | MediaFlange Red LED           |
| 88    | LedGreen           | Digital Output | MediaFlange Green LED         |

**Access:**

```java
ioList.out.set(81,true);   // Set Blue LED
ioList.out.

set(87,true);   // Set Red LED
ioList.out.

set(88,false);  // Turn off Green LED
```

---

## Summary

| Range | Total | Type                |
|-------|-------|---------------------|
| 1-64  | 64    | Virtual Marks       |
| 65-72 | 8     | Ethercat Inputs     |
| 65-72 | 8     | Ethercat Outputs    |
| 73-80 | 8     | IOFlange Inputs     |
| 73-80 | 8     | IOFlange Outputs    |
| 81-86 | 6     | MediaFlange Inputs  |
| 81-88 | 8     | MediaFlange Outputs |

**Total:** 64 virtual marks + 86 input indices + 88 output indices

---

## Common Usage Patterns

### Loop Through All Ethercat Inputs

```java
for(int i = 65;
i <=72;i++){
boolean value = ioList.in.get(i);
    System.out.

println("Input "+(i-64) +": "+value);
        }
```

### Check Multiple Conditions

```java
if(ioList.in.get(65) &&ioList.in.

get(73)){
        // Both Ethercat Input1 and IOFlange Input1 are active
        ioList.out.

set(65,true);  // Activate output
}
```

### Use Marks for State Machine

```java
final int STATE_IDLE = 1;
final int STATE_RUNNING = 2;
final int STATE_ERROR = 3;

ioList.out.

set(STATE_IDLE, true);  // Set to idle state

if(ioList.in.

get(STATE_IDLE)){
        // Start operation
        ioList.out.

set(STATE_IDLE, false);
    ioList.out.

set(STATE_RUNNING, true);
}
```

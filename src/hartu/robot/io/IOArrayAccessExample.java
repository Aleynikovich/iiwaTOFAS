package hartu.robot.io;

/**
 * Example demonstrating array-based access to I/Os.
 * Shows how to use simple indexing like ioList.in.get(65) instead of method calls.
 */
public class IOArrayAccessExample
{

    public static void demonstrateArrayAccess(IOList ioList)
    {
        System.out.println("=== Array-Based I/O Access Example ===\n");

        // ========================================
        // INPUTS - Simple array-like access
        // ========================================

        System.out.println("--- Reading Inputs ---");

        // Virtual marks: 1-64
        boolean mark1 = ioList.in.get(1);   // Virtual mark 1
        boolean mark10 = ioList.in.get(10); // Virtual mark 10
        System.out.println("Mark 1: " + mark1);
        System.out.println("Mark 10: " + mark10);

        // Ethercat inputs: 65-72
        boolean ethercatInput1 = ioList.in.get(65); // Ethercat Input1
        boolean ethercatInput2 = ioList.in.get(66); // Ethercat Input2
        System.out.println("Ethercat Input 1 (index 65): " + ethercatInput1);
        System.out.println("Ethercat Input 2 (index 66): " + ethercatInput2);

        // IOFlange inputs: 73-80
        boolean flangeInput1 = ioList.in.get(73); // IOFlange DI_Flange1
        boolean flangeInput8 = ioList.in.get(80); // IOFlange DI_Flange8
        System.out.println("IOFlange Input 1 (index 73): " + flangeInput1);
        System.out.println("IOFlange Input 8 (index 80): " + flangeInput8);

        // MediaFlange inputs: 81-86
        boolean mediaPin3 = ioList.in.get(81);   // InputX3Pin3
        boolean userButton = ioList.in.get(86);  // UserButton
        System.out.println("MediaFlange Pin3 (index 81): " + mediaPin3);
        System.out.println("User Button (index 86): " + userButton);

        // ========================================
        // OUTPUTS - Simple array-like access
        // ========================================

        System.out.println("\n--- Setting Outputs ---");

        // Virtual marks: 1-64
        ioList.out.set(1, true);   // Set virtual mark 1
        ioList.out.set(5, true);   // Set virtual mark 5
        System.out.println("Set Mark 1 and Mark 5 to TRUE");

        // Ethercat outputs: 65-72
        ioList.out.set(65, true);  // Ethercat Output1
        ioList.out.set(66, false); // Ethercat Output2
        System.out.println("Set Ethercat Output 1 (index 65) to TRUE");
        System.out.println("Set Ethercat Output 2 (index 66) to FALSE");

        // IOFlange outputs: 73-80
        ioList.out.set(73, true);  // IOFlange DO_Flange1
        ioList.out.set(80, false); // IOFlange DO_Flange8
        System.out.println("Set IOFlange Output 1 (index 73) to TRUE");
        System.out.println("Set IOFlange Output 8 (index 80) to FALSE");

        // MediaFlange outputs: 81-88
        ioList.out.set(81, true);  // LEDBlue
        ioList.out.set(87, true);  // LedRed
        ioList.out.set(88, false); // LedGreen
        System.out.println("Set LED Blue (index 81) to TRUE");
        System.out.println("Set LED Red (index 87) to TRUE");
        System.out.println("Set LED Green (index 88) to FALSE");

        // ========================================
        // READING BACK OUTPUT VALUES
        // ========================================

        System.out.println("\n--- Reading Output States ---");
        boolean output65 = ioList.out.get(65);
        boolean output73 = ioList.out.get(73);
        System.out.println("Ethercat Output 1 (index 65) state: " + output65);
        System.out.println("IOFlange Output 1 (index 73) state: " + output73);

        // ========================================
        // USING IN LOOPS
        // ========================================

        System.out.println("\n--- Loop Through All Virtual Marks ---");
        int activeMarks = 0;
        for (int i = 1; i <= 64; i++)
        {
            if (ioList.in.get(i))
            {
                System.out.println("Mark " + i + " is active");
                activeMarks++;
            }
        }
        System.out.println("Total active marks: " + activeMarks);

        System.out.println("\n--- Loop Through Ethercat Inputs ---");
        for (int i = 65; i <= 72; i++)
        {
            boolean value = ioList.in.get(i);
            int inputNum = i - 64;
            System.out.println("Ethercat Input " + inputNum + " (index " + i + "): " + value);
        }
    }

    /**
     * Quick reference guide
     */
    public static void printIndexMap()
    {
        System.out.println("=== I/O Index Map ===\n");
        System.out.println("INPUTS (ioList.in.get(index)):");
        System.out.println("  1-64:   Virtual Marks (Mark1-Mark64)");
        System.out.println("  65-72:  Ethercat Inputs (Input1-Input8)");
        System.out.println("  73-80:  IOFlange Inputs (DI_Flange1-DI_Flange8)");
        System.out.println("  81-86:  MediaFlange Inputs");
        System.out.println("          81=InputX3Pin3, 82=InputX3Pin4, 83=InputX3Pin10");
        System.out.println("          84=InputX3Pin13, 85=InputX3Pin16, 86=UserButton");
        System.out.println();
        System.out.println("OUTPUTS (ioList.out.get(index) / ioList.out.set(index, value)):");
        System.out.println("  1-64:   Virtual Marks (Mark1-Mark64)");
        System.out.println("  65-72:  Ethercat Outputs (Output1-Output8)");
        System.out.println("  73-80:  IOFlange Outputs (DO_Flange1-DO_Flange8)");
        System.out.println("  81-88:  MediaFlange Outputs");
        System.out.println("          81=LEDBlue, 82=SwitchOffX3Voltage");
        System.out.println("          83=OutputX3Pin1, 84=OutputX3Pin2");
        System.out.println("          85=OutputX3Pin11, 86=OutputX3Pin12");
        System.out.println("          87=LedRed, 88=LedGreen");
    }

    /**
     * Comparison of different access methods
     */
    public static void compareAccessMethods(IOList ioList)
    {
        System.out.println("\n=== Access Method Comparison ===\n");

        // Method 1: Named method access
        boolean input1_method = ioList.getEthercat_Input1();

        // Method 2: Array access
        boolean input1_array = ioList.in.get(65);

        // Method 3: Direct IO group access
        boolean input1_direct = ioList.getEthercat().getInput1();

        System.out.println("Three ways to access Ethercat Input 1:");
        System.out.println("  1. ioList.getEthercat_Input1()         = " + input1_method);
        System.out.println("  2. ioList.in.get(65)                   = " + input1_array);
        System.out.println("  3. ioList.getEthercat().getInput1()    = " + input1_direct);
        System.out.println("\nAll methods return the same value!");

        System.out.println("\nThree ways to set Ethercat Output 1:");
        System.out.println("  1. ioList.setEthercat_Output1(true)");
        System.out.println("  2. ioList.out.set(65, true)             <- Simplest!");
        System.out.println("  3. ioList.getEthercat().setOutput1(true)");
    }
}

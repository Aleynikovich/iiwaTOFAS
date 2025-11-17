package hartu.robot.io;

import com.kuka.generated.ioAccess.Ethercat_x44IOGroup;
import com.kuka.generated.ioAccess.IOFlangeIOGroup;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;

/**
 * Example usage of the IOList class showing how to work with virtual marks
 * and physical I/Os.
 * 
 * This is a demonstration file showing common usage patterns.
 */
public class IOListExample {
    
    /**
     * Example: Using virtual marks for workflow state tracking
     */
    public static void demonstrateMarks(IOList ioList) {
        System.out.println("=== Virtual Marks Example ===\n");
        
        // Use marks to track workflow states
        final int MARK_INITIALIZED = 1;
        final int MARK_CALIBRATED = 2;
        final int MARK_HOME_POSITION = 3;
        final int MARK_TOOL_ATTACHED = 4;
        final int MARK_READY_FOR_OPERATION = 5;
        
        // Set marks as workflow progresses
        System.out.println("Starting initialization...");
        ioList.setMark(MARK_INITIALIZED, true);
        System.out.println("Mark " + MARK_INITIALIZED + " (INITIALIZED) set");
        
        System.out.println("Starting calibration...");
        ioList.setMark(MARK_CALIBRATED, true);
        System.out.println("Mark " + MARK_CALIBRATED + " (CALIBRATED) set");
        
        System.out.println("Moving to home position...");
        ioList.setMark(MARK_HOME_POSITION, true);
        System.out.println("Mark " + MARK_HOME_POSITION + " (HOME_POSITION) set");
        
        // Check if prerequisites are met before proceeding
        if (ioList.getMark(MARK_INITIALIZED) && 
            ioList.getMark(MARK_CALIBRATED) && 
            ioList.getMark(MARK_HOME_POSITION)) {
            System.out.println("\nAll prerequisites met for tool attachment!");
            ioList.setMark(MARK_TOOL_ATTACHED, true);
        }
        
        // Final readiness check
        if (ioList.getMark(MARK_TOOL_ATTACHED)) {
            System.out.println("Tool attached, system ready for operation");
            ioList.setMark(MARK_READY_FOR_OPERATION, true);
        }
        
        // Print summary
        System.out.println("\n" + ioList.getCurrentIOStates());
    }
    
    /**
     * Example: Using marks for error tracking and recovery
     */
    public static void demonstrateErrorTracking(IOList ioList) {
        System.out.println("\n=== Error Tracking Example ===\n");
        
        // Define error marks
        final int ERROR_COMMUNICATION = 50;
        final int ERROR_SENSOR = 51;
        final int ERROR_MOTION = 52;
        final int ERROR_RECOVERED = 60;
        
        // Simulate an error condition
        System.out.println("Communication error detected!");
        ioList.setMark(ERROR_COMMUNICATION, true);
        
        // Check for any errors
        if (ioList.getMark(ERROR_COMMUNICATION) || 
            ioList.getMark(ERROR_SENSOR) || 
            ioList.getMark(ERROR_MOTION)) {
            System.out.println("System in error state, attempting recovery...");
            
            // Recovery procedure
            ioList.setMark(ERROR_COMMUNICATION, false);
            ioList.setMark(ERROR_RECOVERED, true);
            System.out.println("Recovery complete");
        }
        
        System.out.println("\nCurrent error marks:");
        System.out.println("  Communication Error: " + ioList.getMark(ERROR_COMMUNICATION));
        System.out.println("  Sensor Error: " + ioList.getMark(ERROR_SENSOR));
        System.out.println("  Motion Error: " + ioList.getMark(ERROR_MOTION));
        System.out.println("  Recovered: " + ioList.getMark(ERROR_RECOVERED));
    }
    
    /**
     * Example: Combining marks with physical I/Os
     */
    public static void demonstrateIntegration(IOList ioList) {
        System.out.println("\n=== Integration Example ===\n");
        
        final int MARK_VACUUM_READY = 10;
        final int MARK_GRIPPER_CLOSED = 11;
        
        // Check physical sensor and set mark
        boolean sensorActive = ioList.getEthercat_Input1();
        System.out.println("Sensor state: " + (sensorActive ? "ACTIVE" : "INACTIVE"));
        
        if (sensorActive) {
            ioList.setMark(MARK_VACUUM_READY, true);
            System.out.println("Vacuum system marked as ready");
            
            // Activate output based on mark
            ioList.setEthercat_Output1(true);
            System.out.println("Output activated");
            
            ioList.setMark(MARK_GRIPPER_CLOSED, true);
            System.out.println("Gripper marked as closed");
        }
    }
    
    /**
     * Example: Reset and clear operations
     */
    public static void demonstrateReset(IOList ioList) {
        System.out.println("\n=== Reset Example ===\n");
        
        // Set some marks
        ioList.setMark(1, true);
        ioList.setMark(5, true);
        ioList.setMark(10, true);
        System.out.println("Set marks 1, 5, and 10");
        
        // Check total available
        System.out.println("Total marks available: " + ioList.getTotalMarks());
        
        // Reset all marks
        System.out.println("Resetting all marks...");
        ioList.resetAllMarks();
        
        // Verify reset
        System.out.println("Mark 1 after reset: " + ioList.getMark(1));
        System.out.println("Mark 5 after reset: " + ioList.getMark(5));
        System.out.println("Mark 10 after reset: " + ioList.getMark(10));
    }
    
    /**
     * Main method for demonstration (would need actual IO groups to run)
     */
    public static void main(String[] args) {
        System.out.println("IOList Virtual Marks Demonstration\n");
        System.out.println("Note: This example requires actual IO group instances to run.");
        System.out.println("In a real application, you would initialize with:");
        System.out.println("  - Ethercat_x44IOGroup");
        System.out.println("  - IOFlangeIOGroup");
        System.out.println("  - MediaFlangeIOGroup");
        System.out.println("\nThen call: IOList ioList = new IOList(ethercat, ioFlange, mediaFlange);");
    }
}

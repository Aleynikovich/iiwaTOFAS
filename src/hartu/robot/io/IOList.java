package hartu.robot.io;

import com.kuka.generated.ioAccess.Ethercat_x44IOGroup;
import com.kuka.generated.ioAccess.IOFlangeIOGroup;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;

/**
 * Centralized list of all available I/Os on the robot.
 * Provides easy access to all physical I/O groups and their individual I/Os,
 * as well as 64 virtual I/O marks for custom use.
 * 
 * This class serves as a single point of reference for all I/O operations,
 * making it easier to work with the robot's I/O system.
 */
public class IOList {
    
    // Virtual I/O marks (software flags) - 64 marks for custom use
    private final boolean[] virtualMarks;
    
    private final Ethercat_x44IOGroup ethercat;
    private final IOFlangeIOGroup ioFlange;
    private final MediaFlangeIOGroup mediaFlange;
    
    /**
     * Creates a new IOList with references to all IO groups.
     * 
     * @param ethercat The Ethercat_x44 IO group
     * @param ioFlange The IOFlange IO group
     * @param mediaFlange The MediaFlange IO group
     */
    public IOList(Ethercat_x44IOGroup ethercat, IOFlangeIOGroup ioFlange, MediaFlangeIOGroup mediaFlange) {
        this.virtualMarks = new boolean[64];
        this.ethercat = ethercat;
        this.ioFlange = ioFlange;
        this.mediaFlange = mediaFlange;
    }
    
    // ========================================
    // VIRTUAL MARKS (1-64)
    // ========================================
    
    /**
     * Gets the state of a virtual mark.
     * Virtual marks are software flags (not physical I/Os) that can be used for custom purposes.
     * 
     * @param markNumber The mark number (1-64)
     * @return The current state of the mark
     * @throws IllegalArgumentException if markNumber is not in range 1-64
     */
    public boolean getMark(int markNumber) {
        if (markNumber < 1 || markNumber > 64) {
            throw new IllegalArgumentException("Mark number must be between 1 and 64, got: " + markNumber);
        }
        return virtualMarks[markNumber - 1];
    }
    
    /**
     * Sets the state of a virtual mark.
     * Virtual marks are software flags (not physical I/Os) that can be used for custom purposes.
     * 
     * @param markNumber The mark number (1-64)
     * @param value The value to set
     * @throws IllegalArgumentException if markNumber is not in range 1-64
     */
    public void setMark(int markNumber, boolean value) {
        if (markNumber < 1 || markNumber > 64) {
            throw new IllegalArgumentException("Mark number must be between 1 and 64, got: " + markNumber);
        }
        virtualMarks[markNumber - 1] = value;
    }
    
    /**
     * Resets all virtual marks to false.
     */
    public void resetAllMarks() {
        for (int i = 0; i < virtualMarks.length; i++) {
            virtualMarks[i] = false;
        }
    }
    
    /**
     * Gets the total number of virtual marks available.
     * 
     * @return 64
     */
    public int getTotalMarks() {
        return virtualMarks.length;
    }
    
    // ========================================
    // ETHERCAT_X44 I/O GROUP
    // ========================================
    
    /**
     * @return The Ethercat_x44 IO group
     */
    public Ethercat_x44IOGroup getEthercat() {
        return ethercat;
    }
    
    // Ethercat Inputs
    public boolean getEthercat_Input1() { return ethercat.getInput1(); }
    public boolean getEthercat_Input2() { return ethercat.getInput2(); }
    public boolean getEthercat_Input3() { return ethercat.getInput3(); }
    public boolean getEthercat_Input4() { return ethercat.getInput4(); }
    public boolean getEthercat_Input5() { return ethercat.getInput5(); }
    public boolean getEthercat_Input6() { return ethercat.getInput6(); }
    public boolean getEthercat_Input7() { return ethercat.getInput7(); }
    public boolean getEthercat_Input8() { return ethercat.getInput8(); }
    
    // Ethercat Outputs
    public boolean getEthercat_Output1() { return ethercat.getOutput1(); }
    public void setEthercat_Output1(boolean value) { ethercat.setOutput1(value); }
    
    public boolean getEthercat_Output2() { return ethercat.getOutput2(); }
    public void setEthercat_Output2(boolean value) { ethercat.setOutput2(value); }
    
    public boolean getEthercat_Output3() { return ethercat.getOutput3(); }
    public void setEthercat_Output3(boolean value) { ethercat.setOutput3(value); }
    
    public boolean getEthercat_Output4() { return ethercat.getOutput4(); }
    public void setEthercat_Output4(boolean value) { ethercat.setOutput4(value); }
    
    public boolean getEthercat_Output5() { return ethercat.getOutput5(); }
    public void setEthercat_Output5(boolean value) { ethercat.setOutput5(value); }
    
    public boolean getEthercat_Output6() { return ethercat.getOutput6(); }
    public void setEthercat_Output6(boolean value) { ethercat.setOutput6(value); }
    
    public boolean getEthercat_Output7() { return ethercat.getOutput7(); }
    public void setEthercat_Output7(boolean value) { ethercat.setOutput7(value); }
    
    public boolean getEthercat_Output8() { return ethercat.getOutput8(); }
    public void setEthercat_Output8(boolean value) { ethercat.setOutput8(value); }
    
    // ========================================
    // IO FLANGE I/O GROUP
    // ========================================
    
    /**
     * @return The IOFlange IO group
     */
    public IOFlangeIOGroup getIOFlange() {
        return ioFlange;
    }
    
    // IOFlange Inputs
    public boolean getIOFlange_DI_Flange1() { return ioFlange.getDI_Flange1(); }
    public boolean getIOFlange_DI_Flange2() { return ioFlange.getDI_Flange2(); }
    public boolean getIOFlange_DI_Flange3() { return ioFlange.getDI_Flange3(); }
    public boolean getIOFlange_DI_Flange4() { return ioFlange.getDI_Flange4(); }
    public boolean getIOFlange_DI_Flange5() { return ioFlange.getDI_Flange5(); }
    public boolean getIOFlange_DI_Flange6() { return ioFlange.getDI_Flange6(); }
    public boolean getIOFlange_DI_Flange7() { return ioFlange.getDI_Flange7(); }
    public boolean getIOFlange_DI_Flange8() { return ioFlange.getDI_Flange8(); }
    
    // IOFlange Outputs
    public boolean getIOFlange_DO_Flange1() { return ioFlange.getDO_Flange1(); }
    public void setIOFlange_DO_Flange1(boolean value) { ioFlange.setDO_Flange1(value); }
    
    public boolean getIOFlange_DO_Flange2() { return ioFlange.getDO_Flange2(); }
    public void setIOFlange_DO_Flange2(boolean value) { ioFlange.setDO_Flange2(value); }
    
    public boolean getIOFlange_DO_Flange3() { return ioFlange.getDO_Flange3(); }
    public void setIOFlange_DO_Flange3(boolean value) { ioFlange.setDO_Flange3(value); }
    
    public boolean getIOFlange_DO_Flange4() { return ioFlange.getDO_Flange4(); }
    public void setIOFlange_DO_Flange4(boolean value) { ioFlange.setDO_Flange4(value); }
    
    public boolean getIOFlange_DO_Flange5() { return ioFlange.getDO_Flange5(); }
    public void setIOFlange_DO_Flange5(boolean value) { ioFlange.setDO_Flange5(value); }
    
    public boolean getIOFlange_DO_Flange6() { return ioFlange.getDO_Flange6(); }
    public void setIOFlange_DO_Flange6(boolean value) { ioFlange.setDO_Flange6(value); }
    
    public boolean getIOFlange_DO_Flange7() { return ioFlange.getDO_Flange7(); }
    public void setIOFlange_DO_Flange7(boolean value) { ioFlange.setDO_Flange7(value); }
    
    public boolean getIOFlange_DO_Flange8() { return ioFlange.getDO_Flange8(); }
    public void setIOFlange_DO_Flange8(boolean value) { ioFlange.setDO_Flange8(value); }
    
    // ========================================
    // MEDIA FLANGE I/O GROUP
    // ========================================
    
    /**
     * @return The MediaFlange IO group
     */
    public MediaFlangeIOGroup getMediaFlange() {
        return mediaFlange;
    }
    
    // MediaFlange Inputs
    public boolean getMediaFlange_InputX3Pin3() { return mediaFlange.getInputX3Pin3(); }
    public boolean getMediaFlange_InputX3Pin4() { return mediaFlange.getInputX3Pin4(); }
    public boolean getMediaFlange_InputX3Pin10() { return mediaFlange.getInputX3Pin10(); }
    public boolean getMediaFlange_InputX3Pin13() { return mediaFlange.getInputX3Pin13(); }
    public boolean getMediaFlange_InputX3Pin16() { return mediaFlange.getInputX3Pin16(); }
    public boolean getMediaFlange_UserButton() { return mediaFlange.getUserButton(); }
    
    // MediaFlange Outputs
    public boolean getMediaFlange_LEDBlue() { return mediaFlange.getLEDBlue(); }
    public void setMediaFlange_LEDBlue(boolean value) { mediaFlange.setLEDBlue(value); }
    
    public boolean getMediaFlange_SwitchOffX3Voltage() { return mediaFlange.getSwitchOffX3Voltage(); }
    public void setMediaFlange_SwitchOffX3Voltage(boolean value) { mediaFlange.setSwitchOffX3Voltage(value); }
    
    public boolean getMediaFlange_OutputX3Pin1() { return mediaFlange.getOutputX3Pin1(); }
    public void setMediaFlange_OutputX3Pin1(boolean value) { mediaFlange.setOutputX3Pin1(value); }
    
    public boolean getMediaFlange_OutputX3Pin2() { return mediaFlange.getOutputX3Pin2(); }
    public void setMediaFlange_OutputX3Pin2(boolean value) { mediaFlange.setOutputX3Pin2(value); }
    
    public boolean getMediaFlange_OutputX3Pin11() { return mediaFlange.getOutputX3Pin11(); }
    public void setMediaFlange_OutputX3Pin11(boolean value) { mediaFlange.setOutputX3Pin11(value); }
    
    public boolean getMediaFlange_OutputX3Pin12() { return mediaFlange.getOutputX3Pin12(); }
    public void setMediaFlange_OutputX3Pin12(boolean value) { mediaFlange.setOutputX3Pin12(value); }
    
    public boolean getMediaFlange_LedRed() { return mediaFlange.getLedRed(); }
    public void setMediaFlange_LedRed(boolean value) { mediaFlange.setLedRed(value); }
    
    public boolean getMediaFlange_LedGreen() { return mediaFlange.getLedGreen(); }
    public void setMediaFlange_LedGreen(boolean value) { mediaFlange.setLedGreen(value); }
    
    // ========================================
    // CONVENIENCE METHODS
    // ========================================
    
    /**
     * Gets a summary of all available I/Os in the system.
     * 
     * @return A string describing all available I/Os
     */
    public String getIOSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("=== Available I/Os ===\n\n");
        
        summary.append("VIRTUAL MARKS (SOFTWARE FLAGS):\n");
        summary.append("  Marks: Mark1-Mark64 (64 virtual software flags for custom use)\n");
        summary.append("  Note: These are NOT physical I/Os, but software flags you can use for any purpose\n\n");
        
        summary.append("ETHERCAT_X44 I/O GROUP:\n");
        summary.append("  Inputs: Input1-Input8 (8 digital inputs)\n");
        summary.append("  Outputs: Output1-Output8 (8 digital outputs)\n\n");
        
        summary.append("IO FLANGE I/O GROUP:\n");
        summary.append("  Inputs: DI_Flange1-DI_Flange8 (8 digital inputs)\n");
        summary.append("  Outputs: DO_Flange1-DO_Flange8 (8 digital outputs)\n\n");
        
        summary.append("MEDIA FLANGE I/O GROUP:\n");
        summary.append("  Inputs: InputX3Pin3, InputX3Pin4, InputX3Pin10, InputX3Pin13, InputX3Pin16, UserButton (6 digital inputs)\n");
        summary.append("  Outputs: LEDBlue, SwitchOffX3Voltage, OutputX3Pin1, OutputX3Pin2, OutputX3Pin11, OutputX3Pin12, LedRed, LedGreen (8 digital outputs)\n");
        
        return summary.toString();
    }
    
    /**
     * Prints the current state of all I/Os.
     * 
     * @return A string showing the current state of all I/Os
     */
    public String getCurrentIOStates() {
        StringBuilder states = new StringBuilder();
        states.append("=== Current I/O States ===\n\n");
        
        // Virtual Marks
        states.append("VIRTUAL MARKS:\n");
        states.append("  ");
        int activeMarks = 0;
        for (int i = 1; i <= 64; i++) {
            if (virtualMarks[i - 1]) {
                if (activeMarks > 0) states.append(", ");
                states.append("Mark").append(i);
                activeMarks++;
                if (activeMarks % 8 == 0 && i < 64) states.append("\n  ");
            }
        }
        if (activeMarks == 0) {
            states.append("(All marks are OFF)");
        }
        states.append("\n\n");
        
        states.append("ETHERCAT_X44:\n");
        states.append("  Inputs: ");
        for (int i = 1; i <= 8; i++) {
            boolean value = false;
            switch(i) {
                case 1: value = ethercat.getInput1(); break;
                case 2: value = ethercat.getInput2(); break;
                case 3: value = ethercat.getInput3(); break;
                case 4: value = ethercat.getInput4(); break;
                case 5: value = ethercat.getInput5(); break;
                case 6: value = ethercat.getInput6(); break;
                case 7: value = ethercat.getInput7(); break;
                case 8: value = ethercat.getInput8(); break;
            }
            states.append("Input").append(i).append("=").append(value ? "HIGH" : "LOW").append(" ");
        }
        states.append("\n  Outputs: ");
        for (int i = 1; i <= 8; i++) {
            boolean value = false;
            switch(i) {
                case 1: value = ethercat.getOutput1(); break;
                case 2: value = ethercat.getOutput2(); break;
                case 3: value = ethercat.getOutput3(); break;
                case 4: value = ethercat.getOutput4(); break;
                case 5: value = ethercat.getOutput5(); break;
                case 6: value = ethercat.getOutput6(); break;
                case 7: value = ethercat.getOutput7(); break;
                case 8: value = ethercat.getOutput8(); break;
            }
            states.append("Output").append(i).append("=").append(value ? "HIGH" : "LOW").append(" ");
        }
        
        states.append("\n\nIO FLANGE:\n");
        states.append("  Inputs: ");
        for (int i = 1; i <= 8; i++) {
            boolean value = false;
            switch(i) {
                case 1: value = ioFlange.getDI_Flange1(); break;
                case 2: value = ioFlange.getDI_Flange2(); break;
                case 3: value = ioFlange.getDI_Flange3(); break;
                case 4: value = ioFlange.getDI_Flange4(); break;
                case 5: value = ioFlange.getDI_Flange5(); break;
                case 6: value = ioFlange.getDI_Flange6(); break;
                case 7: value = ioFlange.getDI_Flange7(); break;
                case 8: value = ioFlange.getDI_Flange8(); break;
            }
            states.append("DI_Flange").append(i).append("=").append(value ? "HIGH" : "LOW").append(" ");
        }
        states.append("\n  Outputs: ");
        for (int i = 1; i <= 8; i++) {
            boolean value = false;
            switch(i) {
                case 1: value = ioFlange.getDO_Flange1(); break;
                case 2: value = ioFlange.getDO_Flange2(); break;
                case 3: value = ioFlange.getDO_Flange3(); break;
                case 4: value = ioFlange.getDO_Flange4(); break;
                case 5: value = ioFlange.getDO_Flange5(); break;
                case 6: value = ioFlange.getDO_Flange6(); break;
                case 7: value = ioFlange.getDO_Flange7(); break;
                case 8: value = ioFlange.getDO_Flange8(); break;
            }
            states.append("DO_Flange").append(i).append("=").append(value ? "HIGH" : "LOW").append(" ");
        }
        
        states.append("\n\nMEDIA FLANGE:\n");
        states.append("  Inputs: ");
        states.append("InputX3Pin3=").append(mediaFlange.getInputX3Pin3() ? "HIGH" : "LOW").append(" ");
        states.append("InputX3Pin4=").append(mediaFlange.getInputX3Pin4() ? "HIGH" : "LOW").append(" ");
        states.append("InputX3Pin10=").append(mediaFlange.getInputX3Pin10() ? "HIGH" : "LOW").append(" ");
        states.append("InputX3Pin13=").append(mediaFlange.getInputX3Pin13() ? "HIGH" : "LOW").append(" ");
        states.append("InputX3Pin16=").append(mediaFlange.getInputX3Pin16() ? "HIGH" : "LOW").append(" ");
        states.append("UserButton=").append(mediaFlange.getUserButton() ? "PRESSED" : "RELEASED").append(" ");
        states.append("\n  Outputs: ");
        states.append("LEDBlue=").append(mediaFlange.getLEDBlue() ? "ON" : "OFF").append(" ");
        states.append("SwitchOffX3Voltage=").append(mediaFlange.getSwitchOffX3Voltage() ? "HIGH" : "LOW").append(" ");
        states.append("OutputX3Pin1=").append(mediaFlange.getOutputX3Pin1() ? "HIGH" : "LOW").append(" ");
        states.append("OutputX3Pin2=").append(mediaFlange.getOutputX3Pin2() ? "HIGH" : "LOW").append(" ");
        states.append("OutputX3Pin11=").append(mediaFlange.getOutputX3Pin11() ? "HIGH" : "LOW").append(" ");
        states.append("OutputX3Pin12=").append(mediaFlange.getOutputX3Pin12() ? "HIGH" : "LOW").append(" ");
        states.append("LedRed=").append(mediaFlange.getLedRed() ? "ON" : "OFF").append(" ");
        states.append("LedGreen=").append(mediaFlange.getLedGreen() ? "ON" : "OFF").append(" ");
        
        return states.toString();
    }
}

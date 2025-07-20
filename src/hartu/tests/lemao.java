//// File: MessageHandler.java
//package hartuTofas;
//
//import com.kuka.generated.ioAccess.Ethercat_x44IOGroup;
//import com.kuka.generated.ioAccess.IOFlangeIOGroup;
//import com.kuka.roboticsAPI.deviceModel.LBR;
//import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
//
//import com.kuka.roboticsAPI.geometricModel.Frame;
//import com.kuka.roboticsAPI.geometricModel.Tool;
//import com.kuka.roboticsAPI.geometricModel.World;
//import com.kuka.roboticsAPI.geometricModel.math.Transformation;
//import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication; // --- NEW IMPORT ---
//import java.util.Arrays;
//import java.util.List;
//import java.lang.Math;
//
//import javax.inject.Inject;
//
//public class MessageHandler {
//
//    private LBR robot;
//    private IOFlangeIOGroup gimatic;
//    private Ethercat_x44IOGroup IOs;
//
//    // --- NEW FIELD ---
//    // This field will store the RoboticsAPIApplication instance passed from AAHartuTCPIPServer.
//    private RoboticsAPIApplication application;
//
//    private Tool flexTool;
//
//
//    // Action types
//    public static final int PTP_AXIS = 0;
//    public static final int PTP_FRAME = 1;
//    public static final int LIN_AXIS = 2;
//    public static final int LIN_FRAME = 3;
//    public static final int CIRC_AXIS = 4;
//    public static final int CIRC_FRAME = 5;
//    public static final int PTP_AXIS_C = 6;
//    public static final int PTP_FRAME_C = 7;
//    public static final int LIN_FRAME_C = 8;
//    public static final int ACTIVATE_IO = 9;
//    public static final int LIN_REL_TOOL = 10;
//    public static final int LIN_REL_BASE = 11;
//
//    // 100+ = program call with ID actiontype-100
//
//    // --- MODIFIED CONSTRUCTOR ---
//    // It now accepts RoboticsAPIApplication as the fourth parameter and stores it.
//    public MessageHandler(LBR robot, IOFlangeIOGroup gimatic, Ethercat_x44IOGroup IOs, RoboticsAPIApplication application) {
//        this.robot = robot;
//        this.gimatic = gimatic;
//        this.IOs = IOs;
//        this.application = application; // Store the application instance
//    }
//
//    public class Command {
//        public int actionType;
//        public int numPoints;
//        public int ioPoint;
//        public int ioPin;
//        public boolean ioState;
//        public String tool;
//        public String base;
//        public double speedOverride;
//
//        public boolean programCall;
//        public String targetPoints;
//        public String id;
//
//        private String get(String[] parts, int index, String defaultValue) {
//            return (index < parts.length && parts[index] != null && !parts[index]
//                    .trim().isEmpty()) ? parts[index].trim() : defaultValue;
//        }
//
//        public Command(String[] parts) {
//            System.out.println("Generating command");
//
//            try {
//                int rawActionType = Integer.parseInt(get(parts, 0, "999"));
//                this.programCall = rawActionType > 100;
//                this.actionType = this.programCall ? rawActionType - 100
//                        : rawActionType;
//                this.numPoints = Integer.parseInt(get(parts, 1, "0"));
//                this.targetPoints = get(parts, 2, "");
//                this.ioPoint = Integer.parseInt(get(parts, 3, "0"));
//                this.ioPin = Integer.parseInt(get(parts, 4, "0"));
//                this.ioState = Boolean.parseBoolean(get(parts, 5, "false"));
//                this.tool = get(parts, 6, "0");
//                this.base = get(parts, 7, "0");
//                this.speedOverride = Double.parseDouble(get(parts, 8, "100.0")) / 100.0;
//                this.id = get(parts, 9, "N/A");
//
//            } catch (NumberFormatException e) {
//                throw new IllegalArgumentException(
//                        "Invalid number format in input: " + e.getMessage());
//            }
//
//            printCommand();
//        }
//
//        public void printCommand() {
//            System.out.println("Command{" + "actionType=" + actionType
//                                       + ", programCall=" + programCall + ", numPoints="
//                                       + numPoints + ", ioPoint=" + ioPoint + ", ioPin=" + ioPin
//                                       + ", ioState=" + ioState + ", tool=" + tool + ", base="
//                                       + base + ", speedOverride=" + speedOverride
//                                       + ", targetPoints='" + targetPoints + '\'' + ", id='" + id
//                                       + '\'' + '}');
//        }
//    }
//
//    public String handleMessage(String message) {
//        if (!message.endsWith("#")) {
//            System.out.println("Message does not end with '#'");
//            return "Invalid message format";
//        }
//
//        message = message.substring(0, message.length() - 1);
//        String[] parts = message.split("\\|");
//
//        System.out.println("Parsed parts: " + Arrays.toString(parts));
//        try {
//            Command cmd = new Command(parts);
//            cmd.printCommand();
//
//            // --- CRITICAL CHANGE: UNCOMMENT AND MODIFY THIS LINE ---
//            // 1. Map the tool ID from the message to the WorkVisual tool name.
//            String toolNameToLoad = mapToolIdToWorkVisualName(cmd.tool);
//            if (toolNameToLoad == null) {
//                System.err.println("Unknown tool ID received: " + cmd.tool + ". Cannot load tool.");
//                //return "Error: Unknown tool ID '" + cmd.tool + "'.";
//            }
//
//            else {
//                // 2. Load the tool using createFromTemplate and the 'application' instance.
//                flexTool = application.createFromTemplate(toolNameToLoad);
//                // This line will now work because flexTool is initialized
//                flexTool.attachTo(robot.getFlange());
//            }
//
//            if (!cmd.programCall) {
//                System.out.println("Entered Movetype");
//                switch (cmd.actionType) {
//                    case PTP_AXIS:
//                    case PTP_AXIS_C:
//                        System.out.println("Entered PTP AXIS");
//                        return handlePTPAxis(cmd);
//                    case PTP_FRAME:
//                    case PTP_FRAME_C:
//                        return handlePTPFrame(cmd);
//                    case LIN_FRAME:
//                    case LIN_FRAME_C:
//                        return handleLINFrame(cmd);
//                    case LIN_REL_TOOL:
//                    case LIN_REL_BASE:
//                        // --- MODIFIED: Ensure LIN_REL_TOOL and LIN_REL_BASE are handled by handleLINREL ---
//                        return handleLINREL(cmd); // Added this line to call the handler
//                    // --- END MODIFIED ---
//                    case ACTIVATE_IO:
//                        System.out.println("Entered Activate IO");
//                        return handleActivateIO(cmd);
//                    default:
//                        System.out.println("Unknown move type: " + cmd.actionType);
//                        return "Unknown move type: " + cmd.actionType;
//                }
//            } else {
//                System.out.println("Entered Programcall");
//                switch (cmd.actionType) {
//
//                    case 1:
//                        System.out.println("Called demo");
//                        Demo();
//                        return "Program demo called";
//                    case 2:
//                        return "Program 2 called";
//                    default:
//                        return "Program " + cmd.actionType + " not found.";
//                }
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace(); // Print stack trace for debugging
//            return "Failed to parse command: " + e.getMessage();		}
//    }
//
//    // --- NEW HELPER METHOD ---
//    // This maps your incoming tool IDs to the exact WorkVisual tool template names.
//    // You MUST ensure these names are EXACTLY as they appear in WorkVisual (case-sensitive)
//    // based on the image you provided previously.
//    private String mapToolIdToWorkVisualName(String toolId) {
//        switch (toolId) {
//            case "0": return "Tool";            // Generic "Tool" in your image
//            case "1": return "RollScan";        // "RollScan" from your image
//            case "2": return "BinPick_Tool";    // "BinPick_Tool" from your image
//            case "3": return "GimaticCamera";   // "GimaticCamera" from your image
//            case "4": return "GimaticGripperV"; // "GimaticGripperV" from your image
//            case "5": return "GimaticIxtur";    // "Gimaticlxtur" from your image
//            case "6": return "IxturPlatoGrande"; // "lxturPlatoGrande" from your image
//            case "7": return "RealSense";       // "RealSense" from your image
//            case "8": return "Roldana";         // "Roldana" from your image
//            case "9": return "ToolTemplate";    // "ToolTemplate" from your image
//            case "10": return "TrackerTool";    // "TrackerTool" from your image
//            default:
//                System.err.println("Tool ID '" + toolId + "' not recognized in mapToolIdToWorkVisualName.");
//                return null; // Return null if the ID does not map to a known tool name
//        }
//    }
//
//    // --- Movement Handler Methods ---
//
//    private String handlePTPAxis(Command cmd) {
//        List<String> jointPositionGroups = Arrays.asList(cmd.targetPoints
//                                                                 .split(","));
//
//        if (jointPositionGroups.size() != cmd.numPoints) {
//            System.out.println("Invalid number of point groups for ID: "
//                                       + cmd.id);
//            return "Invalid number of point groups";
//        }
//
//        try {
//            for (String jointPositions : jointPositionGroups) {
//                List<String> jointValuesStr = Arrays.asList(jointPositions
//                                                                    .split(";"));
//
//                if (jointValuesStr.size() != 7) {
//                    System.out
//                            .println("Invalid number of joint positions in a group for ID: "
//                                             + cmd.id);
//                    return "Invalid number of joint positions";
//                }
//
//                double[] jointValues = new double[7];
//                for (int i = 0; i < jointValuesStr.size(); i++) {
//                    double jointValueDeg = Double.parseDouble(jointValuesStr
//                                                                      .get(i));
//                    double jointValueRad = Math.toRadians(jointValueDeg); // Convert
//                    // degrees
//                    // to
//                    // radians
//
//                    if (!isWithinLimits(i, jointValueRad)) {
//                        System.out.println("Joint " + (i + 1)
//                                                   + " value out of limits: " + jointValueRad
//                                                   + " for ID: " + cmd.id);
//                        return "Joint " + (i + 1) + " value out of limits";
//                    }
//
//                    jointValues[i] = jointValueRad;
//                }
//
//                if (cmd.actionType == PTP_AXIS) {
//                    // Execute synchronous PTP motion
//                    robot.move(ptp(jointValues).setJointVelocityRel(
//                            cmd.speedOverride));
//                } else if (cmd.actionType == PTP_AXIS_C) {
//                    // Execute asynchronous PTP motion for each point
//                    robot.moveAsync(ptp(jointValues).setJointVelocityRel(
//                            cmd.speedOverride).setBlendingRel(0.5));
//                }
//            }
//        } catch (NumberFormatException e) {
//            System.out.println("Invalid joint position values for ID: "
//                                       + cmd.id);
//            return "Invalid joint position values";
//        }
//
//        if (cmd.actionType == PTP_AXIS) {
//            System.out.println("PTP_AXIS command executed for ID: " + cmd.id);
//            return "PTP_AXIS command executed";
//        } else {
//            System.out.println("PTP_AXIS_C command executed for ID: " + cmd.id);
//            return "PTP_AXIS_C command executed";
//        }
//    }
//
//    private String handlePTPFrame(Command cmd) {
//        // Process the PTP_FRAME command
//        List<String> points = Arrays.asList(cmd.targetPoints.split(","));
//        try {
//            for (int i = 0; i < points.size(); i++) {
//                String point = points.get(i);
//                List<String> coordinates = Arrays.asList(point.split(";"));
//                double x = Double.parseDouble(coordinates.get(0));
//                double y = Double.parseDouble(coordinates.get(1));
//                double z = Double.parseDouble(coordinates.get(2));
//                double roll = Math.toRadians(Double.parseDouble(coordinates
//                                                                        .get(3))); // Convert degrees to radians
//                double pitch = Math.toRadians(Double.parseDouble(coordinates
//                                                                         .get(4)));
//                double yaw = Math.toRadians(Double.parseDouble(coordinates
//                                                                       .get(5)));
//                Frame targetFrameVirgin = new Frame(
//                        World.Current.getRootFrame(), x, y, z, roll, pitch, yaw);
//                if (cmd.actionType == PTP_FRAME) {
//                    robot.move(ptp(targetFrameVirgin).setJointVelocityRel(
//                            cmd.speedOverride));
//                } else if (cmd.actionType == PTP_FRAME_C) {
//                    // Execute asynchronous PTP motion for each point
//                    robot.moveAsync(ptp(targetFrameVirgin).setBlendingRel(0.5)
//                                            .setJointVelocityRel(cmd.speedOverride));
//                }
//            }
//        } catch (NumberFormatException e) {
//            System.out.println("Invalid coordinate values for ID: " + cmd.id);
//            return "Invalid coordinate values";
//        } catch (IndexOutOfBoundsException e) {
//            System.out.println("Coordinate values are incomplete for ID: "
//                                       + cmd.id);
//            return "Coordinate values are incomplete";
//        }
//        System.out.println("PTP_FRAME command executed for ID: " + cmd.id);
//        return "PTP_FRAME command executed";
//    }
//
//    private String handleLINFrame(Command cmd) {
//        // Process the LIN_FRAME commands
//        List<String> points = Arrays.asList(cmd.targetPoints.split(","));
//
//        // --- MODIFIED: Add a null check for flexTool ---
//        if (flexTool == null) {
//            System.err.println("Error: flexTool is null for LIN_FRAME command. Tool was not loaded correctly. ID: " + cmd.id);
//            return "Error: Tool not initialized for LIN_FRAME.";
//        }
//        // --- END MODIFIED ---
//
//        try {
//            for (int i = 0; i < points.size(); i++) {
//                String point = points.get(i);
//                List<String> coordinates = Arrays.asList(point.split(";"));
//                double x = Double.parseDouble(coordinates.get(0));
//                double y = Double.parseDouble(coordinates.get(1));
//                double z = Double.parseDouble(coordinates.get(2));
//                double roll = Math.toRadians(Double.parseDouble(coordinates
//                                                                        .get(3))); // Convert degrees to radians
//                double pitch = Math.toRadians(Double.parseDouble(coordinates
//                                                                         .get(4)));
//                double yaw = Math.toRadians(Double.parseDouble(coordinates
//                                                                       .get(5)));
//                Frame targetFrameVirgin = new Frame(
//                        World.Current.getRootFrame(), x, y, z, roll, pitch, yaw);
//                if (cmd.actionType == LIN_FRAME) {
//                    // --- MODIFIED: Use flexTool for the move ---
//                    flexTool.move(lin(targetFrameVirgin).setCartVelocity(
//                            cmd.speedOverride * 200)); // Assuming max 200mm/s
//                } else if (cmd.actionType == LIN_FRAME_C) {
//                    // --- MODIFIED: Use flexTool for the move ---
//                    flexTool.moveAsync(lin(targetFrameVirgin).setCartVelocity(
//                            cmd.speedOverride * 200).setBlendingRel(0.5));
//                }
//
//            }
//        } catch (NumberFormatException e) {
//            System.out.println("Invalid coordinate values for ID: " + cmd.id);
//            return "Invalid coordinate values";
//        } catch (IndexOutOfBoundsException e) {
//            System.out.println("Coordinate values are incomplete for ID: "
//                                       + cmd.id);
//            return "Coordinate values are incomplete";
//        }
//        System.out.println("LIN_FRAME command executed for ID: " + cmd.id);
//        return "LIN_FRAME command executed";
//    }
//
//    // --- MODIFIED handleLINREL method ---
//    private String handleLINREL(Command cmd) {
//        List<String> offsetData = Arrays.asList(cmd.targetPoints.split(";"));
//        try {
//            if (offsetData.size() != 6) {
//                System.out.println("Incomplete offset values for LIN_REL command for ID: " + cmd.id + ". Expected 6, got " + offsetData.size());
//                return "Incomplete offset values for LIN_REL";
//            }
//            double xOffset = Double.parseDouble(offsetData.get(0));
//            double yOffset = Double.parseDouble(offsetData.get(1));
//            double zOffset = Double.parseDouble(offsetData.get(2));
//            double aOffset = Math.toRadians(Double.parseDouble(offsetData.get(3)));
//            double bOffset = Math.toRadians(Double.parseDouble(offsetData.get(4)));
//            double cOffset = Math.toRadians(Double.parseDouble(offsetData.get(5)));
//
//            Transformation offsetTransformation = Transformation.ofRad(xOffset, yOffset, zOffset, aOffset, bOffset, cOffset);
//
//            if (cmd.actionType == LIN_REL_TOOL) {
//                // --- MODIFIED: Add null check and use flexTool ---
//                if (flexTool == null) {
//                    System.err.println("Error: flexTool is null for LIN_REL_TOOL command. Tool was not loaded correctly. ID: " + cmd.id);
//                    return "Error: Tool not initialized for LIN_REL_TOOL.";
//                }
//                flexTool.move(linRel(offsetTransformation, flexTool.getFrame(flexTool.getDefaultMotionFrame().getName()))
//                                      .setCartVelocity(cmd.speedOverride * 200));
//            } else if (cmd.actionType == LIN_REL_BASE) {
//                // --- MODIFIED: Use robot for base-relative move ---
//                robot.move(linRel(offsetTransformation)
//                                   .setCartVelocity(cmd.speedOverride * 200));
//            }
//        } catch (NumberFormatException e) {
//            System.out.println("Invalid offset values for LIN_REL command for ID: " + cmd.id + ". Error: " + e.getMessage());
//            return "Invalid offset values for LIN_REL";
//        } catch (IndexOutOfBoundsException e) {
//            System.out.println("Incomplete offset values for LIN_REL command for ID: " + cmd.id + ". Error: " + e.getMessage());
//            return "Incomplete offset values for LIN_REL";
//        }
//        System.out.println("LIN_REL command executed for ID: " + cmd.id);
//        return "LIN_REL command executed for ID: " + cmd.id;
//    }
//
//    // --- IO Handling Method (UNMODIFIED from your last provided code) ---
//    private String handleActivateIO(Command cmd) {
//        System.out.println(cmd.ioPin);
//        switch (cmd.ioPin) {
//            case 1:
//                gimatic.setDO_Flange7(cmd.ioState);
//                break;
//            case 2:
//                IOs.setOutput2(cmd.ioState);
//                break;
//            case 3:
//                IOs.setOutput1(cmd.ioState);
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                break;
//            default:
//                System.out.println("Invalid IO pin: " + cmd.ioPin);
//        }
//        return "ACTIVATE_IO command executed for ID: " + cmd.id;
//    }
//
//    // --- Joint Limit Check Helper (UNMODIFIED from your last provided code) ---
//    private boolean isWithinLimits(int jointIndex, double jointValue) {
//        switch (jointIndex) {
//            case 0:
//                return jointValue >= Math.toRadians(-170)
//                        && jointValue <= Math.toRadians(170);
//            case 1:
//                return jointValue >= Math.toRadians(-120)
//                        && jointValue <= Math.toRadians(120);
//            case 2:
//                return jointValue >= Math.toRadians(-170)
//                        && jointValue <= Math.toRadians(170);
//            case 3:
//                return jointValue >= Math.toRadians(-120)
//                        && jointValue <= Math.toRadians(120);
//            case 4:
//                return jointValue >= Math.toRadians(-170)
//                        && jointValue <= Math.toRadians(170);
//            case 5:
//                return jointValue >= Math.toRadians(-120)
//                        && jointValue <= Math.toRadians(120);
//            case 6:
//                return jointValue >= Math.toRadians(-175)
//                        && jointValue <= Math.toRadians(175);
//            default:
//                return false;
//        }
//    }
//
//    private String Demo(){
//        flexTool = application.createFromTemplate("GimaticIxtur");
//        flexTool.attachTo(robot.getFlange());
//
//        flexTool.move(linRel(0, 0, -100));
//        flexTool.move(ptp(application.getApplicationData().getFrame("/Demo/AfterPick")));
//        flexTool.move(ptp(application.getApplicationData().getFrame("/Demo/PrePlace")));
//        flexTool.move(lin(application.getApplicationData().getFrame("/Demo/PlacePos")));
//
//        //PLACE///////////////////////// SHIT CODE WARNING
//        try {
//            Thread.sleep(500);
//        } catch (InterruptedException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        //Place
//        IOs.setOutput2(true);
//        try {
//            Thread.sleep(500);
//        } catch (InterruptedException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        IOs.setOutput2(false);
//        ////////////////////////////////////
//
//        flexTool.move(linRel(0, 0, -30));
//        flexTool.move(lin(application.getApplicationData().getFrame("/Demo/PrePlace")));
//        flexTool.move(lin(application.getApplicationData().getFrame("/Demo/ToolChangePos")));
//
//        while(!gimatic.getDO_Flange8()){
//            System.out.println("Activate GIMATIC DO 8, tool will fall after 5 seconds");
//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//
//        while(gimatic.getDO_Flange8()){
//            System.out.println("Deactivate GIMATIC DO 8, tool will grip after 5 seconds");
//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//
//        flexTool = application.createFromTemplate("GimaticGripperV");
//        flexTool.attachTo(robot.getFlange());
//
//        flexTool.move(ptp(application.getApplicationData().getFrame("/Demo/PrePick1")));
//        flexTool.move(ptp(application.getApplicationData().getFrame("/Demo/PrePick2")));
//        flexTool.move(lin(application.getApplicationData().getFrame("/Demo/PickGripp")));
//
//        //PICK///////////////////////// SHIT CODE WARNING
//        try {
//            Thread.sleep(500);
//        } catch (InterruptedException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        //Pick
//        IOs.setOutput1(true);
//        try {
//            Thread.sleep(500);
//        } catch (InterruptedException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        IOs.setOutput1(false);
//        ////////////////////////////////////
//        flexTool.move(lin(application.getApplicationData().getFrame("/Demo/AfterGripp")));
//        flexTool.move(lin(application.getApplicationData().getFrame("/Demo/AfterGripp2")));
//
//
//
//        return "Demo executed";
//    }
//}

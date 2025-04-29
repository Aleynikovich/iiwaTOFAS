package hartuTofas;

import static com.kuka.roboticsAPI.motionModel.BasicMotions.lin;

import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.motionModel.BasicMotions;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.World;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;

import java.util.Arrays;
import java.util.List;

public class MessageHandler {

    private LBR robot;

    // Action types
    public static final int PTP_AXIS = 0;
    public static final int PTP_FRAME = 1;
    public static final int LIN_AXIS = 2;
    public static final int LIN_FRAME = 3;
    public static final int CIRC_AXIS = 4;
    public static final int CIRC_FRAME = 5;
    public static final int PTP_AXIS_C = 6;
    public static final int PTP_FRAME_C = 7;
    public static final int LIN_FRAME_C = 8;
    public static final int ACTIVATE_IO = 9;
    //100+ = program call with ID actiontype-100

    public MessageHandler(LBR robot) {
        this.robot = robot;
    }
    
    public class Command {
        public int actionType;
        public int numPoints;
        public int ioPoint;
        public int ioPin;
        public int ioState;
        public int tool;
        public int base;
        public int speedOverride;

        public boolean programCall;
        public String targetPoints;
        public String id;

        public Command(String[] parts) {
            if (parts.length < 10) {
                throw new IllegalArgumentException("Invalid number of parameters. Expected at least 10, got " + parts.length);
            }

            try {
                int rawActionType = Integer.parseInt(parts[0]);

                if (rawActionType > 100) {
                    this.programCall = true;
                    this.actionType = rawActionType - 100; // normalize to actual program ID
                } else {
                    this.programCall = false;
                    this.actionType = rawActionType;
                }

                this.numPoints     = Integer.parseInt(parts[1]);
                this.targetPoints  = parts[2];
                this.ioPoint       = Integer.parseInt(parts[3]);
                this.ioPin         = Integer.parseInt(parts[4]);
                this.ioState       = Integer.parseInt(parts[5]);
                this.tool          = Integer.parseInt(parts[6]);
                this.base          = Integer.parseInt(parts[7]);
                this.speedOverride = Integer.parseInt(parts[8]);
                this.id            = parts[9];

            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid number format in input: " + e.getMessage());
            }
        }
    }

    public String handleMessage(String message) {
        if (!message.endsWith("#")) {
            System.out.println("Message does not end with '#'");
            return "Invalid message format";
        }

        message = message.substring(0, message.length() - 1);
        String[] parts = message.split("\\|");

        System.out.println("Parsed parts: " + Arrays.toString(parts));

        try {
            Command cmd = new Command(parts);

            if (!cmd.programCall) {
            	System.out.println("Entered Movetype");
                switch (cmd.actionType) {
                    case PTP_AXIS:
                    case PTP_AXIS_C:
                    	System.out.println("Entered PTP AXIS");
                        return handlePTPAxis(cmd.actionType, cmd.numPoints, cmd.targetPoints, cmd.id);
                    case PTP_FRAME:
                    case PTP_FRAME_C:
                        return handlePTPFrame(cmd.numPoints, cmd.targetPoints, cmd.id);
                    case LIN_FRAME:
                        return handleLINFrame(cmd.numPoints, cmd.targetPoints, cmd.id);
                    default:
                        System.out.println("Unknown move type: " + cmd.actionType);
                        return "Unknown move type: " + cmd.actionType;
                }
            } else {
            	System.out.println("Entered Programcall");
                switch (cmd.actionType){
               
                    case 1:
                        return "Program 1 called";
                    case 2:
                        return "Program 2 called";
                    default:
                        return "Program " + cmd.actionType + " not found.";
                }
            }

        } catch (Exception e) {
            return "Failed to parse command: " + e.getMessage();
        }
    }

    
  

    private String handlePTPAxis(int moveType, int numPoints, String targetPoints, String id) {
        // Process the PTP_AXIS or PTP_AXIS_C command
        List<String> jointPositionGroups = Arrays.asList(targetPoints.split(","));
        
        if (jointPositionGroups.size() != numPoints) {
            System.out.println("Invalid number of point groups for ID: " + id);
            return "Invalid number of point groups";
        }

        try {
            for (String jointPositions : jointPositionGroups) {
                List<String> jointValuesStr = Arrays.asList(jointPositions.split(";"));
                
                if (jointValuesStr.size() != 7) {
                    System.out.println("Invalid number of joint positions in a group for ID: " + id);
                    return "Invalid number of joint positions";
                }

                double[] jointValues = new double[7];
                for (int i = 0; i < jointValuesStr.size(); i++) {
                    double jointValueDeg = Double.parseDouble(jointValuesStr.get(i));
                    double jointValueRad = Math.toRadians(jointValueDeg); // Convert degrees to radians
                    
                    if (!isWithinLimits(i, jointValueRad)) {
                        System.out.println("Joint " + (i + 1) + " value out of limits: " + jointValueRad + " for ID: " + id);
                        return "Joint " + (i + 1) + " value out of limits";
                    }

                    jointValues[i] = jointValueRad;
                }

                if (moveType == PTP_AXIS) {
                    // Execute synchronous PTP motion
                    robot.move(BasicMotions.ptp(jointValues));
                } else if (moveType == PTP_AXIS_C) {
                    // Execute asynchronous PTP motion for each point
                    robot.moveAsync(BasicMotions.ptp(jointValues));
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid joint position values for ID: " + id);
            return "Invalid joint position values";
        }

        if (moveType == PTP_AXIS) {
            System.out.println("PTP_AXIS command executed for ID: " + id);
            return "PTP_AXIS command executed";
        } else {
            System.out.println("PTP_AXIS_C command executed for ID: " + id);
            return "PTP_AXIS_C command executed";
        }
    }


    private String handlePTPFrame(int numPoints, String targetPoints, String id) {
        // Process the PTP_FRAME command
        List<String> points = Arrays.asList(targetPoints.split(","));
        try {
            for (int i = 0; i < points.size(); i++) {
                String point = points.get(i);
                List<String> coordinates = Arrays.asList(point.split(";"));
                double x = Double.parseDouble(coordinates.get(0));
                double y = Double.parseDouble(coordinates.get(1));
                double z = Double.parseDouble(coordinates.get(2));
                double roll = Math.toRadians(Double.parseDouble(coordinates.get(3))); // Convert degrees to radians
                double pitch = Math.toRadians(Double.parseDouble(coordinates.get(4))); 
                double yaw = Math.toRadians(Double.parseDouble(coordinates.get(5))); 
                Frame targetFrameVirgin = new Frame(World.Current.getRootFrame(), x, y, z, roll, pitch, yaw);
                robot.move(BasicMotions.ptp(targetFrameVirgin));
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid coordinate values for ID: " + id);
            return "Invalid coordinate values";
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Coordinate values are incomplete for ID: " + id);
            return "Coordinate values are incomplete";
        }
        System.out.println("PTP_FRAME command executed for ID: " + id);
        return "PTP_FRAME command executed";
    }

    private String handleLINFrame(int numPoints, String targetPoints, String id) {
        // Process the LIN_FRAME commands
        List<String> points = Arrays.asList(targetPoints.split(","));
        try {
            for (int i = 0; i < points.size(); i++) {
                String point = points.get(i);
                List<String> coordinates = Arrays.asList(point.split(";"));
                double x = Double.parseDouble(coordinates.get(0));
                double y = Double.parseDouble(coordinates.get(1));
                double z = Double.parseDouble(coordinates.get(2));
                double roll = Math.toRadians(Double.parseDouble(coordinates.get(3))); // Convert degrees to radians
                double pitch = Math.toRadians(Double.parseDouble(coordinates.get(4))); 
                double yaw = Math.toRadians(Double.parseDouble(coordinates.get(5))); 
                Frame targetFrameVirgin = new Frame(World.Current.getRootFrame(), x, y, z, roll, pitch, yaw);
                robot.move(lin(targetFrameVirgin));
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid coordinate values for ID: " + id);
            return "Invalid coordinate values";
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Coordinate values are incomplete for ID: " + id);
            return "Coordinate values are incomplete";
        }
        System.out.println("LIN_FRAME command executed for ID: " + id);
        return "LIN_FRAME command executed";
    }

    private boolean isWithinLimits(int jointIndex, double jointValue) {
        switch (jointIndex) {
            case 0:
                return jointValue >= Math.toRadians(-170) && jointValue <= Math.toRadians(170);
            case 1:
                return jointValue >= Math.toRadians(-120) && jointValue <= Math.toRadians(120);
            case 2:
                return jointValue >= Math.toRadians(-170) && jointValue <= Math.toRadians(170);
            case 3:
                return jointValue >= Math.toRadians(-120) && jointValue <= Math.toRadians(120);
            case 4:
                return jointValue >= Math.toRadians(-170) && jointValue <= Math.toRadians(170);
            case 5:
                return jointValue >= Math.toRadians(-120) && jointValue <= Math.toRadians(120);
            case 6:
                return jointValue >= Math.toRadians(-175) && jointValue <= Math.toRadians(175);
            default:
                return false;
        }
    }
}

package hartuTofas;

import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.motionModel.BasicMotions;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;
import java.util.Arrays;
import java.util.List;

public class MessageHandler {

    private LBR robot;

    // Move types
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

    public MessageHandler(LBR robot) {
        this.robot = robot;
    }

    public String handleMessage(String message) {
        System.out.println("Received message: " + message);
        // Check if the message ends with #
        if (!message.endsWith("#")) {
            System.out.println("Message does not end with '#'");
            return "Invalid message format";
        }

        // Remove the trailing #
        message = message.substring(0, message.length() - 1);

        // Split the message by | and keep empty parts
        String[] parts = message.split("\\|", -1);

        // Log the parts
        System.out.println("Parsed parts: " + Arrays.toString(parts));

        // Parse the parts of the message
        if (parts.length < 8) {
            System.out.println("Message does not have 8 parts");
            return "Invalid message format";
        }

        int moveType;
        int numPoints;
        try {
            moveType = Integer.parseInt(parts[0]);
            numPoints = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid moveType or numPoints");
            return "Invalid message format";
        }

        String targetPoints = parts[2];
        String ioPoint = parts[3];
        String ioPin = parts[4];
        String ioState = parts[5];
        String tool = parts[6];
        String base = parts[7];

        // Handle the moveType
        switch (moveType) {
            case PTP_AXIS:
                return handlePTPAxis(numPoints, targetPoints, ioPoint, ioPin, ioState, tool, base);
            case PTP_FRAME:
                return handlePTPFrame(numPoints, targetPoints, ioPoint, ioPin, ioState, tool, base);
            case LIN_AXIS:
                return handleLINAxis(numPoints, targetPoints, ioPoint, ioPin, ioState, tool, base);
            case LIN_FRAME:
                return handleLINFrame(numPoints, targetPoints, ioPoint, ioPin, ioState, tool, base);
            // Add other cases for different move types as needed
            default:
                System.out.println("Unknown move type: " + moveType);
                return "Unknown move type: " + moveType;
        }
    }

    private String handlePTPAxis(int numPoints, String targetPoints, String ioPoint, String ioPin, String ioState, String tool, String base) {
        // Process the PTP_AXIS command
        List jointPositions = Arrays.asList(targetPoints.split(";"));
        double[] jointValues = new double[jointPositions.size()];
        try {
            for (int i = 0; i < jointPositions.size(); i++) {
                jointValues[i] = Double.parseDouble((String) jointPositions.get(i));
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid joint position values");
            return "Invalid joint position values";
        }
        robot.move(BasicMotions.ptp(jointValues));
        handleIOOperations(ioPoint, ioPin, ioState);
        configureToolAndBase(tool, base);
        return "PTP_AXIS command executed";
    }

    private String handlePTPFrame(int numPoints, String targetPoints, String ioPoint, String ioPin, String ioState, String tool, String base) {
        // Process the PTP_FRAME command
        List points = Arrays.asList(targetPoints.split(","));
        try {
            for (int i = 0; i < points.size(); i++) {
                String point = (String) points.get(i);
                List coordinates = Arrays.asList(point.split(";"));
                double x = Double.parseDouble((String) coordinates.get(0));
                double y = Double.parseDouble((String) coordinates.get(1));
                double z = Double.parseDouble((String) coordinates.get(2));
                double roll = Double.parseDouble((String) coordinates.get(3));
                double pitch = Double.parseDouble((String) coordinates.get(4));
                double yaw = Double.parseDouble((String) coordinates.get(5));
                Frame targetFrame = robot.getCurrentCartesianPosition(robot.getFlange()).transform(Transformation.ofDeg(x, y, z, roll, pitch, yaw));
                robot.move(BasicMotions.ptp(targetFrame));
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid coordinate values");
            return "Invalid coordinate values";
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Coordinate values are incomplete");
            return "Coordinate values are incomplete";
        }
        handleIOOperations(ioPoint, ioPin, ioState);
        configureToolAndBase(tool, base);
        return "PTP_FRAME command executed";
    }

    private String handleLINAxis(int numPoints, String targetPoints, String ioPoint, String ioPin, String ioState, String tool, String base) {
        // Process the LIN_AXIS command
        List jointPositions = Arrays.asList(targetPoints.split(";"));
        double[] jointValues = new double[jointPositions.size()];
        try {
            for (int i = 0; i < jointPositions.size(); i++) {
                jointValues[i] = Double.parseDouble((String) jointPositions.get(i));
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid joint position values");
            return "Invalid joint position values";
        }
       // robot.move(BasicMotions.lin(jointValues));
        handleIOOperations(ioPoint, ioPin, ioState);
        configureToolAndBase(tool, base);
        return "LIN_AXIS command executed";
    }

    private String handleLINFrame(int numPoints, String targetPoints, String ioPoint, String ioPin, String ioState, String tool, String base) {
        // Process the LIN_FRAME command
        List points = Arrays.asList(targetPoints.split(","));
        try {
            for (int i = 0; i < points.size(); i++) {
                String point = (String) points.get(i);
                List coordinates = Arrays.asList(point.split(";"));
                double x = Double.parseDouble((String) coordinates.get(0));
                double y = Double.parseDouble((String) coordinates.get(1));
                double z = Double.parseDouble((String) coordinates.get(2));
                double roll = Double.parseDouble((String) coordinates.get(3));
                double pitch = Double.parseDouble((String) coordinates.get(4));
                double yaw = Double.parseDouble((String) coordinates.get(5));
                Frame targetFrame = robot.getCurrentCartesianPosition(robot.getFlange()).transform(Transformation.ofDeg(x, y, z, roll, pitch, yaw));
                robot.move(BasicMotions.lin(targetFrame));
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid coordinate values");
            return "Invalid coordinate values";
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Coordinate values are incomplete");
            return "Coordinate values are incomplete";
        }
        handleIOOperations(ioPoint, ioPin, ioState);
        configureToolAndBase(tool, base);
        return "LIN_FRAME command executed";
    }

    private void handleIOOperations(String ioPoint, String ioPin, String ioState) {
        // Implement IO operations based on ioPoint, ioPin, and ioState
        // Example: Set the state of a digital output
        if (!ioPoint.isEmpty() && !ioPin.isEmpty() && !ioState.isEmpty()) {
            int point = Integer.parseInt(ioPoint);
            int pin = Integer.parseInt(ioPin);
            boolean state = Boolean.parseBoolean(ioState);
            // Use robot API to set the IO state
            // Example: robot.getIOInterface().setOutput(pin, state);
        }
    }

    private void configureToolAndBase(String tool, String base) {
        // Implement tool and base configuration based on tool and base strings
        if (!tool.isEmpty()) {
            int toolId = Integer.parseInt(tool);
            // Use robot API to set the tool
            // Example: robot.getFlange().attach(toolId);
        }
        if (!base.isEmpty()) {
            int baseId = Integer.parseInt(base);
            // Use robot API to set the base
            // Example: robot.getBaseFrame().setBase(baseId);
        }
    }
}

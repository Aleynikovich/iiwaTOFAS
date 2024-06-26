package hartuTofas;

import static com.kuka.roboticsAPI.motionModel.BasicMotions.lin;

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
                return handlePTPAxis(numPoints, targetPoints);
            case PTP_FRAME:
                return handlePTPFrame(numPoints, targetPoints);
            case LIN_AXIS:
                return handleLINAxis(numPoints, targetPoints);
            case LIN_FRAME:
                return handleLINFrame(numPoints, targetPoints);
            // Add other cases for different move types as needed
            default:
                System.out.println("Unknown move type: " + moveType);
                return "Unknown move type: " + moveType;
        }
    }

    private String handlePTPAxis(int numPoints, String targetPoints) {
        // Process the PTP_AXIS command
        List<String> jointPositions = Arrays.asList(targetPoints.split(";"));
        if (jointPositions.size() != 7) {
            System.out.println("Invalid number of joint positions");
            return "Invalid number of joint positions";
        }

        double[] jointValues = new double[jointPositions.size()];
        try {
            for (int i = 0; i < jointPositions.size(); i++) {
                double jointValueDeg = Double.parseDouble(jointPositions.get(i));
                double jointValueRad = Math.toRadians(jointValueDeg);  // Convert degrees to radians
                if (!isWithinLimits(i, jointValueRad)) {
                    System.out.println("Joint " + (i + 1) + " value out of limits: " + jointValueRad);
                    return "Joint " + (i + 1) + " value out of limits: " + jointValueRad;
                }
                jointValues[i] = jointValueRad;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid joint position values");
            return "Invalid joint position values";
        }

        robot.move(BasicMotions.ptp(jointValues));
        return "PTP_AXIS command executed";
    }

    private String handlePTPFrame(int numPoints, String targetPoints) {
        // Process the PTP_FRAME command
        List<String> points = Arrays.asList(targetPoints.split(","));
        try {
            for (int i = 0; i < points.size(); i++) {
                String point = points.get(i);
                List<String> coordinates = Arrays.asList(point.split(";"));
                double x = Double.parseDouble(coordinates.get(0));
                double y = Double.parseDouble(coordinates.get(1));
                double z = Double.parseDouble(coordinates.get(2));
                double roll = Math.toRadians(Double.parseDouble(coordinates.get(3)));  // Convert degrees to radians
                double pitch = Math.toRadians(Double.parseDouble(coordinates.get(4)));  // Convert degrees to radians
                double yaw = Math.toRadians(Double.parseDouble(coordinates.get(5)));  // Convert degrees to radians
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
        return "PTP_FRAME command executed";
    }

    private String handleLINAxis(int numPoints, String targetPoints) {
        // Process the LIN_AXIS command
        List<String> jointPositions = Arrays.asList(targetPoints.split(";"));
        if (jointPositions.size() != 7) {
            System.out.println("Invalid number of joint positions");
            return "Invalid number of joint positions";
        }

        double[] jointValues = new double[jointPositions.size()];
        try {
            for (int i = 0; i < jointPositions.size(); i++) {
                double jointValueDeg = Double.parseDouble(jointPositions.get(i));
                double jointValueRad = Math.toRadians(jointValueDeg);  // Convert degrees to radians
                if (!isWithinLimits(i, jointValueRad)) {
                    System.out.println("Joint " + (i + 1) + " value out of limits: " + jointValueRad);
                    return "Joint " + (i + 1) + " value out of limits: " + jointValueRad;
                }
                jointValues[i] = jointValueRad;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid joint position values");
            return "Invalid joint position values";
        }

        //robot.move(BasicMotions.lin(jointValues));
        return "LIN_AXIS command executed";
    }

    private String handleLINFrame(int numPoints, String targetPoints) {
        // Process the LIN_FRAME command
        List<String> points = Arrays.asList(targetPoints.split(","));
        try {
            for (int i = 0; i < points.size(); i++) {
                String point = points.get(i);
                List<String> coordinates = Arrays.asList(point.split(";"));
                double x = Double.parseDouble(coordinates.get(0));
                double y = Double.parseDouble(coordinates.get(1));
                double z = Double.parseDouble(coordinates.get(2));
                double roll = Math.toRadians(Double.parseDouble(coordinates.get(3)));  // Convert degrees to radians
                double pitch = Math.toRadians(Double.parseDouble(coordinates.get(4)));  // Convert degrees to radians
                double yaw = Math.toRadians(Double.parseDouble(coordinates.get(5)));  // Convert degrees to radians
                Frame targetFrame = robot.getCurrentCartesianPosition(robot.getFlange()).transform(Transformation.ofDeg(x, y, z, roll, pitch, yaw));
                robot.move(BasicMotions.lin(targetFrame));
                robot.move(lin(targetFrame));
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid coordinate values");
            return "Invalid coordinate values";
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Coordinate values are incomplete");
            return "Coordinate values are incomplete";
        }
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

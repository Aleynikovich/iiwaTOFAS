package hartuTofas;

import static com.kuka.roboticsAPI.motionModel.BasicMotions.ptp;

import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.motionModel.BasicMotions;

import java.util.Arrays;
import java.util.List;

public class MessageHandler {

    private LBR robot;

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

        // Handle the moveType 0 (PTP_AXIS)
        if (moveType == 0) {
            return handlePTPAxis(numPoints, targetPoints);
        } else {
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
                jointValues[i] = Double.parseDouble(jointPositions.get(i));
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid joint position values");
            return "Invalid joint position values";
        }

        robot.move(ptp(jointValues));
        return "PTP_AXIS command executed";
    }
}

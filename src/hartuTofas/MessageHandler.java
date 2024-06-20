package hartuTofas;

import com.kuka.roboticsAPI.deviceModel.LBR;
import java.util.Arrays;
import java.util.List;

public class MessageHandler {

    private LBR robot;

    public MessageHandler(LBR robot) {
        this.robot = robot;
    }

    public String handleMessage(String message) {
        // Check if the message ends with #
        if (!message.endsWith("#")) {
            return "Invalid message format";
        }

        // Remove the trailing #
        message = message.substring(0, message.length() - 1);

        // Split the message by |
        String[] parts = message.split("\\|");

        // Parse the parts of the message
        if (parts.length < 8) {
            return "Invalid message format";
        }

        String moveType = parts[0];
        int numPoints = Integer.parseInt(parts[1]);
        String targetPoints = parts[2];
        String ioPoint = parts[3];
        String ioPin = parts[4];
        String ioState = parts[5];
        String tool = parts[6];
        String base = parts[7];

        // Handle the moveType
        if ("1".equals(moveType)) {
            return handleMoveType1(numPoints, targetPoints, ioPoint, ioPin, ioState, tool, base);
        } else if ("2".equals(moveType)) {
            return handleMoveType2(numPoints, targetPoints);
        } else {
            return "Unknown move type: " + moveType;
        }
    }

    private String handleMoveType1(int numPoints, String targetPoints, String ioPoint, String ioPin, String ioState, String tool, String base) {
        // Process the moveType 1 command
        // Example: Parse the target points and perform the movement
        List<String> points = Arrays.asList(targetPoints.split(","));
        for (String point : points) {
            List<String> coordinates = Arrays.asList(point.split(";"));
            // Process each coordinate: x, y, z, roll, pitch, yaw
            // Implement your robot movement logic here
        }
        
        // Handle IO operations if provided
        if (!ioPoint.isEmpty() && !ioPin.isEmpty() && !ioState.isEmpty()) {
            // Implement IO operations
        }

        // Handle tool and base configuration if provided
        if (!tool.isEmpty() && !base.isEmpty()) {
            // Implement tool and base configuration
        }

        return "MoveType 1 command executed";
    }

    private String handleMoveType2(int numPoints, String targetPoints) {
        // Process the moveType 2 command
        // Example: Parse the joint positions and perform the movement
        List<String> jointPositions = Arrays.asList(targetPoints.split(";"));
        // Process each joint position
        // Implement your robot joint movement logic here

        return "MoveType 2 command executed";
    }
}

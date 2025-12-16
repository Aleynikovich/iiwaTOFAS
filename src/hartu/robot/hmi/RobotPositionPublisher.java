package hartu.robot.hmi;

import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;
import com.kuka.roboticsAPI.geometricModel.math.Vector;
import hartu.robot.communication.server.ClientHandler;
import hartu.robot.communication.server.Logger;
import hartu.robot.communication.server.Ros2ServerManager;
import hartu.robot.executor.CommandExecutor;
/**
 * Publishes current robot position data to connected ROS2 clients in an XML-like format.
 * Sends position in both joint space and Cartesian space (relative to flange and current tool).
 */
public class RobotPositionPublisher
{
    private final LBR robot;
    private final CommandExecutor commandExecutor;

    /**
     * Creates a new robot position publisher.
     *
     * @param robot The LBR robot instance
     * @param commandExecutor The command executor for accessing current tool state
     */
    public RobotPositionPublisher(LBR robot, CommandExecutor commandExecutor)
    {
        this.robot = robot;
        this.commandExecutor = commandExecutor;
    }

    /**
     * Publishes the current robot position to all connected ROS2 task clients.
     * Format:
     * <Joints name="CurrentJoints" j1="..." j2="..." ... j7="..."/>
     * <Pose name="CurrentCartesianFlange" x="..." y="..." z="..." roll="..." pitch="..." yaw="..."/>
     * <Pose name="CurrentCartesianTool[TOOLNAME]" x="..." y="..." z="..." roll="..." pitch="..." yaw="..."/>
     */
    public void publishCurrentPosition()
    {
        try
        {
            StringBuilder message = new StringBuilder();

            // 1. Get and format current joint position
            JointPosition jointPos = robot.getCurrentJointPosition();
            message.append("<Joints name=\"CurrentJoints\"");
            // Assuming 7 joints (Axis 0 to 6) for LBR, using j1 to j7 for names
            for (int i = 0; i < jointPos.getAxisCount(); i++)
            {
                double angleInDegrees = Math.toDegrees(jointPos.get(i));
                // Note: The original example used j4, j3, j1, j2... We will use j1, j2, j3... for order
                // The provided XML example has j4, j3, j1, j2, j5, j6, j7.
                // We will stick to the index order (j1, j2, ...) for consistency.
                message.append(String.format(" j%d=\"%.4f\"", i + 1, angleInDegrees));
            }
            message.append("/> "); // Space added for separation, as in the example

            // 2. Get and format flange position
            Frame flangeFrame = robot.getCurrentCartesianPosition(robot.getFlange());
            message.append(createPoseTag("CurrentCartesianFlange", flangeFrame));
            message.append(" "); // Space added for separation

            // 3. Get and format tool position
            Tool currentTool = commandExecutor.getCurrentlyAttachedTool();
            if (currentTool != null)
            {
                String toolName = currentTool.getName();
                try
                {
                    Frame toolFrame = robot.getCurrentCartesianPosition(currentTool.getDefaultMotionFrame());
                    message.append(createPoseTag("CurrentCartesianTool[" + toolName + "]", toolFrame));
                } catch (Exception e)
                {
                    Logger.getInstance().warn("HMI", "Could not get tool position: " + e.getMessage());
                    // If tool position is unavailable, send zeros in the required format
                    message.append(String.format("<Pose name=\"CurrentCartesianTool[%s]\" x=\"0.000\" y=\"0.000\" z=\"0.000\" roll=\"0.000\" pitch=\"0.000\" yaw=\"0.000\"/>", toolName));
                }
            } else
            {
                // No tool attached, send zeros for a generic "Tool" placeholder
                // Since the original example uses [TOOLNUMBER], we'll use [NONE] if no tool is attached
                message.append("<Pose name=\"CurrentCartesianTool[NONE]\" x=\"0.000\" y=\"0.000\" z=\"0.000\" roll=\"0.000\" pitch=\"0.000\" yaw=\"0.000\"/>");
            }

            // Note: Removed message terminator as it's not present in the new XML-like format example.

            // Send to connected task client via Ros2ServerManager
            String positionData = message.toString();
            Ros2ServerManager serverManager = Ros2ServerManager.getInstance();

            if (serverManager != null && serverManager.getTaskServer() != null)
            {
                ClientHandler taskClient = serverManager.getTaskServer().getClientHandler();
                if (taskClient != null)
                {
                    try
                    {
                        taskClient.sendMessage(positionData);
                        Logger.getInstance().low("HMI", "Position data sent to ROS2 task client");
                        Logger.getInstance().debug("HMI", "Position data: " + positionData.substring(0, Math.min(200, positionData.length())));
                    } catch (Exception e)
                    {
                        Logger.getInstance().warn("HMI", "Failed to send position to task client: " + e.getMessage());
                    }
                } else
                {
                    Logger.getInstance().warn("HMI", "No task client connected to receive position data");
                }
            } else
            {
                Logger.getInstance().warn("HMI", "ROS2 server manager not initialized");
            }

        } catch (Exception e)
        {
            Logger.getInstance().error("HMI", "Error publishing robot position: " + e.getMessage());
            Logger.getInstance().error("HMI", "Stack trace:", e);
        }
    }

    /**
     * Creates an XML-like <Pose> tag from a frame.
     * Format: <Pose name="[name]" x="..." y="..." z="..." roll="..." pitch="..." yaw="..."/>
     * X,Y,Z in mm. Roll (Alpha), Pitch (Beta), Yaw (Gamma) in degrees.
     *
     * @param name The name attribute for the Pose tag
     * @param frame The frame containing position and orientation data
     * @return The formatted <Pose> string
     */
    private String createPoseTag(String name, Frame frame)
    {
        Transformation trans = frame.getTransformationFromParent();
        Vector translation = trans.getTranslation();

        // Roll (Alpha), Pitch (Beta), Yaw (Gamma)
        double rollDeg = Math.toDegrees(trans.getAlphaRad());
        double pitchDeg = Math.toDegrees(trans.getBetaRad());
        double yawDeg = Math.toDegrees(trans.getGammaRad());

        // The original code used Gamma (C), Beta (B), Alpha (A) in the order A, B, C for output.
        // The new format requires Roll, Pitch, Yaw. In KUKA's standard, these often map to Alpha, Beta, Gamma (Z-Y-X rotation sequence, for instance).
        // I will map: Roll -> Alpha, Pitch -> Beta, Yaw -> Gamma to match common robotics conventions and the KUKA API structure used previously.

        return String.format(
                "<Pose name=\"%s\" x=\"%.3f\" y=\"%.3f\" z=\"%.3f\" roll=\"%.3f\" pitch=\"%.3f\" yaw=\"%.3f\"/>",
                name,
                translation.getX(),
                translation.getY(),
                translation.getZ(),
                rollDeg,
                pitchDeg,
                yawDeg
        );
    }
}
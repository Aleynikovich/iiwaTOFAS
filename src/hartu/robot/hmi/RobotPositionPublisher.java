package hartu.robot.hmi;

import hartu.robot.communication.server.ClientHandler;
import hartu.robot.communication.server.Logger;
import hartu.robot.communication.server.Ros2ServerManager;
import hartu.robot.executor.CommandExecutor;

import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;
import com.kuka.roboticsAPI.geometricModel.math.Vector;
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
     * The position is now sent in three separate messages to avoid exceeding message length limits:
     * 1. <Joints name="CurrentJoints" ... />
     * 2. <Pose name="CurrentCartesianFlange" ... />
     * 3. <Pose name="CurrentCartesianTool[TOOLNAME]" ... />
     */
    public void publishCurrentPosition()
    {
        try
        {
            // 1. Get and format current joint position
            JointPosition jointPos = robot.getCurrentJointPosition();
            StringBuilder jointMessage = new StringBuilder();
            jointMessage.append("<Joints name=\"CurrentJoints\"");
            // Assuming 7 joints (Axis 0 to 6) for LBR, using j1 to j7 for names
            for (int i = 0; i < jointPos.getAxisCount(); i++)
            {
                double angleInDegrees = Math.toDegrees(jointPos.get(i));
                jointMessage.append(String.format(" j%d=\"%.4f\"", i + 1, angleInDegrees));
            }
            jointMessage.append("/>");
            sendPositionMessage(jointMessage.toString());


            // 2. Get and format flange position
            Frame flangeFrame = robot.getCurrentCartesianPosition(robot.getFlange());
            String flangeMessage = createPoseTag("CurrentCartesianFlange", flangeFrame);
            sendPositionMessage(flangeMessage);


            // 3. Get and format tool position
            Tool currentTool = commandExecutor.getCurrentlyAttachedTool();
            String toolMessage;
            if (currentTool != null)
            {
                String toolName = currentTool.getName();
                try
                {
                    Frame toolFrame = robot.getCurrentCartesianPosition(currentTool.getDefaultMotionFrame());
                    toolMessage = createPoseTag("CurrentCartesianTool[" + toolName + "]", toolFrame);
                } catch (Exception e)
                {
                    Logger.getInstance().warn("HMI", "Could not get tool position: " + e.getMessage());
                    // If tool position is unavailable, send zeros in the required format
                    toolMessage = String.format("<Pose name=\"CurrentCartesianTool[%s]\" x=\"0.000\" y=\"0.000\" z=\"0.000\" roll=\"0.000\" pitch=\"0.000\" yaw=\"0.000\"/>", toolName);
                }
            } else
            {
                // No tool attached, send zeros for a generic "Tool" placeholder
                toolMessage = "<Pose name=\"CurrentCartesianTool[NONE]\" x=\"0.000\" y=\"0.000\" z=\"0.000\" roll=\"0.000\" pitch=\"0.000\" yaw=\"0.000\"/>";
            }
            sendPositionMessage(toolMessage);

            Logger.getInstance().low("HMI", "All three position components sent to ROS2 task client");

        } catch (Exception e)
        {
            Logger.getInstance().error("HMI", "Error publishing robot position: " + e.getMessage());
            Logger.getInstance().error("HMI", "Stack trace:", e);
        }
    }

    /**
     * Encapsulates the logic to get the ROS2 client and send the position data.
     * This helper method simplifies the main publishCurrentPosition logic.
     *
     * @param positionData The XML-like string containing one position component (Joints or Pose).
     */
    private void sendPositionMessage(String positionData)
    {
        Ros2ServerManager serverManager = Ros2ServerManager.getInstance();

        if (serverManager != null && serverManager.getTaskServer() != null)
        {
            ClientHandler taskClient = serverManager.getTaskServer().getClientHandler();
            if (taskClient != null)
            {
                try
                {
                    // Send the individual message
                    taskClient.sendMessage(positionData);
                    Logger.getInstance().debug("HMI", "Sent position component: " + positionData.substring(0, Math.min(60, positionData.length())) + "...");
                } catch (Exception e)
                {
                    Logger.getInstance().warn("HMI", "Failed to send position component to task client: " + e.getMessage());
                }
            } else
            {
                Logger.getInstance().warn("HMI", "No task client connected to receive position data");
            }
        } else
        {
            Logger.getInstance().warn("HMI", "ROS2 server manager not initialized");
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
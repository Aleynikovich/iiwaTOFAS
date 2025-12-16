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
import hartu.protocols.constants.ProtocolConstants;

/**
 * Publishes current robot position data to connected ROS2 clients.
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
     * Format: POSITION|joint1;joint2;...;joint7|flange_x;y;z;a;b;c|tool_x;y;z;a;b;c#
     * 
     * Where:
     * - joint1-joint7: Joint angles in radians
     * - flange position: X,Y,Z in mm and A,B,C in degrees (relative to robot base)
     * - tool position: X,Y,Z in mm and A,B,C in degrees (relative to robot base, at tool TCP)
     */
    public void publishCurrentPosition()
    {
        try
        {
            // Get current joint position
            JointPosition jointPos = robot.getCurrentJointPosition();
            StringBuilder message = new StringBuilder("POSITION");
            message.append(ProtocolConstants.PRIMARY_DELIMITER);
            
            // Add joint positions (in radians)
            for (int i = 0; i < jointPos.getAxisCount(); i++)
            {
                message.append(String.format("%.6f", jointPos.get(i)));
                if (i < jointPos.getAxisCount() - 1)
                {
                    message.append(ProtocolConstants.SECONDARY_DELIMITER);
                }
            }
            message.append(ProtocolConstants.PRIMARY_DELIMITER);
            
            // Get flange position (Cartesian coordinates relative to robot base)
            Frame flangeFrame = robot.getCurrentCartesianPosition(robot.getFlange());
            appendCartesianPosition(message, flangeFrame);
            message.append(ProtocolConstants.PRIMARY_DELIMITER);
            
            // Get tool position (if tool is attached)
            Tool currentTool = commandExecutor.getCurrentlyAttachedTool();
            if (currentTool != null)
            {
                try
                {
                    // Get position at tool TCP (Tool Center Point)
                    Frame toolFrame = robot.getCurrentCartesianPosition(currentTool.getDefaultMotionFrame());
                    appendCartesianPosition(message, toolFrame);
                } catch (Exception e)
                {
                    Logger.getInstance().warn("HMI", "Could not get tool position: " + e.getMessage());
                    // Send zeros if tool position unavailable
                    message.append("0.0;0.0;0.0;0.0;0.0;0.0");
                }
            } else
            {
                // No tool attached, send zeros
                message.append("0.0;0.0;0.0;0.0;0.0;0.0");
            }
            
            message.append(ProtocolConstants.MESSAGE_TERMINATOR);
            
            // Send to connected task client via Ros2ServerManager
            String positionData = message.toString();
            Ros2ServerManager serverManager = Ros2ServerManager.getInstance();
            
            if (serverManager != null && serverManager.getTaskServer() != null)
            {
                ClientHandler taskClient = serverManager.getTaskServer().getClientHandler();
                if (taskClient != null && taskClient.isConnected())
                {
                    try
                    {
                        taskClient.sendMessage(positionData);
                        Logger.getInstance().info("HMI", "Position data sent to ROS2 task client");
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
     * Appends Cartesian position data from a frame to the message.
     * Format: X;Y;Z;A;B;C where X,Y,Z are in mm and A,B,C are in degrees.
     *
     * @param message The string builder to append to
     * @param frame The frame containing position data
     */
    private void appendCartesianPosition(StringBuilder message, Frame frame)
    {
        // Get translation (position in mm)
        Transformation trans = frame.getTransformationFromParent();
        Vector translation = trans.getTranslation();
        
        // Get rotation angles in degrees (A, B, C - around Z, Y, X axes)
        double[] abc = trans.getAlphaBetagamma();
        
        message.append(String.format("%.3f", translation.getX()));
        message.append(ProtocolConstants.SECONDARY_DELIMITER);
        message.append(String.format("%.3f", translation.getY()));
        message.append(ProtocolConstants.SECONDARY_DELIMITER);
        message.append(String.format("%.3f", translation.getZ()));
        message.append(ProtocolConstants.SECONDARY_DELIMITER);
        message.append(String.format("%.3f", Math.toDegrees(abc[0])));  // A (alpha)
        message.append(ProtocolConstants.SECONDARY_DELIMITER);
        message.append(String.format("%.3f", Math.toDegrees(abc[1])));  // B (beta)
        message.append(ProtocolConstants.SECONDARY_DELIMITER);
        message.append(String.format("%.3f", Math.toDegrees(abc[2])));  // C (gamma)
    }
}

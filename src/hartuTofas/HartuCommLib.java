package hartuTofas;

import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import java.io.IOException;
import java.util.logging.Logger;

public class HartuCommLib {

    private static final Logger LOGGER = Logger.getLogger(HartuCommLib.class.getName());

    /**
     * Formats the joint positions into a string with the specified format.
     *
     * @param joints The joint positions of the robot.
     * @return A string with the joint positions separated by semicolons and terminated with '#'.
     */
    private static String formatJointPosition(JointPosition joints) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < joints.getAxisCount(); i++) {
            sb.append(joints.get(i));
            if (i < joints.getAxisCount() - 1) {
                sb.append(";");
            }
        }
        sb.append("#");
        return sb.toString();
    }

    /**
     * Sends the joint state to the server.
     *
     * @param robot     The LBR robot instance.
     * @param tcpClient The IiwaTcpClient instance.
     */
    public static void sendJointStateData(LBR robot, IiwaTcpClient tcpClient) {
        try {
            JointPosition joints = robot.getCurrentJointPosition();
            String jointStr = formatJointPosition(joints);
            tcpClient.sendOnly(jointStr);
            LOGGER.info("Sending joint state: " + jointStr);
        } catch (IOException e) {
            LOGGER.severe("Error sending joint state: " + e.getMessage());
            //  Important:  Handle the error appropriately.  You might want to set a
            //  flag to indicate an error, or attempt to reconnect.  *Do not* throw
            //  an exception here, as this method will be called from the SPS, and
            //  you don't want to crash the SPS.
        }
    }
}

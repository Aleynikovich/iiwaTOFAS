package hartu.robot.utils;

import com.kuka.roboticsAPI.deviceModel.JointPosition;
import hartu.protocols.constants.ProtocolConstants;

/**
 * Utility class for formatting robot joint positions into a string message.
 */
public class JointDataFormatter
{

    private JointDataFormatter()
    {
        // Private constructor to prevent instantiation of utility class
    }

    /**
     * Formats a JointPosition object into a semicolon-separated string of joint values,
     * terminated by the ProtocolConstants.MESSAGE_TERMINATOR.
     * Example: "J1_value;J2_value;J3_value;J4_value;J5_value;J6_value;J7_value#"
     *
     * @param joints The JointPosition object to format.
     * @return A formatted string representing the joint positions.
     */
    public static String formatJointPosition(JointPosition joints)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < joints.getAxisCount(); i++)
        {
            sb.append(joints.get(i));
            if (i < joints.getAxisCount() - 1)
            {
                sb.append(ProtocolConstants.SECONDARY_DELIMITER);
            }
        }
        sb.append(ProtocolConstants.MESSAGE_TERMINATOR);
        return sb.toString();
    }
}


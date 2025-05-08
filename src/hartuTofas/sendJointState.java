package hartuTofas;

import javax.inject.Inject;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Robotics API application to send KUKA IIWA robot joint states to a server.
 */
public class sendJointState extends RoboticsAPIApplication {
    @Inject
    private LBR iiwa;
    @Inject
    private Controller kukaController;  // Needed to access the LBR

    private IiwaTcpClient tcpClient;
    private static final String SERVER_IP = "10.66.171.69"; // Server IP address
    private static final int SERVER_PORT = 30002; // Server port

    @Override
    public void initialize() {
        // Initializes the application. This is automatically called by the framework.
        super.initialize(); // Call the superclass method
        try {
            // Initialize the TCP client with the server IP and port.
            tcpClient = new IiwaTcpClient(SERVER_IP, SERVER_PORT);
            tcpClient.connect(); // Establish the connection to the server.
            getLogger().info("TCP connection established with server: " + SERVER_IP + ":" + SERVER_PORT);
        } catch (IOException | TimeoutException e) {
            getLogger().error("Error connecting to server: " + e.getMessage(), e);
            // It's important to handle the exception here. You might choose to terminate the application,
            // attempt to reconnect, or take other appropriate action. Here, we terminate.
            throw new RuntimeException("Failed to connect to the server.", e); // Propagate exception to stop app
        }
    }

    @Override
    public void run() {
        // Main application method. Called after initialize().
        try {
            // 1. Get the current joint positions of the robot.
            JointPosition joints = iiwa.getCurrentJointPosition();

            // 2. Format the joint positions into a string.
            String jointStr = formatJointPosition(joints);

            // 3. Send the string to the server via the TCP connection.
            tcpClient.sendCommand(jointStr);
            getLogger().info("Sending joint state: " + jointStr);

        } catch (IOException | TimeoutException e) {
            getLogger().error("Error during communication with the server: " + e.getMessage(), e);
            // Handle the exception here. Possible actions: reconnect, terminate the application, etc.
            // Here, we terminate the program.
        } finally {
            // 5.  Close the connection in a finally block to ensure it's closed
            //     even if exceptions occur.
            if (tcpClient != null) {
                tcpClient.closeConnection();
                getLogger().info("TCP connection closed.");
            }
        }
    }

    /**
     * Formats the joint positions into a string with the specified format.
     *
     * @param joints The joint positions of the robot.
     * @return A string with the joint positions separated by semicolons and terminated with '#'.
     */
    private String formatJointPosition(JointPosition joints) {
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

    @Override
    public void dispose() {
        // Called when the application is stopped. It's important to release resources.
        super.dispose(); // Call the superclass method
        if (tcpClient != null) {
            tcpClient.closeConnection();
            getLogger().info("TCP connection closed in dispose().");
        }
        // Here you can release other resources, such as motion handlers, etc.
    }
}

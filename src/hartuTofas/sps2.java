package hartuTofas;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;
import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Implementation of a cyclic background task.
 * <p>
 * It provides the {@link RoboticsAPICyclicBackgroundTask#runCyclic} method
 * which will be called cyclically with the specified period.<br>
 * Cycle period and initial delay can be set by calling
 * {@link RoboticsAPICyclicBackgroundTask#initializeCyclic} method in the
 * {@link RoboticsAPIBackgroundTask#initialize()} method of the inheriting
 * class.<br>
 * The cyclic background task can be terminated via
 * {@link RoboticsAPICyclicBackgroundTask#getCyclicFuture()#cancel()} method or
 * stopping of the task.
 * @see UseRoboticsAPIContext
 *
 */
public class sps2 extends RoboticsAPICyclicBackgroundTask {
    @Inject
    private Controller kUKA_Sunrise_Cabinet_1;
    private LBR lbr;
    private Socket socket;
    private OutputStream outputStream;
    private static final String SERVER_IP = "10.66.171.69";
    private static final int SERVER_PORT = 30002;

    @Override
    public void initialize() {
        // initialize your task here
        initializeCyclic(0, 500, TimeUnit.MILLISECONDS,
                CycleBehavior.BestEffort);
        
        try {
            // Create socket connection in initialize
            socket = new Socket(SERVER_IP, SERVER_PORT);
            outputStream = socket.getOutputStream();
        } catch (IOException e) {
            getLogger().error("Error creating socket connection: ", e);
            // Consider how to handle this error.  Possible solutions:
            // 1.  Stop the task.
            // 2.  Attempt to reconnect in runCyclic().
            // 3.  Set a flag and don't send data.
        }
    }

    @Override
    public void runCyclic() {
        // your task execution starts here
        if (lbr != null) {
            JointPosition currentPosition = lbr.getCurrentJointPosition();
            String message = formatJointPosition(currentPosition);
            try {
                if (outputStream != null) {
                    outputStream.write(message.getBytes());
                    outputStream.flush(); // Ensure data is sent immediately.
                    getLogger().info("Sent: " + message); // Log the sent message
                }
                else
                {
                   getLogger().warn("Output stream is null. Not sending data.");
                }

            } catch (IOException e) {
                getLogger().error("Error sending data: ", e);
                // Consider how to handle this error:
                // 1. Attempt to reconnect?
                // 2.  Set a flag to stop sending.
                // 3.  Log and continue.
            }
        }
    }

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

     @Override
    public void dispose() {
        super.dispose();
        // Close the socket and output stream in dispose()
        try {
            if (outputStream != null) {
                outputStream.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            getLogger().error("Error closing socket: ", e);
        }
    }
}

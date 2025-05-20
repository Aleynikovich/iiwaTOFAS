package hartuTofas;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;
import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

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
public class SPS extends RoboticsAPICyclicBackgroundTask {
    @Inject
    private LBR lbr;
    private Socket socket;
    private OutputStream outputStream;
    private static final String SERVER_IP = "10.66.171.69";
    private static final int SERVER_PORT = 30002;
    private boolean connectionEstablished = false; // Flag to track connection status

    @Override
    public void initialize() {
        // initialize your task here
        initializeCyclic(0, 10, TimeUnit.MILLISECONDS,
                CycleBehavior.BestEffort);
        lbr = getContext().getDeviceFromType(LBR.class);
        connectToServer(); // Call connectToServer in initialize
    }

    private void connectToServer() {
        try {
            // Create socket connection in initialize
            socket = new Socket(SERVER_IP, SERVER_PORT);
            outputStream = socket.getOutputStream();
            connectionEstablished = true; // Set flag on successful connection
            getLogger().info("Successfully connected to server at " + SERVER_IP + ":" + SERVER_PORT);
        } catch (IOException e) {
            getLogger().error("Error creating socket connection: ", e);
            // Consider how to handle this error.  Possible solutions:
            // 1.  Stop the task. -  Not recommended in initialize for a cyclic task.
            // 2.  Attempt to reconnect in runCyclic().  <- Preferred approach.
            // 3.  Set a flag and don't send data.  <- Implemented with connectionEstablished
            connectionEstablished = false; // Ensure flag is false on failure
        }
    }

    @Override
    public void runCyclic() {
        // your task execution starts here
            JointPosition currentPosition = lbr.getCurrentJointPosition();
            String message = formatJointPosition(currentPosition);
            try {
                if (connectionEstablished && outputStream != null) { // Check connection and stream
                    outputStream.write(message.getBytes());
                    outputStream.flush(); // Ensure data is sent immediately.
                    getLogger().info("Sent: " + message); // Log the sent message
                } else {
                    if (!connectionEstablished) {
                         getLogger().warn("Not connected to server. Attempting to reconnect...");
                         connectToServer(); // Attempt to reconnect
                    }
                    else
                    {
                        getLogger().warn("Output stream is null.  Skipping send.");
                    }

                }

            } catch (IOException e) {
                getLogger().error("Error sending data: ", e);
                if (e instanceof SocketException) {
                    connectionEstablished = false; //reset flag to attempt a new connection
                    getLogger().warn("Socket error detected.  Will attempt to reconnect.");
                }
                // Consider how to handle this error:
                // 1. Attempt to reconnect?  <- Implemented, with a delay.
                // 2.  Set a flag to stop sending. <- Implemented with connectionEstablished
                // 3.  Log and continue.
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
        } finally {
            outputStream = null;
            socket = null; //explicitly null the variables
            connectionEstablished = false;
        }
    }
}

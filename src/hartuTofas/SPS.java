package hartuTofas;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;
import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import java.io.IOException;
import java.net.ConnectException; // Import ConnectException
import java.util.concurrent.TimeoutException;

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
    private LBR iiwa;
    @Inject
    private Controller kukaController;

    private IiwaTcpClient tcpClient; // Declare the TCP client here.
    private static final String SERVER_IP = "10.66.171.69";
    private static final int SERVER_PORT = 30002;
    private boolean connected = false; // Add a connection status flag

    @Override
    public void initialize() {
        // initialize your task here
        initializeCyclic(0, 10, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);
        // Attempt to connect in initialize as well
        connectToServer();
    }

    private void connectToServer() {
        try {
            if (tcpClient == null || !tcpClient.isConnected()) { // Check if client is null or not connected
                if (tcpClient != null) {
                    tcpClient.closeConnection();
                    tcpClient = null; // Ensure tcpClient is null after closing
                }
                tcpClient = new IiwaTcpClient(SERVER_IP, SERVER_PORT);
                tcpClient.connect();
                getLogger().info("TCP connection established with server: " + SERVER_IP + ":" + SERVER_PORT);
                connected = true; // Set the flag to true on successful connection
            }
        } catch (ConnectException e) { // Catch ConnectException specifically
            getLogger().error("Connection refused: " + e.getMessage());
            connected = false;
        } catch (IOException | TimeoutException e) {
            getLogger().error("Error initializing TCP connection: " + e.getMessage());
            connected = false;
        }
    }

    @Override
    public void runCyclic() {
        // your task execution starts here
        if (!connected) {
            // Attempt to reconnect only if not connected
            connectToServer();
            if (!connected) {
                // If still not connected, wait and try again in the next cycle
                return; // Exit the current cycle and try again later.
            }
        }

        // If we get here, we are connected (or have reconnected)
        try {
            // 1. Send the joint positions using the HartuCommLib.
            HartuCommLib.sendJointStateData(iiwa, tcpClient);

        }  catch (Exception e) {
            getLogger().error("Error in SPS cyclic task: " + e.getMessage(), e);
            connected = false; // Reset connection status
            connectToServer(); // attempt to reconnect
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (tcpClient != null) {
            tcpClient.closeConnection();
        }
    }
}

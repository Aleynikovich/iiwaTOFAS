package hartuTofas;

import javax.inject.Inject;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

/**
 * Robotics application to send joint data to a TCP server.
 */
public class ping extends RoboticsAPIApplication {
    @Inject
    private Controller kUKA_Sunrise_Cabinet_1;
    private LBR lbr;
    private Socket socket;
    private OutputStream outputStream;
    private static final String SERVER_IP = "10.66.171.69";
    private static final int SERVER_PORT = 30002;
    private boolean connectionEstablished = false; // Flag to track connection status

    @Override
    public void initialize() {
        // initialize your application here
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
            connectionEstablished = false; // Ensure flag is false on failure
        }
    }

    @Override
    public void run() {
        // application execution starts here
        if (lbr != null) {
            JointPosition currentPosition = lbr.getCurrentJointPosition(); 
            String message = formatJointPosition(currentPosition);
            try {
                if (connectionEstablished && outputStream != null) { // Check connection and stream
                    outputStream.write(message.getBytes());
                    outputStream.flush(); // Ensure data is sent immediately.
                    getLogger().info("Sent: " + message); // Log the sent message
                } else {
                    getLogger().warn("Not connected to server.  Aborting send.");
                    return; // Exit run method if not connected
                }
            } catch (IOException e) {
                getLogger().error("Error sending data: ", e);
                if (e instanceof SocketException) {
                    connectionEstablished = false;
                    getLogger().warn("Socket error.");
                }
            }
        }
        // Close resources.
        dispose();
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
            socket = null; // explicitly null the variables
            connectionEstablished = false;
        }
    }

}

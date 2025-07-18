package hartu.robot.logging;

import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;

public class RemoteLogger extends RoboticsAPICyclicBackgroundTask {

    private static RemoteLogger instance;
    private DatagramSocket socket;
    private volatile boolean running;

    private RemoteLogger() {
        running = true;
    }

    public static RemoteLogger getInstance() { // Removed synchronized
        if (instance == null) {
            instance = new RemoteLogger();
        }
        return instance;
    }

    @Override
    public void initialize() {
        initializeCyclic(0, 200, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);
        try {
            socket = new DatagramSocket(); // Attempt to create the socket
        } catch (SocketException e) {
            running = false;
        } catch (Exception e) {
            running = false;
        }
    }

    @Override
    public void runCyclic() {
        // Do nothing in runCyclic for this minimal test
    }

    @Override
    public void dispose() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        socket = null;
        instance = null;
    }

    public void log(String message) {
        // This method does nothing in this minimal version
    }
}
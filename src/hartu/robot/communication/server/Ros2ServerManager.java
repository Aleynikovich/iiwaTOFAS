package hartu.robot.communication.server;

import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import hartu.protocols.constants.ProtocolConstants.ListenerType;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Background task that manages the ROS2 task command server.
 * Listens on port 30001 for task commands from external clients.
 * <p>
 * Note: Logging is now handled by LoggingServerManager on port 30002.
 * This follows the single responsibility principle - one server, one port, one function.
 */
public class Ros2ServerManager extends RoboticsAPICyclicBackgroundTask
{
    private static final int TASK_PORT = 30001;
    private ServerClass taskServer;

    @Override
    public void initialize()
    {
        initializeCyclic(0, 1000, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);
        Thread serverThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    // Create server for task commands only
                    taskServer = new ServerClass(TASK_PORT, ListenerType.TASK_LISTENER);
                } catch (IOException e)
                {
                    Logger.getInstance().debug("APP", "Error initializing ROS2 Task Server: " + e.getMessage());
                    throw new RuntimeException(e);
                }
                taskServer.start();
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
        Logger.getInstance().debug("APP", "ROS2 Task Server Manager initialized and server thread started on port " + TASK_PORT);
    }

    @Override
    public void runCyclic()
    {

    }

    @Override
    public void dispose()
    {
        if (taskServer != null)
        {
            try
            {
                taskServer.stop();
                Logger.getInstance().debug("APP", "ROS2 Task Server stopped.");
            } catch (IOException e)
            {
                Logger.getInstance().debug("APP", "Error stopping ROS2 Task Server: " + e.getMessage());
                throw new RuntimeException("Error stopping robot communication server: " + e.getMessage(), e);
            }
        }
        Logger.getInstance().debug("APP", "ROS2 Task Server Manager disposed.");
        super.dispose();
    }
}

package hartu.robot.communication.server;

import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Ros2Manager extends RoboticsAPICyclicBackgroundTask
{
    private static final int TASK_PORT = 30001;
    private static final int LOG_PORT = 30002;
    private ServerClass rosCommunicationServer;

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
                    rosCommunicationServer = new ServerClass(TASK_PORT, LOG_PORT);
                } catch (IOException e)
                {
                    Logger.getInstance().log("APP", "Error initializing ROS Communication Server: " + e.getMessage());
                    throw new RuntimeException(e);
                }
                rosCommunicationServer.start();
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
        Logger.getInstance().log("APP", "ROS Communication Server Manager initialized and server thread started.");
    }

    @Override
    public void runCyclic()
    {
        // This cyclic task currently does nothing in its runCyclic,
        // but it could be used for periodic checks or heartbeat if needed.
    }

    @Override
    public void dispose()
    {
        if (rosCommunicationServer != null)
        {
            try
            {
                rosCommunicationServer.stop();
                Logger.getInstance().log("APP", "ROS Communication Server stopped.");
            } catch (IOException e)
            {
                Logger.getInstance().log("APP", "Error stopping ROS Communication Server: " + e.getMessage());
                throw new RuntimeException("Error stopping robot communication server: " + e.getMessage(), e);
            }
        }
        Logger.getInstance().log("APP", "ROS Communication Server Manager disposed.");
        super.dispose();
    }
}

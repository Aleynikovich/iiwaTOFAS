package hartu.robot.communication.server;

import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Ros2ServerManager extends RoboticsAPICyclicBackgroundTask
{
    private ServerClass rosCommunicationServer;
    private Thread serverThread;

    @Override
    public void initialize()
    {
        initializeCyclic(0, 1000, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);
        serverThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {

                    rosCommunicationServer = new ServerClass();
                }
                catch (IOException e)
                {

                    Logger.getInstance().error(
                            "SERVER_MGR",
                            "Error initializing ROS Communication Server: " + e.getMessage()
                                              );
                    throw new RuntimeException(e);
                }
                rosCommunicationServer.start();
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        Logger.getInstance().log(
                "SERVER_MGR",
                "ROS Communication Server Manager initialized and server thread started."
                                );

    }

    @Override
    public void runCyclic()
    {

    }

    @Override
    public void dispose()
    {

        if (rosCommunicationServer != null)
        {
            try
            {
                rosCommunicationServer.stop();
                Logger.getInstance().log(
                        "SERVER_MGR",
                        "ROS Communication Server signaled to stop. Waiting for thread to finish..."
                                        );

                if (serverThread != null && serverThread.isAlive())
                {
                    serverThread.join(2000);
                    if (serverThread.isAlive())
                    {
                        Logger.getInstance().warn(
                                "SERVER_MGR",
                                "Warning: Server thread did not terminate within timeout."
                                                 );
                    }
                    else
                    {
                        Logger.getInstance().log("SERVER_MGR", "ROS Communication Server thread finished gracefully.");
                    }
                }

                Logger.getInstance().log("SERVER_MGR", "ROS Communication Server stopped.");
            }
            catch (IOException e)
            {
                Logger.getInstance().error("SERVER_MGR", "Error stopping ROS Communication Server: " + e.getMessage());
                throw new RuntimeException("Error stopping robot communication server: " + e.getMessage(), e);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                Logger.getInstance().warn(
                        "SERVER_MGR",
                        "ROS Communication Server Manager interrupted while waiting for server thread to stop."
                                         );
            }
        }
        Logger.getInstance().log("SERVER_MGR", "ROS Communication Server Manager disposed.");
        super.dispose();
    }
}

package hartu.robot.communication.server;

import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class rosCommunicationServerManager extends RoboticsAPICyclicBackgroundTask
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
                    throw new RuntimeException(e);
                }
                rosCommunicationServer.start();
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
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
            } catch (IOException e)
            {
                throw new RuntimeException("Error stopping robot communication server: " + e.getMessage(), e);
            }
        }
        super.dispose();
    }
}
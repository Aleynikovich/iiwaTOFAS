package hartu.robot.communication.client;

import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import hartu.robot.communication.server.Logger;
import hartu.robot.utils.JointDataFormatter;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Ros2ClientManager extends RoboticsAPICyclicBackgroundTask
{
    @Inject
    private LBR lbr;
    private ClientClass jointLogClient;
    private boolean connectionEstablished = false;

    @Override
    public void initialize()
    {

        String[] config = ClientConfigLoader.loadConnectionConfig("jointLog.port", "30002");
        String serverIp = config[0];
        int serverPort = Integer.parseInt(config[1]);

        jointLogClient = new ClientClass(serverIp, serverPort);

        initializeCyclic(0, 10, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);
        lbr = getContext().getDeviceFromType(LBR.class);
        connectToServer();
    }

    private void connectToServer()
    {
        try
        {
            jointLogClient.connect();
            connectionEstablished = true;

            Logger.getInstance().log(
                    "CLIENT_MGR",
                    "Successfully connected to server at " + jointLogClient.getServerIp() + ":" + jointLogClient.getServerPort() + " (via ClientClass)."
                                    );
        }
        catch (IOException e)
        {
            Logger.getInstance().log(
                    "CLIENT_MGR",
                    "Error creating socket connection (via ClientClass): " + e.getMessage()
                                    );
            connectionEstablished = false;
        }
    }

    @Override
    public void runCyclic()
    {
        JointPosition currentPosition = lbr.getCurrentJointPosition();

        String message = JointDataFormatter.formatJointPosition(currentPosition);

        if (connectionEstablished && jointLogClient.isConnected())
        {
            try
            {
                jointLogClient.sendMessage(message);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }

            Logger.getInstance().log("CLIENT_MGR", "Sent: " + message);
        }
        else
        {
            if (!connectionEstablished)
            {
                Logger.getInstance().warn("CLIENT_MGR", "Not connected to server. Attempting to reconnect...");
                connectToServer();
            }
            else
            {
                Logger.getInstance().warn(
                        "CLIENT_MGR",
                        "ClientClass connection is not established or is closed. Skipping send."
                                         );

                connectionEstablished = false;
            }
        }

    }

    @Override
    public void dispose()
    {
        super.dispose();

        try
        {
            if (jointLogClient != null)
            {
                jointLogClient.close();
                Logger.getInstance().log("CLIENT_MGR", "ClientClass connection closed.");
            }
        }
        catch (IOException e)
        {
            Logger.getInstance().error("CLIENT_MGR", "Error closing ClientClass socket: " + e.getMessage());
        }
        finally
        {
            jointLogClient = null;
            connectionEstablished = false;
            Logger.getInstance().log("CLIENT_MGR", "Ros2ClientManager resources disposed.");
        }
    }
}

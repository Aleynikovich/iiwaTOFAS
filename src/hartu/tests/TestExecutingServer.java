package hartu.tests;

import com.kuka.generated.ioAccess.Ethercat_x44IOGroup;
import com.kuka.generated.ioAccess.IOFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.LBR;
import hartu.robot.commands.ParsedCommand;
import hartu.robot.commands.io.IoCommandData;
import hartu.robot.communication.server.CommandQueue;
import hartu.robot.communication.server.CommandResultHolder;
import hartu.robot.communication.server.Logger;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

/**
 * A temporary background KUKA application that acts as the command executor.
 * It continuously polls the shared CommandQueue for ParsedCommand objects,
 * executes them (currently only IO commands), and signals the result back
 * via the CommandResultHolder.
 */
public class TestExecutingServer extends RoboticsAPICyclicBackgroundTask
{

    @Inject
    private Controller robotController;

    @Inject
    private LBR iiwa;

    @Inject
    private IOFlangeIOGroup gimaticIO;
    @Inject
    private Ethercat_x44IOGroup toolControlIO;

    @Override
    public void initialize()
    {

        initializeCyclic(0, 50, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);

        Logger.getInstance().log("TestExecutingServer: Initializing. Ready to take commands from queue.");
    }

    @Override
    public void runCyclic()
    {

        CommandResultHolder resultHolder = CommandQueue.pollCommand(0, TimeUnit.MILLISECONDS);

        if (resultHolder != null)
        {
            ParsedCommand command = resultHolder.getCommand();
            Logger.getInstance().log("TestExecutingServer: Received command ID " + command.getId() + " from queue for execution.");
            boolean executionSuccess = false;

            try
            {
                if (command.isIoCommand())
                {
                    IoCommandData ioData = command.getIoCommandData();
                    int ioPin = ioData.getIoPin();
                    boolean ioState = ioData.getIoState();

                    Logger.getInstance().log("TestExecutingServer: Executing IO command. Pin: " + ioPin + ", State: " + ioState);

                    switch (ioPin)
                    {
                        case 1:
                            gimaticIO.setDO_Flange7(ioState);
                            Logger.getInstance().log("TestExecutingServer: Set DO_Flange7 to " + ioState);
                            executionSuccess = true;
                            break;
                        case 2:
                            toolControlIO.setOutput2(ioState);
                            Logger.getInstance().log("TestExecutingServer: Set Ethercat_x44 Output2 to " + ioState);
                            executionSuccess = true;
                            break;
                        case 3:
                            toolControlIO.setOutput1(ioState);
                            Logger.getInstance().log("TestExecutingServer: Set Ethercat_x44 Output1 to " + ioState);
                            executionSuccess = true;
                            break;
                        default:
                            Logger.getInstance().log(
                                    "TestExecutingServer Error: Invalid IO pin in parsed command for direct mapping: " + ioPin);
                    }
                }
                else
                {
                    Logger.getInstance().log(
                            "TestExecutingServer Warning: Received non-IO command. Only IO commands are supported in this test: " + command.getActionType().name());

                }
            }
            catch (Exception e)
            {
                Logger.getInstance().log("TestExecutingServer Error: Exception during command execution for ID " + command.getId() + ": " + e.getMessage());
            }
            finally
            {

                resultHolder.setSuccess(executionSuccess);
                resultHolder.getLatch().countDown();
                Logger.getInstance().log("TestExecutingServer: Signaled completion for command ID " + command.getId() + ". Success: " + executionSuccess);
            }
        }
    }

    @Override
    public void dispose()
    {
        Logger.getInstance().log("TestExecutingServer: Disposing...");
        super.dispose();
    }
}

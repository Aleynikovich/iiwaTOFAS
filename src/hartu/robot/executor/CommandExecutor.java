package hartu.robot.executor;

import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.deviceModel.LBR;

import javax.inject.Inject;

import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;

public class CommandExecutor extends RoboticsAPIApplication
{
    @Inject
    private LBR iiwa;

    @Override
    public void initialize()
    {

    }

    @Override
    public void run()
    {

    }
}
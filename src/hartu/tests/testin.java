package hartu.tests;


import javax.inject.Inject;

import com.kuka.generated.ioAccess.Ethercat_x44IOGroup;
import com.kuka.generated.ioAccess.IOFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;

import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;

import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.motionModel.IMotion;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.motionModel.RobotMotion;
import hartu.protocols.constants.ActionTypes;
import hartu.protocols.constants.MovementType;
import hartu.robot.commands.MotionParameters;
import hartu.robot.commands.MovementTargets;
import hartu.robot.commands.ParsedCommand;
import hartu.robot.commands.positions.AxisPosition;
import hartu.robot.commands.positions.PositionClass;
import hartu.robot.communication.server.CommandQueue;
import hartu.robot.communication.server.CommandResultHolder;
import hartu.robot.communication.server.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class testin extends RoboticsAPIApplication {

    @Inject
    private Controller robotController;
    @Inject
    private LBR iiwa;
    @Inject
    private IOFlangeIOGroup gimaticIO;
    @Inject
    private Ethercat_x44IOGroup toolControlIO;

    @Override
    public void initialize() {
        Logger.getInstance().log("ROBOT_EXEC", "Initializing. Ready to take commands from queue.");
    }

    @Override
    public void run() {
        while (true) {
            CommandResultHolder resultHolder = CommandQueue.pollCommand(100, TimeUnit.MILLISECONDS);

            if (resultHolder != null) {
                ParsedCommand command = resultHolder.getCommand();
                Logger.getInstance().log(
                        "ROBOT_EXEC",
                        "Received command ID " + command.getId() + " from queue for execution."
                );
                boolean executionSuccess = false;

                try {
                    switch (command.getCommandCategory()) {
                        case MOVEMENT:
                            executionSuccess = executeMovementCommand(command);
                            break;
                        case IO:
                            //executionSuccess = executeIO(command);
                            break;
                        case PROGRAM_CALL:
                            //executionSuccess = executeProgramCallCommand(command);
                            break;
                        case UNKNOWN:
                        default:
                            Logger.getInstance().warn("ROBOT_EXEC", "Unknown or unsupported primary command category for ID " + command.getId() + ": " + command.getCommandCategory().name());
                            executionSuccess = false;
                            break;
                    }
                } catch (Exception e) {
                    Logger.getInstance().error(
                            "ROBOT_EXEC",
                            "Error: Exception during command execution for ID " + command.getId() + ": " + e.getMessage()
                    );
                    executionSuccess = false;
                } finally {
                    resultHolder.setSuccess(executionSuccess);
                    resultHolder.getLatch().countDown();
                    Logger.getInstance().log(
                            "ROBOT_EXEC",
                            "Signaled completion for command ID " + command.getId() + ". Success: " + executionSuccess
                    );
                }
            }
        }
    }

    private boolean executeMovementCommand(ParsedCommand command)
    {
        MotionParameters motionParams = command.getMotionParameters();
        double speed = motionParams.getSpeedOverride();
        String commandId = command.getId();
        ActionTypes actionType = command.getActionType();
        MovementType movementType = MovementType.fromActionType(actionType);

        Logger.getInstance().log("ROBOT_EXEC", "Executing " + actionType.name() + " command ID " + commandId + " with speed override: " + speed);

        List<IMotion> motionsToExecute = new ArrayList<>();

        for (AxisPosition targetPoint : command.getAxisTargetPoints())
        {
            motionsToExecute.add(ptp(targetPoint.toJointPosition()).setBlendingRel(0.5));
        }

        List<IMotionContainer> motionContainer;

        for (IMotion motion : motionsToExecute)
        {
            RobotMotion<?> robotMotion = (RobotMotion<?>) motion;
            motionContainer = Collections.singletonList(iiwa.moveAsync(robotMotion.setJointVelocityRel(speed)));
        }

        return true;

    }

    @Override
    public void dispose()
    {
        Logger.getInstance().log("ROBOT_EXEC", "Disposing CommandExecutor.");
        super.dispose();
    }
}


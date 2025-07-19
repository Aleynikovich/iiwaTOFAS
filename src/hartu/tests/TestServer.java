package hartu.tests;


import javax.inject.Inject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPIBackgroundTask;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.applicationModel.tasks.UseRoboticsAPIContext;
import com.kuka.roboticsAPI.controllerModel.Controller;
import hartu.robot.communication.server.*;

/**
 * Implementation of a cyclic background task.
 * <p>
 * It provides the {@link RoboticsAPICyclicBackgroundTask#runCyclic} method 
 * which will be called cyclically with the specified period.<br>
 * Cycle period and initial delay can be set by calling 
 * {@link RoboticsAPICyclicBackgroundTask#initializeCyclic} method in the 
 * {@link RoboticsAPIBackgroundTask#initialize()} method of the inheriting 
 * class.<br>
 * The cyclic background task can be terminated via 
 * {@link RoboticsAPICyclicBackgroundTask#getCyclicFuture()#cancel()} method or 
 * stopping of the task.
 * @see UseRoboticsAPIContext
 * 
 */
public class TestServer extends RoboticsAPICyclicBackgroundTask {
	@Inject
	Controller kUKA_Sunrise_Cabinet_1;
	
	private ServerClass robotCommunicationServer;
	private static final int TASK_PORT = 30001;
    private static final int LOG_PORT = 40001;
	@Override
	public void initialize() {
		// initialize your task here
		initializeCyclic(0, 500, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);
		
        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try
                {
                    robotCommunicationServer = new ServerClass(TASK_PORT, LOG_PORT);
                } catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
                robotCommunicationServer.start();
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
    }

	@Override
	public void runCyclic() {

	}
}
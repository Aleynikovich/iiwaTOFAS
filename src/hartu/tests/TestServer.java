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
    private static final int LOG_PORT = 30002;
	@Override
	public void initialize() {
		// initialize your task here
		initializeCyclic(0, 1000, TimeUnit.MILLISECONDS,
				CycleBehavior.BestEffort);
		
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

    @Override
    public void dispose() {
        // This method is called when the background task is terminated.
        // It's the perfect place to clean up resources.
        if (robotCommunicationServer != null) {
            try {
                robotCommunicationServer.stop();
            } catch (IOException e) {
                // Re-throwing as RuntimeException to align with previous error handling style.
                // In a production robot system, you would want robust error logging here.
                throw new RuntimeException("Error stopping robot communication server: " + e.getMessage(), e);
            }
        }
        super.dispose(); // Call the superclass's dispose method
    }
}
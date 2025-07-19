package hartu.tests;


import javax.inject.Inject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
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
	private static final int SERVER_PORT = 30003;
	
	@Override
	public void initialize() {
		// initialize your task here
		initializeCyclic(0, 500, TimeUnit.MILLISECONDS,
				CycleBehavior.BestEffort);
		
        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    robotCommunicationServer = new ServerClass(SERVER_PORT);
                    robotCommunicationServer.start();
                } catch (IOException e) {
                    // Handle the exception if the server fails to start.
                    // In a real robot application, you'd want robust logging here.
                    System.err.println("Failed to start robot communication server: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
        serverThread.setDaemon(true); // Set as daemon so it doesn't prevent JVM exit
        serverThread.start(); // Start the server thread
    }

	@Override
	public void runCyclic() {
		// your task execution starts here
	}
}
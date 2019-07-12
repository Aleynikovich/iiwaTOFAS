package application;


import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.controllerModel.Controller;

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
public class DataSinchronizer extends RoboticsAPICyclicBackgroundTask {
	
	
	@Inject
	Controller controller;
	
	//Media flange instance
    @Inject
	private MediaFlangeIOGroup mediaFIO;
	
	@Override
	public void initialize() {
		// initialize your task here
		initializeCyclic(0, 1000, TimeUnit.MILLISECONDS,CycleBehavior.BestEffort);
	}

	@Override
	public void runCyclic() {
		if(SharedData.sinc_data)
		{	
			if(mediaFIO.getLEDBlue())
			{	
				//mediaFIO.setOutputX3Pin1(false);
				mediaFIO.setLEDBlue(false);
			}
			else
			{
				mediaFIO.setLEDBlue(true);
				//mediaFIO.setOutputX3Pin1(true);	
			}
		}
	}
}
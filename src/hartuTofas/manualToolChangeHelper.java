package hartuTofas;


import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import com.kuka.generated.ioAccess.IOFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.LBR;

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
public class manualToolChangeHelper extends RoboticsAPICyclicBackgroundTask {
	@Inject
	Controller kUKA_Sunrise_Cabinet_1;
	@Inject
	private LBR iiwa;
	@Inject
	private IOFlangeIOGroup gimatic;

	@Override
	public void initialize() {
		// initialize your task here
		initializeCyclic(0, 500, TimeUnit.MILLISECONDS,
				CycleBehavior.BestEffort);
	}

	@Override
	public void runCyclic() {
		// your task execution starts here
		if (gimatic.getDO_Flange8()){
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			gimatic.setDO_Flange7(true);
		}
		
		if (!gimatic.getDO_Flange8()){
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			gimatic.setDO_Flange7(false);
		}		
	}
}
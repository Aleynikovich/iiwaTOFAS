package hartuTofas;


import javax.inject.Inject;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.motionModel.BasicMotions;

/**
 * Implementation of a robot application.
 * <p>
 * The application provides a {@link RoboticsAPITask#initialize()} and a 
 * {@link RoboticsAPITask#run()} method, which will be called successively in 
 * the application lifecycle. The application will terminate automatically after 
 * the {@link RoboticsAPITask#run()} method has finished or after stopping the 
 * task. The {@link RoboticsAPITask#dispose()} method will be called, even if an 
 * exception is thrown during initialization or run. 
 * <p>
 * <b>It is imperative to call <code>super.dispose()</code> when overriding the 
 * {@link RoboticsAPITask#dispose()} method.</b> 
 * 
 * @see UseRoboticsAPIContext
 * @see #initialize()
 * @see #run()
 * @see #dispose()
 */
public class AAHartuTesting extends RoboticsAPIApplication {
	@Inject
	private LBR iiwa;

	@Override
	public void initialize() {
		// initialize your application here
	}

	@Override
	public void run() {
		// your application execution starts here
		iiwa.move(ptpHome());
		for (int i = 0; i <10; i++){
			iiwa.moveAsync(BasicMotions.ptp(0.75049, 0.59341, 0.92502, 0.59341, 0.40143, 0.59341, 0.75049));
			iiwa.moveAsync(BasicMotions.ptp(0.40143, 1.30899, 0.59341, 0.36652, 0.92502, 0.05236, 0.05236));

		}
	}
}
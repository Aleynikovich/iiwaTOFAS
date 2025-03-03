package hartuTofas;


import javax.inject.Inject;

import com.kuka.common.ThreadUtil;
import com.kuka.generated.ioAccess.Ethercat_x44IOGroup;
import com.kuka.generated.ioAccess.IOFlangeIOGroup;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import com.kuka.roboticsAPI.deviceModel.LBR;

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


public class AApickPlace extends RoboticsAPIApplication {
	@Inject
	private LBR lBR_iiwa_14_R820_1;

	@Inject
	private IOFlangeIOGroup flangeBeckhoffIO;
	
	@Inject
	private MediaFlangeIOGroup mediaFlangeIO;
	
	@Inject
	private Ethercat_x44IOGroup X44BeckhoffIO;
	
	@Override
	public void initialize() {
		// initialize your application here
	}

	@Override
	public void run() {
		// your application execution starts here
		lBR_iiwa_14_R820_1.move(ptpHome());
		ThreadUtil.milliSleep(1000);
		flangeBeckhoffIO.setDO_Flange7(true);
		
	}
}
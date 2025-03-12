package hartuTofas;


import javax.inject.Inject;

import com.kuka.common.ThreadUtil;
import com.kuka.generated.ioAccess.Ethercat_x44IOGroup;
import com.kuka.generated.ioAccess.IOFlangeIOGroup;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Tool;

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


public class AAplaceTool1 extends RoboticsAPIApplication {
	@Inject
	private LBR iiwa;
	private Tool gimaticIxtur;
	
	@Inject
	private IOFlangeIOGroup gimaticIO;
	
	@Override
	public void initialize() {
		// initialize your application here
		gimaticIxtur = createFromTemplate("GimaticIxtur");
	}

	@Override
	public void run() {
		// your application execution starts here
		gimaticIxtur.attachTo(iiwa.getFlange());
		gimaticIO.setDO_Flange7(true);
		ThreadUtil.milliSleep(200);
		iiwa.move(ptp(getApplicationData().getFrame("/ATOFAS/PrepickTool")));
		iiwa.move(lin(getApplicationData().getFrame("/ATOFAS/PickTool")));
		gimaticIO.setDO_Flange7(false);
		ThreadUtil.milliSleep(200);
		iiwa.move(lin(getApplicationData().getFrame("/ATOFAS/P11")));
		iiwa.move(lin(getApplicationData().getFrame("/ATOFAS/P12")));
		iiwa.move(lin(getApplicationData().getFrame("/ATOFAS/P13")));
	}
}
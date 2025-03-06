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


public class AApickPlace extends RoboticsAPIApplication {
	@Inject
	private LBR iiwa;
	private Tool tool,tool1;
	
	@Inject
	private Ethercat_x44IOGroup X44BeckhoffIO;
	
	@Override
	public void initialize() {
		// initialize your application here
		
	}

	@Override
	public void run() {
		// your application execution starts here
		tool = createFromTemplate("GimaticIxtur");
		tool1 = createFromTemplate("RollScan"); 
		tool.attachTo(iiwa.getFlange());
		iiwa.move(ptpHome());
		iiwa.move(ptp(getApplicationData().getFrame("/ATOFAS/PickPlace/Prepick")));
		iiwa.move(ptp(getApplicationData().getFrame("/ATOFAS/PickPlace/P1")));
		iiwa.move(lin(getApplicationData().getFrame("/ATOFAS/PickPlace/P2")));
		X44BeckhoffIO.setOutput1(true);
		ThreadUtil.milliSleep(200);
		X44BeckhoffIO.setOutput1(false);
		tool.detach();
		tool = createFromTemplate("IxturPlatoGrande");
		tool.attachTo(iiwa.getFlange());
		iiwa.move(lin(getApplicationData().getFrame("/ATOFAS/PickPlace/P3")));
		iiwa.move(ptp(getApplicationData().getFrame("/ATOFAS/PickPlace/P4")));
		iiwa.move(lin(getApplicationData().getFrame("/ATOFAS/PickPlace/P5")));
		iiwa.move(lin(getApplicationData().getFrame("/ATOFAS/PickPlace/P6")));
		X44BeckhoffIO.setOutput2(true);
		ThreadUtil.milliSleep(200);
		X44BeckhoffIO.setOutput2(false);
		tool.detach();
		tool = createFromTemplate("GimaticIxtur");
		tool.attachTo(iiwa.getFlange());
		iiwa.move(lin(getApplicationData().getFrame("/ATOFAS/PickPlace/P7")));
		iiwa.move(lin(getApplicationData().getFrame("/ATOFAS/PickPlace/P8")));
	}
}
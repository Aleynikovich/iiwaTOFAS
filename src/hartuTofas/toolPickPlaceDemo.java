package hartuTofas;


import javax.inject.Inject;
import javax.inject.Named;

import com.kuka.generated.ioAccess.IOFlangeIOGroup;
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
public class toolPickPlaceDemo extends RoboticsAPIApplication {
	@Inject
	private LBR iiwa;
	@Inject
	private IOFlangeIOGroup gimatic;
	
	@Named("GimaticCamera")
	private Tool GimaticCamera;
	@Override
	public void initialize() {
		// initialize your application here
		GimaticCamera = createFromTemplate("GimaticCamera");
		
	}

	@Override
	public void run() {
		// your application execution starts here
		GimaticCamera = createFromTemplate("GimaticCamera");
		//Tool 1
		GimaticCamera.attachTo(iiwa.getFlange());
		gimatic.setDO_Flange7(true); //Open
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		iiwa.move(lin((getApplicationData().getFrame("/TofasBase/Kitting/Tool1/P10"))));
		iiwa.move(lin((getApplicationData().getFrame("/TofasBase/Kitting/Tool1/P1"))));
		gimatic.setDO_Flange7(false); //Close
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		iiwa.move(lin((getApplicationData().getFrame("/TofasBase/Kitting/Tool1/P2"))));
		iiwa.move(lin((getApplicationData().getFrame("/TofasBase/Kitting/Tool1/P3"))));
		iiwa.move(lin((getApplicationData().getFrame("/TofasBase/Kitting/Tool1/P4"))));
		iiwa.move(lin((getApplicationData().getFrame("/TofasBase/Kitting/Tool1/P5"))));
		iiwa.move(lin((getApplicationData().getFrame("/TofasBase/Kitting/Tool1/P4"))));
		iiwa.move(lin((getApplicationData().getFrame("/TofasBase/Kitting/Tool1/P3"))));
		iiwa.move(lin((getApplicationData().getFrame("/TofasBase/Kitting/Tool1/P2"))));
		iiwa.move(lin((getApplicationData().getFrame("/TofasBase/Kitting/Tool1/P1"))));
		gimatic.setDO_Flange7(true); //Open
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		iiwa.move(lin((getApplicationData().getFrame("/TofasBase/Kitting/Tool1/P10"))));
		
		//Tool 2
		gimatic.setDO_Flange7(true); //Open
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		iiwa.move(lin((getApplicationData().getFrame("/TofasBase/Kitting/Tool2/P10"))));
		iiwa.move(lin((getApplicationData().getFrame("/TofasBase/Kitting/Tool2/P1"))));
		gimatic.setDO_Flange7(false); //Close
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		iiwa.move(lin((getApplicationData().getFrame("/TofasBase/Kitting/Tool2/P2"))));
		iiwa.move(lin((getApplicationData().getFrame("/TofasBase/Kitting/Tool2/P3"))));
		iiwa.move(lin((getApplicationData().getFrame("/TofasBase/Kitting/Tool2/P2"))));
		iiwa.move(lin((getApplicationData().getFrame("/TofasBase/Kitting/Tool2/P1"))));
		gimatic.setDO_Flange7(true); //Open
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		iiwa.move(lin((getApplicationData().getFrame("/TofasBase/Kitting/Tool1/P10"))));
		
		//Tool 3
		
		gimatic.setDO_Flange7(true); //Open
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		iiwa.move(lin((getApplicationData().getFrame("/TofasBase/Kitting/Tool3/P10"))));
		iiwa.move(lin((getApplicationData().getFrame("/TofasBase/Kitting/Tool3/P1"))));
		gimatic.setDO_Flange7(false); //Close
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		iiwa.move(lin((getApplicationData().getFrame("/TofasBase/Kitting/Tool3/P2"))));
		iiwa.move(lin((getApplicationData().getFrame("/TofasBase/Kitting/Tool3/P3"))));
		iiwa.move(lin((getApplicationData().getFrame("/TofasBase/Kitting/Tool3/P2"))));
		iiwa.move(lin((getApplicationData().getFrame("/TofasBase/Kitting/Tool3/P1"))));
		gimatic.setDO_Flange7(true); //Open
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		iiwa.move(lin((getApplicationData().getFrame("/TofasBase/Kitting/Tool3/P10"))));
		
		
		
	}
}
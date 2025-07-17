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
public class Demo extends RoboticsAPIApplication {
	@Inject
	private LBR iiwa;
	@Inject
	private IOFlangeIOGroup gimatic;
	@Named("GimaticIxtur")
	private Tool GimaticIxtur;
	@Named("GimaticGripperV")
	private Tool GimaticGripperV;
	
	@Override
	public void initialize() {
		// initialize your application here
		GimaticIxtur = createFromTemplate("GimaticIxtur");
		GimaticGripperV = createFromTemplate("GimaticGripperV");
	}

	@Override
	public void run() {
		// your application execution starts here
		GimaticIxtur.attachTo(iiwa.getFlange());
		
		
		
		
		
	}
}
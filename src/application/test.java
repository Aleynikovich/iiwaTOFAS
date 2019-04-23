package application;


import javax.inject.Inject;

import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;

import com.kuka.roboticsAPI.conditionModel.BooleanIOCondition;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.executionModel.IFiredConditionInfo;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;

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
public class test extends RoboticsAPIApplication implements ISignalListener {
	@Inject
	private LBR lbr;
    private Tool roll_scan;
    
    @Inject
    private MediaFlangeIOGroup mediaFIO;

	@Override
	public void initialize() {
		// initialize your application here
		roll_scan = createFromTemplate("RollScan");
		roll_scan.attachTo(lbr.getFlange());
	}

	@Override
	public void run() {
		// your application execution starts here
		//lBR_iiwa_14_R820_1.move(ptpHome());
		
		
		BooleanIOCondition switch1_active = new BooleanIOCondition(mediaFIO.getInput("InputX3Pin3"), true);
				 
		//IMotionContainer motionCmd =
		//roll_scan.getFrame("Gripper").move(ptp(getFrame("/robot_base/SafePos")).breakWhen(switch1_active));
		IMotionContainer motionCmd =
			roll_scan.getFrame("Gripper").moveAsync(ptp(getFrame("/robot_base/SafePos")));
		
		/*IFiredConditionInfo firedInfo =  motionCmd.getFiredBreakConditionInfo();
				 
		 if(firedInfo != null){
		  getLogger().info("pulsador 1 ");
		 }
		 */
		

	}
	
	@Override
	public void OnSignalReceived(Boolean data) {
		// TODO Auto-generated method stub
		
		System.out.println("Boton pulsado");
		
	}
}
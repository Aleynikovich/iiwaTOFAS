package application;


import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import com.kuka.common.ThreadUtil;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;

import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.controllerModel.sunrise.ResumeMode;
import com.kuka.roboticsAPI.controllerModel.sunrise.SunriseExecutionService;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.deviceModel.LBRE1Redundancy;
import com.kuka.roboticsAPI.geometricModel.CartDOF;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.math.XyzAbcTransformation;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.motionModel.MotionBatch;
import com.kuka.roboticsAPI.motionModel.Spline;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianImpedanceControlMode;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianSineImpedanceControlMode;
import com.kuka.roboticsAPI.sensorModel.DataRecorder;
import com.kuka.roboticsAPI.sensorModel.ForceSensorData;
import com.kuka.roboticsAPI.uiModel.ApplicationDialogType;

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
//public class AleronTest extends RoboticsAPIApplication implements ITCPListener {
	public class GreenPatrolConsumptionTest extends RoboticsAPIApplication   {

		@Inject
		private LBR lbr;
	    //private Tool flange;
	    private Tool roll_scan;
		
		@Override
		public void initialize() {
			// initialize your application here
			//flange = createFromTemplate("Flange");
			//flange.attachTo(lbr.getFlange());
			roll_scan = createFromTemplate("RollScan");
			roll_scan.attachTo(lbr.getFlange());

			System.out.println("Roll scan frame: " + roll_scan.getFrame("roll_tcp").toString());

			roll_scan.getLoadData().setMass(2.82);
			roll_scan.getLoadData().setCenterOfMass(-0.0076*1000, 0.00473*1000, 0.12047*1000);
			
		}

		@Override
		public void run() {
			// your application execution starts here
			//lbr.move(ptpHome());
			
			roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));
			int counter = 0;
			
			do {

				roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/Greenpatrol/ApplicationPoses/High1_up")).setJointVelocityRel(0.75));
				roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/Greenpatrol/ApplicationPoses/High1_bottom")).setJointVelocityRel(0.75));
				counter++;
				System.out.println(counter );

			} while (counter<10);
				
			roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));

		}
			
	}
	
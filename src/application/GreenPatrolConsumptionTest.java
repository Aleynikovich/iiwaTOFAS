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
	    private Tool flange;
	    private Frame up_fr;
		private Frame down_fr;
	    boolean exit;
	    private double[] gripper_tool_xyz = new double[]{0,0,0.26448};
		private double[] gripper_tool_rpy = new double[]{0.0,0,-Math.PI/2};
		int forces;
		String fname;
		
	    CartesianImpedanceControlMode impedanceControlMode;
	    CartesianImpedanceControlMode impedanceControlModeD;
	    
		private static final int stiffnessZ = 300;
		private static final int stiffnessY = 5000;
		private static final int stiffnessX = 5000;
		
		boolean pivota;
		double select_velocity;
		double overlap, overlapt;
		DataRecorder rec;
		@Override
		public void initialize() {
			// initialize your application here
			overlap=0.06*1000;
			overlapt=0;
			flange = createFromTemplate("Flange");
			//flange.changeFramePosition(flange.getFrame("Gripper"), XyzAbcTransformation.ofRad(gripper_tool_xyz[0]*1000, 
		    //gripper_tool_xyz[1]*1000, gripper_tool_xyz[2]*1000, gripper_tool_rpy[2], gripper_tool_rpy[1], gripper_tool_rpy[0]));
			flange.attachTo(lbr.getFlange());
			
			System.out.println("Roll scan frame: " + flange.getFrame("roll_tcp").toString());

			flange.getLoadData().setMass(2.82);
			flange.getLoadData().setCenterOfMass(-0.0076*1000, 0.00473*1000, 0.12047*1000);
			
		}

		@Override
		public void run() {
			// your application execution starts here
			//lbr.move(ptpHome());
			
			flange.getFrame("Flange").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));
			int counter = 0;
			
			do {

				flange.getFrame("Flange").move(ptp(getFrame("/Greenpatrol/ApplicationPoses/High1_up")).setJointVelocityRel(0.75));
				flange.getFrame("Flange").move(ptp(getFrame("/Greenpatrol/ApplicationPoses/High1_bottom")).setJointVelocityRel(0.75));
				counter++;
				System.out.println(counter );

			} while (counter<10);
				
			flange.getFrame("Flange").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));

		}
			
	}
	
package application;


import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;

import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.deviceModel.LBRE1Redundancy;
import com.kuka.roboticsAPI.geometricModel.CartDOF;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.math.XyzAbcTransformation;
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
public class ForceTest extends RoboticsAPIApplication {
	@Inject
	private LBR lbr;
    private Tool roll_scan;
    
    boolean exit;
    private double[] gripper_tool_xyz = new double[]{0,0,0.26448};
	private double[] gripper_tool_rpy = new double[]{0.0,0,-Math.PI/2};
	int forces;
	String fname;
	
    CartesianImpedanceControlMode impedanceControlMode;
    
    private static final int stiffnessZ = 300;
	private static final int stiffnessY = 5000;
	private static final int stiffnessX = 5000;
	
	DataRecorder rec;
	@Override
	public void initialize() {
		// initialize your application here
		
		roll_scan = createFromTemplate("Tool");
		roll_scan.attachTo(lbr.getFlange());
		
		System.out.println("Roll scan frame: " + roll_scan.getFrame("flange_2_tool").toString());

		roll_scan.getLoadData().setMass(2.07);
		roll_scan.getLoadData().setCenterOfMass(1.36,2.68,42.81);

				
		impedanceControlMode = 	new CartesianImpedanceControlMode();
	
	}

	@Override
	public void run() {
		// your application execution starts here
		
		rec = new DataRecorder();
	 	rec.setFileName("force_monitoring_test.log");
		rec.addCartesianForce(roll_scan.getFrame("flange_2_tool"),roll_scan.getFrame("flange_2_tool"));
		rec.addCurrentCartesianPositionXYZ(roll_scan.getFrame("flange_2_tool"), getApplicationData().getFrame("/robot_base"));
	 
		rec.enable();
		rec.startRecording();
		
		roll_scan.getFrame("flange_2_tool").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));

		JointPosition joints = lbr.getCurrentJointPosition();
		
		joints.set(0, 160*Math.PI/180);
		joints.set(1, -49.35*Math.PI/180);
		joints.set(2, 0*Math.PI/180);
		joints.set(3, -22.28*Math.PI/180);
		joints.set(4, 0.0*Math.PI/180);
		joints.set(5, -51.79*Math.PI/180);
		joints.set(6, 0.0*Math.PI/180);

		
		switch (getApplicationUI().displayModalDialog(
				ApplicationDialogType.QUESTION,"Which movement type do I perform? ", 
				"Normal", "Impendance")) {

				case 0:
				
					lbr.move(ptp(joints).setJointVelocityRel(1.0));
		
					joints.set(0, -160*Math.PI/180);
		
					lbr.move(ptp(joints).setJointVelocityRel(1.0));
		
					joints.set(0, 0);
		
					lbr.move(ptp(joints).setJointVelocityRel(1.0));

				case 1:
				
					impedanceControlMode.setMaxCartesianVelocity(1000.0,1000.0,1000.0,Math.toRadians(60),Math.toRadians(60),Math.toRadians(60));
					impedanceControlMode.setSpringPosition(roll_scan.getFrame("flange_2_tool"));
					impedanceControlMode.parametrize(CartDOF.X).setStiffness(stiffnessX).setDamping(0.7);
					impedanceControlMode.parametrize(CartDOF.Y).setStiffness(stiffnessY).setDamping(0.7);
					impedanceControlMode.parametrize(CartDOF.Z).setStiffness(stiffnessZ).setDamping(0.7);
					impedanceControlMode.parametrize(CartDOF.ROT).setStiffness(300).setDamping(0.7);
						
				    lbr.move(ptp(joints).setJointVelocityRel(1.0).setMode(impedanceControlMode));
				    
				    joints.set(0, -Math.PI);
					
					lbr.move(ptp(joints).setJointVelocityRel(1.0).setMode(impedanceControlMode));
		
					joints.set(0, 0);
		
					lbr.move(ptp(joints).setJointVelocityRel(1.0).setMode(impedanceControlMode));
		}
		rec.stopRecording();

	}	
}
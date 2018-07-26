package application;


import java.text.DecimalFormat;

import com.kuka.common.ThreadUtil;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.CartDOF;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.ObjectFrame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.motionModel.IMotionOverlay;
import com.kuka.roboticsAPI.motionModel.PositionHold;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianImpedanceControlMode;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianSineImpedanceControlMode;
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
 * @see #initialize()
 * @see #run()
 * @see #dispose()
 */
public class ZeroGTestAitor extends RoboticsAPIApplication {
	
	// Robot & controller
	private Controller control;
	private LBR robot;
	
	// Tools
	private Tool toolSchunk;
	
	// Impedance control
	CartesianImpedanceControlMode softSpring,mediumSpring,hardSpring;
	CartesianSineImpedanceControlMode softSpringSin,mediumSpringSin,hardSpringSin;
	
	// Zero G
	CartesianImpedanceControlMode zeroG;
	
	// Helper functions
	private Frame getActualPosition(ObjectFrame refFrame) {
		return robot.getCurrentCartesianPosition(refFrame, getFrame("/MyWorld"));
	}
	
	double roundTwoDecimals(double d) { 
	      DecimalFormat twoDForm = new DecimalFormat("#.##"); 
	      return Double.valueOf(twoDForm.format(d));
	}
	
	//

	public void initialize() {
		control = getController("KUKA_Sunrise_Cabinet_1");
		robot = (LBR) getDevice(control,
				"LBR_iiwa_14_R820_1");
		
		// Init tool
		toolSchunk = createFromTemplate("Tool");
		toolSchunk.attachTo(robot.getFlange());
		
		// Init springs
		softSpring = new CartesianImpedanceControlMode();
		softSpring.parametrize(CartDOF.X).setStiffness(250).setDamping(0.7);
		softSpring.parametrize(CartDOF.Y).setStiffness(250).setDamping(0.7);
		softSpring.parametrize(CartDOF.Z).setStiffness(250).setDamping(0.7);
		softSpring.parametrize(CartDOF.A).setStiffness(25).setDamping(0.7);
		softSpring.parametrize(CartDOF.B).setStiffness(25).setDamping(0.7);
		softSpring.parametrize(CartDOF.C).setStiffness(25).setDamping(0.7);
		
		mediumSpring = new CartesianImpedanceControlMode();
		mediumSpring.parametrize(CartDOF.X).setStiffness(500).setDamping(0.7);
		mediumSpring.parametrize(CartDOF.Y).setStiffness(500).setDamping(0.7);
		mediumSpring.parametrize(CartDOF.Z).setStiffness(500).setDamping(0.7);
		mediumSpring.parametrize(CartDOF.A).setStiffness(50).setDamping(0.7);
		mediumSpring.parametrize(CartDOF.B).setStiffness(50).setDamping(0.7);
		mediumSpring.parametrize(CartDOF.C).setStiffness(50).setDamping(0.7);
		
		hardSpring = new CartesianImpedanceControlMode();
		hardSpring.parametrize(CartDOF.X).setStiffness(1000).setDamping(0.8);
		hardSpring.parametrize(CartDOF.Y).setStiffness(1000).setDamping(0.8);
		hardSpring.parametrize(CartDOF.Z).setStiffness(1000).setDamping(0.8);
		hardSpring.parametrize(CartDOF.A).setStiffness(100).setDamping(0.8);
		hardSpring.parametrize(CartDOF.B).setStiffness(100).setDamping(0.8);
		hardSpring.parametrize(CartDOF.C).setStiffness(100).setDamping(0.8);
		
		softSpringSin = new CartesianSineImpedanceControlMode();
		softSpringSin.parametrize(CartDOF.X).setStiffness(250).setDamping(0.7)
						.setAmplitude(5).setFrequency(1);
		softSpringSin.parametrize(CartDOF.Y).setStiffness(250).setDamping(0.7)
						.setAmplitude(5).setFrequency(1);
		softSpringSin.parametrize(CartDOF.Z).setStiffness(250).setDamping(0.7)
						.setAmplitude(0).setFrequency(0);
		softSpringSin.parametrize(CartDOF.A).setStiffness(25).setDamping(0.7)
						.setAmplitude(0).setFrequency(0);
		softSpringSin.parametrize(CartDOF.B).setStiffness(25).setDamping(0.7)
						.setAmplitude(0).setFrequency(0);
		softSpringSin.parametrize(CartDOF.C).setStiffness(25).setDamping(0.7)
						.setAmplitude(0).setFrequency(0);
		
		zeroG = new CartesianImpedanceControlMode();
		zeroG.setMaxCartesianVelocity(2000.0,2000.0,2000.0,Math.toRadians(120),Math.toRadians(120),Math.toRadians(120));
		zeroG.parametrize(CartDOF.X).setStiffness(0).setDamping(1.0);
		zeroG.parametrize(CartDOF.Y).setStiffness(0).setDamping(1.0);
		zeroG.parametrize(CartDOF.Z).setStiffness(0).setDamping(1.0);
		zeroG.parametrize(CartDOF.A).setStiffness(0).setDamping(1.0);
		zeroG.parametrize(CartDOF.B).setStiffness(0).setDamping(1.0);
		zeroG.parametrize(CartDOF.C).setStiffness(0).setDamping(1.0);	
		
	}

	public void run() {
		
		
		// Move home
		robot.setHomePosition(new JointPosition(-Math.PI/4,0,0,-Math.PI/2,0,Math.PI/2,-Math.PI/2));
		robot.move(ptpHome().setJointVelocityRel(0.35));
		
		getLogger().info("Robot in home position!!!");
		
		// Zero G
		IMotionContainer ZeroGContainer=toolSchunk.getFrame("/flange_2_tool").moveAsync((new PositionHold(zeroG, -1, null)));

		getApplicationUI().displayModalDialog(ApplicationDialogType.INFORMATION, "Pulsar OK para finalizar.", "OK");
		
		ThreadUtil.milliSleep(100);
		
		ZeroGContainer.cancel();
		
		getLogger().info("Program finished!!!");
		
	}

	/**
	 * Auto-generated method stub. Do not modify the contents of this method.
	 */
	public static void main(String[] args) {
		ZeroGTestAitor app = new ZeroGTestAitor();
		app.runApplication();
	}
}

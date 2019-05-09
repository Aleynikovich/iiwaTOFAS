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
	public class GreenPatrol extends RoboticsAPIApplication   {

		@Inject
		private LBR lbr;
	    private Tool roll_scan;
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
			roll_scan = createFromTemplate("RollScan");
			//roll_scan.changeFramePosition(roll_scan.getFrame("Gripper"), XyzAbcTransformation.ofRad(gripper_tool_xyz[0]*1000, 
		    //gripper_tool_xyz[1]*1000, gripper_tool_xyz[2]*1000, gripper_tool_rpy[2], gripper_tool_rpy[1], gripper_tool_rpy[0]));
			roll_scan.attachTo(lbr.getFlange());
			
			System.out.println("Roll scan frame: " + roll_scan.getFrame("roll_tcp").toString());

			roll_scan.getLoadData().setMass(2.82);
			roll_scan.getLoadData().setCenterOfMass(-0.0076*1000, 0.00473*1000, 0.12047*1000);
			
		}

		@Override
		public void run() {
			// your application execution starts here
			//lbr.move(ptpHome());
			
			exit=false;
			
			do {
				rec = new DataRecorder();
				rec.setTimeout(2L, TimeUnit.MINUTES);
				roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));
				switch (getApplicationUI().displayModalDialog(
						ApplicationDialogType.QUESTION,"Which trajectory I have to do?", 
						"UpperZone", "MiddleZone", "DownZone", "All Zones", "END DO NOTHING")) {

						case 0:
							
							pivota=pivotaje();
							getLogger().info("Selected UpperZone and " + pivota);				

							if (pivota==false){
												
								UpperZone();
							}
							else if (pivota==true){
									UpperZoneP();
							}	
					
							roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));
							break;				
						case 1:
						
							pivota=pivotaje();
							getLogger().info("Selected MiddleZone and " + pivota);
							
							if (pivota==false){
								MiddleZone();					

							}
							else if (pivota==true){
								MiddleZoneP();
							}	
					

							roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));			
							break;					
						case 2:
							
							pivota=pivotaje();
							getLogger().info("Selected DownZoneZone and " + pivota);
								if (pivota==false){
									DownZone();			

							}
							else if (pivota==true){
									DownZoneP();
							}	
					
								roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));
							break;			
						case 3:
						
							pivota=pivotaje();
							getLogger().info("Selected AllZones and " + pivota);
								if (pivota==false){
									UpperZone();	
									MiddleZone();
									DownZone();

							}
							else if (pivota==true){
									UpperZoneP();	
									MiddleZoneP();
									DownZoneP();

							}	
					
								roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));
							break;			
						case 4:

							getLogger().info("App Terminated\n"+"***END***");
							exit = true;
							break;
				}
			} while (!exit);
				
		}
			
			
		private void UpperZone(){
			
			//velocidad movimientos 0.25 para ptp 50  lin
			//roll_scan.getFrame("Gripper").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));
			roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/Greenpatrol/P1")).setJointVelocityRel(0.25));
			
			roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/Greenpatrol/P4")).setJointVelocityRel(0.25));
			
			roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/Greenpatrol/P7")).setJointVelocityRel(0.25));
			
			roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/Greenpatrol/P10")).setJointVelocityRel(0.25));
			
		}

		private void UpperZoneP(){
			//velocidad movimientos 0.25 para ptp 50  lin
			//roll_scan.getFrame("Gripper").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));
			
			
			
		}
		


	private void MiddleZone(){
		
		//velocidad movimientos 0.25 para ptp 50  lin
		roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/Greenpatrol/P13")).setJointVelocityRel(0.25));
		
		roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/Greenpatrol/P16")).setJointVelocityRel(0.25));
		
		roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/Greenpatrol/P19")).setJointVelocityRel(0.25));
		
		roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/Greenpatrol/P22")).setJointVelocityRel(0.25));
		
		
			
	}

	private void MiddleZoneP(){
		
		//velocidad movimientos 0.25 para ptp 50  lin
			roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));
			
	}

	private void DownZone(){
		
		//velocidad movimientos 0.25 para ptp 50  lin
		roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/Greenpatrol/P25")).setJointVelocityRel(0.25));
		
		roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/Greenpatrol/P28")).setJointVelocityRel(0.25));
		
		roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/Greenpatrol/P31")).setJointVelocityRel(0.25));
		
		roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/Greenpatrol/P34")).setJointVelocityRel(0.25));
		
		roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/Greenpatrol/P37")).setJointVelocityRel(0.25));
			
	}

	private void DownZoneP(){
	//velocidad movimientos 0.25 para ptp 50  lin
			roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));
			
	}


	private boolean pivotaje(){

			boolean pivot=false;
		
		switch (getApplicationUI().displayModalDialog(
				ApplicationDialogType.QUESTION,"Do you want to pivot during the path?",
				"YES", "NO")) {

				case 0:
					pivot=true;
					break;				
				case 1:
					pivot=false;				
					break;					
			
		}
		return pivot;
	}
	}
	
package application;

import static com.kuka.roboticsAPI.motionModel.BasicMotions.lin;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.ptp;

/*import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
*/

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;


import com.kuka.common.ThreadUtil;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.conditionModel.BooleanIOCondition;
import com.kuka.roboticsAPI.deviceModel.Device;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.deviceModel.LBRE1Redundancy;
import com.kuka.roboticsAPI.executionModel.CommandInvalidException;
import com.kuka.roboticsAPI.executionModel.ExecutionState;
import com.kuka.roboticsAPI.executionModel.IFiredConditionInfo;
import com.kuka.roboticsAPI.geometricModel.CartDOF;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;
import com.kuka.roboticsAPI.geometricModel.math.XyzAbcTransformation;
import com.kuka.roboticsAPI.motionModel.ErrorHandlingAction;
import com.kuka.roboticsAPI.motionModel.IErrorHandler;
import com.kuka.roboticsAPI.motionModel.IMotion;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.motionModel.PositionHold;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianImpedanceControlMode;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianSineImpedanceControlMode;
import com.kuka.roboticsAPI.sensorModel.DataRecorder;
//import com.kuka.roboticsAPI.sensorModel.ForceSensorData;
import com.kuka.roboticsAPI.uiModel.ApplicationDialogType;

public class AleronDemoCollaboration extends RoboticsAPIApplication implements ITCPListener{
	
	@Inject
	private LBR lbr;
    private Tool roll_scan;
    boolean exit;
    
    String fname;
    FileWriter fw;

    CartesianImpedanceControlMode gravityCompensatioMode;
    CartesianImpedanceControlMode impedanceControlMode;
	
    private static final int stiffnessZ = 300;
	private static final int stiffnessY = 5000;
	private static final int stiffnessX = 5000;
	private static final int stiffnessRot = 300;

	
	
	private TCPServer tcp_server;
	AtomicBoolean data_received;
	
	//Exchanged data info 
	String operation_type;
	String time_stamp;
	int frame_id;
	Frame caltab_robot_fr;
		
	//Movement execution state
	AtomicBoolean movement_failed;
	AtomicInteger failed_movement_nbr;
	
	
	//Media flange instance
    @Inject
    private MediaFlangeIOGroup mediaFIO;
    
	double select_velocity;
    
	
	@Override
	public void initialize() {
		
		// initialize your application here
		roll_scan = createFromTemplate("RollScan");
		roll_scan.attachTo(lbr.getFlange());
		
		System.out.println("Roll scan frame: " + roll_scan.getFrame("roll_tcp").toString());

		data_received = new AtomicBoolean(false);
		
		movement_failed = new AtomicBoolean(false);
		failed_movement_nbr = new AtomicInteger(0);
		
				
		try {
			fw = new FileWriter("C:\\Users\\KukaUser\\Desktop\\logs\\failed_movements.txt");
		} catch (IOException e1) {
			System.out.println("Error creating log file: " + e1);
		}
      
	
		//Application TCPServer object
		try {
			tcp_server = new TCPServer();
				
			tcp_server.addListener(this);
			tcp_server.enable();
					
		} catch (IOException e) {
			//TODO Bloque catch generado automáticamente
			System.err.println("Could not create TCPServer:" +e.getMessage());
		}
		
		//Media flange management
		mediaFIO.setLEDBlue(true);
				
		// Init springs
		gravityCompensatioMode = new CartesianImpedanceControlMode();
		gravityCompensatioMode.setMaxCartesianVelocity(500.0,500.0,500.0,Math.toRadians(120),Math.toRadians(120),Math.toRadians(120));
		gravityCompensatioMode.parametrize(CartDOF.X).setStiffness(0).setDamping(1.0);
		gravityCompensatioMode.parametrize(CartDOF.Y).setStiffness(0).setDamping(1.0);
		gravityCompensatioMode.parametrize(CartDOF.Z).setStiffness(0).setDamping(1.0);
		gravityCompensatioMode.parametrize(CartDOF.A).setStiffness(0).setDamping(1.0);
		gravityCompensatioMode.parametrize(CartDOF.B).setStiffness(0).setDamping(1.0);
		gravityCompensatioMode.parametrize(CartDOF.C).setStiffness(0).setDamping(1.0);	

				
	} 
	
	@Override
	public void run() {	
		
		// your application execution starts here
		//lbr.move(ptpHome());
		roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/DemoCroinspect/SafePose")).setJointVelocityRel(0.25));
		
		exit=false;
		
		do {
			
			if(data_received.get())
			{
				data_received.set(false);
				
				System.out.println("Type:" + operation_type);
			
				if(operation_type.compareTo("manual_inspection") == 0)
				{
					
					// Zero G
					IMotionContainer ZeroGContainer=roll_scan.getFrame("/roll_tcp").moveAsync((new PositionHold(gravityCompensatioMode, -1, null)));

					getApplicationUI().displayModalDialog(ApplicationDialogType.INFORMATION, "Push OK to finish", "OK");
					
					ThreadUtil.milliSleep(100);
					
					ZeroGContainer.cancel();

					String response_data = frame_id + ";" + operation_type + ";1" ;
					tcp_server.setResponseData(response_data);
				}
				else if (operation_type.compareTo("collaborative_inspection") == 0)
				{
					
					// Zero G
					IMotionContainer ZeroGContainer=roll_scan.getFrame("/roll_tcp").moveAsync((new PositionHold(gravityCompensatioMode, -1, null)));

					getApplicationUI().displayModalDialog(ApplicationDialogType.INFORMATION, "Push OK to finish", "OK");
					
					ThreadUtil.milliSleep(100);
					
					ZeroGContainer.cancel();

					switch (getApplicationUI().displayModalDialog(
						ApplicationDialogType.QUESTION,"How many Force do I have to do?", 
							"10N", "15N", "20N", "24N", "END DO NOTHING")) {
		
							case 0:
								
								select_velocity=velocity();
								getLogger().info("Selected 10N and " + select_velocity + "mm/s");
								//Force_10N(select_velocity);
		
								
								fname="measured_force_10ND_stiffZ_300_"+select_velocity+"mm_S.log";
								try {
									Force_XND(10,fname,select_velocity);
								} catch (IOException e) {
									System.out.println("IO Exception in Force_XND 10");
								}
								
								break;				
							case 1:
								//15N=500*0.03
								select_velocity=velocity();
								getLogger().info("Selected 15N and " + select_velocity + "mm/s");
								
								
								fname="measured_force_15ND_stiffZ_500_"+select_velocity+"mm_S.log";
								try {
									Force_XND(15,fname,select_velocity);
								} catch (IOException e) {
									System.out.println("IO Exception in Force_XND 15");
								}	
						
								break;					
							case 2:
								//20N=500*0.04 REPASAR DESIRED
								select_velocity=velocity();
								getLogger().info("Selected 20N and " + select_velocity + "mm/s");
								
								
								fname="measured_force_20ND_stiffZ_500_"+select_velocity+"mm_S.log";
								try {
									Force_XND(20,fname,select_velocity);
								} catch (IOException e1) {
									System.out.println("IO Exception in Force_XND 20");

								}
															
								break;
							case 3:
								//24N=500*0.048
		
								select_velocity=velocity();
								getLogger().info("Selected 24N and mm/s: " + select_velocity + "mm/s");
								
							
								fname="measured_force_24ND_stiffZ_500_"+select_velocity+"mm_S.log";
								try {
									Force_XND(24,fname,select_velocity);
								} catch (IOException e) {
									System.out.println("IO Exception in Force_XND 24");
								}
								
								break;
						
							case 4:
								getLogger().info("App Terminated\n"+"***END***");
								
								try {
									closeCommunication();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								exit = true;
								break;
					}
				}
				else if(operation_type.compareTo("end") == 0)
				{
					
				}
			}
		} while (!exit);
		
	}
	
	private void closeCommunication() throws IOException
	{
		try {
			
			tcp_server.dispose();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			System.out.println("App InterruptedException");
		}
	}
	
	private double velocity(){

		double velocidad=0.0;
	
		switch (getApplicationUI().displayModalDialog(
			ApplicationDialogType.QUESTION,"Select velocity to do the Linear movements?", 
			"25 mm/s", "50 mm/s", "100 mm/s", "150 mm/s", "200 mm/s")) {

			case 0:
				velocidad=25;
				break;				
			case 1:
				velocidad=50;				
				break;					
			case 2:
				velocidad=100;
				break;
			case 3:
				velocidad=150;
				break;
			case 4:
				velocidad=200;
			
		}
		return velocidad;
	}
	
	//Rastering execution with force control
	private void Force_XND(int force, String nfichero, double velocidad ) throws IOException
	{
		
		//Impedance mode configuration
		impedanceControlMode= CartesianSineImpedanceControlMode.createDesiredForce(CartDOF.Z, force, stiffnessZ);
		
		impedanceControlMode.setMaxCartesianVelocity(1000.0,1000.0,1000.0,Math.toRadians(60),Math.toRadians(60),Math.toRadians(60));
		impedanceControlMode.setSpringPosition(roll_scan.getFrame("roll_tcp"));
		impedanceControlMode.parametrize(CartDOF.X).setStiffness(stiffnessX).setDamping(0.7);
		impedanceControlMode.parametrize(CartDOF.Y).setStiffness(stiffnessY).setDamping(0.7);
		impedanceControlMode.parametrize(CartDOF.Z).setStiffness(stiffnessZ).setDamping(0.7);
		impedanceControlMode.parametrize(CartDOF.ROT).setStiffness(stiffnessRot).setDamping(0.7);
	
		
		//Getting the current pose and executing a rescanning process
		Frame current_pos = lbr.getCurrentCartesianPosition(roll_scan.getFrame("roll_tcp"));
		
		for(int i=0; i<10; i++ )
		{
			
			System.out.println("Current point --> x: " + current_pos.getX() + " y: " + current_pos.getY() + " z: " + current_pos.getZ() + 
					" A: " + current_pos.getAlphaRad() + " B: " + current_pos.getBetaRad() + " C: " + current_pos.getGammaRad());
								
			Frame pose = current_pos.copy();
			pose.setGammaRad(current_pos.getGammaRad() + 30*Math.PI/180);
			
			System.out.println("First point --> x: " + pose.getX() + " y: " + pose.getY() + " z: " + pose.getZ() + 
				" A: " + pose.getAlphaRad() + " B: " + pose.getBetaRad() + " C: " + pose.getGammaRad());

			try
			{
				roll_scan.getFrame("roll_tcp").move(lin(pose).setCartVelocity(velocidad).setJointVelocityRel(0.25).setBlendingCart(0));//.setMode(impedanceControlMode)
			}
			catch(CommandInvalidException e)
			{
				fw.write(i + "Re-scan up movement " + pose.toString());
			}
			pose.setGammaRad(current_pos.getGammaRad() - 30*Math.PI/180); 
			System.out.println("Second point --> x: " + pose.getX() + " y: " + pose.getY() + " z: " + pose.getZ() + 
				" A: " + pose.getAlphaRad() + " B: " + pose.getBetaRad() + " C: " + pose.getGammaRad());
			
			try
			{
				roll_scan.getFrame("roll_tcp").move(lin(pose).setCartVelocity(velocidad).setJointVelocityRel(0.25).setBlendingCart(0));//.setMode(impedanceControlMode).setBlendingCart(0));
			}
			catch(CommandInvalidException e)
			{
				fw.write(i + "Re-scan down movement " + pose.toString());
			}
			
			
			pose = current_pos.copy();
			current_pos.setY(current_pos.getY() - i*10);
			try
			{
				roll_scan.getFrame("roll_tcp").move(lin(current_pos).setCartVelocity(velocidad).setJointVelocityRel(0.5).setBlendingCart(0));//.setMode(impedanceControlMode).setBlendingCart(0));
			}
			catch(CommandInvalidException e)
			{
				fw.write(i + "Returning to the " + i + " traj point" + current_pos.toString());
			}	
			
			
			current_pos.setY(current_pos.getY() - (i+1)*10);
			try
			{
				roll_scan.getFrame("roll_tcp").move(lin(current_pos).setCartVelocity(velocidad).setJointVelocityRel(0.25).setBlendingCart(0).setMode(impedanceControlMode).setBlendingCart(0));
			}
			catch(CommandInvalidException e)
			{
				fw.write(i + "Relative movement to next Re-scanning point " + current_pos.toString());
			}
	
		}
			
		System.out.println("Re-scanning done");
	}
	
	
	@Override
	public void OnTCPMessageReceived(String datagram)
	{
		System.out.println("OnTCPMessageReceived: " + datagram);

		String splittedData[] = datagram.split(";");
		
		time_stamp = splittedData[0];
		frame_id = Integer.parseInt(splittedData[1]);
		operation_type = splittedData[2];
	
		data_received.set(true);
	}


	@Override
	public void OnTCPConnection() {
		// TODO Auto-generated method stub
		
	}
}


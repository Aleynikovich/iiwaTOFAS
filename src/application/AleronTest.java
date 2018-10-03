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
	public class AleronTest extends RoboticsAPIApplication   {

	@Inject
	private LBR lbr;
	private SunriseExecutionService executionServiceLbr;
	private Controller sunrise;
    private Tool roll_scan;
    private Frame up_fr;
	private Frame down_fr;
	private Frame exit_fr;
	private double exit_x;
    boolean exit;
    private double[] gripper_tool_xyz = new double[]{0,0,0.26448};
	private double[] gripper_tool_rpy = new double[]{0.0,0,-Math.PI/2};
	private double[] joint_values = new double[7];
	private Frame framepos;
	double x_init,y_init,z_init;
	
	@Inject
	private MediaFlangeIOGroup mediaFIO;
	
	
	/*
	private TCPServer tcp_server;
	private AtomicBoolean last_pause_state;
	private double last_override_value;
*/
	int forces;
	String fname;
	
    CartesianImpedanceControlMode impedanceControlMode;
    CartesianImpedanceControlMode impedanceControlModeD;
    
	private static final int stiffnessZ = 300;
	private static final int stiffnessY = 5000;
	private static final int stiffnessX = 5000;
	
	protected static final long OVERRIDE_RAMP_TIME_LENGTH_MS = 200;
	protected static final int OVERRIDE_RAMP_NUM_STEPS = 10;
	
	double select_velocity;
	double overlap, overlapt;
	double x;
	DataRecorder rec;
	IMotionContainer motion;
	@Override
	public void initialize() {
		
		MediaFlangeIOGroup  FlangeIO= new  MediaFlangeIOGroup(sunrise);
		// initialize your application here
		overlap=0.06*1000;
		overlapt=0;
		roll_scan = createFromTemplate("RollScan");
		roll_scan.attachTo(lbr.getFlange());
		
		System.out.println("Roll scan frame: " + roll_scan.getFrame("Gripper").toString());
		
		roll_scan.getLoadData().setMass(2.82);
		roll_scan.getLoadData().setCenterOfMass(-0.0076*1000, 0.00473*1000, 0.12047*1000);
			
		impedanceControlMode = 	new CartesianImpedanceControlMode();
		impedanceControlModeD = 	new CartesianImpedanceControlMode();

		LBRE1Redundancy redundancyInfo = new LBRE1Redundancy(Math.toRadians(0.09), 2, 24);
	 	//Up measurement point frame
	 	up_fr = new Frame(getFrame("/aleron"));
		up_fr.setX(0.400*1000); up_fr.setY(0.074*1000); up_fr.setZ(0.0333*1000); 
	 	//up_fr.setAlphaRad(0.0); up_fr.setBetaRad(0.0); up_fr.setGammaRad(0.0);
	 	up_fr.setAlphaRad(1.6); up_fr.setBetaRad(0.0); up_fr.setGammaRad(0.0);
		up_fr.setRedundancyInformation(lbr, redundancyInfo);

	 	//downFrame
		down_fr = new Frame(getFrame("/aleron"));
		down_fr.setX(0.0); down_fr.setY(0.074*1000); down_fr.setZ(-3.0); 
	 	down_fr.setAlphaRad(1.6); down_fr.setBetaRad(0.0); down_fr.setGammaRad(0.0);
		down_fr.setRedundancyInformation(lbr, redundancyInfo);

		//TCPServer object
		/*
		try {
		tcp_server = new TCPServer();
		
		tcp_server.addListener(this);
		tcp_server.enable();
		last_pause_state = new AtomicBoolean(false);
		
		
		} catch (IOException e) {
			// TODO Bloque catch generado automáticamente
			System.err.println("Could not create TCPServer:" +e.getMessage());
		}

			*/	
	}

	@Override
	public void run() {
		// your application execution starts here
		//lbr.move(ptpHome());
		framepos=lbr.getCurrentCartesianPosition(roll_scan.getFrame("/Gripper"));
		x_init=framepos.getX();
		y_init=framepos.getY();
		z_init=framepos.getZ();
		getLogger().info("Actual Position: "+framepos);
		getLogger().info("****************************");
		getLogger().info("      Moving SafePos");
		getLogger().info("****************************");

	   
		exit=false;
		
		do {
			rec = new DataRecorder();
			rec.setTimeout(60L, TimeUnit.SECONDS);
			roll_scan.getFrame("Gripper").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));
			switch (getApplicationUI().displayModalDialog(
					ApplicationDialogType.QUESTION,"How many Force do I have to do?", 
					"10N", "15N", "20N", "24N", "35N", "100N (no usar metodo DESIRED)", "END DO NOTHING")) {

					case 0:
						//10=300*0.033330
						//10=1000*0.01
						forces=select_typeForce(10,300);
						mediaFIO.setLEDBlue(true);
						select_velocity=velocity();
						
						//Force_10N(select_velocity);

						if (forces==0){
							fname="measured_force_10N_stiffZ_1000_"+select_velocity+"mm_S.log";
							getLogger().info("Selected 10N and " + select_velocity + "mm/s");
							getLogger().info("Moving left part of the aleron");
							Force_XNL(0.03333,fname,select_velocity);
							getLogger().info("Moving right part of the aleron");
							roll_scan.getFrame("Gripper").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));
							//Force_XNR(0.03333,fname,select_velocity);
												}
						else if (forces==1){
							getLogger().info("Selected 10ND and " + select_velocity + "mm/s");
							fname="measured_force_10ND_stiffZ_1000_"+select_velocity+"mm_S.log";
							Force_XND(0.03333,fname,select_velocity);
							Force_XNDR(0.03333,fname,select_velocity);


						}	
						mediaFIO.setLEDBlue(false);

				

						break;				
					case 1:
						//15N=300*0.03
						forces=select_typeForce(15,300);
						select_velocity=velocity();
						
						mediaFIO.setLEDBlue(true);
						
						if (forces==0){
							getLogger().info("Selected 15N and " + select_velocity + "mm/s");
							fname="measured_force_15N_stiffZ_300_"+select_velocity+"mm_S.log";
							getLogger().info("Moving left part of the aleron");
							Force_XNL(0.03,fname,select_velocity);
							getLogger().info("Moving right part of the aleron");
							Force_XNR(0.03,fname,select_velocity);
							Force_XNDR(0.03,fname,select_velocity);



						}
						else if (forces==1){
							getLogger().info("Selected 15ND and " + select_velocity + "mm/s");
							fname="measured_force_15ND_stiffZ_300_"+select_velocity+"mm_S.log";
							Force_XND(0.03,fname,select_velocity);

						}	
						mediaFIO.setLEDBlue(false);

				
						break;					
					case 2:
						//20N=300*0.067 REPASAR DESIRED
						
						forces=select_typeForce(20,300);
						select_velocity=velocity();
						
						mediaFIO.setLEDBlue(true);
						
						if (forces==0){
							getLogger().info("Selected 20N and " + select_velocity + "mm/s");
							fname="measured_force_20N_stiffZ_300_"+select_velocity+"mm_S.log";
							getLogger().info("Moving left part of the aleron");
							Force_XNL(0.067,fname,select_velocity);
							getLogger().info("Moving right part of the aleron");
							Force_XNR(0.067,fname,select_velocity);

						}
						else if (forces==1){
							getLogger().info("Selected 20ND and " + select_velocity + "mm/s");
							fname="measured_force_20ND_stiffZ_300_"+select_velocity+"mm_S.log";
							Force_XND(0.067,fname,select_velocity);
							Force_XNDR(0.067,fname,select_velocity);

						}	
						mediaFIO.setLEDBlue(false);

						break;
					case 3:
						//24N=500*0.048
						//24=1000*0.024
						forces=select_typeForce(24,300);
						select_velocity=velocity();
						
						mediaFIO.setLEDBlue(true);

						
						if (forces==0){
							getLogger().info("Selected 24N and mm/s: " + select_velocity + "mm/s");
							fname="measured_force_24N_stiffZ_1000_"+select_velocity+"mm_S.log";
							getLogger().info("Moving left part of the aleron");
							Force_XNL(0.048,fname,select_velocity);
							getLogger().info("Moving right part of the aleron");
							Force_XNR(0.048,fname,select_velocity);

						}
						else if (forces==1){
							getLogger().info("Selected 24ND and mm/s: " + select_velocity + "mm/s");
							fname="measured_force_24ND_stiffZ_300_"+select_velocity+"mm_S.log";
							Force_XND(0.048,fname,select_velocity);
							Force_XNDR(0.048,fname,select_velocity);


						}				
						mediaFIO.setLEDBlue(false);

						break;
					case 4:
						//30N=300*0.1
						//35=300*0.116666
						forces=select_typeForce(35,300);
						select_velocity=velocity();
						
						mediaFIO.setLEDBlue(true);

						
						if (forces==0){
							getLogger().info("Selected 50N and mm/s: " + select_velocity + "mm/s");
							fname="measured_force_30N_stiffZ_300_"+select_velocity+"mm_S.log";
							getLogger().info("Moving left part of the aleron");
							Force_XNL(0.11666,fname,select_velocity);
							getLogger().info("Moving right part of the aleron");
							Force_XNR(0.11666,fname,select_velocity);

						}
						else if (forces==1){
							getLogger().info("Selected 35ND and mm/s: " + select_velocity + "mm/s");
							fname="measured_force_35D_stiffZ_300_"+select_velocity+"mm_S.log";
							Force_XND(0.11666,fname,select_velocity);
							Force_XNDR(0.11666,fname,select_velocity);


						}				
						mediaFIO.setLEDBlue(false);
						
						break;
						/*
					case 5:
						//de aqui hasta el doble asterisco comentado
						Spline mySpline= new Spline(
								spl(getApplicationData().getFrame("/aleron/Aprox1")),
								spl(getApplicationData().getFrame("/aleron/Aprox")))
								
								;

						roll_scan.getFrame("Gripper").move(mySpline);
						**
						//
						getLogger().info("Moving SafePos");
						//roll_scan.getFrame("Gripper").move(ptp(getFrame("/robot_base/P2")).setJointVelocityRel(0.2));
						
						MotionBatch mov_home;
						mov_home  = new MotionBatch(
								ptp(getFrame("/robot_base/P2")).setJointVelocityRel(0.2).setBlendingCart(80),
								ptp(getFrame("/robot_base/P3")).setJointVelocityRel(0.2).setBlendingCart(80),
								ptp(getFrame("/robot_base/P4")).setJointVelocityRel(0.2).setBlendingCart(82),
								ptp(getFrame("/robot_base/P2")).setJointVelocityRel(0.2).setBlendingCart(80),

								ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25)
							);
						motion = roll_scan.getFrame("Gripper").move(mov_home);
						
						break;
						
					case 6:
						
						Spline mySpline= new Spline(
								spl(getApplicationData().getFrame("/aleron/Aprox1")),
								spl(getApplicationData().getFrame("/aleron/Aprox")))
								
								;

						roll_scan.getFrame("Gripper").move(mySpline);
						
						break;
						*/
					case 5:
						//100N=300*0.3333333
						//38=300*0.134
						//100=1000*0.1
						forces=select_typeForce(100,1000);
						select_velocity=velocity();
						
						mediaFIO.setLEDBlue(true);

						
						if (forces==0){
							getLogger().info("Selected 100N and mm/s: " + select_velocity + "mm/s");
							fname="measured_force_100N_stiffZ_1000_"+select_velocity+"mm_S.log";
							getLogger().info("Moving left part of the aleron");
							Force_XNL(0.1,fname,select_velocity);
							getLogger().info("Moving right part of the aleron");
							Force_XNR(0.1,fname,select_velocity);

						}
						else if (forces==1){
							getLogger().info("Fuerza no implentada para Desired");
							

						}				
						mediaFIO.setLEDBlue(false);
						
						break;
					case 6:
						
						getLogger().info("App Terminated\n"+"***END***");
						exit = true;
						break;
						
						
						
			}
		} while (!exit);
			
	}
		
	private void Force_XNL(double distancia, String nfichero, double velocidad ){
	

 	LBRE1Redundancy redundancyInfo = new LBRE1Redundancy(Math.toRadians(0.2), 2, 24);
	
 	rec.setFileName(nfichero);
	rec.addCartesianForce(roll_scan.getFrame("Gripper"),roll_scan.getFrame("Gripper"));
	rec.addCurrentCartesianPositionXYZ(roll_scan.getFrame("Gripper"), getApplicationData().getFrame("/robot_base"));
 	//downFrame
	down_fr = new Frame(getFrame("/aleron"));
	down_fr.setX(0.0685*1000); down_fr.setY(1.5); down_fr.setZ(distancia*1000); //3.0 o 0.0333*1000 para 10N = 300K*0.0333
 	/*down_fr.setAlphaRad(0.0); down_fr.setBetaRad(0.0);*/ down_fr.setGammaRad(Math.PI);
	down_fr.setRedundancyInformation(lbr, redundancyInfo);
	
	//upFrame
	up_fr = new Frame(getFrame("/aleron"));
	up_fr.setX(0.0685*1000); up_fr.setY(0.400*1000); up_fr.setZ(-distancia*1000); 
 	/*up_fr.setAlphaRad(0.0); up_fr.setBetaRad(0.0)*/ up_fr.setGammaRad(Math.PI);
	up_fr.setRedundancyInformation(lbr, redundancyInfo);
	
	//int optionforce = getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, "How many Force do I have to do?", "10N", "15N", "20N", "24N", "END DO NOTHING");
	roll_scan.getFrame("Gripper").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));
	//roll_scan.getFrame("Gripper").move(ptp(getFrame("/aleron/Aprox1")).setJointVelocityRel(0.25));
	
	MotionBatch right;
	right  = new MotionBatch(
			ptp(getFrame("/aleron/Aprox1")).setJointVelocityRel(0.25).setBlendingCart(80),
			ptp(getFrame("/aleron/Aprox")).setJointVelocityRel(0.25)
			);
	motion = roll_scan.getFrame("Gripper").move(right);
	

	rec.enable();
	//roll_scan.getFrame("Gripper").move(ptp(getFrame("/aleron/Aprox")).setJointVelocityRel(0.25));
	
	rec.startRecording();
    
	x=down_fr.getX();
	
	while (x<460){
		
	roll_scan.getFrame("Gripper").move(lin(down_fr).setCartVelocity(velocidad));
	down_fr.setZ(-distancia*1000);
	roll_scan.getFrame("Gripper").move(lin(down_fr).setCartVelocity(25).setMode(impedanceControlMode));

	ForceSensorData current_force = lbr.getExternalForceTorque(roll_scan.getFrame("Gripper"),roll_scan.getFrame("Gripper"));
	System.out.println("First Contact Z force: " + current_force.getForce().getZ());
	
	roll_scan.getFrame("Gripper").move(lin(up_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));

	// movimiento hacia atras
	up_fr.setZ(distancia*1000);
	roll_scan.getFrame("Gripper").move(lin(up_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));
	
	//moviemiento en x
	overlapt=up_fr.getX()+overlap;
	up_fr.setX(overlapt);
	down_fr.setX(overlapt);
	//up_fr.setX(0.120*1000);
	//setX(0.120*1000);
	roll_scan.getFrame("Gripper").move(lin(up_fr).setCartVelocity(velocidad));
	
	// 2 movimiento al aleron
	up_fr.setZ(-distancia*1000);
	roll_scan.getFrame("Gripper").move(lin(up_fr).setCartVelocity(25).setMode(impedanceControlMode));
	ForceSensorData current_force2 = lbr.getExternalForceTorque(roll_scan.getFrame("Gripper"),roll_scan.getFrame("Gripper"));
	System.out.println("Second Contact Z force: " + current_force2.getForce().getZ());
	roll_scan.getFrame("Gripper").move(lin(down_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));
	down_fr.setZ(distancia*1000);
	roll_scan.getFrame("Gripper").move(lin(down_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));
	//moviemiento en x
	overlapt=up_fr.getX()+overlap;
	up_fr.setX(overlapt);
	down_fr.setX(overlapt);
	
	x=down_fr.getX();
	}
	rec.stopRecording();
	/*
	try {
		tcp_server.dispose();
	} catch (InterruptedException e) {
		System.out.println("Impedance:" + e.getMessage());
	}
*/
	

	MotionBatch right_side;
	right_side  = new MotionBatch(
			ptp(getFrame("/aleron/Aprox_Ret")).setJointVelocityRel(0.2).setBlendingCart(30),
			ptp(getFrame("/aleron/P_LR")).setJointVelocityRel(0.2).setBlendingCart(30),
			ptp(getFrame("/aleron/aproxrig")).setJointVelocityRel(0.2).setBlendingCart(30),
			ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25)
		);
	motion = roll_scan.getFrame("Gripper").move(right_side);

/*
	roll_scan.getFrame("Gripper").move(ptp(getFrame("/aleron/Aprox_Ret")).setJointVelocityRel(0.1).setBlendingCart(80));

	roll_scan.getFrame("Gripper").move(ptp(getFrame("/aleron/P_LR")).setJointVelocityRel(0.1));
*/
}
	
	private void Force_XNR(double distancia, String nfichero, double velocidad ){
		
	//impedanceControlMode.setMaxCartesianVelocity(1000.0,1000.0,1000.0,Math.toRadians(60),Math.toRadians(60),Math.toRadians(60));
	//impedanceControlMode.setSpringPosition(roll_scan.getFrame("gripper"));
	//impedanceControlMode.parametrize(CartDOF.X).setStiffness(stiffnessX).setDamping(0.7);
	//impedanceControlMode.parametrize(CartDOF.Y).setStiffness(stiffnessY).setDamping(0.7);
	//impedanceControlMode.parametrize(CartDOF.Z).setStiffness(Force).setDamping(0.7);
	//impedanceControlMode.parametrize(CartDOF.ROT).setStiffness(300).setDamping(0.7);
	
 	LBRE1Redundancy redundancyInfo = new LBRE1Redundancy(Math.toRadians(0.1), 1, 82);
	
 //	rec.setFileName(nfichero);
//	rec.addCartesianForce(roll_scan.getFrame("Gripper"),roll_scan.getFrame("Gripper"));
//	rec.addCurrentCartesianPositionXYZ(roll_scan.getFrame("Gripper"), getApplicationData().getFrame("/robot_base"));
 	//downFrame
	down_fr = new Frame(getFrame("/aleron"));
	down_fr.setX(0.80247*1000); down_fr.setY(1.6); down_fr.setZ(distancia*1000); //3.0 o 0.0333*1000 para 10N = 300K*0.0333
 	down_fr.setAlphaRad(Math.toRadians(-180)); /*down_fr.setBetaRad(0.0);*/ down_fr.setGammaRad(Math.toRadians(180));
	down_fr.setRedundancyInformation(lbr, redundancyInfo);
	
	//upFrame
	up_fr = new Frame(getFrame("/aleron"));
	up_fr.setX(0.80247*1000); up_fr.setY(0.400*1000); up_fr.setZ(-distancia*1000); 
 	up_fr.setAlphaRad(Math.toRadians(-180)); /*up_fr.setBetaRad(0.0)*/ up_fr.setGammaRad(Math.toRadians(180));
	up_fr.setRedundancyInformation(lbr, redundancyInfo);
	
	//int optionforce = getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, "How many Force do I have to do?", "10N", "15N", "20N", "24N", "END DO NOTHING");
	//roll_scan.getFrame("Gripper").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));
	/*
	MotionBatch left;
	left  = new MotionBatch(
			ptp(getFrame("/robot_base/AproxRW")).setJointVelocityRel(0.25).setBlendingCart(80),
			ptp(getFrame("/aleron/AproxRight")).setJointVelocityRel(0.25)
			);
	motion = roll_scan.getFrame("Gripper").move(left);
	
*/
	//roll_scan.getFrame("Gripper").move(ptp(getFrame("/robot_base/AproxRW")).setJointVelocityRel(0.25).setBlendingCart(80));
	//roll_scan.getFrame("Gripper").move(ptp(getFrame("/aleron/AproxRight")).setJointVelocityRel(0.25));
	//rec.enable();
	
	//rec.startRecording();
    
	x=down_fr.getX();
	
	while (x<1260){
		
	roll_scan.getFrame("Gripper").move(lin(down_fr).setCartVelocity(velocidad));
	down_fr.setZ(-distancia*1000);
	roll_scan.getFrame("Gripper").move(lin(down_fr).setCartVelocity(25).setMode(impedanceControlMode));

	ForceSensorData current_force = lbr.getExternalForceTorque(roll_scan.getFrame("Gripper"),roll_scan.getFrame("Gripper"));
	System.out.println("First Contact Z force: " + current_force.getForce().getZ());
	
	roll_scan.getFrame("Gripper").move(lin(up_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));

	// movimiento hacia atras
	up_fr.setZ(distancia*1000);
	roll_scan.getFrame("Gripper").move(lin(up_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));
	
	//moviemiento en x
	overlapt=up_fr.getX()+overlap;
	up_fr.setX(overlapt);
	down_fr.setX(overlapt);
	//up_fr.setX(0.120*1000);
	//setX(0.120*1000);
	roll_scan.getFrame("Gripper").move(lin(up_fr).setCartVelocity(velocidad));
	
	// 2 movimiento al aleron
	up_fr.setZ(-distancia*1000);
	roll_scan.getFrame("Gripper").move(lin(up_fr).setCartVelocity(25).setMode(impedanceControlMode));
	ForceSensorData current_force2 = lbr.getExternalForceTorque(roll_scan.getFrame("Gripper"),roll_scan.getFrame("Gripper"));
	System.out.println("Second Contact Z force: " + current_force2.getForce().getZ());
	roll_scan.getFrame("Gripper").move(lin(down_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));
	down_fr.setZ(distancia*1000);
	roll_scan.getFrame("Gripper").move(lin(down_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));
	//moviemiento en x
	exit_fr=down_fr;
	overlapt=up_fr.getX()+overlap;
	up_fr.setX(overlapt);
	down_fr.setX(overlapt);
	
	x=down_fr.getX();
	}
	//rec.stopRecording();
	/*
	try {
		tcp_server.dispose();
	} catch (InterruptedException e) {
		System.out.println("Impedance:" + e.getMessage());
	}
*/
	
	getLogger().info("****************************");
	getLogger().info("      Moving SafePos");
	getLogger().info("****************************");
	roll_scan.getFrame("Gripper").move(lin(getFrame("/aleron/AproxRightSalida")).setCartVelocity(25));

	//roll_scan.getFrame("Gripper").move(linRel(-overlapt,460,0).setCartVelocity(25));
	roll_scan.getFrame("Gripper").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));

	//gripper.getFrame("/TCP2").move(linRel(100, 0, -200));
}

private void Force_XNT(double distancia, String nfichero, double velocidad ){
	

   LBRE1Redundancy redundancyInfo = new LBRE1Redundancy(Math.toRadians(0.9), 2, 24);
	
 	rec.setFileName(nfichero);
	rec.addCartesianForce(roll_scan.getFrame("Gripper"),roll_scan.getFrame("Gripper"));
	rec.addCurrentCartesianPositionXYZ(roll_scan.getFrame("Gripper"), getApplicationData().getFrame("/robot_base"));
 	//downFrame
	down_fr = new Frame(getFrame("/aleron"));
	down_fr.setX(0.151*1000); down_fr.setY(-6.5); down_fr.setZ(distancia*1000); //3.0 o 0.0333*1000 para 10N = 300K*0.0333
 	down_fr.setAlphaRad(Math.PI/2); down_fr.setBetaRad(-0.047123); down_fr.setGammaRad(Math.PI);
	down_fr.setRedundancyInformation(lbr, redundancyInfo);
	
	//upFrame
	up_fr = new Frame(getFrame("/aleron"));
	up_fr.setX(0.350*1000); up_fr.setY(-6.5); up_fr.setZ(-distancia*1000); 
 	up_fr.setAlphaRad(Math.PI/2); up_fr.setBetaRad(-0.047123); up_fr.setGammaRad(Math.PI);
	up_fr.setRedundancyInformation(lbr, redundancyInfo);

	
	//int optionforce = getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, "How many Force do I have to do?", "10N", "15N", "20N", "24N", "END DO NOTHING");
	roll_scan.getFrame("Gripper").move(ptp(getFrame("/aleron/Aprox1")).setJointVelocityRel(0.25));
	rec.enable();
	roll_scan.getFrame("Gripper").move(ptp(getFrame("/aleron/Aprox")).setJointVelocityRel(0.25));
	
	rec.startRecording();
	
	roll_scan.getFrame("Gripper").move(lin(down_fr).setCartVelocity(velocidad));
	down_fr.setZ(-distancia*1000);
	roll_scan.getFrame("Gripper").move(lin(down_fr).setCartVelocity(25).setMode(impedanceControlMode));

	ForceSensorData current_force = lbr.getExternalForceTorque(roll_scan.getFrame("Gripper"),roll_scan.getFrame("Gripper"));
	System.out.println("First Contact Z force: " + current_force.getForce().getZ());
	
	roll_scan.getFrame("Gripper").move(lin(up_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));

	// movimiento hacia atras
	up_fr.setZ(distancia*1000);
	roll_scan.getFrame("Gripper").move(lin(up_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));
	
	//moviemiento en y
	up_fr.setY(0.120*1000);
	down_fr.setY(0.120*1000);
	roll_scan.getFrame("Gripper").move(lin(up_fr).setCartVelocity(velocidad));
	
	// 2 movimiento al aleron
	up_fr.setZ(-distancia*1000);
	roll_scan.getFrame("Gripper").move(lin(up_fr).setCartVelocity(25).setMode(impedanceControlMode));
	ForceSensorData current_force2 = lbr.getExternalForceTorque(roll_scan.getFrame("Gripper"),roll_scan.getFrame("Gripper"));
	System.out.println("Second Contact Z force: " + current_force2.getForce().getZ());
	roll_scan.getFrame("Gripper").move(lin(down_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));
	
    //movimiento hacia atras parte baja
	down_fr.setZ(distancia*1000);
	roll_scan.getFrame("Gripper").move(lin(down_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));
	
	//movimiento en y
	up_fr.setY(0.240*1000);
	down_fr.setY(0.240*1000);
	roll_scan.getFrame("Gripper").move(lin(down_fr).setCartVelocity(velocidad));
	
    // movimiento hacia el aleron parte baja
	down_fr.setZ(-distancia*1000);
	roll_scan.getFrame("Gripper").move(lin(down_fr).setCartVelocity(25).setMode(impedanceControlMode));

	ForceSensorData current_force3 = lbr.getExternalForceTorque(roll_scan.getFrame("Gripper"),roll_scan.getFrame("Gripper"));
	System.out.println("Third Contact Z force: " + current_force3.getForce().getZ());

	roll_scan.getFrame("Gripper").move(lin(up_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));

	
	//roll_scan.getFrame("Gripper").move(lin(getFrame("/aleron/P1")).setCartVelocity(50));
	//roll_scan.getFrame("Gripper").move(lin(getFrame("/aleron/P2")).setCartVelocity(50).setMode(impedanceControlMode));
	//roll_scan.getFrame("Gripper").move(lin(getFrame("/aleron/P3")).setCartVelocity(50).setMode(impedanceControlMode));
	//.getFrame("Gripper").move(lin(getFrame("/aleron/P4")).setCartVelocity(50).setMode(impedanceControlMode));
	rec.stopRecording();
	roll_scan.getFrame("Gripper").move(linRel(0,0,-200).setCartVelocity(100));
	//gripper.getFrame("/TCP2").move(linRel(100, 0, -200));
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

private int select_typeForce(double force, double stiffnessZ){
	int force_s=0;
	switch (getApplicationUI().displayModalDialog(
			ApplicationDialogType.QUESTION,"What type of Force do you want to do?", 
			"Normal", "Sinusoidal")) {

			case 0:
				impedanceControlMode.setMaxCartesianVelocity(1000.0,1000.0,1000.0,Math.toRadians(60),Math.toRadians(60),Math.toRadians(60));
				impedanceControlMode.setSpringPosition(roll_scan.getFrame("gripper"));
				impedanceControlMode.parametrize(CartDOF.X).setStiffness(stiffnessX).setDamping(0.7);
				impedanceControlMode.parametrize(CartDOF.Y).setStiffness(stiffnessY).setDamping(0.7);
				impedanceControlMode.parametrize(CartDOF.Z).setStiffness(stiffnessZ).setDamping(0.7);
				impedanceControlMode.parametrize(CartDOF.ROT).setStiffness(300).setDamping(0.7);
				force_s=0;
				break;				
			case 1:
				impedanceControlModeD = CartesianSineImpedanceControlMode.createDesiredForce(CartDOF.Z, force, stiffnessZ)
				//.setTotalTime(totalTimeSecs)
				//.setRiseTime(riseTimeSecs)
				.setMaxCartesianVelocity(1000.0,1000.0,1000.0,Math.toRadians(60),Math.toRadians(60),Math.toRadians(60))
				.setSpringPosition(roll_scan.getFrame("gripper"));
				impedanceControlModeD.parametrize(CartDOF.X).setStiffness(stiffnessX).setDamping(0.7);
				impedanceControlModeD.parametrize(CartDOF.Y).setStiffness(stiffnessY).setDamping(0.7);
				impedanceControlModeD.parametrize(CartDOF.ROT).setStiffness(300).setDamping(0.7);
				force_s=1;
				break;					
			
			
	}
	return force_s;
	
}

private void Force_XND(double distancia, String nfichero, double velocidad ){
	
	//impedanceControlMode.setMaxCartesianVelocity(1000.0,1000.0,1000.0,Math.toRadians(60),Math.toRadians(60),Math.toRadians(60));
	//impedanceControlMode.setSpringPosition(roll_scan.getFrame("gripper"));
	//impedanceControlMode.parametrize(CartDOF.X).setStiffness(stiffnessX).setDamping(0.7);
	//impedanceControlMode.parametrize(CartDOF.Y).setStiffness(stiffnessY).setDamping(0.7);
	//impedanceControlMode.parametrize(CartDOF.Z).setStiffness(Force).setDamping(0.7);
	//impedanceControlMode.parametrize(CartDOF.ROT).setStiffness(300).setDamping(0.7);
 	LBRE1Redundancy redundancyInfo = new LBRE1Redundancy(Math.toRadians(0.2), 2, 24);
	
 	rec.setFileName(nfichero);
	rec.addCartesianForce(roll_scan.getFrame("Gripper"),roll_scan.getFrame("Gripper"));
	rec.addCurrentCartesianPositionXYZ(roll_scan.getFrame("Gripper"), getApplicationData().getFrame("/robot_base"));
 	//downFrame
	down_fr = new Frame(getFrame("/aleron"));
	down_fr.setX(0.0685*1000); down_fr.setY(1.5); down_fr.setZ(distancia*1000); //3.0 o 0.0333*1000 para 10N = 300K*0.0333
 	/*down_fr.setAlphaRad(0.0); down_fr.setBetaRad(0.0);*/ down_fr.setGammaRad(Math.PI);
	down_fr.setRedundancyInformation(lbr, redundancyInfo);
	
	//upFrame
	up_fr = new Frame(getFrame("/aleron"));
	up_fr.setX(0.0685*1000); up_fr.setY(0.400*1000); up_fr.setZ(0.0*1000); 
 	/*up_fr.setAlphaRad(0.0); up_fr.setBetaRad(0.0)*/ up_fr.setGammaRad(Math.PI);
	up_fr.setRedundancyInformation(lbr, redundancyInfo);
	
	//int optionforce = getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, "How many Force do I have to do?", "10N", "15N", "20N", "24N", "END DO NOTHING");
	roll_scan.getFrame("Gripper").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));
	roll_scan.getFrame("Gripper").move(ptp(getFrame("/aleron/Aprox1")).setJointVelocityRel(0.25));
	rec.enable();
	roll_scan.getFrame("Gripper").move(ptp(getFrame("/aleron/Aprox")).setJointVelocityRel(0.25));
	
	rec.startRecording();

	x=down_fr.getX();
	while (x<460){

	roll_scan.getFrame("Gripper").move(lin(down_fr).setCartVelocity(velocidad));
	down_fr.setZ(0.0*1000);
	roll_scan.getFrame("Gripper").move(lin(down_fr).setCartVelocity(10).setMode(impedanceControlModeD));

	ForceSensorData current_force = lbr.getExternalForceTorque(roll_scan.getFrame("Gripper"),roll_scan.getFrame("Gripper"));
	System.out.println("First Contact Z force: " + current_force.getForce().getZ());
	
	roll_scan.getFrame("Gripper").move(lin(up_fr).setCartVelocity(velocidad).setMode(impedanceControlModeD));

	// movimiento hacia atras
	up_fr.setZ(distancia*1000);
	roll_scan.getFrame("Gripper").move(lin(up_fr).setCartVelocity(velocidad).setMode(impedanceControlModeD));
	
	//moviemiento en x
	overlapt=up_fr.getX()+overlap;
	up_fr.setX(overlapt);
	down_fr.setX(overlapt);
	roll_scan.getFrame("Gripper").move(lin(up_fr).setCartVelocity(velocidad));
	
	// 2 movimiento al aleron
	up_fr.setZ(-0.005*1000);
	roll_scan.getFrame("Gripper").move(lin(up_fr).setCartVelocity(10).setMode(impedanceControlMode));
	ForceSensorData current_force2 = lbr.getExternalForceTorque(roll_scan.getFrame("Gripper"),roll_scan.getFrame("Gripper"));
	System.out.println("Second Contact Z force: " + current_force2.getForce().getZ());
	roll_scan.getFrame("Gripper").move(lin(down_fr).setCartVelocity(velocidad).setMode(impedanceControlModeD));
	
    //movimiento hacia atras parte baja
	down_fr.setZ(distancia*1000);
	roll_scan.getFrame("Gripper").move(lin(down_fr).setCartVelocity(velocidad).setMode(impedanceControlModeD));
	//moviemiento en x
	overlapt=up_fr.getX()+overlap;
	up_fr.setX(overlapt);
	down_fr.setX(overlapt);
	x=down_fr.getX();
	}
	rec.stopRecording();
	/*
	try {
		tcp_server.dispose();
	} catch (InterruptedException e) {
		System.out.println("Impedance:" + e.getMessage());
	}
*/
	MotionBatch right_side;
	right_side  = new MotionBatch(
			ptp(getFrame("/aleron/Aprox_Ret")).setJointVelocityRel(0.2).setBlendingCart(30),
			ptp(getFrame("/aleron/P_LR")).setJointVelocityRel(0.2).setBlendingCart(30),
			ptp(getFrame("/aleron/AproxRight")).setJointVelocityRel(0.2)
		);
	motion = roll_scan.getFrame("Gripper").move(right_side);
	//roll_scan.getFrame("Gripper").move(ptp(getFrame("/aleron/Aprox_Ret")).setJointVelocityRel(0.2).setBlendingCart(50));
	//roll_scan.getFrame("Gripper").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));

	//gripper.getFrame("/TCP2").move(linRel(100, 0, -200));
}

private void Force_XNDR(double distancia, String nfichero, double velocidad ){
	
	//impedanceControlMode.setMaxCartesianVelocity(1000.0,1000.0,1000.0,Math.toRadians(60),Math.toRadians(60),Math.toRadians(60));
	//impedanceControlMode.setSpringPosition(roll_scan.getFrame("gripper"));
	//impedanceControlMode.parametrize(CartDOF.X).setStiffness(stiffnessX).setDamping(0.7);
	//impedanceControlMode.parametrize(CartDOF.Y).setStiffness(stiffnessY).setDamping(0.7);
	//impedanceControlMode.parametrize(CartDOF.Z).setStiffness(Force).setDamping(0.7);
	//impedanceControlMode.parametrize(CartDOF.ROT).setStiffness(300).setDamping(0.7);
	
 	LBRE1Redundancy redundancyInfo = new LBRE1Redundancy(Math.toRadians(0.2), 1, 82);
	
 //	rec.setFileName(nfichero);
//	rec.addCartesianForce(roll_scan.getFrame("Gripper"),roll_scan.getFrame("Gripper"));
//	rec.addCurrentCartesianPositionXYZ(roll_scan.getFrame("Gripper"), getApplicationData().getFrame("/robot_base"));
 	//downFrame
	down_fr = new Frame(getFrame("/aleron"));
	down_fr.setX(0.8055*1000); down_fr.setY(2); down_fr.setZ(distancia*1000); //3.0 o 0.0333*1000 para 10N = 300K*0.0333
 	down_fr.setAlphaRad(Math.toRadians(-180)); /*down_fr.setBetaRad(0.0);*/ down_fr.setGammaRad(Math.toRadians(180));
	down_fr.setRedundancyInformation(lbr, redundancyInfo);
	
	//upFrame
	up_fr = new Frame(getFrame("/aleron"));
	up_fr.setX(0.8055*1000); up_fr.setY(0.400*1000); up_fr.setZ(0.0*1000); 
 	up_fr.setAlphaRad(Math.toRadians(-180)); /*up_fr.setBetaRad(0.0)*/ up_fr.setGammaRad(Math.toRadians(180));
	up_fr.setRedundancyInformation(lbr, redundancyInfo);
	
	//int optionforce = getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, "How many Force do I have to do?", "10N", "15N", "20N", "24N", "END DO NOTHING");
	//roll_scan.getFrame("Gripper").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));
	/*
	MotionBatch left;
	left  = new MotionBatch(
			ptp(getFrame("/robot_base/AproxRW")).setJointVelocityRel(0.25).setBlendingCart(80),
			ptp(getFrame("/aleron/AproxRight")).setJointVelocityRel(0.25)
			);
	motion = roll_scan.getFrame("Gripper").move(left);
	
*/
	//roll_scan.getFrame("Gripper").move(ptp(getFrame("/robot_base/AproxRW")).setJointVelocityRel(0.25).setBlendingCart(80));
	roll_scan.getFrame("Gripper").move(ptp(getFrame("/aleron/AproxRight")).setJointVelocityRel(0.25));
	//rec.enable();
	
	//rec.startRecording();
    
	x=down_fr.getX();
	
	while (x<1260){
		
	roll_scan.getFrame("Gripper").move(lin(down_fr).setCartVelocity(velocidad));
	down_fr.setZ(0.0*1000);
	roll_scan.getFrame("Gripper").move(lin(down_fr).setCartVelocity(25).setMode(impedanceControlModeD));

	ForceSensorData current_force = lbr.getExternalForceTorque(roll_scan.getFrame("Gripper"),roll_scan.getFrame("Gripper"));
	System.out.println("First Contact Z force: " + current_force.getForce().getZ());
	
	roll_scan.getFrame("Gripper").move(lin(up_fr).setCartVelocity(velocidad).setMode(impedanceControlModeD));

	// movimiento hacia atras
	up_fr.setZ(distancia*1000);
	roll_scan.getFrame("Gripper").move(lin(up_fr).setCartVelocity(velocidad).setMode(impedanceControlModeD));
	
	//moviemiento en x
	overlapt=up_fr.getX()+overlap;
	up_fr.setX(overlapt);
	down_fr.setX(overlapt);
	//up_fr.setX(0.120*1000);
	//setX(0.120*1000);
	roll_scan.getFrame("Gripper").move(lin(up_fr).setCartVelocity(velocidad));
	
	// 2 movimiento al aleron
	up_fr.setZ(0.0*1000);
	roll_scan.getFrame("Gripper").move(lin(up_fr).setCartVelocity(25).setMode(impedanceControlModeD));
	ForceSensorData current_force2 = lbr.getExternalForceTorque(roll_scan.getFrame("Gripper"),roll_scan.getFrame("Gripper"));
	System.out.println("Second Contact Z force: " + current_force2.getForce().getZ());
	roll_scan.getFrame("Gripper").move(lin(down_fr).setCartVelocity(velocidad).setMode(impedanceControlModeD));
	down_fr.setZ(distancia*1000);
	roll_scan.getFrame("Gripper").move(lin(down_fr).setCartVelocity(velocidad).setMode(impedanceControlModeD));
	//moviemiento en x
	exit_fr=down_fr;
	overlapt=up_fr.getX()+overlap;
	up_fr.setX(overlapt);
	down_fr.setX(overlapt);
	
	x=down_fr.getX();
	}
	//rec.stopRecording();
	/*
	try {
		tcp_server.dispose();
	} catch (InterruptedException e) {
		System.out.println("Impedance:" + e.getMessage());
	}
*/
	
	getLogger().info("****************************");
	getLogger().info("      Moving SafePos");
	getLogger().info("****************************");
	roll_scan.getFrame("Gripper").move(lin(getFrame("/aleron/AproxRightSalida")).setCartVelocity(25));

	//roll_scan.getFrame("Gripper").move(linRel(-overlapt,460,0).setCartVelocity(25));
	roll_scan.getFrame("Gripper").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));

	//gripper.getFrame("/TCP2").move(linRel(100, 0, -200));
}
/*
private void doOverrideRamp(double current_override, long timeLength, int steps) {

	//double currentOverride = appControl.getApplicationOverride();
	double overrideStep = (1.0 - current_override) / steps;
	while (current_override < 1.0) {
		double nextOverrride = current_override + overrideStep;
		if (nextOverrride > 1.0)
			nextOverrride = 1.0;
		getApplicationControl().setApplicationOverride(nextOverrride);
		//currentOverride = appControl.getApplicationOverride();
		ThreadUtil.milliSleep(timeLength / steps);
	}
}

@Override
public void OnTCPMessageReceived(boolean t)
{	
	
	System.out.println("OnTCPMessageReceived Boolean: " + t);
	System.out.println("last_pause_state.get(): " + last_pause_state.get());
	
	if(last_pause_state.get() != t)
	{
		if(t)
		{
			getApplicationControl().pause();
		}
		else
		{
			if(executionServiceLbr.isPaused())
			{
				System.out.println("executionServiceLbr.isPaused(): " + executionServiceLbr.isPaused());
				executionServiceLbr.resumeExecution(ResumeMode.OnPath);
			}
		}
		last_pause_state.set(t);
	}
}
@Override
public void OnTCPMessageReceived(double t)
{
	System.out.println("OnTCPMessageReceived Double: " + t);

	if(last_override_value != t)
	{
		doOverrideRamp(t, OVERRIDE_RAMP_TIME_LENGTH_MS, OVERRIDE_RAMP_NUM_STEPS);
		last_override_value = t;
	}
}
*/



}
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
import com.kuka.roboticsAPI.controllerModel.Controller;
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
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianImpedanceControlMode;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianSineImpedanceControlMode;
import com.kuka.roboticsAPI.sensorModel.DataRecorder;
//import com.kuka.roboticsAPI.sensorModel.ForceSensorData;
import com.kuka.roboticsAPI.uiModel.ApplicationDialogType;

public class AleronDemo2 extends RoboticsAPIApplication implements ITCPListener, ISignalListener{
	
	@Inject
	private LBR lbr;
	
    private Controller controller;
    private Tool roll_scan;
    boolean exit;
    String fname;
	
    CartesianImpedanceControlMode impedanceControlMode;
	
    private static final int stiffnessZ = 300;
	private static final int stiffnessY = 5000;
	private static final int stiffnessX = 5000;
	
	private ArrayList<Double> caltab_pose_data = new ArrayList<Double>();

	
	double select_velocity;
	
	ArrayList<Double> x = new ArrayList<Double>();
	ArrayList<Double> y = new ArrayList<Double>();
	ArrayList<Double> z = new ArrayList<Double>();
	ArrayList<Double> a = new ArrayList<Double>();
	ArrayList<Double> b = new ArrayList<Double>();
	ArrayList<Double> c = new ArrayList<Double>();
	ArrayList<Double> a_n = new ArrayList<Double>();
	ArrayList<Double> b_n = new ArrayList<Double>();
	ArrayList<Double> c_n = new ArrayList<Double>();

	//Frames
	Frame tcp_camera_fr;
		
	ArrayList<Frame> aileron_caltabs_fr_list = new ArrayList<Frame>();
	ArrayList<Frame> traj_caltab_ref_fr = new ArrayList<Frame>();
	
	DataRecorder rec;
	
	private TCPServer tcp_server;
	AtomicBoolean data_received;
	
	//Exchanged data info 
	String operation_type;
	String time_stamp;
	int frame_id;
	Frame caltab_robot_fr;

	// not injected fields
	private IErrorHandler errorHandler;
	
	//Movement execution state
	AtomicBoolean movement_failed;
	AtomicInteger failed_movement_nbr;
	
	
	//Motion list	
	ArrayList<IMotionContainer> motion_list = new ArrayList<IMotionContainer>();
	IMotion canceled_motion;
	
	//Media flange instance
    @Inject
    private MediaFlangeIOGroup mediaFIO;
    
    //Media flange input signal manager
	private SignalsMonitor signal_monitor;

	//Movement counter
	AtomicInteger move_cont; 
	AtomicBoolean warning_signal;
	int next_movement;
	
	double current_override;
	
	int working_zone;
	
	@Override
	public void initialize() {
		
		controller = getController("KUKA_Sunrise_Cabinet_1");

		// initialize your application here
		roll_scan = createFromTemplate("RollScan");
		roll_scan.attachTo(lbr.getFlange());
		
		System.out.println("Roll scan frame: " + roll_scan.getFrame("roll_tcp").toString());

		data_received = new AtomicBoolean(false);
		
		movement_failed = new AtomicBoolean(false);
		failed_movement_nbr = new AtomicInteger(0);
		
		warning_signal = new AtomicBoolean(false);
		move_cont = new AtomicInteger(0);
		next_movement = 0;
		
		current_override= 1;
		
		//Frames definition
		tcp_camera_fr = new Frame(lbr.getFlange());
		//tcp_camera_fr.setX(-20.0); tcp_camera_fr.setY(-101.902); tcp_camera_fr.setZ(105.038);
		//tcp_camera_fr.setAlphaRad(0.375 *(Math.PI/180)); tcp_camera_fr.setBetaRad(359.535*(Math.PI/180)); tcp_camera_fr.setGammaRad(1.39*(Math.PI/180));
		tcp_camera_fr.setX(-14.7231); tcp_camera_fr.setY(-102.098); tcp_camera_fr.setZ(106.308);
		tcp_camera_fr.setAlphaRad(1.3937*(Math.PI/180)); tcp_camera_fr.setBetaRad(3.086*(Math.PI/180)); tcp_camera_fr.setGammaRad(0.316*(Math.PI/180));
	
		Frame pose = new Frame(getFrame("/DemoCroinspect/aileron"));
		
		//Catlab1 Aileron frame definition
		/*pose.setX(0.012800498646568 * 1000); pose.setY(0.416067618840187*1000); pose.setZ(0.0958228212077537*1000);
		pose.setAlphaRad(93.631342620452*(Math.PI/180)); pose.setBetaRad(350.074462670324*(Math.PI/180)); pose.setGammaRad(178.711336963848*(Math.PI/180));
		*/
		pose.setX(0.02 * 1000); pose.setY(0.43*1000); pose.setZ(0.005*1000);
		pose.setAlphaRad(-Math.PI/2); pose.setBetaRad(Math.PI); pose.setGammaRad(0.0);
	
		System.out.println("Caltab Aileron Frame --> x: " + pose.getX() + "  y: " + pose.getY() + "  z: " + pose.getZ() 
				+ "  A: " + pose.getAlphaRad() + "  B: " + pose.getBetaRad() + "  C: " + pose.getGammaRad());
		
		//Getting the inverse frame (Aileron - Caltab)
		Transformation t = pose.getTransformationFromParent().invert();
		Frame pose_inv = new Frame(getFrame("/DemoCroinspect/caltab"), t);
		
		System.out.println("Aileron caltab  --> x: " + pose_inv.getX() + "  y: " + pose_inv.getY() + "  z: " + pose_inv.getZ() 
			+ "  A: " + pose_inv.getAlphaRad() + "  B: " + pose_inv.getBetaRad() + "  C: " + pose_inv.getGammaRad());
	
		aileron_caltabs_fr_list.add(pose_inv);

		//Catlab2 Aileron frame definition
		/*pose.setX(1.0725191339774 * 1000); pose.setY(0.471247604621729*1000); pose.setZ(0.109787528885935*1000);
		pose.setAlphaRad(91.3387188096205*(Math.PI/180)); pose.setBetaRad(351.594265564923*(Math.PI/180)); pose.setGammaRad(180.617825190025*(Math.PI/180));
		*/
		
		pose.setX(1.078 * 1000); pose.setY(0.43*1000); pose.setZ(0.005*1000);
		pose.setAlphaRad(-Math.PI/2); pose.setBetaRad(Math.PI); pose.setGammaRad(0.0);

		//Getting the inverse frame (Aileron - Caltab2)
		t = pose.getTransformationFromParent().invert();
		pose_inv = new Frame(getFrame("/DemoCroinspect/caltab"), t);
	
		//Adding the frame to the list
		aileron_caltabs_fr_list.add(pose_inv);
		
		//Adding the frame to the list
		aileron_caltabs_fr_list.add(pose);
		
		//Impedance control object definition
		impedanceControlMode =	new CartesianImpedanceControlMode();
				
		String str;
		String file = "C:\\Users\\KukaUser\\Desktop\\CADTraj\\Plana_Sin.MPF";
		FileReader f;
      
		String val_str[];
		Double val;
		
		//Trajectory file parsing
		try 
		{
			f = new FileReader(file);
		
			 BufferedReader br = new BufferedReader(f);
			 int cont = 0;
			 
		     while((str = br.readLine())!=null) 
		     {
		    	 
		    	 String data[] = str.split(" ");
		    	  	    	  
		    	 for(int i=0; i<data.length; i++)
		    	 {
		    		 if(data[i].contains("X="))
		    		 {
		    			 val_str= data[i].split("X=");
		    			 val = Double.parseDouble(val_str[1]);
		    			 x.add(val);
		    		 }
		    		 else if(data[i].contains("Y="))
		    		 {
		    			 val_str= data[i].split("Y=");
		    			 val = Double.parseDouble(val_str[1]);
		    			 y.add(val);
		    		 }
		    		 else if(data[i].contains("Z="))
		    		 {
		    			 val_str= data[i].split("Z=");
		    			 val = Double.parseDouble(val_str[1]);
		    			 z.add(val);
		    		 }
		    		 else if(data[i].contains("A3="))
		    		 {
		    			 val_str= data[i].split("A3=");
		    			 val = Double.parseDouble(val_str[1]);
		    			 a.add(val*Math.PI/180);
		    		 }
		    		 else if(data[i].contains("B3="))
		    		 {
		    			 val_str= data[i].split("B3=");
		    			 val = Double.parseDouble(val_str[1]);
		    			 b.add(val*Math.PI/180);
		    		 }
		    		 else if(data[i].contains("C3="))
		    		 {
		    			 val_str= data[i].split("C3=");
		    			 val = Double.parseDouble(val_str[1]);
		    			 c.add(val*Math.PI/180);
		    		 }
		    		 else if(data[i].contains("AN3="))
		    		 {
		    			 val_str= data[i].split("AN3=");
		    			 val = Double.parseDouble(val_str[1]);
		    			 a_n.add(val);
		    		 }
		    		 else if(data[i].contains("BN3="))
		    		 {
		    			 val_str= data[i].split("BN3=");
		    			 val = Double.parseDouble(val_str[1]);
		    			 b_n.add(val);
		    		 }
		    		 else if(data[i].contains("CN3="))
		    		 {
		    			 val_str= data[i].split("CN3=");
		    			 val = Double.parseDouble(val_str[1]);
		    			 c_n.add(val);
		    		 }
		    	 }
		    	 
		    	 pose.setParent(getFrame("/DemoCroinspect/aileron"));
		    	 pose.setX(x.get(cont)); pose.setY(y.get(cont)); pose.setZ(z.get(cont));
		    	 pose.setAlphaRad(a.get(cont)); pose.setBetaRad(b.get(cont)); pose.setGammaRad(c.get(cont));
	 		 
		    	 pose.transform(XyzAbcTransformation.ofDeg(0.0, 0.0, 0.0, 0.0, 0.0, 180.0));

	    		 Frame aileron_caltab_fr;
	    		 //Definicion de la recta en el punto x=1106 (ultimo punto asociado a la primera caltab)
	    		 // y = -3.319181909*x + 3934.20009684124
	 		
				 int zone_id = poseChecking(x.get(cont), y.get(cont));
	    		 
	    		 if(zone_id ==1)
	    		 {
		    		 aileron_caltab_fr = aileron_caltabs_fr_list.get(0).copy();
		    		 //System.out.println("Caltab 1 --> x: " + aileron_caltab_fr.getX() + " y: " + aileron_caltab_fr.getY() + " z: " + aileron_caltab_fr.getZ() + 
						//		" A: " + aileron_caltab_fr.getAlphaRad() + " B: " + aileron_caltab_fr.getBetaRad() + " C: " + aileron_caltab_fr.getGammaRad());	
	    		 }
	    		 else
	    		 {
		    		 aileron_caltab_fr = aileron_caltabs_fr_list.get(1).copy();
		    		//System.out.println("Caltab 2 --> x: " + aileron_caltab_fr.getX() + " y: " + aileron_caltab_fr.getY() + " z: " + aileron_caltab_fr.getZ() + 
						//		" A: " + aileron_caltab_fr.getAlphaRad() + " B: " + aileron_caltab_fr.getBetaRad() + " C: " + aileron_caltab_fr.getGammaRad());	

	    		 }																									  
    	 
		    	//System.out.println("Ref Caltab frame --> x: " + aileron_caltab_fr.getX() + " y: " + aileron_caltab_fr.getY() + " z: " + aileron_caltab_fr.getZ() + 
					//	" A: " + aileron_caltab_fr.getAlphaRad() + " B: " + aileron_caltab_fr.getBetaRad() + " C: " + aileron_caltab_fr.getGammaRad());
				
	    		
    		   	 aileron_caltab_fr.transform(XyzAbcTransformation.ofRad(pose.getX(), pose.getY(), pose.getZ(),
		    	 pose.getAlphaRad(), pose.getBetaRad(), pose.getGammaRad()));
	    		 traj_caltab_ref_fr.add(aileron_caltab_fr);
	    		 cont++;
	    		 
		     }
		    
		     br.close();
	      } 
	      catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
	      } 
	      catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		//Modifing the lateral movements 
		
		for(int i=1; i<traj_caltab_ref_fr.size();i++)
		{
			if(x.get(i)== x.get(i-1))
			{	
				traj_caltab_ref_fr.get(i).transform(XyzAbcTransformation.ofDeg(0.0, 0.0, -25.0, 0.0, 0.0, 0.0));
				
				System.out.println(i + " Traj Position in caltab frame --> x: " + traj_caltab_ref_fr.get(i).getX() + 
					" y: " + traj_caltab_ref_fr.get(i).getY() + " z: " + traj_caltab_ref_fr.get(i).getZ() + 
						" A: " + traj_caltab_ref_fr.get(i).getAlphaRad() + " B: " + traj_caltab_ref_fr.get(i).getBetaRad() + 
							" C: " + traj_caltab_ref_fr.get(i).getGammaRad());
		
				traj_caltab_ref_fr.get(i+1).transform(XyzAbcTransformation.ofDeg(0.0, 0.0, -25.0, 0.0, 0.0, 0.0));
				
				i++;
			}
			
			System.out.println(i + " Traj Position in caltab frame --> x: " + traj_caltab_ref_fr.get(i).getX() + 
					" y: " + traj_caltab_ref_fr.get(i).getY() + " z: " + traj_caltab_ref_fr.get(i).getZ() + 
						" A: " + traj_caltab_ref_fr.get(i).getAlphaRad() + " B: " + traj_caltab_ref_fr.get(i).getBetaRad() + 
							" C: " + traj_caltab_ref_fr.get(i).getGammaRad());

		}
		
		
		//Application TCPServer object
		try {
			tcp_server = new TCPServer();
				
			tcp_server.addListener(this);
			tcp_server.enable();
					
		} catch (IOException e) {
			//TODO Bloque catch generado autom�ticamente
			System.err.println("Could not create TCPServer:" +e.getMessage());
		}
		
		//Media flange management
		mediaFIO.setLEDBlue(true);
		signal_monitor = new SignalsMonitor(mediaFIO);
		signal_monitor.addListener(this);
		signal_monitor.enable();
	
		//Asyncronous movement error handling
		errorHandler = new IErrorHandler() {
			 @Override
			 public ErrorHandlingAction handleError(Device device, IMotionContainer failedContainer,
			 List<IMotionContainer> canceledContainers)
			 {
				 System.out.println("Excecution of the following motion failed: " + failedContainer.getCommand().toString());
				 //logger.info("The following motions will not be executed:");
				 //for (int i = 0; i < canceledContainers.size(); i++) {
					 //logger.info(canceledContainers.get(i).getCommand().toString());
					 
				 //}
				System.out.println("The following " + canceledContainers.size() + " motions will not be executed");
				 
				for (int i = 0; i < motion_list.size(); i++) 
				{ 
					if(!motion_list.get(i).isFinished())
						System.out.println(i + " Motion state: " + motion_list.get(i).getState());
				}
				
				//motion_list.clear();
				//controller.getExecutionService().cancelAll();
				
				current_override = getApplicationControl().getApplicationOverride();
				getApplicationControl().clipApplicationOverride(0.0);
		
				waitUntilRobotAlmostStopped(-1);
				
				movement_failed.set(true);
				
				return ErrorHandlingAction.Ignore;
			 }
		};
		
		
		getApplicationControl().registerMoveAsyncErrorHandler(errorHandler);		 
	} 
    
	
	@Override
	public void run() {	
		
		// your application execution starts here
		roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/DemoCroinspect/SafePose")).setJointVelocityRel(0.25));
		exit=false;
		
		do {
			
			
			if(data_received.get())
			{
				data_received.set(false);
				
				System.out.println("Type:" + operation_type);
				
				if(operation_type.compareTo("calibration") == 0)
				{
					roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/DemoCroinspect/Aprox3")).setJointVelocityRel(0.25));

					String response_data = frame_id + ";" + operation_type + ";1" ;
					tcp_server.setResponseData(response_data);																							
				}
				else if (operation_type.compareTo("inspection") == 0)
				{
				
					rec = new DataRecorder();
					rec.setTimeout(2L, TimeUnit.MINUTES);
					switch (getApplicationUI().displayModalDialog(
							ApplicationDialogType.QUESTION,"How many Force do I have to do?", 
							"10N", "15N", "20N", "24N", "END DO NOTHING")) {
		
							case 0:
								
								select_velocity=velocity();
								getLogger().info("Selected 10N and " + select_velocity + "mm/s");
		
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
	

	private int poseChecking(double x, double y)
	{
	
		Double y_val =  -3.326786450896398540377597969221*x + 4020.7878085276852292559098841821;
	
		 if(y_val > y)
			 return 1;
		 else 
			 return 2;
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
		impedanceControlMode.parametrize(CartDOF.ROT).setStiffness(300).setDamping(0.7);
		//impedanceControlMode.parametrize(CartDOF.C).setStiffness(100).setDamping(0.7);

		//Data recorder configuration
		rec.setFileName(nfichero);
		rec.addCartesianForce(roll_scan.getFrame("roll_tcp"),roll_scan.getFrame("roll_tcp"));
		rec.addCurrentCartesianPositionXYZ(roll_scan.getFrame("roll_tcp"), getApplicationData().getFrame("/robot_base"));
	 	rec.addCartesianTorque(roll_scan.getFrame("roll_tcp"),roll_scan.getFrame("roll_tcp"));
	 	rec.enable();
		rec.startRecording();
	
		Frame point = new Frame(getFrame("/DemoCroinspect/caltab"));
		LBRE1Redundancy redundancyInfo = new LBRE1Redundancy(Math.toRadians(-0.03), 6, 108);
		Frame aprox_pose = caltab_robot_fr.copy();
		
		int i = move_cont.get();
		
		working_zone = poseChecking(x.get(i), y.get(i));
			
		point  = traj_caltab_ref_fr.get(i).copy();
		aprox_pose.transform(XyzAbcTransformation.ofRad(point.getX(), point.getY(), point.getZ(), 
				point.getAlphaRad(), point.getBetaRad(), point.getGammaRad()));
							
		System.out.println("Traj point in robot base frame --> x: " + aprox_pose.getX() + " y: " + aprox_pose.getY() + " z: " + aprox_pose.getZ() + 
				" A: " + aprox_pose.getAlphaRad() + " B: " + aprox_pose.getBetaRad() + " C: " + aprox_pose.getGammaRad());
					
		//aprox_pose.transform(XyzAbcTransformation.ofRad(0.0,0.0,-300, Math.PI/2,0.0,0.0));
		aprox_pose.transform(XyzAbcTransformation.ofRad(0.0,0.0,-50, 0.0,0.0,0.0));

		//System.out.println("Safety traj point in robot base frame --> x: " + aprox_pose.getX() + " y: " + aprox_pose.getY() + " z: " + aprox_pose.getZ() + 
			//	" A: " + aprox_pose.getAlphaRad() + " B: " + aprox_pose.getBetaRad() + " C: " + aprox_pose.getGammaRad());
	
		roll_scan.getFrame("roll_tcp").move(ptp(aprox_pose).setJointVelocityRel(0.1));
		
		Frame copy_caltab_robot_fr;
		
		copy_caltab_robot_fr = caltab_robot_fr.copy();
		
		point  = traj_caltab_ref_fr.get(i).copy();
								
		copy_caltab_robot_fr.transform(XyzAbcTransformation.ofRad(point.getX(), point.getY(), point.getZ(), 
				point.getAlphaRad(), point.getBetaRad(), point.getGammaRad()));
			
		copy_caltab_robot_fr.setRedundancyInformation(lbr, redundancyInfo);
		
		roll_scan.getFrame("roll_tcp").moveAsync(lin(copy_caltab_robot_fr).setCartVelocity(10).setMode(impedanceControlMode).setBlendingCart(10));
	
		System.out.println("Aprox point in robot base frame --> x: " + copy_caltab_robot_fr.getX() + " y: " + copy_caltab_robot_fr.getY() + " z: " + copy_caltab_robot_fr.getZ() + 
			" A: " + copy_caltab_robot_fr.getAlphaRad() + " B: " + copy_caltab_robot_fr.getBetaRad() + " C: " + copy_caltab_robot_fr.getGammaRad());

		i++;
		
		int point_zone;
		for(; i<x.size();i++)
		{
			point_zone = poseChecking(x.get(i), y.get(i));
			
			if(working_zone == point_zone)
			{
				copy_caltab_robot_fr = caltab_robot_fr.copy();
				
				point  = traj_caltab_ref_fr.get(i).copy();
				
				copy_caltab_robot_fr.transform(XyzAbcTransformation.ofRad(point.getX(), point.getY(), point.getZ(), 
				point.getAlphaRad(), point.getBetaRad(), point.getGammaRad()));
					
				copy_caltab_robot_fr.setRedundancyInformation(lbr, redundancyInfo);
	
				System.out.println(i + " Traj point in robot frame --> x: " + copy_caltab_robot_fr.getX() + " y: " + copy_caltab_robot_fr.getY() + " z: " + copy_caltab_robot_fr.getZ() + 
						" A: " + copy_caltab_robot_fr.getAlphaRad() + " B: " + copy_caltab_robot_fr.getBetaRad() + " C: " + copy_caltab_robot_fr.getGammaRad());
										
				int next_point_zone = poseChecking(x.get(i+1), y.get(i+1));
				
				//if(i<x.size()-1 && !warning_signal.get())
				if((next_point_zone==point_zone) && !warning_signal.get())
				{
					//System.out.println("Warning signal: " + warning_signal.get());
					IMotionContainer motion_cmd = roll_scan.getFrame("roll_tcp").moveAsync(lin(copy_caltab_robot_fr).setCartVelocity(velocidad).setMode(impedanceControlMode).setBlendingCart(10));
					motion_list.add(motion_cmd);
					//System.out.println("Movement list: " + motion_list.size());
				}	
				else
				{
					try
					{								
						//roll_scan.getFrame("roll_tcp").move(lin(copy_caltab_robot_fr).setCartVelocity(velocidad).setBlendingCart(0));
						IMotionContainer motion_cmd = roll_scan.getFrame("roll_tcp").move(lin(copy_caltab_robot_fr).setCartVelocity(velocidad).setMode(impedanceControlMode).setBlendingCart(0));
											
						IFiredConditionInfo firedInfo =  motion_cmd.getFiredBreakConditionInfo();
								 
						 if(firedInfo != null)
						 {
						  System.out.println("pulsador 1 ");
						  warning_signal.set(true);
						 }
						 else
						 {
							Frame current_pose = lbr.getCurrentCartesianPosition(roll_scan.getFrame("roll_tcp"));
							
							current_pose.transform(XyzAbcTransformation.ofRad(0.0,-490,-400,0.0,0.0,0.0));
							
							roll_scan.getFrame("roll_tcp").move(lin(current_pose).setJointVelocityRel(0.25));
							
							roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/DemoCroinspect/SafePose")).setJointVelocityRel(0.25));
		
							move_cont.set(i+1);
							String response_data = frame_id + ";" + operation_type + ";1" ;
							tcp_server.setResponseData(response_data);
							
							
						 }
						
					}
					catch(CommandInvalidException e)
					{
						System.out.println("Last Movement failed and the app was finished");
						try {
							
							String response_data = frame_id + ";" + operation_type + ";0" ;
							tcp_server.setResponseData(response_data);
							
							tcp_server.dispose();
							
						} catch (InterruptedException e1) {
							System.out.println("Closing TCP server from App");
							break;
						}	
					}		
				}
				
				if(warning_signal.get())
				{		
					System.out.println("Warning motion list: " + motion_list.size());
					for(int k=0; k<motion_list.size();k++)
						motion_list.get(k).cancel();
					
					getApplicationControl().setApplicationOverride(current_override);
					
					System.out.println("Performing new scan");
					Frame current_pos = lbr.getCurrentCartesianPosition(roll_scan.getFrame("roll_tcp"));
					
					System.out.println("Current point --> x: " + current_pos.getX() + " y: " + current_pos.getY() + " z: " + current_pos.getZ() + 
						" A: " + current_pos.getAlphaRad() + " B: " + current_pos.getBetaRad() + " C: " + current_pos.getGammaRad());
								
					Frame pose = current_pos.copy();
					pose.setGammaRad(current_pos.getGammaRad() + 30*Math.PI/180);
				
					System.out.println("First point --> x: " + pose.getX() + " y: " + pose.getY() + " z: " + pose.getZ() + 
							" A: " + pose.getAlphaRad() + " B: " + pose.getBetaRad() + " C: " + pose.getGammaRad());
				
					roll_scan.getFrame("roll_tcp").move(lin(pose).setCartVelocity(velocidad).setJointVelocityRel(0.1).setBlendingCart(0));//.setMode(impedanceControlMode)
									
					pose.setGammaRad(current_pos.getGammaRad() - 30*Math.PI/180); 
					System.out.println("Second point --> x: " + pose.getX() + " y: " + pose.getY() + " z: " + pose.getZ() + 
							" A: " + pose.getAlphaRad() + " B: " + pose.getBetaRad() + " C: " + pose.getGammaRad());
				
					roll_scan.getFrame("roll_tcp").move(lin(pose).setCartVelocity(velocidad).setJointVelocityRel(0.1).setBlendingCart(0));//.setMode(impedanceControlMode).setBlendingCart(0));
					
					roll_scan.getFrame("roll_tcp").move(lin(current_pos).setCartVelocity(velocidad).setJointVelocityRel(0.1).setBlendingCart(0));//.setMode(impedanceControlMode).setBlendingCart(0));
					
					next_movement = next_movement + move_cont.get(); 
					motion_list.clear();
					i = next_movement;
					System.out.println("Next movement: " + i);
					
					warning_signal.set(false);
				}
				
				if(movement_failed.get())
				{
					mediaFIO.setLEDBlue(false);
	
					System.out.println("Warning motion list: " + motion_list.size());
					for(int k=0; k<motion_list.size();k++)
						motion_list.get(k).cancel();
					
					getApplicationControl().setApplicationOverride(current_override);
									
					System.out.println("Movement failed. Moving the robot to safe position");
					Frame current_pos = lbr.getCurrentCartesianPosition(roll_scan.getFrame("roll_tcp"));
					
					current_pos.transform(XyzAbcTransformation.ofRad(0.0,-490.0,-40,0.0,0.0,0.0));
					
					roll_scan.getFrame("roll_tcp").move(lin(current_pos).setCartVelocity(25));
					
					roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/DemoCroinspect/SafePose")).setJointVelocityRel(0.25));
				
					String response_data = frame_id + ";" + operation_type + ";0" ;
					tcp_server.setResponseData(response_data);
					
					try {
						tcp_server.dispose();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					break;
				}
				
				copy_caltab_robot_fr= null; // new Frame(caltab_robot_fr);
					
			}
		}
		
		if(i>=x.size())
			move_cont.set(0);
		
		rec.stopRecording();
		System.out.println("Trajectory done");
	}
	
	
	@Override
	public void OnTCPMessageReceived(String datagram)
	{
		System.out.println("OnTCPMessageReceived: " + datagram);

		String splittedData[] = datagram.split(";");
		
		time_stamp = splittedData[0];
		frame_id = Integer.parseInt(splittedData[1]);
		operation_type = splittedData[2];
		
		if(operation_type.compareTo("inspection")==0)
		{
			for(int i=3; i<splittedData.length; i++)
			{
				caltab_pose_data.add(Double.parseDouble(splittedData[i]));
			}
	
			//Frame definition
			caltab_robot_fr = new Frame(getFrame("/robot_base"));
			
			Frame robot_pose = lbr.getCurrentCartesianPosition(lbr.getFlange());
			
			Frame cam_robot_fr = robot_pose.transform(XyzAbcTransformation.ofRad(tcp_camera_fr.getX(),tcp_camera_fr.getY(),tcp_camera_fr.getZ(),
				tcp_camera_fr.getAlphaRad(),tcp_camera_fr.getBetaRad(), tcp_camera_fr.getGammaRad()));
				
			caltab_robot_fr = cam_robot_fr.transform(XyzAbcTransformation.ofRad(caltab_pose_data.get(0)*1000, caltab_pose_data.get(1)*1000, caltab_pose_data.get(2)*1000, 
					caltab_pose_data.get(5), caltab_pose_data.get(4), caltab_pose_data.get(3)));
					
			System.out.println("Caltab in robot base frame --> x: " + caltab_robot_fr.getX() + " y: " + caltab_robot_fr.getY() + " z: " + caltab_robot_fr.getZ() + 
				" A: " + caltab_robot_fr.getAlphaRad()*(180/Math.PI)+ " B: " + caltab_robot_fr.getBetaRad()*(180/Math.PI)+ " C: " + caltab_robot_fr.getGammaRad()*(180/Math.PI));
		}
			
		data_received.set(true);
	}
	
	private boolean waitUntilRobotAlmostStopped(double timeOut)
	{
		JointPosition currentJP;
		JointPosition lastJP = new JointPosition(0, 0, 0, 0, 0, 0, 0);
		long before = System.currentTimeMillis();
		long now;
		boolean robotAlmostStopped = false;
		
		do {
			currentJP = lbr.getCurrentJointPosition();
			ThreadUtil.milliSleep(10);
			robotAlmostStopped = currentJP.isNearlyEqual(lastJP, Math.toRadians(0.2));
			lastJP = currentJP;
			now = System.currentTimeMillis();
			if (timeOut > 0 && ((now - before) > timeOut)) {
				break;
			}
		} while (!robotAlmostStopped);
		if (robotAlmostStopped) {
			System.out.println("Utils.waitUntilRobotAlmostStopped(...): Robot almost stopped");
		}
		return robotAlmostStopped;
	}


	@Override
	public void OnTCPConnection() {
		// TODO Auto-generated method stub
		
	}

	

	@Override
	public void OnSignalReceived(Boolean data) {
		
		// TODO Auto-generated method stub
		System.out.println("Boton pulsado");
		
		current_override = getApplicationControl().getApplicationOverride();
		warning_signal.set(true);
		
		getApplicationControl().clipApplicationOverride(0.0);
		
		waitUntilRobotAlmostStopped(-1);
		
		for(int j=0; j < motion_list.size(); j++ )
		{
			if(!motion_list.get(j).isFinished())
			{
				if(motion_list.get(j).getState() == ExecutionState.Executing)
				{
					move_cont.set(j);
					canceled_motion = motion_list.get(j).getCurrentMotion();
					System.out.println("Running motion--> " + motion_list.get(j).getCurrentMotion().toString());
					break;
				}
				//motion_list.get(j).cancel();
			}
		}
		
		controller.getExecutionService().cancelAll();
		
		System.out.println("OnSignalReceived motion list: " + motion_list.size());
			
		//motion_list.clear();
		System.out.println("Alarma activado");
	}
}


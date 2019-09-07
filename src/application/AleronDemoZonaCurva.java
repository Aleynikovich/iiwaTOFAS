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
import java.text.DecimalFormat;
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

public class AleronDemoZonaCurva extends RoboticsAPIApplication implements ITCPListener, ISignalListener{
	
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
	ArrayList<Double> i = new ArrayList<Double>();
	ArrayList<Double> j = new ArrayList<Double>();
	ArrayList<Double> k = new ArrayList<Double>();
	ArrayList<Double> z_rot = new ArrayList<Double>();											   
	ArrayList<Double> a = new ArrayList<Double>();
	ArrayList<Double> b = new ArrayList<Double>();
	ArrayList<Double> c = new ArrayList<Double>();
	/*ArrayList<Double> a_n = new ArrayList<Double>();
	ArrayList<Double> b_n = new ArrayList<Double>();
	ArrayList<Double> c_n = new ArrayList<Double>();*/

	//Frames
	Frame tcp_camera_fr;
		
	Frame aileron_caltab_fr;
	ArrayList<Frame> traj_caltab_ref_fr = new ArrayList<Frame>();
	
	DataRecorder rec;
	
	private TCPServer tcp_server;
	AtomicBoolean data_received;
	
	//Exchanged data info 
	String operation_type;
	String time_stamp;
	int frame_id, frame_id_2;
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
		System.out.println("Roll scan frame: " + roll_scan.getFrame("Gripper").toString());

		data_received = new AtomicBoolean(false);
		
		movement_failed = new AtomicBoolean(false);
		failed_movement_nbr = new AtomicInteger(0);
		
		warning_signal = new AtomicBoolean(false);
		move_cont = new AtomicInteger(0);
		next_movement = 0;
		
		current_override= 1;
		
		//Frames definition
		tcp_camera_fr = new Frame(lbr.getFlange());
		
		tcp_camera_fr.setX(-0.0159354768400467*1000); tcp_camera_fr.setY(-0.104514274387134*1000); tcp_camera_fr.setZ(0.0970593038223472*1000);
		tcp_camera_fr.setAlphaRad(359.645275027744*(Math.PI/180)); tcp_camera_fr.setBetaRad(0.554167986910158*(Math.PI/180)); tcp_camera_fr.setGammaRad(0.77876180705853*(Math.PI/180));


		Frame pose = new Frame(getFrame("/DemoCroinspect/aileron"));
	
		//Catlab Aileron frame definition
		pose.setX(-1.2073015441932*1000); pose.setY(-0.248037023536067*1000); pose.setZ(-0.277826501203178*1000);
		pose.setAlphaRad(272.688617372343*(Math.PI/180)); pose.setBetaRad(348.11918885943 *(Math.PI/180)); pose.setGammaRad(179.235054731383*(Math.PI/180));
		
		//Getting the inverse frame (Aileron - Caltab2)
		Transformation t = pose.getTransformationFromParent().invert();
		aileron_caltab_fr = new Frame(getFrame("/DemoCroinspect/caltab"), t);
	
		//Impedance control object definition
		impedanceControlMode =	new CartesianImpedanceControlMode();
		
				
		String str;
		String file = "C:\\Users\\KukaUser\\Desktop\\CADTraj\\Plana_Con.MPF";
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
		    	  	    	  
		    	 for(int l=0; l<data.length; l++)
		    	 {
		    		 if(data[l].contains("X="))
		    		 {
		    			 val_str= data[l].split("X=");
		    			 val = Double.parseDouble(val_str[1]);
		    			 x.add(val);
		    		 }
		    		 else if(data[l].contains("Y="))
		    		 {
		    			 val_str= data[l].split("Y=");
		    			 val = Double.parseDouble(val_str[1]);
		    			 y.add(val);
		    		 }
		    		 else if(data[l].contains("Z="))
		    		 {
		    			 val_str= data[l].split("Z=");
		    			 val = Double.parseDouble(val_str[1]);
		    			 z.add(val);
		    		 }
		    		 else if(data[l].contains("A="))
		    		 {
		    			 val_str= data[l].split("A=");
		    			 val = Double.parseDouble(val_str[1]);
		    			 a.add(val*(Math.PI/180)) ;
		    		 }
		    		 else if(data[l].contains("B="))
		    		 {
		    			 val_str= data[l].split("B=");
		    			 val = Double.parseDouble(val_str[1]);
		    			 b.add(val* (Math.PI/180));
		    		 }
		    		 else if(data[l].contains("C="))
		    		 {
		    			 val_str= data[l].split("C=");
		    			 val = Double.parseDouble(val_str[1]);
		    			 c.add(val*(Math.PI/180));
		    		 }
		    	 }
		    	  
		    	 pose.setParent(getFrame("/DemoCroinspect/aileron"));
		    	 pose.setX(x.get(cont)); pose.setY(y.get(cont)); pose.setZ(z.get(cont));
		    	 pose.setAlphaRad(a.get(cont)); pose.setBetaRad(b.get(cont)); pose.setGammaRad(c.get(cont));
	 		 
				 pose.transform(XyzAbcTransformation.ofDeg(0.0, 0.0, 0.0, -90, 0.0, 180.0));	
				 
				 Frame copy_aileron_caltab_fr = aileron_caltab_fr.copy();
				 
				 copy_aileron_caltab_fr.transform(XyzAbcTransformation.ofRad(pose.getX(), pose.getY(), pose.getZ(),
		    	 pose.getAlphaRad(), pose.getBetaRad(), pose.getGammaRad()));
    		   	 
 		    	//System.out.println("Ref Caltab frame --> x: " + aileron_caltab_fr.getX() + " y: " + aileron_caltab_fr.getY() + " z: " + aileron_caltab_fr.getZ() + 
 					//" A: " + aileron_caltab_fr.getAlphaRad()*(180/Math.PI) + " B: " + aileron_caltab_fr.getBetaRad()*(180/Math.PI) + " C: " + aileron_caltab_fr.getGammaRad()*(180/Math.PI));
 		
	    		 traj_caltab_ref_fr.add(copy_aileron_caltab_fr);
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
			if(traj_caltab_ref_fr.get(i).getX()== traj_caltab_ref_fr.get(i-1).getX())
			{	
				traj_caltab_ref_fr.get(i).transform(XyzAbcTransformation.ofDeg(0.0, 0.0, -60.0, 0.0, 0.0, 0.0));
				
				/*System.out.println(i + " Traj Position in caltab frame --> x: " + traj_caltab_ref_fr.get(i).getX() + 
					" y: " + traj_caltab_ref_fr.get(i).getY() + " z: " + traj_caltab_ref_fr.get(i).getZ() + 
						" A: " + traj_caltab_ref_fr.get(i).getAlphaRad() + " B: " + traj_caltab_ref_fr.get(i).getBetaRad() + 
							" C: " + traj_caltab_ref_fr.get(i).getGammaRad());
				*/
				i++;
				
				traj_caltab_ref_fr.get(i).transform(XyzAbcTransformation.ofDeg(0.0, 0.0, -60.0, 0.0, 0.0, 0.0));
				
				/*System.out.println(i + " Traj Position in caltab frame --> x: " + traj_caltab_ref_fr.get(i).getX() + 
						" y: " + traj_caltab_ref_fr.get(i).getY() + " z: " + traj_caltab_ref_fr.get(i).getZ() + 
							" A: " + traj_caltab_ref_fr.get(i).getAlphaRad() + " B: " + traj_caltab_ref_fr.get(i).getBetaRad() + 
								" C: " + traj_caltab_ref_fr.get(i).getGammaRad());
				*/
				i++;
			}
			
			/*System.out.println(i + " Traj Position in caltab frame --> x: " + traj_caltab_ref_fr.get(i).getX() + 
					" y: " + traj_caltab_ref_fr.get(i).getY() + " z: " + traj_caltab_ref_fr.get(i).getZ() + 
						" A: " + traj_caltab_ref_fr.get(i).getAlphaRad() + " B: " + traj_caltab_ref_fr.get(i).getBetaRad() + 
							" C: " + traj_caltab_ref_fr.get(i).getGammaRad());
			*/
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
		signal_monitor = new SignalsMonitor(mediaFIO);
		signal_monitor.addListener(this);
		signal_monitor.enable();
		
		//Configure media outputs
		mediaFIO.setOutputX3Pin1(false);
		mediaFIO.setOutputX3Pin11(false);
		mediaFIO.setOutputX3Pin12(false);
		
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
					roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/DemoCroinspect/Aprox5")).setJointVelocityRel(0.25));
					
					roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/DemoCroinspect/Aprox6")).setJointVelocityRel(0.25));
			
					String response_data = frame_id + ";" + operation_type + ";1" ;
					tcp_server.setResponseData(response_data);	
					
				}
				else if (operation_type.compareTo("inspection") == 0)
				{
				
					roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/DemoCroinspect/Aprox6")).setJointVelocityRel(0.25));

					rec = new DataRecorder();
					rec.setTimeout(2L, TimeUnit.MINUTES);
				
					getLogger().info("Selected 20N and 25mm/s");
					select_velocity=10;
					fname="measured_force_20ND_stiffZ_300_"+select_velocity+"mm_S.log";
					try {
						Force_XND(10,fname,10);
					} catch (IOException e) {
						System.out.println("IO Exception in Force_XND 10");
					}
					
				}
				else if(operation_type.compareTo("end") == 0)
				{
					
				}
			}
		} while (!exit);
		
		System.out.println("Finish AleronDemoZonaCurva Run ");
		
	}
	
	private void closeCommunication() throws IOException
	{
		try {
			
			System.out.println("closeCommunication");
			tcp_server.dispose();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			System.out.println("App InterruptedException");
		}
	}
	
	boolean checkEqualPoints(Frame A, Frame B)
	{
		
		DecimalFormat formato = new DecimalFormat("#.0000000000");
		String x_a = formato.format(A.getX()); String x_b = formato.format(B.getX()); 
		String y_a = formato.format(A.getY()); String y_b = formato.format(B.getY());
		String z_a = formato.format(A.getZ()); String z_b = formato.format(B.getZ());
		String alpha_a = formato.format(A.getAlphaRad()); String alpha_b = formato.format(B.getAlphaRad());
		String beta_a = formato.format(A.getBetaRad()); String beta_b = formato.format(B.getBetaRad());
		String gamma_a = formato.format(A.getGammaRad()); String gamma_b = formato.format(B.getGammaRad());
		
		/*System.out.println("A Traj point in caltab frame --> x: " + x_a + " y: " + y_a + " z: " + z_a + 
				" A: " + alpha_a + " B: " + beta_a + " C: " + gamma_a);

		System.out.println("B Traj point in caltab frame --> x: " +  x_b + " y: " + y_b + " z: " + z_b + 
				" A: " + alpha_b + " B: " + beta_b + " C: " + gamma_b );
		*/
		//if((x_a == x_b) && (y_a == y_b) && (z_a == z_b) && (alpha_a == alpha_b) && (beta_a == beta_b) && (gamma_a==gamma_b))
		if((x_a.equals(x_b)) && (y_a.equals(y_b)) && (z_a.equals(z_b)) && (alpha_a.equals(alpha_b)) && (beta_a.equals(beta_b)) && (gamma_a.equals(gamma_b)))
			return true;
		else
			return false;
	}
	
	
	void setVelOuputConf()
	{
		double override = getApplicationControl().getApplicationOverride();
		if(override == 1.0)
		{
			mediaFIO.setOutputX3Pin11(true);
			mediaFIO.setOutputX3Pin12(false);
		}
		else if(override == 0.5)
		{
			mediaFIO.setOutputX3Pin11(true);
			mediaFIO.setOutputX3Pin12(false);
		}
		else if(override == 0.0)
		{
			mediaFIO.setOutputX3Pin11(true);
			mediaFIO.setOutputX3Pin12(true);
		}
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
		System.out.println("i value: " + i);
			
		point  = traj_caltab_ref_fr.get(i).copy();
		aprox_pose.transform(XyzAbcTransformation.ofRad(point.getX(), point.getY(), point.getZ(), 
				point.getAlphaRad(), point.getBetaRad(), point.getGammaRad()));
							
		System.out.println("Traj point in robot base frame --> x: " + aprox_pose.getX() + " y: " + aprox_pose.getY() + " z: " + aprox_pose.getZ() + 
				" A: " + aprox_pose.getAlphaRad()*(180/Math.PI)  + " B: " + aprox_pose.getBetaRad()*(180/Math.PI)  + " C: " + aprox_pose.getGammaRad()*(180/Math.PI));
					
		aprox_pose.transform(XyzAbcTransformation.ofRad(0.0,0.0,-50, 0.0,0.0,0.0));

		roll_scan.getFrame("Gripper").move(ptp(aprox_pose).setJointVelocityRel(0.1));
		
		Frame copy_caltab_robot_fr;
		
		copy_caltab_robot_fr = caltab_robot_fr.copy();
		
		point  = traj_caltab_ref_fr.get(i).copy();
								
		copy_caltab_robot_fr.transform(XyzAbcTransformation.ofRad(point.getX(), point.getY(), point.getZ(), 
				point.getAlphaRad(), point.getBetaRad(), point.getGammaRad()));
			
		copy_caltab_robot_fr.setRedundancyInformation(lbr, redundancyInfo);
		
		roll_scan.getFrame("Gripper").move(lin(copy_caltab_robot_fr).setCartVelocity(10).setMode(impedanceControlMode).setBlendingCart(0));
		
		//Robot in contact with the aileron, notify to NDT system
		mediaFIO.setOutputX3Pin1(true);
		setVelOuputConf();
		
		SharedData.sinc_data=true;
		
		System.out.println("Aprox point in robot base frame --> x: " + copy_caltab_robot_fr.getX() + " y: " + copy_caltab_robot_fr.getY() + " z: " + copy_caltab_robot_fr.getZ() + 
			" A: " + copy_caltab_robot_fr.getAlphaRad()*(180/Math.PI)  + " B: " + copy_caltab_robot_fr.getBetaRad()*(180/Math.PI) + " C: " + copy_caltab_robot_fr.getGammaRad()*(180/Math.PI) );

		i++;
		
		int point_zone;
		for(; i<x.size();i++)
		{			
			
			copy_caltab_robot_fr = caltab_robot_fr.copy();
				
			point  = traj_caltab_ref_fr.get(i).copy();
				
			copy_caltab_robot_fr.transform(XyzAbcTransformation.ofRad(point.getX(), point.getY(), point.getZ(), 
			point.getAlphaRad(), point.getBetaRad(), point.getGammaRad()));
					
			copy_caltab_robot_fr.setRedundancyInformation(lbr, redundancyInfo);
				
			if((i< x.size()-1) && !warning_signal.get())
			{
					
				//Comprobar si en el proximo punto tiene contacto o no con la superficie
				Frame contactless_point = traj_caltab_ref_fr.get(i+1).copy();
				Frame contact_point = traj_caltab_ref_fr.get(i-1).copy();
				contactless_point.transform(XyzAbcTransformation.ofRad(0.0, 0.0,60,0.0,0.0,0.0));
				contact_point.transform(XyzAbcTransformation.ofRad(0.0, 0.0,60,0.0,0.0,0.0));
					
				System.out.println("Traj point in caltab frame --> x: " + point.getX() + " y: " + point.getY() + " z: " + point.getZ() + 
					" A: " + point.getAlphaRad()*(180/Math.PI) + " B: " + point.getBetaRad()*(180/Math.PI) + " C: " + point.getGammaRad()*(180/Math.PI) );

				System.out.println("Conctactless Traj point in caltab frame --> x: " + contactless_point.getX() + " y: " + contactless_point.getY() + " z: " + contactless_point.getZ() + 
					" A: " + contactless_point.getAlphaRad()*(180/Math.PI) + " B: " + contactless_point.getBetaRad()*(180/Math.PI) + " C: " + contactless_point.getGammaRad()*(180/Math.PI) );

				System.out.println("Conctact Traj point in caltab frame --> x: " + contact_point.getX() + " y: " + contact_point.getY() + " z: " + contact_point.getZ() + 
					" A: " + contact_point.getAlphaRad()*(180/Math.PI) + " B: " + contact_point.getBetaRad()*(180/Math.PI) + " C: " + contact_point.getGammaRad()*(180/Math.PI) );
					
				//boolean res = checkEqualPoints(point,contactless_point);
				//System.out.println("Response:" +  res);
				if(checkEqualPoints(point,contactless_point))
				{
					System.out.println(i + " Traj point in robot frame --> x: " + copy_caltab_robot_fr.getX() + " y: " + copy_caltab_robot_fr.getY() + " z: " + copy_caltab_robot_fr.getZ() + 
						" A: " + copy_caltab_robot_fr.getAlphaRad()*(180/Math.PI) + " B: " + copy_caltab_robot_fr.getBetaRad()*(180/Math.PI) + " C: " + copy_caltab_robot_fr.getGammaRad()*(180/Math.PI) );
				
					IMotionContainer motion_cmd = roll_scan.getFrame("Gripper").move(lin(copy_caltab_robot_fr).setCartVelocity(velocidad).setMode(impedanceControlMode).setBlendingCart(0));
					motion_list.add(motion_cmd);
						
					IFiredConditionInfo firedInfo =  motion_cmd.getFiredBreakConditionInfo();
						 
					if(firedInfo != null)
					{
						System.out.println("pulsador 1 ");
						warning_signal.set(true);
					}
					else
					{
						System.out.println("Starting lateral movement");
						//El robot se separa del aleron en el proximo movimiento
						//  - Desactivar salida
						mediaFIO.setOutputX3Pin1(false);
					}
				}
				else if (checkEqualPoints(point,contact_point))
				{
					System.out.println(i + " Traj point in robot frame --> x: " + copy_caltab_robot_fr.getX() + " y: " + copy_caltab_robot_fr.getY() + " z: " + copy_caltab_robot_fr.getZ() + 
						" A: " + copy_caltab_robot_fr.getAlphaRad()*(180/Math.PI) + " B: " + copy_caltab_robot_fr.getBetaRad()*(180/Math.PI) + " C: " + copy_caltab_robot_fr.getGammaRad()*(180/Math.PI) );
				
					IMotionContainer motion_cmd = roll_scan.getFrame("Gripper").move(lin(copy_caltab_robot_fr).setCartVelocity(10).setMode(impedanceControlMode).setBlendingCart(0));
					motion_list.add(motion_cmd);
						
					IFiredConditionInfo firedInfo =  motion_cmd.getFiredBreakConditionInfo();
						 
					if(firedInfo != null)
					{
						System.out.println("pulsador 1 ");
						warning_signal.set(true);
					}
					else
					{
						System.out.println("Finishing lateral movement");
						//El robot entra en coctacto con el aleron en el proximo movimiento
						//  - Activar salida
						mediaFIO.setOutputX3Pin1(true);
						setVelOuputConf();
					}
				}
				else
				{
					//Movimiento de flanco de subida o de bajada de la almena
					//System.out.println("Warning signal: " + warning_signal.get());
					IMotionContainer motion_cmd = roll_scan.getFrame("Gripper").moveAsync(lin(copy_caltab_robot_fr).setCartVelocity(velocidad).setMode(impedanceControlMode).setBlendingCart(10));
					motion_list.add(motion_cmd);
					System.out.println(i + " Traj point in robot frame --> x: " + copy_caltab_robot_fr.getX() + " y: " + copy_caltab_robot_fr.getY() + " z: " + copy_caltab_robot_fr.getZ() + 
						" A: " + copy_caltab_robot_fr.getAlphaRad()*(180/Math.PI) + " B: " + copy_caltab_robot_fr.getBetaRad()*(180/Math.PI) + " C: " + copy_caltab_robot_fr.getGammaRad()*(180/Math.PI) );
						
				}
				
				//System.out.println("Movement list: " + motion_list.size());
			}	
			else
			{
				if( !warning_signal.get())
				{
					try
					{								
						//roll_scan.getFrame("roll_tcp").move(lin(copy_caltab_robot_fr).setCartVelocity(velocidad).setBlendingCart(0));
						System.out.println("move_sync");
						IMotionContainer motion_cmd = roll_scan.getFrame("Gripper").move(lin(copy_caltab_robot_fr).setCartVelocity(velocidad).setMode(impedanceControlMode).setBlendingCart(0));
							
						System.out.println(i + " Traj point in robot frame --> x: " + copy_caltab_robot_fr.getX() + " y: " + copy_caltab_robot_fr.getY() + " z: " + copy_caltab_robot_fr.getZ() + 
							" A: " + copy_caltab_robot_fr.getAlphaRad()*(180/Math.PI) + " B: " + copy_caltab_robot_fr.getBetaRad()*(180/Math.PI) + " C: " + copy_caltab_robot_fr.getGammaRad()*(180/Math.PI) );
					
						IFiredConditionInfo firedInfo =  motion_cmd.getFiredBreakConditionInfo();
									 
						if(firedInfo != null)
						{
							System.out.println("pulsador 1 ");
							warning_signal.set(true);
						}
						else
						{
							//Robot move away from the aileron, notify to NDT system
							System.out.println("Robot moves away from the aileron ");
							mediaFIO.setOutputX3Pin1(false);
								
							System.out.println("Back to the safe pose");
							Frame current_pose = lbr.getCurrentCartesianPosition(roll_scan.getFrame("roll_tcp"));
								
							current_pose.transform(XyzAbcTransformation.ofRad(90.0,-400.0,0.0,0.0,0.0,0.0));
								
							System.out.println("Pose before safe pose in robot frame --> x: " + current_pose.getX() + " y: " + current_pose.getY() + " z: " + current_pose.getZ() + 
								" A: " + current_pose.getAlphaRad()*(180/Math.PI) + " B: " + current_pose.getBetaRad()*(180/Math.PI) + " C: " + current_pose.getGammaRad()*(180/Math.PI) );
	
							roll_scan.getFrame("roll_tcp").move(lin(current_pose).setJointVelocityRel(0.25));
								
														
							roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/DemoCroinspect/SafePose")).setJointVelocityRel(0.25));
			
							move_cont.set(i+1);
							String response_data = frame_id + ";" + operation_type + ";1" ;
							tcp_server.setResponseData(response_data);
								
							break;
						}	
					}
					catch(CommandInvalidException e)
					{
						System.out.println("Last Movement failed and the app was finished");
						SharedData.sinc_data=false;
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
				pose.setGammaRad(current_pos.getGammaRad() + 20*Math.PI/180);
				
				System.out.println("First point --> x: " + pose.getX() + " y: " + pose.getY() + " z: " + pose.getZ() + 
					" A: " + pose.getAlphaRad() + " B: " + pose.getBetaRad() + " C: " + pose.getGammaRad());
				
				roll_scan.getFrame("roll_tcp").move(lin(pose).setCartVelocity(velocidad).setJointVelocityRel(0.1).setBlendingCart(0));//.setMode(impedanceControlMode)
									
				pose.setGammaRad(current_pos.getGammaRad() - 20*Math.PI/180); 
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
				System.out.println("Robot moves away from the aileron ");
				mediaFIO.setOutputX3Pin1(false);

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
	
		if(i>=x.size())
			move_cont.set(0);
		
		rec.stopRecording();

		SharedData.sinc_data=false;
		
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
	
			//caltab_pose_data.set(3, (caltab_pose_data.get(3) - 0.025)); 
			
			//Frame definition
			caltab_robot_fr = new Frame(getFrame("/robot_base"));
			
			Frame robot_pose = lbr.getCurrentCartesianPosition(lbr.getFlange());
			
			Frame cam_robot_fr = robot_pose.transform(XyzAbcTransformation.ofRad(tcp_camera_fr.getX(),tcp_camera_fr.getY(),tcp_camera_fr.getZ(),
				tcp_camera_fr.getAlphaRad(),tcp_camera_fr.getBetaRad(), tcp_camera_fr.getGammaRad()));
				
			caltab_robot_fr = cam_robot_fr.transform(XyzAbcTransformation.ofRad(caltab_pose_data.get(0)*1000, caltab_pose_data.get(1)*1000, caltab_pose_data.get(2)*1000, 
					caltab_pose_data.get(5), caltab_pose_data.get(4), caltab_pose_data.get(3)));
					
			System.out.println("Caltab in robot base frame --> x: " + caltab_robot_fr.getX() + " y: " + caltab_robot_fr.getY() + " z: " + caltab_robot_fr.getZ() + 
				" A: " + caltab_robot_fr.getAlphaRad()*(180/Math.PI)+ " B: " + caltab_robot_fr.getBetaRad()*(180/Math.PI)+ " C: " + caltab_robot_fr.getGammaRad()*(180/Math.PI));
			
			
			/*caltab_robot_fr.transform(XyzAbcTransformation.ofDeg(0.0, 0.0, 0.0, 90.0, 0.0, 0.0));

			roll_scan.getFrame("Gripper").move(ptp(caltab_robot_fr).setJointVelocityRel(0.1));
			
			getApplicationUI().displayModalDialog(
					ApplicationDialogType.QUESTION,"OK");
			
			roll_scan.getFrame("Gripper").move(ptp(getFrame("/DemoCroinspect/Aprox3")).setJointVelocityRel(0.25));
			*/
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
					System.out.println("Move_cont after alarm signal" + move_cont.get());
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


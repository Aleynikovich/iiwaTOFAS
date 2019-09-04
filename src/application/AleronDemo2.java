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

public class AleronDemo2 extends RoboticsAPIApplication implements ITCPListener,ITCPListener2, ISignalListener{
	
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
		
	ArrayList<Frame> aileron_caltabs_fr_list = new ArrayList<Frame>();
	ArrayList<Frame> traj_caltab_ref_fr = new ArrayList<Frame>();
	
	DataRecorder rec;
	
	private TCPServer tcp_server;
	AtomicBoolean data_received;
	
	private TCPServer2 tcp_server_2;

	
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
		
		//tcp_camera_fr.setX(-13.7524); tcp_camera_fr.setY(-103.628); tcp_camera_fr.setZ(96.0968);
		//tcp_camera_fr.setAlphaRad(2.11153*(Math.PI/180)); tcp_camera_fr.setBetaRad(1.30715*(Math.PI/180)); tcp_camera_fr.setGammaRad(0.784201*(Math.PI/180));

		//tcp_camera_fr.setX(-0.0160987087577798*1000); tcp_camera_fr.setY(-0.104177406893625*1000); tcp_camera_fr.setZ(0.0971637611000496*1000);
		//tcp_camera_fr.setAlphaRad(359.600196567427*(Math.PI/180)); tcp_camera_fr.setBetaRad(0.553586192503848*(Math.PI/180)); tcp_camera_fr.setGammaRad(0.825847140300407*(Math.PI/180));

		tcp_camera_fr.setX(-0.0159354768400467*1000); tcp_camera_fr.setY(-0.104514274387134*1000); tcp_camera_fr.setZ(0.0970593038223472*1000);
		tcp_camera_fr.setAlphaRad(359.645275027744*(Math.PI/180)); tcp_camera_fr.setBetaRad(0.554167986910158*(Math.PI/180)); tcp_camera_fr.setGammaRad(0.77876180705853*(Math.PI/180));


		Frame pose = new Frame(getFrame("/DemoCroinspect/aileron"));
		
		//Catlab1 Aileron frame definition
		//pose.setX(-0.206668293869192 * 1000); pose.setY(0.0938811705121523*1000); pose.setZ(0.0231866294311087*1000);
		//pose.setAlphaRad(273.904622591501*(Math.PI/180)); pose.setBetaRad(350.013193339373*(Math.PI/180)); pose.setGammaRad(178.636440604839*(Math.PI/180));
				
		pose.setX(-0.2168230548698 * 1000); pose.setY(-0.190052857003747*1000); pose.setZ(-0.244231885123741*1000);
		pose.setAlphaRad(273.241346054155*(Math.PI/180)); pose.setBetaRad(348.152835862991*(Math.PI/180)); pose.setGammaRad(179.885704865538*(Math.PI/180));

		
		//pose.setX(0.02 * 1000); pose.setY(0.43*1000); pose.setZ(0.005*1000);
		//pose.setAlphaRad(-Math.PI/2); pose.setBetaRad(Math.PI); pose.setGammaRad(0.0);
		
		System.out.println("Caltab Aileron Frame --> x: " + pose.getX() + "  y: " + pose.getY() + "  z: " + pose.getZ() 
				+ "  A: " + pose.getAlphaRad() + "  B: " + pose.getBetaRad() + "  C: " + pose.getGammaRad());
		
		//Getting the inverse frame (Aileron - Caltab)
		Transformation t = pose.getTransformationFromParent().invert();
		Frame pose_inv = new Frame(getFrame("/DemoCroinspect/caltab"), t);
		
		System.out.println("Aileron caltab  --> x: " + pose_inv.getX() + "  y: " + pose_inv.getY() + "  z: " + pose_inv.getZ() 
			+ "  A: " + pose_inv.getAlphaRad() + "  B: " + pose_inv.getBetaRad() + "  C: " + pose_inv.getGammaRad());
	
		aileron_caltabs_fr_list.add(pose_inv);

		//Catlab2 Aileron frame definition
		pose.setX(-1.2073015441932*1000); pose.setY(-0.248037023536067*1000); pose.setZ(-0.277826501203178*1000);
		pose.setAlphaRad(272.688617372343*(Math.PI/180)); pose.setBetaRad(348.11918885943 *(Math.PI/180)); pose.setGammaRad(179.235054731383*(Math.PI/180));
		
		//pose.setX(1.078 * 1000); pose.setY(0.43*1000); pose.setZ(0.005*1000);
		//pose.setAlphaRad(-Math.PI/2); pose.setBetaRad(Math.PI); pose.setGammaRad(0.0);

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
		    		
		    		 /*else if(data[l].contains("I="))
		    		 {
		    			 val_str= data[l].split("I=");
		    			 val = Double.parseDouble(val_str[1]);
		    			 i.add(val);
		    		 }
		    		 else if(data[l].contains("J="))
		    		 {
		    			 val_str= data[l].split("J=");
		    			 val = Double.parseDouble(val_str[1]);
		    			 j.add(val);
		    		 }
		    		 else if(data[l].contains("K="))
		    		 {
		    			 val_str= data[l].split("K=");
		    			 val = Double.parseDouble(val_str[1]);
		    			 k.add(val);
		    		 }
		    		 else if(data[l].contains("ZROT="))
		    		 {
		    			 val_str= data[l].split("ZROT=");
		    			 val = Double.parseDouble(val_str[1]);
		    			 z_rot.add(val);
		    		 }*/
		    		 /*else if(data[i].contains("AN3="))
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
		    	 	 */
		    	 }
		    	 
	    		 //Conversion from cosines directors to euler angles
		    	// double[] axis_x, axis_y,axis_z;
		    	 
		    	// axis_x = new double[3]; axis_y = new double[3]; axis_z = new double[3];
		    	 /*
		    	 axis_x[0] = 1; axis_x[1] = 0; axis_x[2] = 0;	
		    	 axis_z[0] = i.get(cont); axis_z[1] = j.get(cont); axis_z[2] = k.get(cont);
		    	 
		    	 double[] result = new double[3];
		    	 result = productVector(axis_z, axis_x); 
		    	 double module = moduleVector(productVector(axis_z, axis_x));
		    	 axis_y = divisionVector(result,module);
		    	 
		    	 result = productVector(axis_y, axis_z); 
		    	 module = moduleVector(productVector(axis_y, axis_z));
		    	 axis_x = divisionVector(result,module);
		    	 */
		    	 
		    	 /*axis_x[0] = i.get(cont); axis_x[1] = 0.0; axis_x[2] = 0.0;	
		    	 axis_y[0] = 0.0; axis_y[1] = j.get(cont); axis_y[2] = 0.0;	
		    	 axis_z[0] = 0.0; axis_z[1] = 0.0; axis_z[2] = k.get(cont);
		
		    	 
		    	 double matrix[][] = new double[3][3];
		    	 
		    	 matrix[0] = axis_x; matrix[1] = axis_y; matrix[2] = axis_z;
		    	 
		    	 double B =Math.atan2(matrix[0][2],Math.sqrt(Math.pow(matrix[1][2],2)+ Math.pow(matrix[2][2],2)));
		    	 
		    	 double A,C;
			    	 
		    	 if(Math.cos(B)!=0)
		    	 {
		    	 	A = -Math.atan2((matrix[1][2]/Math.cos(B)),(matrix[2][2]/Math.cos(B)));
		    	 	C = -Math.atan2((matrix[0][1]/Math.cos(B)),(matrix[0][0]/Math.cos(B)));
		    	 }
		    	 else
		    	 {
		    	 	A = 0;
		    	 	if(B ==90)
		    	 		C = Math.atan2(matrix[2][1],matrix[1][1]);
		    	 
		    	 	else
		    	 		C = -Math.atan2(matrix[2][1],matrix[1][1]);
		    	 }*/
		    	 
		    	 pose.setParent(getFrame("/DemoCroinspect/aileron"));
		    	 pose.setX(x.get(cont)); pose.setY(y.get(cont)); pose.setZ(z.get(cont));
		    	 pose.setAlphaRad(a.get(cont)); pose.setBetaRad(b.get(cont)); pose.setGammaRad(c.get(cont));
	 		 
				 //System.out.println(cont + " Traj point in aileron frame --> x: " + pose.getX() + " y: " + pose.getY() + " z: " + pose.getZ() + 
					//" A: " + pose.getAlphaRad() * (180/Math.PI) + " B: " + pose.getBetaRad()  * (180/Math.PI)+ " C: " + pose.getGammaRad() * (180/Math.PI));	
	
				 //pose.transform(XyzAbcTransformation.ofDeg(0.0, 0.0, 0.0, z_rot.get(cont), 0.0, 0.0));

				 //System.out.println(cont + " Traj point after z_rot in aileron frame --> x: " + pose.getX() + " y: " + pose.getY() + " z: " + pose.getZ() + 
					//" A: " + pose.getAlphaRad() * (180/Math.PI) + " B: " + pose.getBetaRad() * (180/Math.PI) + " C: " + pose.getGammaRad() * (180/Math.PI));	
				 
				 //pose.transform(XyzAbcTransformation.ofDeg(0.0, 0.0, 0.0, 0.0, 180.0, 0.0));
				 pose.transform(XyzAbcTransformation.ofDeg(0.0, 0.0, 0.0, 90, 0.0, 180.0));	
	    		 Frame aileron_caltab_fr;
	    		 //Definicion de la recta en el punto x=1106 (ultimo punto asociado a la primera caltab)
	    		 // y = -3.319181909*x + 3934.20009684124
	 		
				 int zone_id = poseChecking(x.get(cont), y.get(cont));
	    		 
	    		 if(zone_id ==1)
	    		 {
	    			 //System.out.println("Point " + cont + "Caltab 1"); 
		    		 aileron_caltab_fr = aileron_caltabs_fr_list.get(0).copy();
		    		 //System.out.println("Caltab 1 --> x: " + aileron_caltab_fr.getX() + " y: " + aileron_caltab_fr.getY() + " z: " + aileron_caltab_fr.getZ() + 
						//		" A: " + aileron_caltab_fr.getAlphaRad() + " B: " + aileron_caltab_fr.getBetaRad() + " C: " + aileron_caltab_fr.getGammaRad());	
	    		 }
	    		 else
	    		 {
	    			 //System.out.println("Point " + cont + "Caltab 2"); 
		    		 aileron_caltab_fr = aileron_caltabs_fr_list.get(1).copy();
		    		//System.out.println("Caltab 2 --> x: " + aileron_caltab_fr.getX() + " y: " + aileron_caltab_fr.getY() + " z: " + aileron_caltab_fr.getZ() + 
						//	" A: " + aileron_caltab_fr.getAlphaRad() + " B: " + aileron_caltab_fr.getBetaRad() + " C: " + aileron_caltab_fr.getGammaRad());	

	    		 }																									  
    	 
		    	//System.out.println("Ref Caltab frame --> x: " + aileron_caltab_fr.getX() + " y: " + aileron_caltab_fr.getY() + " z: " + aileron_caltab_fr.getZ() + 
					//	" A: " + aileron_caltab_fr.getAlphaRad() + " B: " + aileron_caltab_fr.getBetaRad() + " C: " + aileron_caltab_fr.getGammaRad());
				
	    		
    		   	 aileron_caltab_fr.transform(XyzAbcTransformation.ofRad(pose.getX(), pose.getY(), pose.getZ(),
		    	 pose.getAlphaRad(), pose.getBetaRad(), pose.getGammaRad()));
    		   	 

 		    	//System.out.println("Ref Caltab frame --> x: " + aileron_caltab_fr.getX() + " y: " + aileron_caltab_fr.getY() + " z: " + aileron_caltab_fr.getZ() + 
 					//" A: " + aileron_caltab_fr.getAlphaRad()*(180/Math.PI) + " B: " + aileron_caltab_fr.getBetaRad()*(180/Math.PI) + " C: " + aileron_caltab_fr.getGammaRad()*(180/Math.PI));
 		
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
			if(traj_caltab_ref_fr.get(i).getX()== traj_caltab_ref_fr.get(i-1).getX())
			{	
				traj_caltab_ref_fr.get(i).transform(XyzAbcTransformation.ofDeg(0.0, 0.0, -75.0, 0.0, 0.0, 0.0));
				
				System.out.println(i + " Traj Position in caltab frame --> x: " + traj_caltab_ref_fr.get(i).getX() + 
					" y: " + traj_caltab_ref_fr.get(i).getY() + " z: " + traj_caltab_ref_fr.get(i).getZ() + 
						" A: " + traj_caltab_ref_fr.get(i).getAlphaRad() + " B: " + traj_caltab_ref_fr.get(i).getBetaRad() + 
							" C: " + traj_caltab_ref_fr.get(i).getGammaRad());
				
				i++;
				
				traj_caltab_ref_fr.get(i).transform(XyzAbcTransformation.ofDeg(0.0, 0.0, -75.0, 0.0, 0.0, 0.0));
				
				System.out.println(i + " Traj Position in caltab frame --> x: " + traj_caltab_ref_fr.get(i).getX() + 
						" y: " + traj_caltab_ref_fr.get(i).getY() + " z: " + traj_caltab_ref_fr.get(i).getZ() + 
							" A: " + traj_caltab_ref_fr.get(i).getAlphaRad() + " B: " + traj_caltab_ref_fr.get(i).getBetaRad() + 
								" C: " + traj_caltab_ref_fr.get(i).getGammaRad());
			
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
			//TODO Bloque catch generado automáticamente
			System.err.println("Could not create TCPServer:" +e.getMessage());
		}
		
		//Application TCPServer object
		try {
			tcp_server_2 = new TCPServer2();
				
			tcp_server_2.addListener(this);
			tcp_server_2.enable();
					
		} catch (IOException e) {
			//TODO Bloque catch generado automáticamente
			System.err.println("Could not create TCPServer2:" +e.getMessage());
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
					
					/*JointPosition joints = new JointPosition(0,0,0,0,0,0,0);

					joints.set(0, -92.35*(Math.PI/180));joints.set(1, 27.53*(Math.PI/180));
					joints.set(2, 0.0*(Math.PI/180));joints.set(3, -98.13*(Math.PI/180));
					joints.set(4, 0.09*(Math.PI/180));joints.set(5, 53.47*(Math.PI/180));
					joints.set(6, -90.78*(Math.PI/180));
					
					lbr.move(ptp(joints).setJointVelocityRel(0.25));*/
					
					roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/DemoCroinspect/Aprox4")).setJointVelocityRel(0.25));

					
					String response_data = frame_id + ";" + operation_type + ";1" ;
					tcp_server.setResponseData(response_data);	
					
				}
				else if (operation_type.compareTo("inspection") == 0)
				{
				
					roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/DemoCroinspect/Aprox4")).setJointVelocityRel(0.25));

					rec = new DataRecorder();
					rec.setTimeout(2L, TimeUnit.MINUTES);
					/*switch (getApplicationUI().displayModalDialog(
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
									closeCommunication2();

								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								exit = true;
								break;
					}*/
					
					//select_velocity=velocity();
					getLogger().info("Selected 10N and " + 25 + "mm/s");

					fname="measured_force_10ND_stiffZ_300_"+select_velocity+"mm_S.log";
					try {
						Force_XND(20,fname,25);
					} catch (IOException e) {
						System.out.println("IO Exception in Force_XND 10");
					}
					
				}
				else if(operation_type.compareTo("end") == 0)
				{
					
				}
			}
		} while (!exit);
		
	}
	
	private double[] productVector(double[] vA, double[] vB)
	{
	
		double result[] = new double[3];
		
		result[0] = (vA[1]*vB[2]) - (vA[2]*vB[1]);
		result[1] = -1 * ((vA[0]*vB[2]) - (vA[2]*vB[0]));
		result[2] = (vA[0]*vB[1]) - (vA[1]*vB[0]);
		
		return result;
	}
	
	private double moduleVector(double[] v)
	{
		double result;
		
		result = Math.sqrt(Math.pow(v[0], 2)+Math.pow(v[1], 2)+Math.pow(v[2], 2));
		
		return result;
	}
	
	private double[] divisionVector(double[] v, double mod)
	{
		double[] result = new double[3];
		
		result[0] = v[0]/mod;
		result[1] = v[1]/mod;
		result[2] = v[2]/mod;
		
		return result;
	}
	private int poseChecking(double x, double y)
	{
	
		//Double y_val =  -3.326786450896398540377597969221*x + 4020.7878085276852292559098841821;
		Double y_val =  -2.3033972534175868864766790752964*x -2675.2170127631620108236619013358;
					
		 if(y_val < y)
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
	
	private void closeCommunication2() throws IOException
	{
		try {
			
			tcp_server_2.dispose();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			System.out.println("App InterruptedException2");
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
		System.out.println("i value: " + i);

		working_zone = poseChecking(x.get(i), y.get(i));
			
		point  = traj_caltab_ref_fr.get(i).copy();
		aprox_pose.transform(XyzAbcTransformation.ofRad(point.getX(), point.getY(), point.getZ(), 
				point.getAlphaRad(), point.getBetaRad(), point.getGammaRad()));
							
		System.out.println("Traj point in robot base frame --> x: " + aprox_pose.getX() + " y: " + aprox_pose.getY() + " z: " + aprox_pose.getZ() + 
				" A: " + aprox_pose.getAlphaRad()*(180/Math.PI)  + " B: " + aprox_pose.getBetaRad()*(180/Math.PI)  + " C: " + aprox_pose.getGammaRad()*(180/Math.PI));
					
		//aprox_pose.transform(XyzAbcTransformation.ofRad(0.0,0.0,-300, Math.PI/2,0.0,0.0));
		aprox_pose.transform(XyzAbcTransformation.ofRad(0.0,0.0,-50, 0.0,0.0,0.0));

		//System.out.println("Safety traj point in robot base frame --> x: " + aprox_pose.getX() + " y: " + aprox_pose.getY() + " z: " + aprox_pose.getZ() + 
			//	" A: " + aprox_pose.getAlphaRad() + " B: " + aprox_pose.getBetaRad() + " C: " + aprox_pose.getGammaRad());
	
		roll_scan.getFrame("Gripper").move(ptp(aprox_pose).setJointVelocityRel(0.1));
		
		Frame copy_caltab_robot_fr;
		
		copy_caltab_robot_fr = caltab_robot_fr.copy();
		
		point  = traj_caltab_ref_fr.get(i).copy();
								
		copy_caltab_robot_fr.transform(XyzAbcTransformation.ofRad(point.getX(), point.getY(), point.getZ(), 
				point.getAlphaRad(), point.getBetaRad(), point.getGammaRad()));
			
		copy_caltab_robot_fr.setRedundancyInformation(lbr, redundancyInfo);
		
		roll_scan.getFrame("Gripper").moveAsync(lin(copy_caltab_robot_fr).setCartVelocity(10).setMode(impedanceControlMode).setBlendingCart(10));
	
		SharedData.sinc_data=true;
		
		System.out.println("Aprox point in robot base frame --> x: " + copy_caltab_robot_fr.getX() + " y: " + copy_caltab_robot_fr.getY() + " z: " + copy_caltab_robot_fr.getZ() + 
			" A: " + copy_caltab_robot_fr.getAlphaRad()*(180/Math.PI)  + " B: " + copy_caltab_robot_fr.getBetaRad()*(180/Math.PI) + " C: " + copy_caltab_robot_fr.getGammaRad()*(180/Math.PI) );

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
				
				int next_point_zone;
				if(i<x.size()-1)
					next_point_zone = poseChecking(x.get(i+1), y.get(i+1));
				else
					next_point_zone = 3;
				
				//Comprobar si el proximo 
				
				//if(i<x.size()-1 && !warning_signal.get())
				if((next_point_zone==point_zone) && !warning_signal.get())
				{
					//System.out.println("Warning signal: " + warning_signal.get());
					IMotionContainer motion_cmd = roll_scan.getFrame("Gripper").moveAsync(lin(copy_caltab_robot_fr).setCartVelocity(velocidad).setMode(impedanceControlMode).setBlendingCart(10));
					motion_list.add(motion_cmd);
					System.out.println(i + " Traj point in robot frame --> x: " + copy_caltab_robot_fr.getX() + " y: " + copy_caltab_robot_fr.getY() + " z: " + copy_caltab_robot_fr.getZ() + 
							" A: " + copy_caltab_robot_fr.getAlphaRad()*(180/Math.PI) + " B: " + copy_caltab_robot_fr.getBetaRad()*(180/Math.PI) + " C: " + copy_caltab_robot_fr.getGammaRad()*(180/Math.PI) );
			
					//System.out.println("Movement list: " + motion_list.size());
				}	
				else
				{
					try
					{								
						//roll_scan.getFrame("roll_tcp").move(lin(copy_caltab_robot_fr).setCartVelocity(velocidad).setBlendingCart(0));
						
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
							 
							System.out.println("Back to the safe pose");
							Frame current_pose = lbr.getCurrentCartesianPosition(roll_scan.getFrame("roll_tcp"));
							
							current_pose.transform(XyzAbcTransformation.ofRad(90.0,-400.0,-400,0.0,0.0,0.0));
							
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
							tcp_server_2.dispose();
							
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
						tcp_server_2.dispose();
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
	
		//	caltab_pose_data.set(3, (caltab_pose_data.get(3) - 0.025)); 
			
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


	@Override
	public void OnTCPMessageReceived2(String datagram) {
		
		System.out.println("OnTCPMessageReceived2: " + datagram);

		String splittedData[] = datagram.split(";");
		
		String stamp = splittedData[0];
		String msg_id = splittedData[1];
		String zone_str= splittedData[2];
		
		int zone = Integer.parseInt(zone_str);
		double override_vel = getApplicationControl().getApplicationOverride();
		
		System.out.println("Current Override vel: " + override_vel);

		if(zone == 0)
		{
			//No obstacle 
			System.out.println("No obstacle");
			override_vel = 1.0;
		}
		else if(zone == 1)
		{
			//Obstacle in Warning area
			System.out.println("Obstacle in WARNING area");
			override_vel = 0.5;
		}
		else if(zone == 2)
		{
			///Obstacle in Stop area
			System.out.println("Obstacle in STOP area");
			override_vel = 0.0;
			waitUntilRobotAlmostStopped(-1);
		}
		
		System.out.println("Override vel: " + override_vel);

		getApplicationControl().setApplicationOverride(override_vel);
	}

	@Override
	public void OnTCPConnection2() {
		// TODO Auto-generated method stub
		
	}



}


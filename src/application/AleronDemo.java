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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;


import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.conditionModel.ICondition;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.deviceModel.LBRE1Redundancy;
import com.kuka.roboticsAPI.geometricModel.CartDOF;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;
import com.kuka.roboticsAPI.geometricModel.math.XyzAbcTransformation;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianImpedanceControlMode;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianSineImpedanceControlMode;
import com.kuka.roboticsAPI.sensorModel.DataRecorder;
//import com.kuka.roboticsAPI.sensorModel.ForceSensorData;
import com.kuka.roboticsAPI.uiModel.ApplicationDialogType;

public class AleronDemo extends RoboticsAPIApplication implements ITCPListener{
	
	@Inject
	private LBR lbr;
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
	double time_stamp;
	int frame_id;
	Frame caltab_robot_fr;
	
	@Override
	public void initialize() {
		
		// initialize your application here
		roll_scan = createFromTemplate("RollScan");
		roll_scan.attachTo(lbr.getFlange());
		
		System.out.println("Roll scan frame: " + roll_scan.getFrame("roll_tcp").toString());

		data_received = new AtomicBoolean(false);
		
		//TODO: Fulfill with correct values
		//Frames definition
		tcp_camera_fr = new Frame(getFrame("/robot_base"));
		tcp_camera_fr.setX(0.0); tcp_camera_fr.setY(0.0); tcp_camera_fr.setZ(0.0);
		tcp_camera_fr.setAlphaRad(0.0); tcp_camera_fr.setBetaRad(0.0); tcp_camera_fr.setGammaRad(0.0);
		
		Frame pose = new Frame(getFrame("/DemoCroinspect/aileron"));
		
		//Catlab1 Aileron frame definition
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
		pose.setX(0.75 * 1000); pose.setY(0.43*1000); pose.setZ(0.005*1000);
		pose.setAlphaRad(-Math.PI/2); pose.setBetaRad(Math.PI); pose.setGammaRad(0.0);
			
		//Getting the inverse frame (Aileron - Caltab2)
		t = pose.getTransformationFromParent().invert();
		pose_inv.setParent(getFrame("/DemoCroinspect/caltab"));
		pose_inv.setTransformationFromParent(t);
	
		//Adding the frame to the list
		aileron_caltabs_fr_list.add(pose);

		//Catlab 3 Aileron frame definition
		pose.setX(0.0); pose.setY(0.0); pose.setZ(0.0);
		pose.setAlphaRad(0.0); pose.setBetaRad(0.0); pose.setGammaRad(0.0);
		
		//Getting the inverse frame (Aileron - Caltab3)
		t = pose.getTransformationFromParent().invert();
		pose_inv.setParent(getFrame("/DemoCroinspect/caltab"));
		pose_inv.setTransformationFromParent(t);
		
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
	 		 
  		  
	    		 Frame aileron_caltab_fr;
	    		 
		    	 if(x.get(cont) < 750.0)
		    		 aileron_caltab_fr = aileron_caltabs_fr_list.get(0);		    		 
		    	 else if (750.0 < x.get(cont) &&  x.get(cont) < 1500.0)
		    		 aileron_caltab_fr = aileron_caltabs_fr_list.get(1);
		    	 else
		    		 aileron_caltab_fr = aileron_caltabs_fr_list.get(2);

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
		
		//TCPServer object
		try {
			tcp_server = new TCPServer();
				
			tcp_server.addListener(this);
			tcp_server.enable();
					
		} catch (IOException e) {
			//TODO Bloque catch generado automáticamente
			System.err.println("Could not create TCPServer:" +e.getMessage());
		}
		
		
	} 
    
	
	@Override
	public void run() {	
		
		// your application execution starts here
		//lbr.move(ptpHome());
				
		exit=false;
		
		do {
			
			if(data_received.get())
			{
				data_received.set(false);
				
				if(operation_type.compareTo("calibration") == 0)
				{
					JointPosition joints = new JointPosition();
					
					joints.set(0, 0.0*(180/Math.PI));joints.set(1, -49.0*(180/Math.PI));
					joints.set(2, -5.73*(180/Math.PI));joints.set(3, -90.0*(180/Math.PI));
					joints.set(4, 0.0*(180/Math.PI));joints.set(5, 86.7*(180/Math.PI));
					joints.set(6, 0.0*(180/Math.PI));
					
					lbr.move(ptp(joints).setJointVelocityRel(0.25));
					
					String response_data = frame_id + ";" + operation_type + ";1" ;
					tcp_server.setResponseData(response_data);
	
				}
				else if (operation_type.compareTo("inspection") == 0)
				{
				
					rec = new DataRecorder();
					rec.setTimeout(2L, TimeUnit.MINUTES);
					roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));
					
					switch (getApplicationUI().displayModalDialog(
							ApplicationDialogType.QUESTION,"How many Force do I have to do?", 
							"10N", "15N", "20N", "24N", "END DO NOTHING")) {
		
							case 0:
								
								select_velocity=velocity();
								getLogger().info("Selected 10N and " + select_velocity + "mm/s");
								//Force_10N(select_velocity);
		
								
								fname="measured_force_10ND_stiffZ_300_"+select_velocity+"mm_S.log";
								Force_XND(10,fname,select_velocity);
								//Force_XND(0.0,"measured_force_10ND_stiffZ_300.log",select_velocity);	
						
								exit = true;
								
								break;				
							case 1:
								//15N=500*0.03
								select_velocity=velocity();
								getLogger().info("Selected 15N and " + select_velocity + "mm/s");
								
								
								fname="measured_force_15ND_stiffZ_500_"+select_velocity+"mm_S.log";
								Force_XND(15,fname,select_velocity);	
								
								exit = true;
						
								break;					
							case 2:
								//20N=500*0.04 REPASAR DESIRED
								select_velocity=velocity();
								getLogger().info("Selected 20N and " + select_velocity + "mm/s");
								
								
								fname="measured_force_20ND_stiffZ_500_"+select_velocity+"mm_S.log";
								Force_XND(20,fname,select_velocity);
								
								exit = true;
								
								break;
							case 3:
								//24N=500*0.048
		
								select_velocity=velocity();
								getLogger().info("Selected 24N and mm/s: " + select_velocity + "mm/s");
								
							
								fname="measured_force_24ND_stiffZ_500_"+select_velocity+"mm_S.log";
								Force_XND(24,fname,select_velocity);
								
								exit = true;
								
								break;
						
							case 4:
								getLogger().info("App Terminated\n"+"***END***");
								/*try {
									tcp_server.dispose();
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								exit = true;*/
								closeCommunication();
								exit = true;
								break;
					}
				}
			}
		} while (!exit);
		
	}
	
	private void closeCommunication()
	{
		try {
			tcp_server.dispose();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
	private void Force_XND(int force, String nfichero, double velocidad )
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
	
		//Get close to the aileron 
		//roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));
		//roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/aleron/Aprox1")).setJointVelocityRel(0.25));
		//roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/aleron/Aprox")).setJointVelocityRel(0.25));
		
	 	
		Frame point = new Frame(getFrame("/DemoCroinspect/caltab"));
		LBRE1Redundancy redundancyInfo;
		
		Frame copy_caltab_robot_fr; //= new Frame(caltab_robot_fr);
		for(int i=0; i<x.size();i++)
		{
			copy_caltab_robot_fr = new Frame(caltab_robot_fr);
			point.setX(x.get(i)); point.setY(y.get(i)); point.setZ(z.get(i));
			point.setAlphaRad(a.get(i)); point.setBetaRad(b.get(i)); point.setGammaRad(c.get(i));
					
			/*if(point.getX() > 444)
			 	redundancyInfo = new LBRE1Redundancy(Math.toRadians(0.2), 2, 24);
			else
			 	redundancyInfo = new LBRE1Redundancy(Math.toRadians(0.2), 2, 88);
			 	
			point.setRedundancyInformation(lbr, redundancyInfo);
			*/
			
			
			System.out.println("x: " + point.getX() + " y: " + point.getY() + " z: " + point.getZ() + 
					" A: " + point.getAlphaRad() + " B: " + point.getBetaRad() + " C: " + point.getGammaRad());
			
			
			copy_caltab_robot_fr.transform(XyzAbcTransformation.ofRad(point.getX(), point.getY(), point.getZ(), 
					point.getAlphaRad(), point.getBetaRad(), point.getGammaRad()));
				
			
			/*if(i<x.size()-1)
				roll_scan.getFrame("roll_tcp").moveAsync(lin(copy_caltab_robot_fr).setCartVelocity(velocidad).setMode(impedanceControlMode).setBlendingCart(10));
			else
				roll_scan.getFrame("roll_tcp").moveAsync(lin(copy_caltab_robot_fr).setCartVelocity(velocidad).setMode(impedanceControlMode).setBlendingCart(0));
*/
			//ForceSensorData current_force = lbr.getExternalForceTorque(roll_scan.getFrame("roll_tcp"),roll_scan.getFrame("roll_tcp"));

			//System.out.println("Z: " + current_force.getForce().getZ() + " A: " + current_force.getTorque().getZ()
				//+ " B: " + current_force.getTorque().getY() + " C: " + current_force.getTorque().getX());
			
			System.out.println("Caltab after transformation --> x: " + copy_caltab_robot_fr.getX() + " y: " + copy_caltab_robot_fr.getY() + " z: " + copy_caltab_robot_fr.getZ() + 
					" A: " + copy_caltab_robot_fr.getAlphaRad() + " B: " + copy_caltab_robot_fr.getBetaRad() + " C: " + copy_caltab_robot_fr.getGammaRad());
						
	
			copy_caltab_robot_fr= null; // new Frame(caltab_robot_fr);
			
		
			
		}
		
		rec.stopRecording();
		
		tcp_server.setResponseData("Finished");
		
	}
	
	
	@Override
	public void OnTCPMessageReceived(String datagram)
	{
		System.out.println("OnTCPMessageReceived: " + datagram);

		String splittedData[] = datagram.split(";");
		
		time_stamp = Double.parseDouble(splittedData[0]);
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

			caltab_robot_fr.setX(caltab_pose_data.get(0)*1000); caltab_robot_fr.setY(caltab_pose_data.get(1)*1000); 
			caltab_robot_fr.setZ(caltab_pose_data.get(2)*1000); caltab_robot_fr.setAlphaRad(caltab_pose_data.get(5)); 
			caltab_robot_fr.setBetaRad(caltab_pose_data.get(4)); caltab_robot_fr.setGammaRad(caltab_pose_data.get(3));	
		}
		
		data_received.set(true);
	}
}


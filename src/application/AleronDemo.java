package application;

import static com.kuka.roboticsAPI.motionModel.BasicMotions.lin;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.ptp;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.deviceModel.LBRE1Redundancy;
import com.kuka.roboticsAPI.geometricModel.CartDOF;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
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
	Frame caltab_robot_fr;
	Frame tcp_camera_fr;
	ArrayList<Frame> caltab_aileron_fr_list;
	ArrayList<Frame> traj_caltab_ref_fr;
	
	DataRecorder rec;
	
	private TCPServer tcp_server;
	
	@Override
	public void initialize() {
		// initialize your application here
		roll_scan = createFromTemplate("RollScan");
		roll_scan.attachTo(lbr.getFlange());
		
		System.out.println("Roll scan frame: " + roll_scan.getFrame("roll_tcp").toString());

		//TODO: Fulfill with correct values
		//Frames definition
		tcp_camera_fr = new Frame(getFrame("/robot_base"));
		tcp_camera_fr.setX(0.0); tcp_camera_fr.setY(0.0); tcp_camera_fr.setZ(0.0);
		tcp_camera_fr.setAlphaRad(0.0); tcp_camera_fr.setBetaRad(0.0); tcp_camera_fr.setGammaRad(0.0);

		
		Frame point = new Frame();
		point.setX(0.0); point.setY(0.0); point.setZ(0.0);
		point.setAlphaRad(0.0); point.setBetaRad(0.0); point.setGammaRad(0.0);
		caltab_aileron_fr_list.add(point);
		
		point.setX(0.0); point.setY(0.0); point.setZ(0.0);
		point.setAlphaRad(0.0); point.setBetaRad(0.0); point.setGammaRad(0.0);
		caltab_aileron_fr_list.add(point);
		
		point.setX(0.0); point.setY(0.0); point.setZ(0.0);
		point.setAlphaRad(0.0); point.setBetaRad(0.0); point.setGammaRad(0.0);
		caltab_aileron_fr_list.add(point);
		
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
		    	 
		    	 point.setX(x.get(cont)); point.setY(y.get(cont)); point.setZ(z.get(cont));
	    		 point.setAlphaRad(a.get(cont)); point.setBetaRad(b.get(cont)); point.setGammaRad(c.get(cont));
	 		 
	    		 /*point.transform(XyzAbcTransformation.ofDeg(0, 0, 0, -72.73, 0.56, 179.45));
	    		 
	    		 
	    		 
	    		 Frame caltab_aileron_fr;
	    		 
		    	 if(x.get(cont) < 0.0)
		    	 {
		    		 caltab_aileron_fr = caltab_aileron_fr_list.get(0);
		    		 point.transformationTo(caltab_aileron_fr);
		    		 point.tra
		    		 //point.transform()
		    		 //traj_caltab_ref_fr
		    	 }
		    	 else if (10.0 < x.get(cont) < 100.0)
		    	 {
		    		 caltab_aileron_fr = caltab_aileron_fr_list.get(1);
 
		    	 }
		    	 else
		    	 {
		    		 caltab_aileron_fr = caltab_aileron_fr_list.get(2);

		    	 }*/
		    	 
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
		
		 point.setX(x.get(0)); point.setY(y.get(0)); point.setZ(z.get(0));
		 point.setAlphaRad(a.get(0)); point.setBetaRad(b.get(0)); point.setGammaRad(c.get(0));
		 
		 System.out.println("Point --> x: " + point.getX() + "y: " + point.getY() + "z: " + point.getZ() 
			+ " A: " + point.getAlphaRad() + " B: " + point.getBetaRad() + " C: " + point.getGammaRad());
		 
		 point.transform(XyzAbcTransformation.ofDeg(1.0, 0.0, 0.0, 0.0, 0.0, 0.0));
		 
		 System.out.println("Point Transform--> x: " + point.getX() + "y: " + point.getY() + "z: " + point.getZ() 
					+ " A: " + point.getAlphaRad() + " B: " + point.getBetaRad() + " C: " + point.getGammaRad());

		 Frame new_fr = new Frame();
		 
		 new_fr.setX(1.0); new_fr.setY(0.0); new_fr.setZ(0.0);
		 new_fr.setAlphaRad(0.0); new_fr.setBetaRad(0.0); new_fr.setGammaRad(0.0);
		 
		 point.transformationTo(new_fr);
		 
		 System.out.println("Point TransformationTo--> x: " + point.getX() + "y: " + point.getY() + "z: " + point.getZ() 
					+ " A: " + point.getAlphaRad() + " B: " + point.getBetaRad() + " C: " + point.getGammaRad());

		
		//TCPServer object
		/*try {
			tcp_server = new TCPServer();
				
			tcp_server.addListener(this);
			tcp_server.enable();
					
		} catch (IOException e) {
			//TODO Bloque catch generado automáticamente
			System.err.println("Could not create TCPServer:" +e.getMessage());
		}*/
		
		
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
					ApplicationDialogType.QUESTION,"How many Force do I have to do?", 
					"10N", "15N", "20N", "24N", "END DO NOTHING")) {

					case 0:
						
						select_velocity=velocity();
						getLogger().info("Selected 10N and " + select_velocity + "mm/s");
						//Force_10N(select_velocity);

						
						fname="measured_force_10ND_stiffZ_300_"+select_velocity+"mm_S.log";
						Force_XND(10,fname,select_velocity);
						//Force_XND(0.0,"measured_force_10ND_stiffZ_300.log",select_velocity);	
				
						break;				
					case 1:
						//15N=500*0.03
						select_velocity=velocity();
						getLogger().info("Selected 15N and " + select_velocity + "mm/s");
						
						
						fname="measured_force_15ND_stiffZ_500_"+select_velocity+"mm_S.log";
						Force_XND(15,fname,select_velocity);	
				
						break;					
					case 2:
						//20N=500*0.04 REPASAR DESIRED
						select_velocity=velocity();
						getLogger().info("Selected 20N and " + select_velocity + "mm/s");
						
						
						fname="measured_force_20ND_stiffZ_500_"+select_velocity+"mm_S.log";
						Force_XND(20,fname,select_velocity);
						
						break;
					case 3:
						//24N=500*0.048

						select_velocity=velocity();
						getLogger().info("Selected 24N and mm/s: " + select_velocity + "mm/s");
						
					
						fname="measured_force_24ND_stiffZ_500_"+select_velocity+"mm_S.log";
						Force_XND(24,fname,select_velocity);
						
						break;
				
					case 4:
						getLogger().info("App Terminated\n"+"***END***");
						exit = true;
						break;
			}
		} while (!exit);
		
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
		roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));
		roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/aleron/Aprox1")).setJointVelocityRel(0.25));
		roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/aleron/Aprox")).setJointVelocityRel(0.25));
		
	 	
		Frame point = new Frame();
		LBRE1Redundancy redundancyInfo;
		
		for(int i=0; i<x.size();i++)
		{
			point.setX(x.get(i)); point.setY(y.get(i)); point.setZ(z.get(i));
			point.setAlphaRad(a.get(i)); point.setBetaRad(b.get(i)); point.setGammaRad(c.get(i));
					
			if(point.getX() > 444)
			 	redundancyInfo = new LBRE1Redundancy(Math.toRadians(0.2), 2, 24);
			else
			 	redundancyInfo = new LBRE1Redundancy(Math.toRadians(0.2), 2, 88);

			point.setRedundancyInformation(lbr, redundancyInfo);
			
			
			point.transform(XyzAbcTransformation.ofDeg(0, 0, 0, -72.73, 0.56, 179.45));
			//new_point.setRedundancyInformation(lbr, redundancyInfo);
				
			//System.out.println("x: " + new_point.getX() + " y: " + new_point.getY() + " z: " + new_point.getZ() + 
				//" A: " + new_point.getAlphaRad() + " B: " + new_point.getBetaRad() + " C: " + new_point.getGammaRad());
		
			if(i<x.size()-1)
				roll_scan.getFrame("roll_tcp").moveAsync(lin(point).setCartVelocity(velocidad).setMode(impedanceControlMode).setBlendingCart(10));
			else
				roll_scan.getFrame("roll_tcp").moveAsync(lin(point).setCartVelocity(velocidad).setMode(impedanceControlMode).setBlendingCart(0));

			//ForceSensorData current_force = lbr.getExternalForceTorque(roll_scan.getFrame("roll_tcp"),roll_scan.getFrame("roll_tcp"));

			//System.out.println("Z: " + current_force.getForce().getZ() + " A: " + current_force.getTorque().getZ()
				//+ " B: " + current_force.getTorque().getY() + " C: " + current_force.getTorque().getX());

		}
		
		rec.stopRecording();
		
		tcp_server.setResponseData("Finished");
		
	}
	
	
	@Override
	public void OnTCPMessageReceived(String datagram)
	{
		System.out.println("OnTCPMessageReceived: " + datagram);

		String splittedData[] = datagram.split(";");
				
		for(int i=2; i<splittedData.length; i++)
		{
			caltab_pose_data.add(Double.parseDouble(splittedData[i]));
		}

		//Frame definition
		caltab_robot_fr = new Frame(getFrame("/robot_base"));

		caltab_robot_fr.setX(caltab_pose_data.get(0)*1000); caltab_robot_fr.setY(caltab_pose_data.get(1)*1000); 
		caltab_robot_fr.setZ(caltab_pose_data.get(2)*1000); caltab_robot_fr.setAlphaRad(caltab_pose_data.get(5)*(180/Math.PI)); 
		caltab_robot_fr.setBetaRad(caltab_pose_data.get(4)*(180/Math.PI)); caltab_robot_fr.setGammaRad(caltab_pose_data.get(3)*(180/Math.PI));	
	}
}


package application;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
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
import com.sun.xml.internal.bind.v2.schemagen.xmlschema.List;

import java.io.Serializable;
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

public class AleronCADTest extends RoboticsAPIApplication {
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

	
	DataRecorder rec;
	@Override
	public void initialize() {
		// initialize your application here
		roll_scan = createFromTemplate("RollScan");
		//roll_scan.changeFramePosition(roll_scan.getFrame("Gripper"), XyzAbcTransformation.ofRad(gripper_tool_xyz[0]*1000, 
	    //gripper_tool_xyz[1]*1000, gripper_tool_xyz[2]*1000, gripper_tool_rpy[2], gripper_tool_rpy[1], gripper_tool_rpy[0]));
		roll_scan.attachTo(lbr.getFlange());
		
		System.out.println("Roll scan frame: " + roll_scan.getFrame("roll_tcp").toString());

		//roll_scan.getLoadData().setMass(2.07);
		//roll_scan.getLoadData().setCenterOfMass(-0.0076*1000, 0.00473*1000, 0.12047*1000);
		//roll_scan.getLoadData().setCenterOfMass(1.36,2.68,42.81);

	
		// hasta aqui desired force
		impedanceControlMode =	new CartesianImpedanceControlMode();

		
	 	//LBRE1Redundancy redundancyInfo = new LBRE1Redundancy(Math.toRadians(0.09), 2, 24);
				
		String str;
		String file = "C:\\Users\\KukaUser\\Desktop\\CADTraj\\Plana_Sin.MPF";
		FileReader f;
      
		String val_str[];
		Double val;
      
		try 
		{
			f = new FileReader(file);
		
			 BufferedReader br = new BufferedReader(f);
			 
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
		    			 a.add(val);
		    		 }
		    		 else if(data[i].contains("B3="))
		    		 {
		    			 val_str= data[i].split("B3=");
		    			 val = Double.parseDouble(val_str[1]);
		    			 b.add(val);
		    		 }
		    		 else if(data[i].contains("C3="))
		    		 {
		    			 val_str= data[i].split("C3=");
		    			 val = Double.parseDouble(val_str[1]);
		    			 c.add(val);
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
		     }
      
		     //System.out.println(x.size());
	    	 
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


	private void Force_XND(int force, String nfichero, double velocidad )
	{
		impedanceControlMode= CartesianSineImpedanceControlMode.createDesiredForce(CartDOF.Z, force, stiffnessZ);
	
		impedanceControlMode.setMaxCartesianVelocity(1000.0,1000.0,1000.0,Math.toRadians(60),Math.toRadians(60),Math.toRadians(60));
		impedanceControlMode.setSpringPosition(roll_scan.getFrame("gripper"));
		impedanceControlMode.parametrize(CartDOF.X).setStiffness(stiffnessX).setDamping(0.7);
		impedanceControlMode.parametrize(CartDOF.Y).setStiffness(stiffnessY).setDamping(0.7);
		impedanceControlMode.parametrize(CartDOF.Z).setStiffness(stiffnessZ).setDamping(0.7);
		impedanceControlMode.parametrize(CartDOF.ROT).setStiffness(300).setDamping(0.7);
		

	 	//LBRE1Redundancy redundancyInfo = new LBRE1Redundancy(Math.toRadians(0.2), 2, 24);
		
	 	rec.setFileName(nfichero);
		rec.addCartesianForce(roll_scan.getFrame("roll_tcp"),roll_scan.getFrame("roll_tcp"));
		rec.addCurrentCartesianPositionXYZ(roll_scan.getFrame("roll_tcp"), getApplicationData().getFrame("/robot_base"));
	 	
	 	rec.enable();
		rec.startRecording();
	
		roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));
		roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/aleron/Aprox1")).setJointVelocityRel(0.25));
		roll_scan.getFrame("roll_tcp").move(ptp(getFrame("/aleron/Aprox")).setJointVelocityRel(0.25));
		
	 	Frame point = new Frame(getFrame("/aleron"));
		Frame new_point;
		for(int i=0; i<x.size();i++)
		{
			point.setX(x.get(i)); point.setY(y.get(i)); point.setZ(z.get(i));
			point.setAlphaRad(a.get(i)); point.setBetaRad(b.get(i)); point.setGammaRad(c.get(i));
			
			new_point = point.transform(XyzAbcTransformation.ofDeg(0, 0, 0, -90, 0, 180));
			
			System.out.println("x: " + new_point.getX() + " y: " + new_point.getY() + " z: " + new_point.getZ() + 
				" A: " + new_point.getAlphaRad() + " B: " + new_point.getBetaRad() + " C: " + new_point.getGammaRad());
		
			roll_scan.getFrame("roll_tcp").move(lin(new_point).setCartVelocity(velocidad).setMode(impedanceControlMode));
		}
		
		rec.stopRecording();
	}
}
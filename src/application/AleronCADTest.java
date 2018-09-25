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
	
	double select_velocity;
	double overlap, overlapt;
	
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
				roll_scan = createFromTemplate("Tool");
		//roll_scan.changeFramePosition(roll_scan.getFrame("Gripper"), XyzAbcTransformation.ofRad(gripper_tool_xyz[0]*1000, 
	    //gripper_tool_xyz[1]*1000, gripper_tool_xyz[2]*1000, gripper_tool_rpy[2], gripper_tool_rpy[1], gripper_tool_rpy[0]));
		roll_scan.attachTo(lbr.getFlange());
		
		System.out.println("Roll scan frame: " + roll_scan.getFrame("flange_2_tool").toString());

		roll_scan.getLoadData().setMass(2.07);
		//roll_scan.getLoadData().setCenterOfMass(-0.0076*1000, 0.00473*1000, 0.12047*1000);
		roll_scan.getLoadData().setCenterOfMass(1.36,2.68,42.81);

	
		// hasta aqui desired force
		
		impedanceControlMode = 	new CartesianImpedanceControlMode();
		impedanceControlModeD = 	new CartesianImpedanceControlMode();

		
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
		    	 System.out.println(data[i]);

	    		 if(data[i].contains("X="))
	    		 {
	    			 val_str= data[i].split("X=");
	    	    	 System.out.println(val_str.length);

	    			 val = Double.parseDouble(val_str[1]);
	    	    	 System.out.println(val);

	    			 //x.add(val);
	    		 }
	    		 /*else if(data[i].contains("Y="))
	    		 {
	    			 val_str= data[i].split("Y=");
	    			 val = Double.parseDouble(val_str[0]);
	    			 y.add(val);
	    		 }
	    		 else if(data[i].contains("Z="))
	    		 {
	    			 val_str= data[i].split("Z=");
	    			 val = Double.parseDouble(val_str[0]);
	    			 z.add(val);
	    		 }
	    		 else if(data[i].contains("A3="))
	    		 {
	    			 val_str= data[i].split("A3=");
	    			 val = Double.parseDouble(val_str[0]);
	    			 a.add(val);
	    		 }
	    		 else if(data[i].contains("B3="))
	    		 {
	    			 val_str= data[i].split("B3=");
	    			 val = Double.parseDouble(val_str[0]);
	    			 b.add(val);
	    		 }
	    		 else if(data[i].contains("C3="))
	    		 {
	    			 val_str= data[i].split("C3=");
	    			 val = Double.parseDouble(val_str[0]);
	    			 c.add(val);
	    		 }
	    		 else if(data[i].contains("AN3="))
	    		 {
	    			 val_str= data[i].split("AN3=");
	    			 val = Double.parseDouble(val_str[0]);
	    			 a_n.add(val);
	    		 }
	    		 else if(data[i].contains("BN3="))
	    		 {
	    			 val_str= data[i].split("BN3=");
	    			 val = Double.parseDouble(val_str[0]);
	    			 b_n.add(val);
	    		 }
	    		 else if(data[i].contains("CN3="))
	    		 {
	    			 val_str= data[i].split("CN3=");
	    			 val = Double.parseDouble(val_str[0]);
	    			 c_n.add(val);
	    		 }*/
	    	  }
	    	  
	    	  //for(int j=0; j<x.size(); j++)
	    	    //	System.out.print(x.get(j));
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
     
    	  
   
	}

	@Override
	public void run() {
		// your application execution starts here
		//lbr.move(ptpHome());
		
		exit=false;
		
		do {
			rec = new DataRecorder();
			rec.setTimeout(2L, TimeUnit.MINUTES);
			roll_scan.getFrame("flange_2_tool").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));
			/*switch (getApplicationUI().displayModalDialog(
					ApplicationDialogType.QUESTION,"How many Force do I have to do?", 
					"10N", "15N", "20N", "24N", "GIRADO 10N", "SPLINE", "END DO NOTHING")) {

					case 0:
						forces=select_typeForce(10,300);
						
						select_velocity=velocity();
						getLogger().info("Selected 10N and " + select_velocity + "mm/s");
						//Force_10N(select_velocity);

						if (forces==0){
							fname="measured_force_10N_stiffZ_300_"+select_velocity+"mm_S.log";
							Force_XN(0.03333,fname,select_velocity);

						}
						else if (forces==1){
							fname="measured_force_10ND_stiffZ_300_"+select_velocity+"mm_S.log";
							Force_XND(0.03333,fname,select_velocity);
							//Force_XND(0.0,"measured_force_10ND_stiffZ_300.log",select_velocity);


						}	
				

						break;				
					case 1:
						//15N=500*0.03
						forces=select_typeForce(15,500);
						select_velocity=velocity();
						getLogger().info("Selected 15N and " + select_velocity + "mm/s");
						
						if (forces==0){
							fname="measured_force_15N_stiffZ_500_"+select_velocity+"mm_S.log";
							Force_XN(0.03,fname,select_velocity);

						}
						else if (forces==1){
							fname="measured_force_15ND_stiffZ_500_"+select_velocity+"mm_S.log";
							Force_XND(0.03,fname,select_velocity);

						}	
				
						break;					
					case 2:
						//20N=500*0.04 REPASAR DESIRED
						forces=select_typeForce(20,500);
						select_velocity=velocity();
						getLogger().info("Selected 20N and " + select_velocity + "mm/s");
						
						if (forces==0){
							fname="measured_force_20N_stiffZ_500_"+select_velocity+"mm_S.log";
							Force_XN(0.04,fname,select_velocity);

						}
						else if (forces==1){
							fname="measured_force_20ND_stiffZ_500_"+select_velocity+"mm_S.log";
							Force_XND(0.04,fname,select_velocity);
						}	
						break;
					case 3:
						//24N=500*0.048

						forces=select_typeForce(24,500);

						select_velocity=velocity();
						getLogger().info("Selected 24N and mm/s: " + select_velocity + "mm/s");
						
						if (forces==0){
							fname="measured_force_24N_stiffZ_500_"+select_velocity+"mm_S.log";
							Force_XN(0.048,fname,select_velocity);

						}
						else if (forces==1){
							fname="measured_force_24ND_stiffZ_500_"+select_velocity+"mm_S.log";
							Force_XND(0.048,fname,select_velocity);

						}						
						break;
					case 4:

						forces=select_typeForce(10,300);
						select_velocity=velocity();
						getLogger().info("Selected 10N and " + select_velocity + "mm/s");
						Force_XNT(0.03333,"measured_force_10N_stiffZ_300_girado.log",select_velocity);

						//Force_10N(select_velocity);
						
						break;
					case 5:
						Spline mySpline= new Spline(
								spl(getApplicationData().getFrame("/aleron/Aprox1")),
								spl(getApplicationData().getFrame("/aleron/Aprox")))
								
								;

						roll_scan.getFrame("flange_2_tool").move(mySpline);
						break;
						
					case 6:
						getLogger().info("App Terminated\n"+"***END***");
						exit = true;
						break;
			}*/
		} while (!exit);
			
	}
		
		
	private void Force_10N(double velocidad){
		
		impedanceControlMode.setMaxCartesianVelocity(1000.0,1000.0,1000.0,Math.toRadians(60),Math.toRadians(60),Math.toRadians(60));
		impedanceControlMode.setSpringPosition(roll_scan.getFrame("flange_2_tool"));
		impedanceControlMode.parametrize(CartDOF.X).setStiffness(stiffnessX).setDamping(0.7);
		impedanceControlMode.parametrize(CartDOF.Y).setStiffness(stiffnessY).setDamping(0.7);
		impedanceControlMode.parametrize(CartDOF.Z).setStiffness(stiffnessZ).setDamping(0.7);
		impedanceControlMode.parametrize(CartDOF.ROT).setStiffness(300).setDamping(0.7);
		
	 	//LBRE1Redundancy redundancyInfo = new LBRE1Redundancy(Math.toRadians(-0.01), 2, 29);
	 	LBRE1Redundancy redundancyInfo = new LBRE1Redundancy(Math.toRadians(0.09), 2, 24);

	 	rec.setFileName("measured_force_10N_stiffZ_300.log");
		rec.addCartesianForce(roll_scan.getFrame("flange_2_tool"),roll_scan.getFrame("flange_2_tool"));
		rec.addCurrentCartesianPositionXYZ(roll_scan.getFrame("flange_2_tool"), getApplicationData().getFrame("/robot_base"));
	 	//downFrame
		down_fr = new Frame(getFrame("/aleron"));
		down_fr.setX(150); down_fr.setY(-11); down_fr.setZ(40); 
	 	//down_fr.setAlphaRad(1.6); down_fr.setBetaRad(0.0); down_fr.setGammaRad(0.0);
		down_fr.setRedundancyInformation(lbr, redundancyInfo);
		
		//upFrame
		up_fr = new Frame(getFrame("/aleron"));
		up_fr.setX(0.400*1000); up_fr.setY(0.074*1000); up_fr.setZ(0.0333*1000); 
	 	//up_fr.setAlphaRad(1.6); up_fr.setBetaRad(0.0); up_fr.setGammaRad(0.0);
		up_fr.setRedundancyInformation(lbr, redundancyInfo);
	
		
		//int optionforce = getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, "How many Force do I have to do?", "10N", "15N", "20N", "24N", "END DO NOTHING");
		rec.enable();
		rec.startRecording();

		roll_scan.getFrame("flange_2_tool").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));
		roll_scan.getFrame("flange_2_tool").move(ptp(getFrame("/aleron/Aprox1")).setJointVelocityRel(0.25));
		roll_scan.getFrame("flange_2_tool").move(ptp(getFrame("/aleron/Aprox")).setJointVelocityRel(0.25));
		

		roll_scan.getFrame("flange_2_tool").move(lin(down_fr).setCartVelocity(velocidad));
		down_fr.setZ(0.0333*1000);
		roll_scan.getFrame("flange_2_tool").move(lin(down_fr).setCartVelocity(velocidad));

		ForceSensorData current_force = lbr.getExternalForceTorque(roll_scan.getFrame("flange_2_tool"),roll_scan.getFrame("flange_2_tool"));
		System.out.println("First Contact Z force: " + current_force.getForce().getZ());
		
		roll_scan.getFrame("flange_2_tool").move(lin(up_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));
		up_fr.setY(0.120*1000);
		down_fr.setY(0.120*1000);

		roll_scan.getFrame("flange_2_tool").move(lin(up_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));
		roll_scan.getFrame("flange_2_tool").move(lin(down_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));

		up_fr.setY(0.240*1000);
		down_fr.setY(0.240*1000);
		roll_scan.getFrame("flange_2_tool").move(lin(down_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));
		roll_scan.getFrame("flange_2_tool").move(lin(up_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));

		
		//roll_scan.getFrame("Gripper").move(lin(getFrame("/aleron/P1")).setCartVelocity(50));
		//roll_scan.getFrame("Gripper").move(lin(getFrame("/aleron/P2")).setCartVelocity(50).setMode(impedanceControlMode));
		//roll_scan.getFrame("Gripper").move(lin(getFrame("/aleron/P3")).setCartVelocity(50).setMode(impedanceControlMode));
		//.getFrame("Gripper").move(lin(getFrame("/aleron/P4")).setCartVelocity(50).setMode(impedanceControlMode));
		rec.stopRecording();
		roll_scan.getFrame("flange_2_tool").move(linRel(0,0,-200).setCartVelocity(100));
		//gripper.getFrame("/TCP2").move(linRel(100, 0, -200));
	}



private void Force_XN(double distancia, String nfichero, double velocidad ){
	
	//impedanceControlMode.setMaxCartesianVelocity(1000.0,1000.0,1000.0,Math.toRadians(60),Math.toRadians(60),Math.toRadians(60));
	//impedanceControlMode.setSpringPosition(roll_scan.getFrame("gripper"));
	//impedanceControlMode.parametrize(CartDOF.X).setStiffness(stiffnessX).setDamping(0.7);
	//impedanceControlMode.parametrize(CartDOF.Y).setStiffness(stiffnessY).setDamping(0.7);
	//impedanceControlMode.parametrize(CartDOF.Z).setStiffness(Force).setDamping(0.7);
	//impedanceControlMode.parametrize(CartDOF.ROT).setStiffness(300).setDamping(0.7);
	
 	LBRE1Redundancy redundancyInfo = new LBRE1Redundancy(Math.toRadians(0.2), 2, 24);
	
 	rec.setFileName(nfichero);
	rec.addCartesianForce(roll_scan.getFrame("flange_2_tool"),roll_scan.getFrame("flange_2_tool"));
	rec.addCurrentCartesianPositionXYZ(roll_scan.getFrame("flange_2_tool"), getApplicationData().getFrame("/robot_base"));
 	//downFrame
	down_fr = new Frame(getFrame("/aleron"));
	down_fr.setX(0.0685*1000); down_fr.setY(1.5); down_fr.setZ(distancia*1000); //3.0 o 0.0333*1000 para 10N = 300K*0.0333
 	//down_fr.setAlphaRad(-1.6); down_fr.setBetaRad(0.001);down_fr.setGammaRad(3.1); 
 	down_fr.setAlphaRad(-Math.PI/2); down_fr.setBetaRad(0.0);down_fr.setGammaRad(Math.PI); 

	down_fr.setRedundancyInformation(lbr, redundancyInfo);
	
	//upFrame
	up_fr = new Frame(getFrame("/aleron"));
	up_fr.setX(0.0685*1000); up_fr.setY(0.400*1000); up_fr.setZ(-distancia*1000); 
	//up_fr.setAlphaRad(-1.6); up_fr.setBetaRad(0.001);up_fr.setGammaRad(3.1); 
	up_fr.setAlphaRad(-Math.PI/2); up_fr.setBetaRad(0.0);up_fr.setGammaRad(Math.PI); 

	up_fr.setRedundancyInformation(lbr, redundancyInfo);
	
	rec.enable();
	rec.startRecording();

	//int optionforce = getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, "How many Force do I have to do?", "10N", "15N", "20N", "24N", "END DO NOTHING");
	roll_scan.getFrame("flange_2_tool").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));
	roll_scan.getFrame("flange_2_tool").move(ptp(getFrame("/aleron/Aprox1")).setJointVelocityRel(0.25));
	roll_scan.getFrame("flange_2_tool").move(ptp(getFrame("/aleron/Aprox")).setJointVelocityRel(0.25));
	

	roll_scan.getFrame("flange_2_tool").move(lin(down_fr).setCartVelocity(velocidad));
	down_fr.setZ(-distancia*1000);
	roll_scan.getFrame("flange_2_tool").move(lin(down_fr).setCartVelocity(25).setMode(impedanceControlMode));

	ForceSensorData current_force = lbr.getExternalForceTorque(roll_scan.getFrame("flange_2_tool"),roll_scan.getFrame("flange_2_tool"));
	System.out.println("First Contact Z force: " + current_force.getForce().getZ());
	
	roll_scan.getFrame("flange_2_tool").move(lin(up_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));

	// movimiento hacia atras
	up_fr.setZ(distancia*1000);
	roll_scan.getFrame("flange_2_tool").move(lin(up_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));
	
	//moviemiento en x
	overlapt=up_fr.getX()+overlap;
	up_fr.setX(overlapt);
	down_fr.setX(overlapt);
	//up_fr.setX(0.120*1000);
	//setX(0.120*1000);
	roll_scan.getFrame("flange_2_tool").move(lin(up_fr).setCartVelocity(velocidad));
	
	// 2 movimiento al aleron
	up_fr.setZ(-distancia*1000);
	roll_scan.getFrame("flange_2_tool").move(lin(up_fr).setCartVelocity(25).setMode(impedanceControlMode));
	ForceSensorData current_force2 = lbr.getExternalForceTorque(roll_scan.getFrame("flange_2_tool"),roll_scan.getFrame("flange_2_tool"));
	System.out.println("Second Contact Z force: " + current_force2.getForce().getZ());
	roll_scan.getFrame("flange_2_tool").move(lin(down_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));
	
    //movimiento hacia atras parte baja
	down_fr.setZ(distancia*1000);
	roll_scan.getFrame("flange_2_tool").move(lin(down_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));
	
	//movimiento en x
	overlapt=up_fr.getX()+overlap;
	up_fr.setX(overlapt);
	down_fr.setX(overlapt);
	//up_fr.setX(0.240*1000);
	//down_fr.setX(0.240*1000);
	roll_scan.getFrame("flange_2_tool").move(lin(down_fr).setCartVelocity(velocidad));
	
    // movimiento hacia el aleron parte baja
	down_fr.setZ(-distancia*1000);
	roll_scan.getFrame("flange_2_tool").move(lin(down_fr).setCartVelocity(25).setMode(impedanceControlMode));

	ForceSensorData current_force3 = lbr.getExternalForceTorque(roll_scan.getFrame("flange_2_tool"),roll_scan.getFrame("flange_2_tool"));
	System.out.println("Third Contact Z force: " + current_force3.getForce().getZ());
	
	
	roll_scan.getFrame("flange_2_tool").move(lin(up_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));
	
    //hasta aqui seria 3 PRUEBA


	// movimiento hacia atras
	up_fr.setZ(distancia*1000);
	roll_scan.getFrame("flange_2_tool").move(lin(up_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));
	
	//moviemiento en x
	overlapt=up_fr.getX()+overlap;
	up_fr.setX(overlapt);
	down_fr.setX(overlapt);
	//up_fr.setX(0.120*1000);
	//setX(0.120*1000);
	roll_scan.getFrame("flange_2_tool").move(lin(up_fr).setCartVelocity(velocidad));
	
	// 4 movimiento al aleron
	up_fr.setZ(-distancia*1000);
	roll_scan.getFrame("flange_2_tool").move(lin(up_fr).setCartVelocity(25).setMode(impedanceControlMode));
	ForceSensorData current_force4 = lbr.getExternalForceTorque(roll_scan.getFrame("flange_2_tool"),roll_scan.getFrame("flange_2_tool"));
	System.out.println("Fourth Contact Z force: " + current_force4.getForce().getZ());
	roll_scan.getFrame("flange_2_tool").move(lin(down_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));
	
    //movimiento hacia atras parte baja
	down_fr.setZ(distancia*1000);
	roll_scan.getFrame("flange_2_tool").move(lin(down_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));
	
	//movimiento en x
	overlapt=up_fr.getX()+overlap;
	up_fr.setX(overlapt);
	down_fr.setX(overlapt);
	//up_fr.setX(0.240*1000);
	//down_fr.setX(0.240*1000);
	roll_scan.getFrame("flange_2_tool").move(lin(down_fr).setCartVelocity(velocidad));
	
    // movimiento hacia el aleron parte baja
	down_fr.setZ(-distancia*1000);
	roll_scan.getFrame("flange_2_tool").move(lin(down_fr).setCartVelocity(25).setMode(impedanceControlMode));

	ForceSensorData current_force5 = lbr.getExternalForceTorque(roll_scan.getFrame("flange_2_tool"),roll_scan.getFrame("flange_2_tool"));
	System.out.println("Fifth Contact Z force: " + current_force5.getForce().getZ());

	roll_scan.getFrame("flange_2_tool").move(lin(up_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));


	// movimiento hacia atras
	up_fr.setZ(distancia*1000);
	roll_scan.getFrame("flange_2_tool").move(lin(up_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));
	
	//moviemiento en x
	overlapt=up_fr.getX()+overlap;
	up_fr.setX(overlapt);
	down_fr.setX(overlapt);
	//up_fr.setX(0.120*1000);
	//setX(0.120*1000);
	roll_scan.getFrame("flange_2_tool").move(lin(up_fr).setCartVelocity(velocidad));
	
	// 6 movimiento al aleron
	up_fr.setZ(-distancia*1000);
	roll_scan.getFrame("flange_2_tool").move(lin(up_fr).setCartVelocity(25).setMode(impedanceControlMode));
	ForceSensorData current_force6 = lbr.getExternalForceTorque(roll_scan.getFrame("flange_2_tool"),roll_scan.getFrame("flange_2_tool"));
	System.out.println("Sixth Contact Z force: " + current_force6.getForce().getZ());
	roll_scan.getFrame("flange_2_tool").move(lin(down_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));
	
    //movimiento hacia atras parte baja
	down_fr.setZ(distancia*1000);
	roll_scan.getFrame("flange_2_tool").move(lin(down_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));
	
	//movimiento en x
	overlapt=up_fr.getX()+overlap;
	up_fr.setX(overlapt);
	down_fr.setX(overlapt);
	//up_fr.setX(0.240*1000);
	//down_fr.setX(0.240*1000);
	roll_scan.getFrame("flange_2_tool").move(lin(down_fr).setCartVelocity(velocidad));
	
    // movimiento hacia el aleron parte baja
	down_fr.setZ(-distancia*1000);
	roll_scan.getFrame("flange_2_tool").move(lin(down_fr).setCartVelocity(25).setMode(impedanceControlMode));

	ForceSensorData current_force7 = lbr.getExternalForceTorque(roll_scan.getFrame("flange_2_tool"),roll_scan.getFrame("flange_2_tool"));
	System.out.println("Seventh Contact Z force: " + current_force7.getForce().getZ());

	roll_scan.getFrame("flange_2_tool").move(lin(up_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));


	
	//roll_scan.getFrame("Gripper").move(lin(getFrame("/aleron/P1")).setCartVelocity(50));
	//roll_scan.getFrame("Gripper").move(lin(getFrame("/aleron/P2")).setCartVelocity(50).setMode(impedanceControlMode));
	//roll_scan.getFrame("Gripper").move(lin(getFrame("/aleron/P3")).setCartVelocity(50).setMode(impedanceControlMode));
	//.getFrame("Gripper").move(lin(getFrame("/aleron/P4")).setCartVelocity(50).setMode(impedanceControlMode));
	rec.stopRecording();
	roll_scan.getFrame("flange_2_tool").move(linRel(0,0,-200).setCartVelocity(100));
	roll_scan.getFrame("flange_2_tool").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));

	//gripper.getFrame("/TCP2").move(linRel(100, 0, -200));
}
private void Force_XNT(double distancia, String nfichero, double velocidad ){
	
	//impedanceControlMode.setMaxCartesianVelocity(1000.0,1000.0,1000.0,Math.toRadians(60),Math.toRadians(60),Math.toRadians(60));
	//impedanceControlMode.setSpringPosition(roll_scan.getFrame("gripper"));
	//impedanceControlMode.parametrize(CartDOF.X).setStiffness(stiffnessX).setDamping(0.7);
	//impedanceControlMode.parametrize(CartDOF.Y).setStiffness(stiffnessY).setDamping(0.7);
	//impedanceControlMode.parametrize(CartDOF.Z).setStiffness(Force).setDamping(0.7);
	//impedanceControlMode.parametrize(CartDOF.ROT).setStiffness(300).setDamping(0.7);
	/*
 	LBRE1Redundancy redundancyInfo = new LBRE1Redundancy(Math.toRadians(-0.01), 2, 29);
	
 	rec.setFileName(nfichero);
	rec.addCartesianForce(roll_scan.getFrame("Gripper"),roll_scan.getFrame("Gripper"));
	rec.addCurrentCartesianPositionXYZ(roll_scan.getFrame("Gripper"), getApplicationData().getFrame("/robot_base"));
 	//downFrame
	down_fr = new Frame(getFrame("/aleron"));
	down_fr.setX(0.0); down_fr.setY(0.074*1000); down_fr.setZ(distancia*1000); //3.0 o 0.0333*1000 para 10N = 300K*0.0333
 	down_fr.setAlphaRad(0.0); down_fr.setBetaRad(0.0); down_fr.setGammaRad(0.0);
	down_fr.setRedundancyInformation(lbr, redundancyInfo);
	
	//upFrame
	up_fr = new Frame(getFrame("/aleron"));
	up_fr.setX(0.0*1000); up_fr.setY(0.274*1000); up_fr.setZ(-distancia*1000); 
 	up_fr.setAlphaRad(0.0); up_fr.setBetaRad(0.0); up_fr.setGammaRad(0.0);
	up_fr.setRedundancyInformation(lbr, redundancyInfo);
*/
   LBRE1Redundancy redundancyInfo = new LBRE1Redundancy(Math.toRadians(0.9), 2, 24);
	
 	rec.setFileName(nfichero);
	rec.addCartesianForce(roll_scan.getFrame("flange_2_tool"),roll_scan.getFrame("flange_2_tool"));
	rec.addCurrentCartesianPositionXYZ(roll_scan.getFrame("flange_2_tool"), getApplicationData().getFrame("/robot_base"));
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

	rec.enable();
	rec.startRecording();
	
	//int optionforce = getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, "How many Force do I have to do?", "10N", "15N", "20N", "24N", "END DO NOTHING");
	roll_scan.getFrame("flange_2_tool").move(ptp(getFrame("/aleron/Aprox1")).setJointVelocityRel(0.25));
	roll_scan.getFrame("flange_2_tool").move(ptp(getFrame("/aleron/Aprox")).setJointVelocityRel(0.25));
	
	
	roll_scan.getFrame("flange_2_tool").move(lin(down_fr).setCartVelocity(velocidad));
	down_fr.setZ(-distancia*1000);
	roll_scan.getFrame("flange_2_tool").move(lin(down_fr).setCartVelocity(25).setMode(impedanceControlMode));

	ForceSensorData current_force = lbr.getExternalForceTorque(roll_scan.getFrame("flange_2_tool"),roll_scan.getFrame("flange_2_tool"));
	System.out.println("First Contact Z force: " + current_force.getForce().getZ());
	
	roll_scan.getFrame("flange_2_tool").move(lin(up_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));

	// movimiento hacia atras
	up_fr.setZ(distancia*1000);
	roll_scan.getFrame("flange_2_tool").move(lin(up_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));
	
	//moviemiento en y
	up_fr.setY(0.120*1000);
	down_fr.setY(0.120*1000);
	roll_scan.getFrame("flange_2_tool").move(lin(up_fr).setCartVelocity(velocidad));
	
	// 2 movimiento al aleron
	up_fr.setZ(-distancia*1000);
	roll_scan.getFrame("flange_2_tool").move(lin(up_fr).setCartVelocity(25).setMode(impedanceControlMode));
	ForceSensorData current_force2 = lbr.getExternalForceTorque(roll_scan.getFrame("flange_2_tool"),roll_scan.getFrame("flange_2_tool"));
	System.out.println("Second Contact Z force: " + current_force2.getForce().getZ());
	roll_scan.getFrame("flange_2_tool").move(lin(down_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));
	
    //movimiento hacia atras parte baja
	down_fr.setZ(distancia*1000);
	roll_scan.getFrame("flange_2_tool").move(lin(down_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));
	
	//movimiento en y
	up_fr.setY(0.240*1000);
	down_fr.setY(0.240*1000);
	roll_scan.getFrame("flange_2_tool").move(lin(down_fr).setCartVelocity(velocidad));
	
    // movimiento hacia el aleron parte baja
	down_fr.setZ(-distancia*1000);
	roll_scan.getFrame("flange_2_tool").move(lin(down_fr).setCartVelocity(25).setMode(impedanceControlMode));

	ForceSensorData current_force3 = lbr.getExternalForceTorque(roll_scan.getFrame("flange_2_tool"),roll_scan.getFrame("flange_2_tool"));
	System.out.println("Third Contact Z force: " + current_force3.getForce().getZ());

	roll_scan.getFrame("flange_2_tool").move(lin(up_fr).setCartVelocity(velocidad).setMode(impedanceControlMode));

	
	//roll_scan.getFrame("Gripper").move(lin(getFrame("/aleron/P1")).setCartVelocity(50));
	//roll_scan.getFrame("Gripper").move(lin(getFrame("/aleron/P2")).setCartVelocity(50).setMode(impedanceControlMode));
	//roll_scan.getFrame("Gripper").move(lin(getFrame("/aleron/P3")).setCartVelocity(50).setMode(impedanceControlMode));
	//.getFrame("Gripper").move(lin(getFrame("/aleron/P4")).setCartVelocity(50).setMode(impedanceControlMode));
	rec.stopRecording();
	roll_scan.getFrame("flange_2_tool").move(linRel(0,0,-200).setCartVelocity(100));
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
				impedanceControlMode.setSpringPosition(roll_scan.getFrame("flange_2_tool"));
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
				.setSpringPosition(roll_scan.getFrame("flange_2_tool"));
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
	rec.addCartesianForce(roll_scan.getFrame("flange_2_tool"),roll_scan.getFrame("flange_2_tool"));
	rec.addCurrentCartesianPositionXYZ(roll_scan.getFrame("flange_2_tool"), getApplicationData().getFrame("/robot_base"));
 	//downFrame
	down_fr = new Frame(getFrame("/aleron"));
	down_fr.setX(0.0685*1000); down_fr.setY(1.5); down_fr.setZ(distancia*1000); //3.0 o 0.0333*1000 para 10N = 300K*0.0333
 	up_fr.setAlphaRad(-Math.PI/2);/* up_fr.setBetaRad(0.0)*/ up_fr.setGammaRad(Math.PI);
	down_fr.setRedundancyInformation(lbr, redundancyInfo);
	
	//upFrame
	up_fr = new Frame(getFrame("/aleron"));
	up_fr.setX(0.0685*1000); up_fr.setY(0.400*1000); up_fr.setZ(0.0*1000); 
 	up_fr.setAlphaRad(-Math.PI/2);/* up_fr.setBetaRad(0.0)*/ up_fr.setGammaRad(Math.PI);
	up_fr.setRedundancyInformation(lbr, redundancyInfo);
	
	rec.enable();
	rec.startRecording();

	//int optionforce = getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, "How many Force do I have to do?", "10N", "15N", "20N", "24N", "END DO NOTHING");
	roll_scan.getFrame("flange_2_tool").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));
	roll_scan.getFrame("flange_2_tool").move(ptp(getFrame("/aleron/Aprox1")).setJointVelocityRel(0.25));
	roll_scan.getFrame("flange_2_tool").move(ptp(getFrame("/aleron/Aprox")).setJointVelocityRel(0.25));
	

	roll_scan.getFrame("flange_2_tool").move(lin(down_fr).setCartVelocity(velocidad));
	down_fr.setZ(0.0*1000);
	roll_scan.getFrame("flange_2_tool").move(lin(down_fr).setCartVelocity(25).setMode(impedanceControlModeD));

	ForceSensorData current_force = lbr.getExternalForceTorque(roll_scan.getFrame("flange_2_tool"),roll_scan.getFrame("flange_2_tool"));
	System.out.println("First Contact Z force: " + current_force.getForce().getZ());
	
	roll_scan.getFrame("flange_2_tool").move(lin(up_fr).setCartVelocity(velocidad).setMode(impedanceControlModeD));

	// movimiento hacia atras
	up_fr.setZ(distancia*1000);
	roll_scan.getFrame("flange_2_tool").move(lin(up_fr).setCartVelocity(velocidad).setMode(impedanceControlModeD));
	
	//moviemiento en x
	overlapt=up_fr.getX()+overlap;
	up_fr.setX(overlapt);
	down_fr.setX(overlapt);
	roll_scan.getFrame("flange_2_tool").move(lin(up_fr).setCartVelocity(velocidad));
	
	// 2 movimiento al aleron
	up_fr.setZ(-0.005*1000);
	roll_scan.getFrame("flange_2_tool").move(lin(up_fr).setCartVelocity(25).setMode(impedanceControlMode));
	ForceSensorData current_force2 = lbr.getExternalForceTorque(roll_scan.getFrame("flange_2_tool"),roll_scan.getFrame("flange_2_tool"));
	System.out.println("Second Contact Z force: " + current_force2.getForce().getZ());
	roll_scan.getFrame("flange_2_tool").move(lin(down_fr).setCartVelocity(velocidad).setMode(impedanceControlModeD));
	
    //movimiento hacia atras parte baja
	down_fr.setZ(distancia*1000);
	roll_scan.getFrame("flange_2_tool").move(lin(down_fr).setCartVelocity(velocidad).setMode(impedanceControlModeD));
	
	//movimiento en X
	//up_fr.setX(0.250*1000);
	//down_fr.setX(0.250*1000);
	overlapt=up_fr.getX()+overlap;
	up_fr.setX(overlapt);
	down_fr.setX(overlapt);
	roll_scan.getFrame("flange_2_tool").move(lin(down_fr).setCartVelocity(velocidad));
	
    // movimiento hacia el aleron parte baja
	down_fr.setZ(0.0*1000);
	roll_scan.getFrame("flange_2_tool").move(lin(down_fr).setCartVelocity(25).setMode(impedanceControlModeD));

	ForceSensorData current_force3 = lbr.getExternalForceTorque(roll_scan.getFrame("flange_2_tool"),roll_scan.getFrame("flange_2_tool"));
	System.out.println("Third Contact Z force: " + current_force3.getForce().getZ());

	roll_scan.getFrame("flange_2_tool").move(lin(up_fr).setCartVelocity(velocidad).setMode(impedanceControlModeD));

	
	//roll_scan.getFrame("Gripper").move(lin(getFrame("/aleron/P1")).setCartVelocity(50));
	//roll_scan.getFrame("Gripper").move(lin(getFrame("/aleron/P2")).setCartVelocity(50).setMode(impedanceControlMode));
	//roll_scan.getFrame("Gripper").move(lin(getFrame("/aleron/P3")).setCartVelocity(50).setMode(impedanceControlMode));
	//.getFrame("Gripper").move(lin(getFrame("/aleron/P4")).setCartVelocity(50).setMode(impedanceControlMode));
	rec.stopRecording();
	roll_scan.getFrame("flange_2_tool").move(linRel(0,0,-200).setCartVelocity(100));
	roll_scan.getFrame("flange_2_tool").move(ptp(getFrame("/robot_base/SafePos")).setJointVelocityRel(0.25));

	//gripper.getFrame("/TCP2").move(linRel(100, 0, -200));
}

}
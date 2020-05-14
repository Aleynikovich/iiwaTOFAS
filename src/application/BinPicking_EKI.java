package application;


import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;

import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianImpedanceControlMode;

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
public class BinPicking_EKI extends RoboticsAPIApplication implements BinPicking_ITCPListener{
	
	@Inject
	private LBR lbr;
    private Controller controller;
    private Tool binpick;
    
    private Frame up_fr;
	private Frame down_fr;
	private Frame exit_fr;
	private double[] gripper_tool_xyz = new double[]{0,0,0.26448};
	private double[] gripper_tool_rpy = new double[]{0.0,0,-Math.PI/2};
	
	
	
    boolean exit;
    
    String fname;
    
    CartesianImpedanceControlMode impedanceControlMode;
    
   	private static final int stiffnessZ = 300;
   	private static final int stiffnessY = 5000;
   	private static final int stiffnessX = 5000;
   	

	private BinPicking_TCPServer tcp_server;
	AtomicBoolean data_received;
	
	//Exchanged data info 
	String operation_type;
	String time_stamp;
	Frame caltab_robot_fr;
   	
    @Inject
	private MediaFlangeIOGroup mediaFIO;
    
	@Override
	public void initialize() {
		// initialize your application here
		controller = getController("KUKA_Sunrise_Cabinet_1");
				

		// initialize your application here
		binpick = createFromTemplate("BinPick_Tool");
		binpick.attachTo(lbr.getFlange());
		
		System.out.println("BinPicking Tool frame: " + binpick.getFrame("tcp").toString());
		
		//data_received = new AtomicBoolean(false);
		
		//MediaFlangeIOGroup  FlangeIO= new  MediaFlangeIOGroup(controller);
		/*
		try {
			tcp_server = new BinPicking_TCPServer();				
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
		lbr.move(ptpHome());
	}

	@Override
	public void OnTCPMessageReceived(String datagram) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void OnTCPConnection() {
		// TODO Auto-generated method stub
		
	}
}
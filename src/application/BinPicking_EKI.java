package application;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket; 
import javax.inject.Inject; 
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;

import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianImpedanceControlMode;
import com.kuka.roboticsAPI.sensorModel.DataRecorder;
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
	private BinPicking_TCPClient tcp_client;
	AtomicBoolean data_received;
	AtomicBoolean server_connected;
	//Exchanged data info 
	String operation_type;
	String time_stamp;
	Frame caltab_robot_fr;
	Socket clientSocket = null;
    @Inject
	private MediaFlangeIOGroup mediaFIO;
    
	@Override
	public void initialize() {
		// initialize your application here
		controller = getController("KUKA_Sunrise_Cabinet_1");
				

		// initialize your application here
		binpick = createFromTemplate("BinPick_Tool");
		binpick.attachTo(lbr.getFlange());
		
		System.out.println("BinPicking Tool frame: " + binpick.getFrame("TCP").toString());
		
		data_received = new AtomicBoolean(false);
		
		MediaFlangeIOGroup  FlangeIO= new  MediaFlangeIOGroup(controller);
		
		

		//Servidor TCP
		/* try {
			tcp_server = new BinPicking_TCPServer();				
			tcp_server.addListener(this);
			tcp_server.enable();
					
		} catch (IOException e) {
			//TODO Bloque catch generado automáticamente
			System.err.println("Could not create TCPServer:" +e.getMessage());
		}
		 */
		//Cliente TCP
		try {
			tcp_client = new BinPicking_TCPClient();
			tcp_client.addListener(this);
			tcp_client.enable();
					
					data_received = new AtomicBoolean(false);
					server_connected = new AtomicBoolean(true);
							
			} catch (IOException e) {
				//TODO Bloque catch generado automáticamente
				System.err.println("Could not create TCPServer:" +e.getMessage());
		    }
		


	}

	@Override
	public void run() {
		// your application execution starts here
		getLogger().info("****************************");
		getLogger().info("     Sending Run to the BinPicking API...");
		

			getLogger().info("****************************");
	
			getLogger().info("****************************");
			getLogger().info("      Moving HomePos");
			getLogger().info("****************************");
			
			lbr.move(ptp(getFrame("/HOME_B")).setJointVelocityRel(0.25));
			
			exit=false;
			
			do {
			
				switch (getApplicationUI().displayModalDialog(
						ApplicationDialogType.QUESTION,"BIN PICKING API!!!", 
						"Calibration", "BinPicking Program", "END DO NOTHING")) {
	
						case 0:
							mediaFIO.setLEDBlue(true);
							calibration();
							mediaFIO.setLEDBlue(false);
	
							break;				
						case 1:
							tcp_client.sendData("101");	
							mediaFIO.setLEDBlue(true);
							
					
					
							break;					
			
						case 2:
							
							getLogger().info("App Terminated\n"+"***END***");
							exit = true;
							break;
							
							
							
				}
			} while (!exit);
	
		}
			
	
		
		
		
		
		
	

	@Override
	public void OnTCPMessageReceived(String datagram) {
		System.out.println("OnTCPMessageReceived: " + datagram);

		//data_recv = Integer.parseInt(datagram);
		
		data_received.set(true);
		
		
	}

	@Override
	public void OnTCPConnection() {
		// TODO Auto-generated method stub
		
	}
	
	public void calibration(){
		/* CALIBRATION MODE*/
		int cont=1;
		Frame robot_pose; 
		String request_str;
		getLogger().info("     Sending Run to the BinPicking API...");
		//tcp_client.sendData("102");	
		
		
		get_message("102","14");
				
		while(cont <= 15)
		{
			
			String frame_name = "/Calibration/P" + cont;
			lbr.move(ptp(getFrame(frame_name)).setJointVelocityRel(0.25));
			
			robot_pose = lbr.getCurrentCartesianPosition(lbr.getFlange());
			
			/* EL STRING DE LA POSE JUNTO*/
			  request_str = robot_pose.getX() + ";" + robot_pose.getY() + ";" + robot_pose.getZ() + ";" +
				robot_pose.getGammaRad() + ";" + robot_pose.getBetaRad()+ ";" + robot_pose.getAlphaRad() + ";" + "5";
		
			System.out.println(frame_name + " -->  " + request_str);
			System.out.println("data_recived=false");
			data_received.set(false);
			get_message(request_str,"0");
			cont++;
		}
				
		
		/*MANDAMOS CALIBRATE AL SISTEMA DE BINPICKIN*/
		request_str = "6";
		System.out.println(request_str);
		tcp_client.sendData(request_str);			
	
		/*TEST de la calibracion*/
		
		lbr.move(ptp(getFrame("/Calibration/Test_Calibration")));
		robot_pose = lbr.getCurrentCartesianPosition(lbr.getFlange());
		
		request_str = robot_pose.getX() + ";" + robot_pose.getY() + ";" + robot_pose.getZ() + ";" +
					robot_pose.getGammaRad() + ";" + robot_pose.getBetaRad()+ ";" + robot_pose.getAlphaRad() + ";" + "5";
			
		System.out.println(request_str);
	  	System.out.println("data_recived=false");
	  	data_received.set(false);
		get_message(request_str,"0");
		
		request_str = "14";
		System.out.println(request_str);
		tcp_client.sendData(request_str);			
		
		
}

	public void send_data(String request_str){
		
		
			tcp_server.setResponseData(request_str);
			//tcp_client.sendData(request_str);
		
			while(!data_received.get())
			{
				try {
					Thread.sleep(100);
					if(!server_connected.get())
					{
						System.out.println("Communication with the server has been lost");
						break;
					}
						
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(!server_connected.get())
					break;
				data_received.set(false);
			}
			
		}
public void get_message(String request_str, String ack_str){
	if(server_connected.get())
	{
		System.out.println("server_connected");
		tcp_client.sendData(request_str);
	
		while(!data_received.get())
		{
			try {
				
				Thread.sleep(100);
				if(!server_connected.get())
				{
					System.out.println("Communication with the server has been lost");
					break;
				}
					
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println("data_recived=TRUE");
		
		
		if (tcp_client.request_str.equals(ack_str)) {
			System.out.println("tcp_client.request_str: "
					+tcp_client.request_str+" == ack_str: "+ ack_str);
			
		}
			else{
				System.out.println("tcp_client.request_str:"
						+tcp_client.request_str+" != ack_str: "+ ack_str);
				
				}
		
	}
}

}

	


	
	

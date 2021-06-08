package application;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket; 
import javax.inject.Inject; 

import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.kuka.common.ThreadUtil;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;

import com.kuka.roboticsAPI.conditionModel.ICondition;
import com.kuka.roboticsAPI.conditionModel.JointTorqueCondition;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.JointEnum;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.executionModel.IFiredConditionInfo;
import com.kuka.roboticsAPI.geometricModel.CartDOF;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.motionModel.MotionBatch;
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
public class DEMO_BEC extends RoboticsAPIApplication implements BinPicking_ITCPListener{
	
	@Inject
	private LBR lbr;
    private Controller controller;
    private Tool binpick;
    
    
	private Frame down_fr;
	private Frame exit_fr;
	private Frame bin_pose;
	private Frame aprox_pos_part;
	private Frame pos_part;
	private double[] gripper_tool_xyz = new double[]{0,0,0.26448};
	private double[] gripper_tool_rpy = new double[]{0.0,0,-Math.PI/2};
	IMotionContainer motion;
	
	
	//nuevo
	
	public static JointTorqueCondition parColisiónJ1 = new JointTorqueCondition(JointEnum.J1, -14, 14);
	public static JointTorqueCondition parColisiónJ2 = new JointTorqueCondition(JointEnum.J2, -10, 10); 
	public static JointTorqueCondition parColisiónJ3 = new JointTorqueCondition(JointEnum.J3, -8, 8); 
	public static JointTorqueCondition parColisiónJ4 = new JointTorqueCondition(JointEnum.J4, -8, 8); 
	public static JointTorqueCondition parColisiónJ5 = new JointTorqueCondition(JointEnum.J5, -5, 5); 
	public static JointTorqueCondition parColisiónJ6 = new JointTorqueCondition(JointEnum.J6, -4, 4); 
	public static JointTorqueCondition parColisiónJ7 = new JointTorqueCondition(JointEnum.J7, -2, 2); 
	private static int tiempoEsperaTrasColisión = 1500;
	private static double sensibilidadColisión = 0;
	
	public final static int ORDEN_GOLPEDERECHA = 1;
	public final static int ORDEN_GOLPEIZQUIERDA = 2;
	public final static int ORDEN_GOLPEARRIBA = 3;
	public final static int ORDEN_GOLPEABAJO = 4;
	public final static int ORDEN_GOLPEDELANTE = 5;
	public final static int ORDEN_GOLPEATRÁS = 6;
	
	private static CartesianImpedanceControlMode blandito=new CartesianImpedanceControlMode();
	
	private ICondition condColisión = null;
	private IMotionContainer movimientoSeguro = null;
	private IFiredConditionInfo colisiónDetectada = null;
	
	
	//nuevo fin
	
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
	int pose;
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
		
		bin_pose = new Frame(getFrame("/BinPicking"));
		bin_pose.setX(1); bin_pose.setY(1); bin_pose.setZ(1); 
		bin_pose.setAlphaRad(0.0); bin_pose.setBetaRad(0.0); bin_pose.setGammaRad(0.0);
		
		pos_part = new Frame(getFrame("/BinPicking"));
		pos_part.setX(1); pos_part.setY(1); pos_part.setZ(1); 
		pos_part.setAlphaRad(0.0); pos_part.setBetaRad(0.0); pos_part.setGammaRad(0.0);
		
		
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
		
		//nuevo
		
		
		
		//

	}

	@Override
	public void run() {
		// your application execution starts here
		getLogger().info("****************************");
		getLogger().info("     Connecting to the SERVER API...");
		mediaFIO.setLedGreen(false);
		mediaFIO.setLEDBlue(false);
		mediaFIO.setLedRed(false);
		
		mediaFIO.setLedGreen(false);
		mediaFIO.setLEDBlue(true);
		mediaFIO.setLedRed(true);

			getLogger().info("****************************");
	
			getLogger().info("****************************");
			getLogger().info("      Moving HomePos");
			getLogger().info("****************************");
			
			lbr.move(ptp(getFrame("/HOME_B")).setJointVelocityRel(0.10));
			
			exit=false;
			//new borrar
			//tcp_client.sendData("0");
			data_received.set(false);
			//borrar
			do {
			
				switch (getApplicationUI().displayModalDialog(
						ApplicationDialogType.QUESTION,"BIN PICKING API!!!", 
						"DEMO BEC", "END DO NOTHING")) {
	
						case 0:
							mediaFIO.setLedGreen(true);
							mediaFIO.setLEDBlue(false);
							mediaFIO.setLedRed(true);
							
							calibration();
							
							mediaFIO.setLedGreen(false);
							mediaFIO.setLEDBlue(false);
							mediaFIO.setLedRed(false);
	
							break;				
						
						case 1:
							mediaFIO.setLedGreen(false);
							mediaFIO.setLEDBlue(false);
							mediaFIO.setLedRed(false);
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
		boolean ret=false;
		int noenviar=0;
		int cont=1;
		int pose=0;
		Frame robot_pose; 
		String request_str;
		getLogger().info("     Sending Run to the BinPicking API...");
		//tcp_client.sendData("102");	
		
		
		
			ret=false;
			while (true) {
			while(cont <= 15)
			{
				
				String frame_name = "/Calibration/P" + cont;
				lbr.move(ptp(getFrame(frame_name)).setJointVelocityRel(0.1));
				
				robot_pose = lbr.getCurrentCartesianPosition(lbr.getFlange());
				
				/* EL STRING DE LA POSE JUNTO X,Y,Z,GZ,GA,GB*/ 
				//add calibration point
				
				request_str = "1";
				
			
				System.out.println(request_str);
				System.out.println("data_recived=false");
				data_received.set(false);
				tcp_client.sendData(request_str);
				
				//ret=false;
				ThreadUtil.milliSleep(3000);
				
				
				
				//ret=false;
				
				
			}
	
				
				
				cont++;
			}
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
	
	
public boolean get_message(String request_str, String ack_str){
	boolean ret=false;
	if(server_connected.get())
	{
		System.out.println("server_connected");
		tcp_client.sendData(request_str);
		data_received.set(false);
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
					ret=true;
		}
			else{
				System.out.println("tcp_client.request_str:"
						+tcp_client.request_str+" != ack_str: "+ ack_str);
				ret=false;
				}
		
	}
	return ret;
}
}

	


	
	

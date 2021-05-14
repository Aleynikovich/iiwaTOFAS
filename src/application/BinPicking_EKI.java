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

import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.LBR;
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
public class BinPicking_EKI extends RoboticsAPIApplication implements BinPicking_ITCPListener{
	
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
		


	}

	@Override
	public void run() {
		// your application execution starts here
		getLogger().info("****************************");
		getLogger().info("     Connecting to the BinPicking API...");
		

			getLogger().info("****************************");
	
			getLogger().info("****************************");
			getLogger().info("      Moving HomePos");
			getLogger().info("****************************");
			
			lbr.move(ptp(getFrame("/HOME_B")).setJointVelocityRel(0.25));
			
			exit=false;
			//new borrar
			//tcp_client.sendData("0");
			data_received.set(false);
			//borrar
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
							mediaFIO.setLEDBlue(false);
							
							bin_picking_run();
					
					
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
		boolean ret=false;
		int cont=1;
		Frame robot_pose; 
		String request_str;
		getLogger().info("     Sending Run to the BinPicking API...");
		//tcp_client.sendData("102");	
		
		
		ret=get_message("102","0");
		if (ret==true){
			ret=false;
			
			while(cont <= 15)
			{
				
				String frame_name = "/Calibration/P" + cont;
				lbr.move(ptp(getFrame(frame_name)).setJointVelocityRel(0.25));
				
				robot_pose = lbr.getCurrentCartesianPosition(lbr.getFlange());
				
				/* EL STRING DE LA POSE JUNTO*/
				  request_str = robot_pose.getX() + ";" + robot_pose.getY() + ";" + robot_pose.getZ() + ";" +
					robot_pose.getGammaRad() + ";" + robot_pose.getBetaRad()+ ";" + robot_pose.getAlphaRad() + ";" + "5" + "\n";
			
				System.out.println(frame_name + " -->  " + request_str);
				System.out.println("data_recived=false");
				data_received.set(false);
				ret=get_message(request_str,"0");
				if (ret){
					System.out.println("Calibration Point added");
				}
				else
				{
					System.out.println("Calibration Point NOT added, EXIT");
					return;
					
				}
				
				ret=false;
				cont++;
			}
					
			
			/*MANDAMOS CALIBRATE AL SISTEMA DE BINPICKING*/
			ret=false;
			request_str = "6";
			ret=get_message(request_str,"0");
			if (ret){
				System.out.println("Calibration DONE");
			}
			else
			{
				System.out.println("Calibration NOT done, EXIT");
				return;
				
			}
		
			/*TEST de la calibracion*/
			
			lbr.move(ptp(getFrame("/Calibration/Test_Calibration")));
			robot_pose = lbr.getCurrentCartesianPosition(lbr.getFlange());
			
			request_str = robot_pose.getX() + ";" + robot_pose.getY() + ";" + robot_pose.getZ() + ";" +
						robot_pose.getGammaRad() + ";" + robot_pose.getBetaRad()+ ";" + robot_pose.getAlphaRad() + ";" + "5" + "\n";
				
			System.out.println(request_str);
		  	System.out.println("data_recived=false");
		  	data_received.set(false);
		  	ret=false;
			ret=get_message(request_str,"0");
			
			ret=false;
			request_str = "14";
			ret=get_message(request_str,"0");
			if (ret){
				System.out.println("Calibration TEST DONE");
			}
			else
			{
				System.out.println("Calibration Test NOT done, EXIT");
				return;
				
			}
		}
		else
		{
			System.out.println("Error setting up calibration mode");
			return;
			
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
	
	public void bin_picking_run(){
		
		int cont_pos_part=0;
		int cont_pos_bin=0;
		boolean ret=false;
		boolean bin_no_located=false;
		boolean part_located, part_not_located,new_part,part_error;
		part_located=false;
		part_not_located=false;
		new_part=false;
		part_error=false;
		//load reference	
		ret=get_message("15;d-10597","0");
		if (ret){
			System.out.println("Reference set");
		}
		else
		{
			System.out.println("Reference NOT set, EXIT");
			return;
			
		}
		//set status run
		ret=get_message("101","0");
		if (ret){
			System.out.println("RUN MODE OK");
		}
		else
		{
			System.out.println("RUN MODE NOT OK, EXIT");
			return;
			
		}
		ThreadUtil.milliSleep(1000);
		ret=false;
		//Set reference

		
		
		lbr.move(ptp(getFrame("/HOME_B")).setJointVelocityRel(0.25));
		
	do{
		mediaFIO.setLEDBlue(false);
		if (cont_pos_part==0 || part_not_located==true) {
			ret=false;
			part_located=false;
			part_not_located=false;
			new_part=false;
			part_error=false;
			//TRIGGER CAMARA
			ret=get_message("2","0");
			if (ret){
				System.out.println("TRIGGER DONE");
			}
			else
			{
				System.out.println("Trigger not DONE, EXIT");
				return;
				
			}
			mediaFIO.setLEDBlue(true);
			lbr.move(ptp(getFrame("/HOME_B")).setJointVelocityRel(0.25));
		
		}
		//LOCATE BIN
		if(cont_pos_bin==0 || bin_no_located){
			ret=false;
			ret=get_message("3","0");
			ret=false;
			//GET LOCATE BIN
			ret=get_pose("8","0",bin_pose);
			if (ret){
				System.out.println("Bin located");
				bin_no_located=false;
			}
			else
			{
				System.out.println("Bin not located, EXIT");
				bin_no_located=true;
				
			}
		}
		
		if (bin_no_located==false){
			ret=false;
			//LOCATE PART
			if (cont_pos_part==0){
			ret=get_message("4","0");
			if (ret){
				System.out.println("LocatePart_send");
				part_error=false;
				}
				
			else
			{
				System.out.println("Locate Part_send_error, EXIT");
				part_error=true;
				
			}
			
			//GET LOCATE PART
			part_not_located=false;
			part_located=false;
			ret=false;
			if (part_error==false && cont_pos_part==0) {
			
				ret=get_pose("9","0",pos_part);
				if (ret){
					System.out.println("LocatePart_send");
					part_not_located=false;
					part_located=true;
								}
				else
				{
					System.out.println("Locate Part_send_error, EXIT");
					part_not_located=true;
					part_located=false;
					
				}
				}
			}
			else if (part_error==false && cont_pos_part>0){
				
				//NEW PART
				ret=false;
				ret=get_pose("11","0",pos_part);
				if (ret){
					System.out.println("LocatePart_send");
					part_not_located=false;
					part_located=true;
								}
				else
				{
					System.out.println("Locate Part_send_error, EXIT");
					part_not_located=true;
					part_located=false;
					
				}
				}
				
			if (part_located){
			mediaFIO.setLEDBlue(true);
			MotionBatch pick_pose;
			pick_pose  = new MotionBatch(
					ptp(getFrame("/BinPicking/BinPose")).setJointVelocityRel(0.25).setBlendingCart(80),
					ptp(getFrame("/BinPicking/PartPose")).setJointVelocityRel(0.25).setBlendingCart(80),
					lin(getFrame("/BinPicking/PartPose")).setJointVelocityRel(0.25)
					);
			motion = binpick.getFrame("TCP").move(pick_pose);
			
			cont_pos_part++;
			cont_pos_bin++;
			part_located=false;
			part_not_located=false;
			}
			if (cont_pos_part==4) {
				lbr.move(ptp(getFrame("/HOME_B")).setJointVelocityRel(0.25));
				cont_pos_part=0;
				}
			if (cont_pos_bin==10){
				lbr.move(ptp(getFrame("/HOME_B")).setJointVelocityRel(0.25));
				cont_pos_part=0;
				cont_pos_bin=0;
			}
		}
	
	} while (!exit);
		
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

public boolean get_pose(String request_str, String ack_str, Frame pose){
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

		String delims = "[,]";
		String[] tokens = tcp_client.request_str.split(delims);
/*
		for (int i = 0; i < tokens.length; i++)
		    System.out.println(tokens[i]);]*/		
		
		if (tokens[0].equals(ack_str)) {
			if (request_str.equals("8")){
				pose.setX(Double.parseDouble(tokens[5]));
				pose.setY(Double.parseDouble(tokens[6]));
				pose.setZ(Double.parseDouble(tokens[7]));
				pose.setAlphaRad(Double.parseDouble(tokens[8]));
				pose.setBetaRad(Double.parseDouble(tokens[9]));
				pose.setGammaRad(Double.parseDouble(tokens[10]));
				System.out.println("BIN_POSE: "+pose);
			}
			else
			{
				pose.setX(Double.parseDouble(tokens[5]));
				pose.setY(Double.parseDouble(tokens[6]));
				pose.setZ(Double.parseDouble(tokens[7]));
				pose.setAlphaRad(Double.parseDouble(tokens[8]));
				pose.setBetaRad(Double.parseDouble(tokens[9]));
				pose.setGammaRad(Double.parseDouble(tokens[10]));
				System.out.println("BIN_POSE: "+pose);
			}
			ret=true;
		}
			else{
				System.out.println("No more Parts");
				ret=false;
				}
		
	}
	return ret;
}






}

	


	
	

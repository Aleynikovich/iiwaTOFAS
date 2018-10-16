package application;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.inject.Inject;

import application.ROS_driver.RobotMode;

import com.kuka.connectivity.motionModel.directServo.DirectServo;
import com.kuka.connectivity.motionModel.directServo.IDirectServoRuntime;
import com.kuka.connectivity.motionModel.smartServo.ISmartServoRuntime;
import com.kuka.connectivity.motionModel.smartServo.SmartServo;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;

import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.controllerModel.sunrise.SunriseController;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.math.Vector;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.sensorModel.DataRecorder;
import com.kuka.roboticsAPI.sensorModel.DataRecorder.AngleUnit;
import com.kuka.roboticsAPI.uiModel.ApplicationDialogType;
import com.sun.jmx.snmp.Timestamp;

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



class DirectControl extends Thread {

    private volatile boolean flag = true;
	PrintWriter writer;


	public DirectControl(){
		RosUdpDriver.received_packet_bool = false;
    	try {
			writer = new PrintWriter("C:/KRC/ROBOTER/log/DataRecorder/test/test_control_14.txt", "UTF-8");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
    public void stop_running()
    {
    	flag = false;
    }
    
    void setRobotCommand(String[] parameters)
    {
    	if (RosUdpDriver.directServo == null) RosUdpDriver.directServo = new DirectServo(RosUdpDriver.robot.getCurrentJointPosition());
		
		if (RosUdpDriver.lastRobotMode != RosUdpDriver.RobotMode.direct){
			if (RosUdpDriver.motionContainer != null) RosUdpDriver.motionContainer.cancel();
			RosUdpDriver.motionContainer = RosUdpDriver.robot.moveAsync(RosUdpDriver.directServo);
			RosUdpDriver.directMotion = RosUdpDriver.directServo.getRuntime();
		}
		
		try {
		
			JointPosition jointPosition = new JointPosition(
					Double.parseDouble(parameters[0]), 
					Double.parseDouble(parameters[1]),
					Double.parseDouble(parameters[2]),
					Double.parseDouble(parameters[3]),
					Double.parseDouble(parameters[4]),
					Double.parseDouble(parameters[5]),
					Double.parseDouble(parameters[6]));
			
			JointPosition jointSpeed = new JointPosition(
					Double.parseDouble(parameters[7]), 
					Double.parseDouble(parameters[8]),
					Double.parseDouble(parameters[9]),
					Double.parseDouble(parameters[10]),
					Double.parseDouble(parameters[11]),
					Double.parseDouble(parameters[12]),
					Double.parseDouble(parameters[13]));
	

			//RosUdpDriver.directServo.setJointVelocityRel(jointSpeed.get());
			//RosUdpDriver.directMotion.setMinimumTrajectoryExecutionTime(30e-3);
			
			try{
				RosUdpDriver.directMotion.setDestination(jointPosition);
			} catch(Exception e) {
				System.out.println(e.toString());
				
				RosUdpDriver.directMotion.stopMotion();
				stop_running();
				
			}
			RosUdpDriver.lastRobotMode = RosUdpDriver.RobotMode.direct;
	    } catch(Exception e) {
			//System.out.println(e.toString());
	    }
		if(flag == false)
		{
			System.out.println("HERE, should go out");
			writer.close();
		}
	}
    public String[] parseDatagram(DatagramPacket packet)
    {
    	String command = "";
		String[] parameters = new String[0];
		String line
    		= new String(packet.getData(), 0, packet.getLength());
    	
		String[] processedLine = line.split(":");
		
		if (processedLine.length == 1){
			command = processedLine[0].trim();
		} else if (processedLine.length == 2){
			//System.out.println("aqui");
			command = processedLine[0].trim();
			parameters = processedLine[1].trim().split("\\s+");
		}
		
    	return parameters;
    }
    
    @Override
    public void run() {

    	long delay = 10; // 10 ms for each loop
        while (flag) {
        	
            if(RosUdpDriver.received_packet_bool){

            	RosUdpDriver.received_packet_bool = false;
            	String[] commands;
                synchronized (RosUdpDriver.lock) {
                	commands = RosUdpDriver.received_packet;
                }
                
                
            	long next = System.currentTimeMillis();
            	// -------------- CONTROL LOOP ------------ //
            	writer.println(System.currentTimeMillis() + " " + commands[5] + " " + commands[6]);
            	setRobotCommand(commands); 
            	// -------------------------------------- //
            	
            	next +=delay;
            	long sleep = next - System.currentTimeMillis();
			    if (sleep > 0)
					try {
						Thread.sleep(sleep);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
            }
             
        }
		System.out.println("Leaving the controller server");
    }
}


class EchoServer extends Thread {
	 
    private DatagramSocket socket;
    private boolean running;
    private byte[] buf = new byte[256];
    private volatile boolean flag = true;
    private int port = 30000;
    private int counter = 0;
	
	// -------------------------------------------- // 
	PrintWriter writer;

     
    public EchoServer() {
        try {
			socket = new DatagramSocket(port);
		} catch (SocketException e) {
			System.out.println(e.toString());
		}
        
    	try {
			writer = new PrintWriter("C:/KRC/ROBOTER/log/DataRecorder/test/test1_14.txt", "UTF-8");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public void stop_running()
    {

    	flag = false;

    }
    public String[] parseDatagram(DatagramPacket packet)
    {
    	String command = "";
		String[] parameters = new String[0];
		String line
    		= new String(packet.getData(), 0, packet.getLength());
    	
		String[] processedLine = line.split(":");
		
		if (processedLine.length == 1){
			command = processedLine[0].trim();
		} else if (processedLine.length == 2){
			//System.out.println("aqui");
			command = processedLine[0].trim();
			parameters = processedLine[1].trim().split("\\s+");
		}
		
    	return parameters;
    }
    @Override
    public void run() {

		try {
			socket.setSoTimeout(1000);
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
		
		

		System.out.println("Ready to receive packetes");

        while (flag) {
    		
            DatagramPacket packet 
              = new DatagramPacket(buf, buf.length);
            
            boolean received_packet = true;
            try {
				socket.receive(packet);
			} catch (SocketTimeoutException e) {
				//System.out.println(e.toString());
				received_packet = false;
			} catch (IOException e) {
				System.out.println(e.toString());
				received_packet = false;
			}

            
            if(received_packet){
            	
            	String[] commands = parseDatagram(packet);
            	writer.println(System.currentTimeMillis() + " " + commands[5] + " " + commands[6]);


                synchronized (RosUdpDriver.lock) {
                	RosUdpDriver.received_packet = commands;
                }
            	RosUdpDriver.received_packet_bool = true;

            }
             
        }
		System.out.println("Leaving the thread server");
    	writer.close();
        socket.close();
    }
}



class EchoClient extends Thread {
    private DatagramSocket socket;
    private InetAddress address;
    private String ros_adress = "172.31.1.100";
    private int port = 30000;
    private volatile boolean flag = true;

    
 
    private byte[] buf;
 
    public EchoClient() {
        try {
			socket = new DatagramSocket();
		} catch (SocketException e) {
			System.out.println(e.toString());
			System.out.println("errore");

		}
        try {
			address = InetAddress.getByName(ros_adress);
		} catch (UnknownHostException e) {
			System.out.println(e.toString());
			System.out.println("errore2");

		}
    }
    
    public void stop_running()
    {
    	flag = false;
    	
    	
    }
 
    public void closeServer(){
        
        System.out.println("Commanding the closure of the thread server");
		try {
			address = InetAddress.getByName("localhost");
		} catch (UnknownHostException e1) {
			System.out.println(e1.toString());
		}
		String msg = "close";
        buf = msg.getBytes();
        DatagramPacket packet 
          = new DatagramPacket(buf, buf.length, address, 30201);
        
        DatagramPacket received_packet 
        	= new DatagramPacket(buf, buf.length);

        try {
			socket.setSoTimeout(1000);
		} catch (SocketException e1) {
			e1.printStackTrace();
		}   // set the timeout in millisecounds.

        Boolean received_yes = true;
        while(true){        // recieve data until timeout

        	try {
				socket.send(packet);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

        	received_yes = true;
			try {
				socket.receive(received_packet);
				
			} catch (IOException e) {
				received_yes = false;
				e.printStackTrace();
			}
			if(received_yes)
			{
	        	String received 
	        	= new String(received_packet.getData(), 0, received_packet.getLength());
	        	if(received_packet.equals("close")){
	        		break;
	        	}
			}
        }
    }
    
    
    public String get_state(){
    	
    	
		//STATE
    	    	
		double j[] = RosUdpDriver.robot.getCurrentJointPosition().get();
		
		String result = String.valueOf(j[0]);
		for (int i = 1; i <= 6; i++) result = result + " " + String.valueOf(j[i]);
		
		Vector force = RosUdpDriver.robot.getExternalForceTorque(RosUdpDriver.tool.getDefaultMotionFrame()).getForce();
		Vector torque = RosUdpDriver.robot.getExternalForceTorque(RosUdpDriver.tool.getDefaultMotionFrame()).getTorque();
		result = result + " " + force.getX() + " " + force.getY() + " " + force.getZ() + " " + torque.getX() + " " + torque.getY() + " " + torque.getZ();
			
		return result;	
    }
    
    @Override
    public void run() {

        while (flag) {
        	
        	String robot_state = get_state();
        	
        	sendEcho(robot_state);
        }
        
        //closeServer();

		System.out.println("Leaving the thread client");

        socket.close();
    }
    
    public void sendEcho(String msg) {
        buf = msg.getBytes();
        DatagramPacket packet 
          = new DatagramPacket(buf, buf.length, address, port);
        try {
			socket.send(packet);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println(e.toString());
		}
//        packet = new DatagramPacket(buf, buf.length);
//        socket.receive(packet);
//        String received = new String(
//          packet.getData(), 0, packet.getLength());
//        return received;
    }
 
    public void close() {
        socket.close();
    }
}



public class RosUdpDriver extends RoboticsAPIApplication {
	@Inject
	public static LBR robot;
	private Controller controller;
	public static Tool tool;
	
    public static boolean received_packet_bool = true;    
    public static String[] received_packet; 
    
    public final static Object lock = new Object();
    
	private DataRecorder rec;



	public static IMotionContainer motionContainer = null;
	
	EchoServer server_;
	EchoClient client_;
	DirectControl direct_control_;
	SmartControl  smart_control_;

	
    boolean exit;
    
    public static enum RobotMode {unknown, normal, impedance, smart, direct} // robot behaviour differs between modes
	
	public static RobotMode lastRobotMode = RobotMode.unknown; // Stores current robot move
	
	private int counter = 0;
	
	SmartServo smartServo = null;
	ISmartServoRuntime smartMotion = null;
	
	public static DirectServo directServo = null;
	public static IDirectServoRuntime directMotion = null;
	
	public static JointPosition simulation_joints = null;

	public void main(String[] args) {
		
		RosUdpDriver app = new RosUdpDriver();
		app.runApplication();
	}

	@Override
	public void initialize() {
		controller = (SunriseController) getContext().getDefaultController();
		robot = (LBR) getRobot(controller, "LBR_iiwa_14_R820_1");
		tool = createFromTemplate("Tool");
		tool.attachTo(robot.getFlange()); // Attach the tool
	
		
	}
	
	
	@Override
	public void dispose(){
		System.out.println("Stoping motion... ");
		//Stop the server
		//server_.stopRunning();
		if (motionContainer != null) motionContainer.cancel();

		//if (motionContainer != null) motionContainer.cancel();
		System.out.println("Closing the sockets... ");

//		try { clientSocket.close(); } catch (Exception e) { }
//		try { serverSocket.close(); } catch (Exception e) { }
        super.dispose();

	}

	@Override
	public void run() {
		
		System.out.println("Initializing tcp server... ");
		int port = getApplicationData().getProcessData("port").getValue();
		System.out.println("my port is:"+port);
		
		rec = new DataRecorder();
		rec.setTimeout(-1, TimeUnit.SECONDS);
		rec.setSampleInterval(5);
		rec.setTimeout(60L, TimeUnit.SECONDS);
		rec.setFileName("test_11.txt");
		
		rec.addCommandedJointPosition(RosUdpDriver.robot, AngleUnit.Degree);
		rec.addCurrentJointPosition(RosUdpDriver.robot, AngleUnit.Degree);
	
		
		rec.enable();
		rec.startRecording();

		
		
		
		try{
			server_ = new EchoServer();
			server_.start();
			
			client_ = new EchoClient();
			client_.start();
			
//			direct_control_ = new DirectControl();
//			direct_control_.start();
//			direct_control_.setPriority(Thread.MAX_PRIORITY);
			
			smart_control_ = new SmartControl();
			smart_control_.start();
			
			exit=false;
			do {
				switch (getApplicationUI().displayModalDialog(
						ApplicationDialogType.QUESTION,"Wanna Stop?", 
						"STOP ROBOT")) {
						case 0:
							getLogger().info("App Terminated\n"+"***END***");
							exit = true;
							break;
				}
			}while(!exit);
			
			rec.stopRecording();

			
			client_.stop_running();
			System.out.println("Closed the client ");
			
			direct_control_.stop_running();
			System.out.println("Closed the controller ");


			server_.stop_running();
			System.out.println("Closed the server ");

			
		} catch (Exception e){
			// Stop button clicked in the control pad or critical error
			// Sockets are close in the dispose function
			// Some events like a broken connection could reach this Exception Handler,
			// but I considered better to start again the program than don't stop the robot
			if (motionContainer != null) motionContainer.cancel();
			e.printStackTrace(); // Trace in red in the pad
			
		}
	
	}
}

class SmartControl extends Thread {

    private volatile boolean flag = true;
	PrintWriter writer;
	
	SmartServo smartServo = null;
	ISmartServoRuntime smartMotion = null;


	public SmartControl(){
		RosUdpDriver.received_packet_bool = false;
    	try {
			writer = new PrintWriter("C:/KRC/ROBOTER/log/DataRecorder/test/test_control_14.txt", "UTF-8");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
    public void stop_running()
    {
    	flag = false;
    }
    
    void setRobotCommand(String[] parameters)
    {
		if (smartServo == null) smartServo = new SmartServo(RosUdpDriver.robot.getCurrentJointPosition());
		
		if (RosUdpDriver.lastRobotMode != RosUdpDriver.RobotMode.smart){
			if (RosUdpDriver.motionContainer != null) RosUdpDriver.motionContainer.cancel();
			smartServo.overrideJointAcceleration(8.0);
			smartServo.setJointAccelerationRel(1.0);
			smartServo.setJointVelocityRel(1.0);
			RosUdpDriver.motionContainer = RosUdpDriver.robot.moveAsync(smartServo);
			smartMotion = smartServo.getRuntime();
		}
		
		try {
		
			JointPosition jointPosition = new JointPosition(
					Double.parseDouble(parameters[0]), 
					Double.parseDouble(parameters[1]),
					Double.parseDouble(parameters[2]),
					Double.parseDouble(parameters[3]),
					Double.parseDouble(parameters[4]),
					Double.parseDouble(parameters[5]),
					Double.parseDouble(parameters[6]));
			
			JointPosition jointSpeed = new JointPosition(
					Double.parseDouble(parameters[7]), 
					Double.parseDouble(parameters[8]),
					Double.parseDouble(parameters[9]),
					Double.parseDouble(parameters[10]),
					Double.parseDouble(parameters[11]),
					Double.parseDouble(parameters[12]),
					Double.parseDouble(parameters[13]));

				smartMotion.setMinimumTrajectoryExecutionTime(30e-3);

			try{
				smartMotion.setDestination(jointPosition);//, jointSpeed);
				
			} catch(Exception e) {
				System.out.println(e.toString());
				
				smartMotion.stopMotion();
				stop_running();
				
			}
			RosUdpDriver.lastRobotMode = RosUdpDriver.RobotMode.smart;
	    } catch(Exception e) {
			//System.out.println(e.toString());
	    }
		if(flag == false)
		{
			System.out.println("HERE, should go out");
			writer.close();
		}
	}
    public String[] parseDatagram(DatagramPacket packet)
    {
    	String command = "";
		String[] parameters = new String[0];
		String line
    		= new String(packet.getData(), 0, packet.getLength());
    	
		String[] processedLine = line.split(":");
		
		if (processedLine.length == 1){
			command = processedLine[0].trim();
		} else if (processedLine.length == 2){
			//System.out.println("aqui");
			command = processedLine[0].trim();
			parameters = processedLine[1].trim().split("\\s+");
		}
		
    	return parameters;
    }
    
    @Override
    public void run() {

        while (flag) {
    		
            if(RosUdpDriver.received_packet_bool){
            	RosUdpDriver.received_packet_bool = false;
            	
            	String[] commands;
                synchronized (RosUdpDriver.lock) {
                	commands = RosUdpDriver.received_packet;
                }
            	writer.println(System.currentTimeMillis() + " " + commands[5] + " " + commands[6]);
            	setRobotCommand(commands);  	
            }
             
        }
		System.out.println("Leaving the controller server");
    }
}

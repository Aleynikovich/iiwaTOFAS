package application;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import javax.inject.Inject;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;

import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.controllerModel.sunrise.SunriseController;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
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


class EchoServer extends Thread {
	 
    private DatagramSocket socket;
    private boolean running;
    private byte[] buf = new byte[256];
    private volatile boolean flag = true;
     

 
    public EchoServer() {
        try {
			socket = new DatagramSocket(30203);
		} catch (SocketException e) {
			System.out.println(e.toString());
		}
    }
    
    public void stop_running()
    {
    	flag = false;
    }
 
    @Override
    public void run() {

        while (flag) {
    		
            DatagramPacket packet 
              = new DatagramPacket(buf, buf.length);
            try {
				socket.receive(packet);
			} catch (IOException e) {
				System.out.println(e.toString());
			}
             
//            InetAddress address = packet.getAddress();
//            int port = packet.getPort();
//            packet = new DatagramPacket(buf, buf.length, address, port);
            String received 
              = new String(packet.getData(), 0, packet.getLength());            
            if (received.equals("close")) {
                flag = false;
                continue;
            }
//            socket.send(packet);
        }
		System.out.println("Leaving the thread server");

        socket.close();
    }
}



class EchoClient extends Thread {
    private DatagramSocket socket;
    private InetAddress address;
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
			address = InetAddress.getByName("localhost");
		} catch (UnknownHostException e) {
			System.out.println(e.toString());
			System.out.println("errore2");

		}
    }
    
    public void stop_running()
    {
    	flag = false;
    	
    	
    }
 
    @Override
    public void run() {

        while (flag) {
        	
        	sendEcho("HOLA");
        }
		System.out.println("Commanding the closure of the thread server");
		try {
			address = InetAddress.getByName("localhost");
		} catch (UnknownHostException e1) {
			System.out.println(e1.toString());
		}
		String msg = "close";
        buf = msg.getBytes();
        DatagramPacket packet 
          = new DatagramPacket(buf, buf.length, address, 30203);
        try {
			socket.send(packet);
		} catch (IOException e) {
			System.out.println(e.toString());
			System.out.println("Unable to send command of closing");
		}

		System.out.println("Leaving the thread client");

        socket.close();
    }
    
    public void sendEcho(String msg) {
        buf = msg.getBytes();
        DatagramPacket packet 
          = new DatagramPacket(buf, buf.length, address, 30202);
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
	private LBR robot;
	private Controller controller;
	private Tool tool;

	private IMotionContainer motionContainer = null;
	
	EchoServer server_;
	EchoClient client_;
	
    boolean exit;


	
	
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
		
		try{
			server_ = new EchoServer();
			server_.start();
			
			client_ = new EchoClient();
			client_.start();
			
			exit=false;
			do {
				switch (getApplicationUI().displayModalDialog(
						ApplicationDialogType.QUESTION,"How many Force do I have to do?", 
						"END DO NOTHING")) {
						case 0:
							getLogger().info("App Terminated\n"+"***END***");
							exit = true;
							break;
				}
			}while(!exit);
			
			client_.stop_running();
			System.out.println("Closed the client ");

//			server_.stop_running();
//			System.out.println("Closed the server ");

			
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
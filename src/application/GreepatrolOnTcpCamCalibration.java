package application;

import static com.kuka.roboticsAPI.motionModel.BasicMotions.ptp;

/*import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
*/


import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;


import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.executionModel.IFiredConditionInfo;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.sensorModel.DataRecorder;
//import com.kuka.roboticsAPI.sensorModel.ForceSensorData;
import com.kuka.roboticsAPI.uiModel.ApplicationDialogType;
import com.kuka.roboticsAPI.executionModel.CommandInvalidException;

public class GreepatrolOnTcpCamCalibration extends RoboticsAPIApplication implements ITCPListener{
	
	@Inject
	private LBR lbr;
    boolean exit;
   
	//Frames
	Frame robot_pose;
    private Tool roll_scan;
	
	
	DataRecorder rec;
	
	private TCPClient tcp_client;
	AtomicBoolean data_received;
	AtomicBoolean server_connected;
	
	//Exchanged data info 
	int data_recv;
	
	@Override
	public void initialize() {
		
		// initialize your application here
		//roll_scan = createFromTemplate("RollScan");
		roll_scan = createFromTemplate("RealSense");
		roll_scan.attachTo(lbr.getFlange());
		
		
		//TCPClient object
		try {
			//Modificar IP 
			tcp_client = new TCPClient("127.0.0.1", 9000);
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
		//It may be desirable to move first the robot to an initial safe position. Not used right now
		//lbr.move(ptpHome());
		/*JointPosition joints = new JointPosition(0,0,0,0,0,0,0);
		joints.set(0, -90.0*(Math.PI/180));joints.set(1, 0.0*(Math.PI/180));
		joints.set(2, 0.0*(Math.PI/180));joints.set(3, -90*(Math.PI/180));
		joints.set(4, 0.0*(Math.PI/180));joints.set(5, 90.0*(Math.PI/180));
		joints.set(6, -90.0*(Math.PI/180));
		lbr.move(ptp(joints).setJointVelocityRel(0.25));
		*/
		
		exit=false;
		
		int kont = 1;
		
		Frame robot_pose; 
		String request_str;
		
		while(kont < 16)
		{
			String frame_name = "/Greenpatrol/CalibrationFront/P" + kont;
			IMotionContainer movementResult = null;
			try{
				
				movementResult = lbr.move(ptp(getFrame(frame_name)));

			} catch (CommandInvalidException e) {
				//motion is null
				//movementResult.cancel();
				System.out.println("Not possible to reach desired pose. Continue");
				kont++;
				continue;
				//e.printStackTrace();
			}
			System.out.println(frame_name + " successfully reached");
			
			
			robot_pose = lbr.getCurrentCartesianPosition(lbr.getFlange());
			
			request_str = robot_pose.getX() + ";" + robot_pose.getY() + ";" + robot_pose.getZ() + ";" +
				robot_pose.getGammaRad() + ";" + robot_pose.getBetaRad()+ ";" + robot_pose.getAlphaRad() + "@";
		
			System.out.println(frame_name + " -->  " + request_str);
			
			if(server_connected.get())
			{
				
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
				if(!server_connected.get())
					break;
				data_received.set(false);
				kont++;
			}
			else
			{
				System.out.println("Calibration process failed");
				break;
			}
			
		}
		
		if(server_connected.get())
		{
			tcp_client.sendData("END@");
		}
		
		try {
			tcp_client.dispose();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	@Override
	public void OnTCPMessageReceived(String datagram)
	{
		System.out.println("OnTCPMessageReceived: " + datagram);

		//data_recv = Integer.parseInt(datagram);
		
		data_received.set(true);
	}
	
	@Override public void OnTCPConnection()
	{
		System.out.println("Connection with server has been lost");
		
		server_connected.set(false);
	}

}


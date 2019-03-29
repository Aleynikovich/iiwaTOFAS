package application;

import static com.kuka.roboticsAPI.motionModel.BasicMotions.lin;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.ptp;

/*import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
*/

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;


import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.conditionModel.ICondition;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.deviceModel.LBRE1Redundancy;
import com.kuka.roboticsAPI.geometricModel.CartDOF;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;
import com.kuka.roboticsAPI.geometricModel.math.XyzAbcTransformation;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianImpedanceControlMode;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianSineImpedanceControlMode;
import com.kuka.roboticsAPI.sensorModel.DataRecorder;
//import com.kuka.roboticsAPI.sensorModel.ForceSensorData;
import com.kuka.roboticsAPI.uiModel.ApplicationDialogType;

public class OnTcpCamCalibration extends RoboticsAPIApplication implements ITCPListener{
	
	@Inject
	private LBR lbr;
    boolean exit;
    
	
	//Frames
	Frame robot_pose;
		
	
	DataRecorder rec;
	
	private TCPClient tcp_client;
	AtomicBoolean data_received;
	AtomicBoolean server_connected;
	
	//Exchanged data info 
	int data_recv;
	
	@Override
	public void initialize() {
		
		// initialize your application here
		
		//TCPClient object
		try {
			tcp_client = new TCPClient();
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
		//lbr.move(ptpHome());
				
		exit=false;
		
		int kont = 1;
		
		Frame robot_pose; 
		String request_str;
		
		while(kont < 16)
		{
			
			String frame_name = "/DemoCroinspect/calibration/P" + kont;
			lbr.move(ptp(getFrame(frame_name)));
			
			robot_pose = lbr.getCurrentCartesianPosition(lbr.getFlange());
			
			request_str = robot_pose.getX() + ";" + robot_pose.getY() + ";" + robot_pose.getZ() + ";" +
				robot_pose.getGammaRad() + ";" + robot_pose.getBetaRad()+ ";" + robot_pose.getAlphaRad() + "\n";
		
			if(server_connected.get())
				tcp_client.sendData(request_str);
			else
			{
				System.out.println("Calibration process failed");
				break;
			}
			
		}
		
		try {
			tcp_client.dispose();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void OnTCPMessageReceived(String datagram)
	{
		System.out.println("OnTCPMessageReceived: " + datagram);

		data_recv = Integer.parseInt(datagram);
		
		data_received.set(true);
	}
	
	@Override public void OnTCPConnection()
	{
		System.out.println("Connection with server has been lost");
		
		server_connected.set(false);
	}

}


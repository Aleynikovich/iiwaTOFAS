package application;


import javax.inject.Inject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;

/**
 * Implementation of a cyclic background task.
 * <p>
 * It provides the {@link RoboticsAPICyclicBackgroundTask#runCyclic} method 
 * which will be called cyclically with the specified period.<br>
 * Cycle period and initial delay can be set by calling 
 * {@link RoboticsAPICyclicBackgroundTask#initializeCyclic} method in the 
 * {@link RoboticsAPIBackgroundTask#initialize()} method of the inheriting 
 * class.<br>
 * The cyclic background task can be terminated via 
 * {@link RoboticsAPICyclicBackgroundTask#getCyclicFuture()#cancel()} method or 
 * stopping of the task.
 * @see UseRoboticsAPIContext
 * 
 */
public class DataSinchronizer extends RoboticsAPICyclicBackgroundTask  implements ITCPListener{
	
	
	@Inject
	private LBR lbr;
	
	@Inject
	private MediaFlangeIOGroup mediaFIO;
	
	private TCPClient tcp_client;
	AtomicBoolean data_received;
	AtomicBoolean server_connected;
	boolean connection_stablished;
	
	@Override
	public void initialize() {
		// initialize your task here
		initializeCyclic(0, 200, TimeUnit.MILLISECONDS,CycleBehavior.Strict);
		
		connection_stablished = false;
	}

	@Override
	public void runCyclic() {
		
		System.out.println("runCyclic ");
		if(SharedData.sinc_data)
			mediaFIO.setLEDBlue(true);

		if(SharedData.sinc_data)
		{	
			/*if(mediaFIO.getOutputX3Pin1())
			{	
				mediaFIO.setOutputX3Pin1(false);
				//mediaFIO.setLEDBlue(false);
			}
			else
			{
				//mediaFIO.setLEDBlue(true);
				mediaFIO.setOutputX3Pin1(true);	
			}*/
			//TCPClient object
			
			System.out.println("runCyclic");
			mediaFIO.setLEDBlue(true);
			
			if(!connection_stablished)
			{
				try {
					tcp_client = new TCPClient();
					tcp_client.addListener(this);
					tcp_client.enable();
					
					data_received = new AtomicBoolean(false);
					server_connected = new AtomicBoolean(true);

					connection_stablished = true;
					
					System.out.println("Connection stablished with the server");

		
				} catch (IOException e) {
					//TODO Bloque catch generado autom�ticamente
					System.err.println("Could not create TCPClient:" +e.getMessage());
				}
			}
			
			if(connection_stablished)
			{
				JointPosition joints = lbr.getCurrentJointPosition();
				
				String joint_str = joints.get(0) + ";" + joints.get(1) + ";" + joints.get(2) + ";" + 
						joints.get(3) + ";" + joints.get(4) + ";" + joints.get(5) + ";" + joints.get(6) + "\n";
				
				tcp_client.sendData(joint_str);
				
				System.out.println("Joint state sent");

			}
		
		}
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
package application;


import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import com.kuka.common.ThreadUtil;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;

import com.kuka.roboticsAPI.conditionModel.BooleanIOCondition;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.executionModel.IFiredConditionInfo;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.math.XyzAbcTransformation;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;

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
public class DemoProximity extends RoboticsAPIApplication implements ITCPListener{
	@Inject
	private LBR lbr;
    private Tool flange;
    
    @Inject
    private MediaFlangeIOGroup mediaFIO;
    
    private TCPServer tcp_server;
	AtomicBoolean data_received;
	
	@Override
	public void initialize() {
		// initialize your application here
		flange = createFromTemplate("Gimatic");
		flange.attachTo(lbr.getFlange());	
		
		//Application TCPServer object
		try {
			tcp_server = new TCPServer();
				
			tcp_server.addListener(this);
			tcp_server.enable();
					
		} catch (IOException e) {
			//TODO Bloque catch generado automáticamente
			System.err.println("Could not create TCPServer:" +e.getMessage());
		}
	}


	@Override
	public void run() {
		// your application execution starts here
	
						 
		flange.getFrame("flange").move(ptp(getFrame("/DemoProximity/SafePose")).setJointVelocityRel(0.25));
		
		mediaFIO.setLEDBlue(true);
		
		JointPosition joints = new JointPosition(0,0,0,0,0,0,0);

		joints.set(0, -90.0*(Math.PI/180));joints.set(1,22.0*(Math.PI/180));
		joints.set(2, 0.0*(Math.PI/180));joints.set(3, -103.0*(Math.PI/180));
		joints.set(4, 0.0*(Math.PI/180));joints.set(5, 53.0*(Math.PI/180));
		joints.set(6, 0.0*(Math.PI/180));
		
		lbr.move(ptp(joints).setJointVelocityRel(0.25));
				
		Frame  current_pose = lbr.getCurrentCartesianPosition(lbr.getFlange());
		
		System.out.println("Current pose --> x: " + current_pose.getX() + " y: " + current_pose.getY() + " z: " + current_pose.getZ() + 
			" A: " + current_pose.getAlphaRad()*(180/Math.PI)+ " B: " + current_pose.getBetaRad()*(180/Math.PI)+ " C: " + current_pose.getGammaRad()*(180/Math.PI));
		
		Frame left_pose = current_pose.copy();
		left_pose.transform(XyzAbcTransformation.ofRad(0.0,150.0,0.0,0.0,0.0,0.0));
			
		Frame right_pose = current_pose.copy();
		
		right_pose.transform(XyzAbcTransformation.ofRad(0.0,-150.0,0.0,0.0,0.0,0.0));
		
		System.out.println("Left pose --> x: " + left_pose.getX() + " y: " + left_pose.getY() + " z: " + left_pose.getZ() + 
				" A: " + left_pose.getAlphaRad()*(180/Math.PI)+ " B: " + left_pose.getBetaRad()*(180/Math.PI)+ " C: " + left_pose.getGammaRad()*(180/Math.PI));
		
		
		System.out.println("Right pose --> x: " + right_pose.getX() + " y: " + right_pose.getY() + " z: " + right_pose.getZ() + 
				" A: " + right_pose.getAlphaRad()*(180/Math.PI)+ " B: " + right_pose.getBetaRad()*(180/Math.PI)+ " C: " + right_pose.getGammaRad()*(180/Math.PI));
	
		flange.move(lin(left_pose).setCartVelocity(100));

		while(true)
		{
			System.out.println("Left movement");
			flange.move(lin(left_pose).setCartVelocity(100).setBlendingCart(10));
			
			System.out.println("Right movement");
			flange.move(lin(right_pose).setCartVelocity(100).setBlendingCart(10));
		}
	}
	
	
	private boolean waitUntilRobotAlmostStopped(double timeOut)
	{
		JointPosition currentJP;
		JointPosition lastJP = new JointPosition(0, 0, 0, 0, 0, 0, 0);
		long before = System.currentTimeMillis();
		long now;
		boolean robotAlmostStopped = false;
		
		do {
			currentJP = lbr.getCurrentJointPosition();
			ThreadUtil.milliSleep(10);
			robotAlmostStopped = currentJP.isNearlyEqual(lastJP, Math.toRadians(0.2));
			lastJP = currentJP;
			now = System.currentTimeMillis();
			if (timeOut > 0 && ((now - before) > timeOut)) {
				break;
			}
		} while (!robotAlmostStopped);
		if (robotAlmostStopped) {
			System.out.println("Utils.waitUntilRobotAlmostStopped(...): Robot almost stopped");
		}
		return robotAlmostStopped;
	}

	@Override
	public void OnTCPMessageReceived(String datagram) {
		// TODO Auto-generated method stub
		System.out.println("OnTCPMessageReceived: " + datagram);

		String splittedData[] = datagram.split(";");
		
		String stamp = splittedData[0];
		String msg_id = splittedData[1];
		String zone_str= splittedData[2];
		
		int zone = Integer.parseInt(zone_str);
		double override_vel = getApplicationControl().getApplicationOverride();
		
		System.out.println("Current Override vel: " + override_vel);

		if(zone == 0)
		{
			//No obstacle 
			System.out.println("No obstacle");
			override_vel = 1.0;
		}
		else if(zone == 1)
		{
			//Obstacle in Warning area
			System.out.println("Obstacle in WARNING area");
			override_vel = 0.5;
		}
		else if(zone == 2)
		{
			///Obstacle in Stop area
			System.out.println("Obstacle in STOP area");
			override_vel = 0.0;
			
		}
		
		System.out.println("Override vel: " + override_vel);

		getApplicationControl().setApplicationOverride(override_vel);
		
		if(override_vel ==0)
			waitUntilRobotAlmostStopped(-1);
		
	}

	@Override
	public void OnTCPConnection() {
		// TODO Auto-generated method stub
		
	}

}
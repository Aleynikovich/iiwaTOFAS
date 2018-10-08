package application;


import java.io.IOException;

import javax.inject.Inject;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;

import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.controllerModel.sunrise.SunriseController;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Tool;
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
public class RosUdpDriver extends RoboticsAPIApplication {
	@Inject
	private LBR robot;
	private Controller controller;
	private Tool tool;

	private IMotionContainer motionContainer = null;

	
	
//	public static void main(String[] args) {
//		RosUdpDriver app = new RosUdpDriver();
//		app.runApplication();
//	}

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
		if (motionContainer != null) motionContainer.cancel();
		System.out.println("Closing the sockets... ");
//		try { clientSocket.close(); } catch (Exception e) { }
//		try { serverSocket.close(); } catch (Exception e) { }
        super.dispose();

	}

	@Override
	public void run() {
		
		System.out.println("Initializing tcp server... ");
		try{
			
			while(true)
			{
				// ------------------ Connection acceptance -------------------
//				System.out.println("Waiting for incoming connection...");
//				
//				while(true){
//					
//				}
//				
//				// Prepare robot for a new connection
//				if (motionContainer != null) motionContainer.cancel(); // Stop the robot
//				Thread.sleep(1000); // Wait 1 second before retry
//				
			}
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
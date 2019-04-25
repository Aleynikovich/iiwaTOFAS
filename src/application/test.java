package application;


import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;

import com.kuka.roboticsAPI.conditionModel.BooleanIOCondition;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.executionModel.IFiredConditionInfo;
import com.kuka.roboticsAPI.geometricModel.Frame;
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
public class test extends RoboticsAPIApplication implements ISignalListener {
	@Inject
	private LBR lbr;
    private Tool roll_scan;
    
    @Inject
    private MediaFlangeIOGroup mediaFIO;
    
	private SignalsMonitor signal_monitor;
	ArrayList<IMotionContainer> motion_list = new ArrayList<IMotionContainer>();

	IMotionContainer motion_cmd;

	AtomicBoolean warning_signal;
	
	@Override
	public void initialize() {
		// initialize your application here
		roll_scan = createFromTemplate("RollScan");
		roll_scan.attachTo(lbr.getFlange());
		
		signal_monitor = new SignalsMonitor(mediaFIO);
		signal_monitor.addListener(this);
		signal_monitor.enable();
		
		warning_signal = new AtomicBoolean(false);

	}


	@Override
	public void run() {
		// your application execution starts here
	
						 
		//IMotionContainer motionCmd =
		//roll_scan.getFrame("Gripper").move(ptp(getFrame("/robot_base/SafePos")).breakWhen(switch1_active));
		motion_cmd = roll_scan.getFrame("Gripper").moveAsync(ptp(getFrame("/robot_base/SafePos")));
		motion_list.add(motion_cmd);
		motion_cmd = roll_scan.getFrame("Gripper").moveAsync(ptp(getFrame("/DemoCroinspect/Aprox3")));
		motion_list.add(motion_cmd);
		
		
		BooleanIOCondition switch1_active = new BooleanIOCondition(mediaFIO.getInput("InputX3Pin3"), true);

		motion_cmd = roll_scan.getFrame("Gripper").move(ptp(getFrame("/robot_base/SafePos")).breakWhen(switch1_active));
		
		IFiredConditionInfo firedInfo =  motion_cmd.getFiredBreakConditionInfo();
				 
		if(firedInfo != null){
			getLogger().info("pulsador 1 ");
			warning_signal.set(true);
		
		}
		 
		if(warning_signal.get())
		{
			Frame current_pos = lbr.getCurrentCartesianPosition(roll_scan.getFrame("roll_tcp"));
			
			System.out.println("Current pose point --> x: " + current_pos.getX() + " y: " + current_pos.getY() + " z: " + current_pos.getZ() + 
					" A: " + current_pos.getAlphaRad() + " B: " + current_pos.getBetaRad() + " C: " + current_pos.getGammaRad());
		
			Frame pose = current_pos.copy();
			pose.setGammaRad(current_pos.getGammaRad() + (30*Math.PI/180));  
			roll_scan.getFrame("roll_tcp").move(lin(pose).setCartVelocity(2));
			
			System.out.println("First point --> x: " + pose.getX() + " y: " + pose.getY() + " z: " + pose.getZ() + 
					" A: " + pose.getAlphaRad() + " B: " + pose.getBetaRad() + " C: " + pose.getGammaRad());
		
			//roll_scan.getFrame("roll_tcp").move(lin(pose).setCartVelocity(2));
			
			current_pos = lbr.getCurrentCartesianPosition(roll_scan.getFrame("roll_tcp"));
			
			System.out.println("Current pose point --> x: " + current_pos.getX() + " y: " + current_pos.getY() + " z: " + current_pos.getZ() + 
					" A: " + current_pos.getAlphaRad() + " B: " + current_pos.getBetaRad() + " C: " + current_pos.getGammaRad());
		
			pose.setGammaRad(current_pos.getGammaRad() - (30*Math.PI/180));  
			
			System.out.println("Second point --> x: " + pose.getX() + " y: " + pose.getY() + " z: " + pose.getZ() + 
					" A: " + pose.getAlphaRad() + " B: " + pose.getBetaRad() + " C: " + pose.getGammaRad());
		
			roll_scan.getFrame("roll_tcp").move(lin(pose).setCartVelocity(2));
			
			roll_scan.getFrame("roll_tcp").move(lin(current_pos).setCartVelocity(2));
			
			warning_signal.set(false);
		}
		 
		 try {
			signal_monitor.dispose();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			System.out.println("Test exception executing the dispose method");
		} 
	}
	
	@Override
	public void OnSignalReceived(Boolean data) {
		// TODO Auto-generated method stub
		
		System.out.println("Boton pulsado");
		warning_signal.set(true);
		
		for(IMotionContainer motion : motion_list)
		{
			System.out.println("Motion is finished: " + motion.isFinished());
			if(!motion.isFinished())
			{
				System.out.println("Motion state: " + motion.getState());
				motion.cancel();
				System.out.println("Motion cancelled");
			}
		}
	}

}
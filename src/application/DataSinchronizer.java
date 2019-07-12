package application;

import javax.inject.Inject;

import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.net.*;
import java.io.*;

/**
 * Implementation of a class that observes a TCP  server input (can be a scanner of safety areas) and
 * reduce the application overrides if the input falls to LOW value .
 * @since 27/06/2017 : Ane.F
 * @version 1.0
 * @author Ane.F
 */

public class DataSinchronizer extends RoboticsAPICyclicBackgroundTask {
	
	private MediaFlangeIOGroup mediaFIO;
	AtomicBoolean sinc;
	
	/**
	 * Constructor.
	 * <p>
	 * <code>public OverrideReduction(LBR iiwa, IApplicationControl appControl, ObserverManager observerManager, double reducedOverride)</code>
	 * <p>
	 * @param iiwa - KUKA lightweight robot.
	 * @param appControl - Interface for application controls. IApplicationControl instance.
	 * @param observerManager - ObserverManager instance.
	 * @param reducedOverride - The desired reduced override.
	 * @throws IOException 
	 */
	@Inject
	public DataSinchronizer(MediaFlangeIOGroup mediaFlangeIO)
	{		
		mediaFIO = mediaFlangeIO;
		sinc = new AtomicBoolean(false);
		initializeCyclic(0, 10, TimeUnit.MILLISECONDS,CycleBehavior.Strict);
	} 

	public void enable(){
		
		sinc.set(true);
		System.out.println("Data sinchronization started");

	}
	public void disable(){
		
		sinc.set(false);
		System.out.println("Data sinchronization finished");

	}
	 

	@Override
	protected void runCyclic() {
		
		if(sinc.get())
		{	
			if(mediaFIO.getLEDBlue())
			{	
				//mediaFIO.setOutputX3Pin1(false);
				mediaFIO.setLEDBlue(false);
			}
			else
			{
				mediaFIO.setLEDBlue(true);
				//mediaFIO.setOutputX3Pin1(true);	
			}
		}		
	}
}

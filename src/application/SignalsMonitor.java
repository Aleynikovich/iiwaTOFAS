package application;

import javax.inject.Inject;

import com.kuka.generated.ioAccess.MediaFlangeIOGroup;

import java.util.ArrayList;
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

public class SignalsMonitor implements Runnable {
	
	private Thread monitorThread;
	private ArrayList<ISignalListener> listeners;
	private Boolean input_state;
	private MediaFlangeIOGroup mediaFIO;
	AtomicBoolean monitoring;
	

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
	public SignalsMonitor(MediaFlangeIOGroup mediaFlangeIO)
	{		
		
		listeners = new ArrayList<ISignalListener>();
		monitorThread = null;
		input_state = false;
		mediaFIO = mediaFlangeIO;
		monitoring.set(false);
	}

	public void enable(){
		
		monitoring.set(true);

		monitorThread = new Thread(this);
		monitorThread.start();
		System.out.println("Monitor Thread started");

	}
	  
	public void dispose() throws InterruptedException{
		System.out.println("dispose"); //cont=false;
		
		monitoring.set(false);

		monitorThread.interrupt();
		monitorThread.join();
		
		System.out.println("Monitor Thread interrupted");

	}
	
	public void addListener(ISignalListener listener){
		listeners.add(listener);
	}
	
	@Override
	public void run() {
		
		
		try
		{
			while(true)
			{
				if(monitorThread.isInterrupted()) throw new InterruptedException();
	
				
				if(monitoring.get() && input_state != mediaFIO.getInputX3Pin3())
				{
					input_state = mediaFIO.getInputX3Pin3();
					
					if(input_state)
					{
						for(ISignalListener l : listeners)
							l.OnSignalReceived(input_state);
					}
				}
			}		

	
		}
		catch (InterruptedException ie) {
						
			System.out.println("Thread interrupt");
		}
		catch (Exception e) {
			System.out.println("Signals monitor Exception: " +e.getMessage());
		}	
		System.out.println("Finish Signal Monitor Run ");
	}
}

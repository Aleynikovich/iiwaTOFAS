package application;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.net.*;
import java.io.*;

import com.kuka.task.ITaskLogger;

/**
 * Implementation of a class that observes a TCP  server input (can be a scanner of safety areas) and
 * reduce the application overrides if the input falls to LOW value .
 * @since 27/06/2017 : Ane.F
 * @version 1.0
 * @author Ane.F
 */

public class TCPServer implements Runnable {
	
	private @Inject ITaskLogger appLogger;

	private Thread tcpServerThread;
	private ArrayList<ITCPListener> listeners;

	ServerSocket socket;
	Socket connectionSocket;
	String clientSentence;
	AtomicBoolean response;

	private boolean cont;
	
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
	public TCPServer() throws IOException
	{		
		socket = new ServerSocket(7000);	   
		connectionSocket = null;
		listeners = new ArrayList<ITCPListener>();
		tcpServerThread = null;
		response = new AtomicBoolean(false);
	}

	public void enable(){
		tcpServerThread = new Thread(this);
		tcpServerThread.start();
	}
	  
	public void dispose() throws InterruptedException{
		System.out.println("dispose"); //cont=false;

		tcpServerThread.interrupt();
		tcpServerThread.join();
	}
	
	public void addListener(ITCPListener listener){
		listeners.add(listener);
	}
	
	public void setResponseData(String response_data)
	{
		clientSentence = response_data;
		response.set(true);
	}
	
	
	@Override
	public void run() {
		
		try
		{
			//socket.setSoTimeout(15000);
			
			while(connectionSocket == null)
			{
				try
				{
				connectionSocket = socket.accept();
				}catch(Exception e)
				{
					System.out.println("Socket Accept: " + e.getMessage());
					if(tcpServerThread.isInterrupted()) throw new InterruptedException();
				}
			}

			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
			DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
			String datagram = "";
			
			while(true){

				if(tcpServerThread.isInterrupted()) throw new InterruptedException();

				if(inFromClient.ready())
				{
					datagram = inFromClient.readLine();

					for(ITCPListener l : listeners)
						l.OnTCPMessageReceived(datagram);
				}
				if(response.get())
				{
					outToClient.writeBytes(clientSentence);
					response.set(false);
				}
			}	
		}
		catch (IOException e) {
			System.out.println("IOException: "+e.getMessage());
		}
		catch (InterruptedException ie) {
						
			System.out.println("Thread interrupt");
			
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("OverrideReduction.run(): InterruptedException --> Thread interrupted");
		}
		catch (Exception e) {
			System.out.println("Exception: "+e.getMessage());
			try {
				socket.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		System.out.println("Finish TCP Server Run ");
	}
}

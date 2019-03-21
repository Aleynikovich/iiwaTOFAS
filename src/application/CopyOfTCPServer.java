package application;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.net.*;
import java.io.*;

import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.task.ITaskLogger;

/**
 * Implementation of a class that observes a TCP  server input (can be a scanner of safety areas) and
 * reduce the application overrides if the input falls to LOW value .
 * @since 27/06/2017 : Ane.F
 * @version 1.0
 * @author Ane.F
 */

public class CopyOfTCPServer  extends RoboticsAPIApplication
{
	
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
	public CopyOfTCPServer() throws IOException
	{		
		socket = new ServerSocket(7002);	
		connectionSocket = null;
		response = new AtomicBoolean(false);
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
			socket.setSoTimeout(15000);
			
			while(connectionSocket == null)
			{
				try
				{
				connectionSocket = socket.accept();
				}catch(Exception e)
				{
					System.out.println("Socket Accept: " + e.getMessage());
				}
			}

			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
			DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
			String datagram = "";
			
			/*while(true){

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
			}	*/
		}
		catch (IOException e) {
			System.out.println("IOException: "+e.getMessage());
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

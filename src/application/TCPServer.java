package application;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.net.*;
import java.nio.CharBuffer;
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
		socket = new ServerSocket(30001);	
		connectionSocket = null;
		listeners = new ArrayList<ITCPListener>();
		tcpServerThread = null;
		response = new AtomicBoolean(false);
	}

	public void enable(){
		tcpServerThread = new Thread(this);
		tcpServerThread.start();
		System.out.println("Thread started");

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
		DataOutputStream outToClient;
		try {
			outToClient = new DataOutputStream(connectionSocket.getOutputStream());
			outToClient.writeBytes(clientSentence);
			response.set(false);
			System.out.println("Response sended");
					
			response.set(true);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	@Override
	public void run() {
		
		boolean socket_close = true;
		
		try
		{
			while(socket_close)
			{
				socket.setSoTimeout(15000);
				
				while(connectionSocket == null)
				{
					try
					{
					connectionSocket = socket.accept();
					System.out.println("Socket communication established");
					socket_close = false;
	
					}catch(Exception e)
					{
						System.out.println("Socket Accept: " + e.getMessage());
						if(tcpServerThread.isInterrupted()) throw new InterruptedException();
					}
				}
	
				BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
				//ObjectInputStream inFromClient = new ObjectInputStream(connectionSocket.getInputStream());;
	
				//DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
			    String datagram = "";
				
				while(true)
				{
					if(tcpServerThread.isInterrupted()) throw new InterruptedException();
	
					
					//if(inFromClient.ready())
					//{
						//System.out.println("Request received");
						if((datagram = inFromClient.readLine())!=null)
						{
							System.out.println("Datagram: " + datagram.toString());
							
							//datagram = inFromClient.readUTF();
							for(ITCPListener l : listeners)
								l.OnTCPMessageReceived(datagram.toString());
						}
						else
						{
							System.out.println("Close");
							break;
						}
						
						
					//}
				}
				System.out.println("Socket closed");
				socket_close = true;
				connectionSocket = null;
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

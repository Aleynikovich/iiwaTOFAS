package application;

import javax.inject.Inject;
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

public class ConnectionHandler implements Runnable {
	
	private Socket connectionSocket;
	ArrayList<ITCPListener> connection_listeners;
	private Thread connection_thread;

	BufferedReader inFromClient;
	DataOutputStream outToClient;
	
	String clientSentence;
	AtomicBoolean response;

	AtomicBoolean request;
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
	public ConnectionHandler(Socket socket, ArrayList<ITCPListener> listeners) throws IOException
	{	
		connectionSocket = socket;
		connection_listeners = listeners;
		connection_thread = null;
		response = new AtomicBoolean(false);
		request = new AtomicBoolean(false);
	}

	public void enable(){
		connection_thread = new Thread(this);
		connection_thread.start();
		System.out.println("Thread started");

	}
	  
	public void dispose() throws InterruptedException, IOException{
		System.out.println("dispose"); //cont=false;
		
		if(!connectionSocket.isClosed())
		{	
			
			inFromClient.close();
			outToClient.close();
			connectionSocket.close();
			System.out.println("TCP connection closed");

		}
		
		connection_thread.interrupt();
		connection_thread.join();
		
		System.out.println("Thread interrupted");

	}
	
	public void setResponseData(String response_data)
	{
		clientSentence = response_data + "\r\n";
		try {
			
			outToClient.writeBytes(clientSentence);
			//response.set(false);
			
			System.out.println("Response sended: " + clientSentence);
			request.set(false);		
			//response.set(true);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	@Override
	public void run() {
				
		try
		{
			
			inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
	
			outToClient = new DataOutputStream(connectionSocket.getOutputStream());
			String datagram = "";
				
			while(true)
			{
				if(connection_thread.isInterrupted()) throw new InterruptedException();
		
				if(!request.get())
				{
					if((datagram = inFromClient.readLine())!=null)
					{
						System.out.println("Datagram: " + datagram.toString());
							
						for(ITCPListener l : connection_listeners)
							l.OnTCPMessageReceived(datagram.toString());
							request.set(true);
					}
					else
					{
						System.out.println("Close");
						break;
					}
						
				}	
			}
			System.out.println("Socket closed");
			connectionSocket = null;
		}
		catch (IOException e) {
			System.out.println("IOException: "+e.getMessage());
		}
		catch (InterruptedException ie) {
						
			System.out.println("Thread interrupt");
			
			try {
				
				if(!connectionSocket.isClosed())
				{
					
					inFromClient.close();
					outToClient.close();
					connectionSocket.close();
				}
			} catch (IOException e) {
				System.out.println("IO exception closing the socket after thread interruption");

			}
		}
		catch (Exception e) {
			System.out.println("Exception: "+e.getMessage());
			try {
				connectionSocket.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		System.out.println("Finish TCP Server Run ");
	}
}

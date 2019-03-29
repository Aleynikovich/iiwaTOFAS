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

public class TCPClient implements Runnable {
	
	private Socket clientSocket;
	private BufferedReader inFromServer;
	DataOutputStream outToServer;

	private Thread tcpClientThread;
	
	private ArrayList<ITCPListener> listeners;

	String request_str;
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
	public TCPClient() throws IOException
	{		
		clientSocket = new Socket("10.66.171.149", 23);
		inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		outToServer = new DataOutputStream(clientSocket.getOutputStream());
		listeners = new ArrayList<ITCPListener>();
		tcpClientThread = new Thread(this);
		request = new AtomicBoolean(false);
	}

	public void enable(){
		tcpClientThread.start();
		System.out.println("Thread started");

	}
	  
	public void dispose() throws IOException, InterruptedException {
		System.out.println("dispose"); //cont=false;
		
		
		tcpClientThread.interrupt();
		tcpClientThread.join();
		//IOException exception;
		
		//exception.
	}
	
	public void addListener(ITCPListener listener){
		listeners.add(listener);
	}
	
	public void sendData(String datagram)
	{
		try {
			outToServer.writeBytes(datagram);
			System.out.println("Request sended");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	@Override
	public void run() {
		
		String datagram = "";
		try {
			while(true){
				
				if(Thread.currentThread().isInterrupted()) throw new InterruptedException();
				
				clientSocket.setSoTimeout(5000);
				if((datagram = inFromServer.readLine()) != null)
				{	
					for(ITCPListener l : listeners)
						l.OnTCPMessageReceived(datagram);	
				}
				else
				{				
					System.out.println("Server disconnected");
					for(ITCPListener l : listeners)
						l.OnTCPConnection();
				
					break;
				}
			}
		}catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			clientSocket.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Finish TCP Client Run ");
	}
}

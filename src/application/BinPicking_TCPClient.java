package application;

import javax.inject.Inject;
import javax.swing.Timer;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.net.*;
import java.io.*;

/**
 * Implementation of a class that observes a TCP  server input (can be a scanner of safety areas) and
 * reduce the application overrides if the input falls to LOW value .
 * @since 27/06/2017 : JPOZO
 * @version 1.0
 * @author JPOZO
 */

public class BinPicking_TCPClient implements Runnable {
	
	private Socket clientSocket;
	private BufferedReader inFromServer;
	DataOutputStream outToServer;

	private Thread tcpClientThread;
	
	private ArrayList<BinPicking_ITCPListener> listeners;

	String request_str;
	AtomicBoolean request;
	AtomicBoolean start_listening;
	
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
	public BinPicking_TCPClient() throws IOException
	{		
		clientSocket = new Socket("10.66.171.250", 30001);
		System.out.println("Communication with the server started");
		inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		outToServer = new DataOutputStream(clientSocket.getOutputStream());
		listeners = new ArrayList<BinPicking_ITCPListener>();
		tcpClientThread = new Thread(this);
		request = new AtomicBoolean(false);
		start_listening = new AtomicBoolean(false);
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
	
	public void addListener(BinPicking_ITCPListener listener){
		listeners.add(listener);
	}
	
	public void sendData(String datagram)
	{
		try {
			byte[] byteArrray = datagram.getBytes();
			start_listening.set(true);
//			datagram="255.015;-476.083;395.091;3.129;0.0;3.135\n";
			//outToServer.writeBytes(datagram);
			
			outToServer.write(byteArrray);
			System.out.println(datagram);
			System.out.println("Request sended");
			
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("senData --> IO exception");
			for(BinPicking_ITCPListener l : listeners)
				l.OnTCPConnection();
		}
	}
	
		public void sendData_int(int datagram)
		{
			try {
				start_listening.set(true);
//				datagram="255.015;-476.083;395.091;3.129;0.0;3.135\n";
				//outToServer.writeBytes(datagram);
				
				outToServer.writeInt(datagram);
				System.out.println(datagram);
				System.out.println("Request sended");
				
				
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("senData --> IO exception");
				for(BinPicking_ITCPListener l : listeners)
					l.OnTCPConnection();
			}
		
		}
	
	@Override
	public void run() {
		
		String datagram = "";
		Timer start_time; ;
		try {
			while(true){
				
				if(Thread.currentThread().isInterrupted()) throw new InterruptedException();
				
				if(start_listening.get())
				{
					
					start_listening.set(false);
					if((datagram = inFromServer.readLine()) != null)
					{	
						System.out.println("Data received from server");
						for(BinPicking_ITCPListener l : listeners)
							l.OnTCPMessageReceived(datagram);	
							request_str=datagram;
							System.out.println("datagram: "+datagram+ "request_str: "+request_str);	
					}
					else
					{				
						System.out.println("Server disconnected");
						for(BinPicking_ITCPListener l : listeners)
							l.OnTCPConnection();
					
						break;
					}
				}
			}
		}catch(java.net.SocketTimeoutException e)
		{
			System.out.println("Socket time-out exception");
			for(BinPicking_ITCPListener l : listeners)
				l.OnTCPConnection();
			
		}catch (InterruptedException e) {
			
			System.out.println("Interruption exception");

		} catch (IOException e) {
			
			System.out.println("IO exception");
			//e.printStackTrace();
		}
		
		try {
			clientSocket.close();
			
		} catch (IOException e) {
			System.out.println("Socket close method IO exception");
		}
		System.out.println("Finish TCP Client Run ");
	}
}

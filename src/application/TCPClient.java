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
	AtomicBoolean start_listening;
	public AtomicBoolean is_connected;
	private String serverIP;
	private int serverPort;

	
	@Inject
	public TCPClient(String ip, int port)throws IOException {
	    this.serverIP = ip;
	    this.serverPort = port;
	    this.is_connected.set(false);
	    listeners = new ArrayList<ITCPListener>();
	    tcpClientThread = new Thread(this);
	    request = new AtomicBoolean(false);
	    start_listening = new AtomicBoolean(false);
	}
	
	private boolean connect() {
	    try {
	        System.out.println("Attempting to connect to " + serverIP + ":" + serverPort + "...");
	        clientSocket = new Socket(serverIP, serverPort);
	        inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	        outToServer = new DataOutputStream(clientSocket.getOutputStream());
	        System.out.println("Connected to server.");
	        this.is_connected.set(true);
	        return true;
	    } catch (IOException e) {
	        System.err.println("Connection failed: " + e.getMessage());
	        this.is_connected.set(false);
	        return false;
	    }
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
			
			start_listening.set(true);
//			datagram="255.015;-476.083;395.091;3.129;0.0;3.135\n";
			outToServer.writeBytes(datagram);
			System.out.println(datagram);
			System.out.println("Request sended");
			
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("senData --> IO exception");
			for(ITCPListener l : listeners)
				l.OnTCPConnection();
		}
		
	}
	
	@Override
	public void run() {
	    String datagram;

	    while (!Thread.currentThread().isInterrupted()) {
	        try {
	            // Si no hay conexión, intenta conectar cada 5 segundos
	            while (!connect()) {
	                Thread.sleep(1000);
	            }
        		
	            while (!Thread.currentThread().isInterrupted()) {
	                if (start_listening.get()) {
	                    start_listening.set(false);
	                    datagram = inFromServer.readLine();

	                    if (datagram != null) {
	                        System.out.println("Data received from server");
	                        for (ITCPListener l : listeners)
	                            l.OnTCPMessageReceived(datagram);
	                        request_str = datagram;
	                        System.out.println("after: " + request_str);
	                    } else {
	                        System.out.println("Server disconnected cleanly");
	                        break; // salir del bucle interno para reconectar
	                    }
	                }

	                Thread.sleep(10); // evitar CPU al 100%
	            }
	        } catch (IOException e) {
	            System.out.println("IOException during communication: " + e.getMessage());
	            this.is_connected.set(false);
	        } catch (InterruptedException e) {
	            System.out.println("Thread interrupted. Exiting...");
	            this.is_connected.set(false);
	            break;
	        } finally {
	            for (ITCPListener l : listeners)
	                l.OnTCPConnection();

	            try {
	                if (clientSocket != null && !clientSocket.isClosed())
	                    clientSocket.close();
	            } catch (IOException e) {
	                System.out.println("Error closing socket: " + e.getMessage());
	            }
	        }

	        System.out.println("Disconnected. Reconnecting...");
	    }

	    System.out.println("Finish TCP Client Run");
	}

}

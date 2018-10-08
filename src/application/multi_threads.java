package application;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import javax.inject.Inject;

import com.kuka.connectivity.directServo.examples.DirectServoSampleSimpleCartesian;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import com.kuka.roboticsAPI.deviceModel.LBR;

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


import java.io.*;
import java.net.*;


class EchoServer extends Thread {
	 
    public DatagramSocket socket;
    public boolean running;
    private byte[] buf = new byte[256];
 
    public EchoServer() {
        try {
			socket = new DatagramSocket(30200);
		} catch (SocketException e) {
            System.out.println(e.toString());

		}
    }
 
    public void run() {
        running = true;
 
        while (running) {
			
			DatagramPacket packet 
			  = new DatagramPacket(buf, buf.length);
			
			try {
				socket.receive(packet);	
				 
				InetAddress address = packet.getAddress();
				int port = packet.getPort();
				packet = new DatagramPacket(buf, buf.length, address, port);
				String received 
				  = new String(packet.getData(), 0, packet.getLength());
					
				if (received.equals("end")) {
				    running = false;
				    continue;
				}

				try {
					socket.send(packet);
				} catch (IOException e) {
				    System.out.println("here");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
			    //System.out.println(" fail2 ");

			}
		}
        	
        socket.close();
    }
    public void close() {
        socket.close();
    }

}


class EchoClient {
    private DatagramSocket socket;
    private InetAddress address;
 
    private byte[] buf;
 
    public EchoClient() throws SocketException {
        socket = new DatagramSocket();
        try {
			address = InetAddress.getByName("localhost");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
 
    public String sendEcho(String msg) {
        buf = msg.getBytes();
        DatagramPacket packet 
          = new DatagramPacket(buf, buf.length, address, 4445);
        try {
			socket.send(packet);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        packet = new DatagramPacket(buf, buf.length);
        try {
			socket.receive(packet);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        String received = new String(
          packet.getData(), 0, packet.getLength());
        return received;
    }
 
    public void close() {
        socket.close();
    }
}


class RunnableDemo implements Runnable {
	   private Thread t;
	   private String threadName;
	   
	   RunnableDemo( String name) {
	      threadName = name;
	      System.out.println("Creating " +  threadName );
	   }
	   
	   public void run() {
	      System.out.println("Running " +  threadName );
	      try {
	         for(int i = 4; i > 0; i--) {
	            System.out.println("Thread: " + threadName + ", " + i);
	            // Let the thread sleep for a while.
	            Thread.sleep(50);
	         }
	      } catch (InterruptedException e) {
	         System.out.println("Thread " +  threadName + " interrupted.");
	      }
	      System.out.println("Thread " +  threadName + " exiting.");
	   }
	   
	   public void start () {
	      System.out.println("Starting " +  threadName );
	      if (t == null) {
	         t = new Thread (this, threadName);
	         t.start ();
	      }
	   }
	}




public class multi_threads extends RoboticsAPIApplication {
	@Inject
	private LBR lBR_iiwa_14_R820_1;
	EchoClient client;
	EchoServer server_;

	@Override
	public void initialize() {
		// initialize your application here
		server_ = new EchoServer();
		server_.start();
		
	}

    @Override
    public void dispose()
    {
        server_.close();
        //server_.running = false;
        super.dispose();
    }
	@Override
	public void run() {
		// your application execution starts here
//	      RunnableDemo R1 = new RunnableDemo( "Thread-1");
//	      R1.start();
//	      
//	      RunnableDemo R2 = new RunnableDemo( "Thread-2");
//	      R2.start();
//	      
//	      while(true)
//		      {
//		    	  String echo = client.sendEcho("hello server");
//		          //assertEquals("hello server", echo);
//		          echo = client.sendEcho("server is working");
//		          //assertFalse(echo.equals("hello server"));
//		      }
	      
	      }
    /**
     * Main routine, which starts the application
     */
    public static void main(String[] args)
    {
    	multi_threads app = new multi_threads();

        app.runApplication();
    }
	}
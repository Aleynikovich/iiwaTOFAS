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
import java.io.*;
import java.net.*;


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




public class multi_threads extends RoboticsAPIApplication {
	private volatile DatagramSocket serverSocket;
 
	@Override
	public void run() {
 
		try {
			serverSocket = new DatagramSocket(30200);
 
			byte[] receiveData = new byte[1024];
			while (!Thread.currentThread().isInterrupted()) {
				DatagramPacket receivePacket = new DatagramPacket(receiveData,
						receiveData.length);
				serverSocket.receive(receivePacket);
				// /YOUR CODE
			}
 
		} catch (SocketException e) {
			// DO EXCEPTION HANDLING
		} catch (IOException e) {
			// DO EXCEPTION HANDLING
		} finally {
			if (null != serverSocket)
				serverSocket.close();
		}
	}
 
    @Override
    public void dispose()
    {
		if (null != serverSocket) {
			serverSocket.close();
		}
		Thread.currentThread().interrupt();
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
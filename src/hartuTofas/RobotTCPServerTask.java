package hartuTofas;

import com.kuka.generated.ioAccess.Ethercat_x44IOGroup;
import com.kuka.generated.ioAccess.IOFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.deviceModel.LBR;

import javax.inject.Inject;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RoboticsAPICyclicBackgroundTask implementation for a TCP/IP server
 * that listens for incoming connections and manages communication with clients.
 */
public class RobotTCPServerTask extends RoboticsAPICyclicBackgroundTask
{

    @Inject
    private LBR iiwa; // Corrected: Renamed 'robot' to 'iiwa' for consistency with usage
    @Inject
    private IOFlangeIOGroup gimatic;
    @Inject
    private Ethercat_x44IOGroup iOs;

    private ServerSocket serverSocket = null;
    private static final int SERVER_PORT = 30001;
    private static final int ACCEPT_TIMEOUT = 1000;

    private static final Logger LOGGER = Logger.getLogger(RobotTCPServerTask.class.getName());

    private volatile boolean isRunning = true;

    private List<ClientHandler> activeClientHandlers = Collections.synchronizedList(new ArrayList<ClientHandler>());

    private Thread acceptThread;

    @Override
    public void initialize()
    {
        super.initialize();
        initializeCyclic(0, 100, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);
        LOGGER.info("RobotTCPServerTask initialized.");
        
        acceptThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    serverSocket = new ServerSocket(SERVER_PORT);
                    serverSocket.setSoTimeout(ACCEPT_TIMEOUT);
                    LOGGER.info("Server listening on port " + SERVER_PORT);

                    while (isRunning)
                    {
                        try
                        {
                            Socket clientSocket = serverSocket.accept();
                            LOGGER.info("Client connected: " + clientSocket.getInetAddress().getHostAddress());

                            // Corrected: Passing 'iiwa' instead of 'robot' to MessageHandler
                            MessageHandler messageHandler = new MessageHandler(iiwa, gimatic, iOs, (RoboticsAPIApplication) getApplicationContext());

                            ClientHandler clientHandler = new ClientHandler(clientSocket, messageHandler, LOGGER);
                            activeClientHandlers.add(clientHandler);
                            clientHandler.start();

                        }
                        catch (SocketTimeoutException e)
                        {
                            // Timeout occurred, check if server is still running. Normal behavior.
                        }
                        catch (IOException e)
                        {
                            if (isRunning)
                            {
                                LOGGER.log(Level.SEVERE, "Error accepting client connection: " + e.getMessage(), e);
                            }
                        }
                    }
                }
                catch (IOException e)
                {
                    LOGGER.log(Level.SEVERE, "Could not listen on port " + SERVER_PORT + ": " + e.getMessage(), e);
                    isRunning = false;
                }
                finally
                {
                    if (serverSocket != null && !serverSocket.isClosed())
                    {
                        try
                        {
                            serverSocket.close();
                            LOGGER.info("Server socket closed.");
                        }
                        catch (IOException e)
                        {
                            LOGGER.log(Level.SEVERE, "Error closing server socket: " + e.getMessage(), e);
                        }
                    }
                }
            }
        }, "AcceptThread");
        acceptThread.start();
    }

    @Override
    public void runCyclic()
    {
        // Clean up client handlers that have finished their work
        Iterator<ClientHandler> iterator = activeClientHandlers.iterator();
        while (iterator.hasNext())
        {
            ClientHandler handler = iterator.next();
            if (!handler.isAlive())
            {
                iterator.remove();
            }
        }
    }

    @Override
    public void dispose()
    {
        super.dispose();
        LOGGER.info("RobotTCPServerTask disposing...");

        isRunning = false;
        if (acceptThread != null)
        {
            acceptThread.interrupt();
            try
            {
                acceptThread.join(5000); // Wait up to 5 seconds for the thread to terminate
                if (acceptThread.isAlive())
                {
                    LOGGER.warning("Accept thread did not terminate gracefully after join.");
                }
            }
            catch (InterruptedException e)
            {
                LOGGER.log(Level.WARNING, "Interrupted while waiting for accept thread to terminate.", e);
                Thread.currentThread().interrupt();
            }
        }

        if (serverSocket != null && !serverSocket.isClosed())
        {
            try
            {
                serverSocket.close();
                LOGGER.info("Server socket closed during dispose.");
            }
            catch (IOException e)
            {
                LOGGER.log(Level.SEVERE, "Error closing server socket during dispose: " + e.getMessage(), e);
            }
        }

        for (ClientHandler handler : activeClientHandlers)
        {
            handler.closeClientResources();
            try
            {
                handler.join(1000);
            }
            catch (InterruptedException e)
            {
                LOGGER.log(Level.WARNING, "Interrupted while waiting for client handler to terminate.", e);
                Thread.currentThread().interrupt();
            }
        }
        activeClientHandlers.clear();

        LOGGER.info("RobotTCPServerTask disposed.");
    }
}

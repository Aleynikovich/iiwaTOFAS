package hartu.communication.server;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.controllerModel.Controller;

import hartu.communication.client.LoggerClient;

public class HartuServerBackgroundTask extends RoboticsAPICyclicBackgroundTask {

    private HartuServer hartuServer;
    private Thread serverListenThread;

    private LoggerClient protocolLoggerClient;
    private BlockingQueue<String> rawMessageQueue;

    private final int HARTU_SERVER_PORT = 30001;
    private final int PROTOCOL_LOG_PORT = 30003;

    private final String LOG_SERVER_ADDRESS = "127.0.0.1";

    @Override
    public void initialize() {
       

        try {
        	 Thread.sleep(2000);

            protocolLoggerClient = new LoggerClient(LOG_SERVER_ADDRESS, PROTOCOL_LOG_PORT);

            protocolLoggerClient.sendMessage("HartuServerBackgroundTask: Protocol Logger Client connected successfully to LogServer on port " + PROTOCOL_LOG_PORT + ".");

            protocolLoggerClient.sendMessage("HartuServerBackgroundTask: Execution Logger Client connected successfully to LogServer on port 30004.");

            rawMessageQueue = new LinkedBlockingQueue<>();

            hartuServer = new HartuServer(HARTU_SERVER_PORT, protocolLoggerClient, rawMessageQueue);
            protocolLoggerClient.sendMessage("HartuServerBackgroundTask: HartuServer instance created, linked to protocol logger and raw message queue.");


        } catch (Exception e) {
            getLogger().error("HartuServerBackgroundTask: FATAL ERROR during HartuServerBackgroundTask initialization: " + e.getMessage(), e);
            throw new IllegalStateException("Failed to initialize HartuServerBackgroundTask due to an unexpected error.", e);
        }
        
        protocolLoggerClient.sendMessage("sora1");
        initializeCyclic(0, 500, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);
        
    }

    @Override
    public void runCyclic() {
    	
    	protocolLoggerClient.sendMessage("sora221");
        if (serverListenThread == null || !serverListenThread.isAlive()) {
        	
            protocolLoggerClient.sendMessage("HartuServerBackgroundTask: HartuServer thread not running or has died. Attempting restart...");

            if (serverListenThread != null) {
                serverListenThread.interrupt();
                try {
                    serverListenThread.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    protocolLoggerClient.sendMessage("HartuServerBackgroundTask: Interrupted while waiting for old HartuServer thread to stop: " + e.getMessage());
                }
            }

            try {
                protocolLoggerClient.sendMessage("HartuServerBackgroundTask: Waiting 5 seconds before attempting server restart...");
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                protocolLoggerClient.sendMessage("HartuServerBackgroundTask: Interrupted during pre-restart delay. Aborting restart attempt.");
                return;
            }

            serverListenThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        hartuServer.start();
                    } catch (Exception e) {
                        protocolLoggerClient.sendMessage("HartuServerBackgroundTask: ERROR: HartuServer.start() failed: " + e.getMessage());
                    }
                }
            }, "HartuServerListenThread");

            serverListenThread.start();
            protocolLoggerClient.sendMessage("HartuServerBackgroundTask: HartuServer listening thread (re)started on port " + HARTU_SERVER_PORT);
        }
    }

    @Override
    public void dispose() {
        if (protocolLoggerClient != null) {
            protocolLoggerClient.sendMessage("HartuServerBackgroundTask: dispose() called. Stopping HartuServer...");
        } else {
            getLogger().warn("HartuServerBackgroundTask: protocolLoggerClient is null during dispose. Cannot log shutdown messages fully.");
        }

        if (hartuServer != null) {
            hartuServer.stop();

            try {
                if (serverListenThread != null) {
                    serverListenThread.join(5000);
                    if (serverListenThread.isAlive()) {
                        if (protocolLoggerClient != null) protocolLoggerClient.sendMessage("HartuServerBackgroundTask: HartuServer thread did not terminate within timeout. Forcing interrupt.");
                        serverListenThread.interrupt();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (protocolLoggerClient != null) protocolLoggerClient.sendMessage("HartuServerBackgroundTask: Interrupted while waiting for HartuServer thread to stop.");
            }
        }
        if (protocolLoggerClient != null) {
            protocolLoggerClient.disconnect();
        }
        if (protocolLoggerClient != null) {
            protocolLoggerClient.sendMessage("HartuServerBackgroundTask: HartuServer stopped.");
        }
        super.dispose();
    }

    public BlockingQueue<String> getRawMessageQueue() {
        return rawMessageQueue;
    }
}
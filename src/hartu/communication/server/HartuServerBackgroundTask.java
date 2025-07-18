// File: hartu/communication/server/HartuServerBackgroundTask.java
package hartu.communication.server;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;
import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.controllerModel.Controller;

public class HartuServerBackgroundTask extends RoboticsAPICyclicBackgroundTask {

    @Inject
    private Controller controller;

    @Inject
    private LogServerBackgroundTask logServerBackgroundTask;

    private HartuServer hartuServer;
    private Thread serverListenThread; // This thread runs hartuServer.start()

    private final int SERVER_PORT = 30001;

    @Override
    public void initialize() {
        // Initialize cyclic behavior. runCyclic() will be called every 500ms.
        initializeCyclic(0, 500, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);

        // Get the LogServer instance once during initialization.
        // It's crucial that LogServerBackgroundTask starts before this one.
        LogServer logServer = logServerBackgroundTask.getLogServer();
        if (logServer == null) {
            System.err.println("HartuServerBackgroundTask: LogServer instance is null. Cannot proceed.");
            throw new IllegalStateException("LogServer not initialized by LogServerBackgroundTask.");
        }

        // Create the HartuServer instance once.
        hartuServer = new HartuServer(SERVER_PORT, controller, logServer);
        System.out.println("HartuServerBackgroundTask: Server instance created, linked to LogServer.");
    }

    @Override
    public void runCyclic() {
        // This is the core logic for resilience.
        // Periodically check if the server's listening thread is alive and if the server is running.
        if (serverListenThread == null || !serverListenThread.isAlive() || !hartuServer.isRunning()) {
            System.out.println("HartuServerBackgroundTask: Server thread not running or server stopped. Attempting restart...");

            // Ensure any previous thread is properly shut down before starting a new one
            if (serverListenThread != null && serverListenThread.isAlive()) {
                serverListenThread.interrupt(); // Try to interrupt if it's stuck
                try {
                    serverListenThread.join(1000); // Give it a moment to die
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Create a new thread to run the blocking server start method
            serverListenThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        hartuServer.start(); // This call blocks
                    } catch (Exception e) {
                        System.err.println("HartuServerBackgroundTask: Error during HartuServer.start(): " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }, "HartuServerListenThread");

            serverListenThread.start();
            System.out.println("HartuServerBackgroundTask: Server listening thread (re)started.");
        }
    }

    @Override
    public void dispose() {
        System.out.println("HartuServerBackgroundTask: dispose() called. Stopping HartuServer...");
        if (hartuServer != null && hartuServer.isRunning()) {
            hartuServer.stop(); // Request the server to stop its internal loop

            try {
                // Wait for the server's listening thread to terminate gracefully
                if (serverListenThread != null) {
                    serverListenThread.join(5000); // Wait for up to 5 seconds
                    if (serverListenThread.isAlive()) {
                        System.err.println("HartuServerBackgroundTask: Server thread did not terminate within timeout. Forcing interrupt.");
                        serverListenThread.interrupt(); // Force interrupt if it's still alive
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("HartuServerBackgroundTask: Interrupted while waiting for server thread to stop.");
            }
        }
        System.out.println("HartuServerBackgroundTask: HartuServer stopped.");
        super.dispose();
    }
}
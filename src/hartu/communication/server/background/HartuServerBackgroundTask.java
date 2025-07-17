package hartu.communication.server.background;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;
import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.controllerModel.Controller;
import hartu.communication.server.HartuServer;

public class HartuServerBackgroundTask extends RoboticsAPICyclicBackgroundTask {

    @Inject
    private Controller controller;

    private HartuServer hartuServer;
    private Thread serverListenThread;

    private final int SERVER_PORT = 30001;

    @Override
    public void initialize() {
        initializeCyclic(0, 500, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);

        hartuServer = new HartuServer(SERVER_PORT, controller);
        System.out.println("HartuServerBackgroundTask: Server instance created.");

        // CORRECTED: Using anonymous inner class for Java 7 compatibility
        serverListenThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    hartuServer.start();
                } catch (Exception e) {
                    System.err.println("HartuServerBackgroundTask: Error starting HartuServer: " + e.getMessage());
                }
            }
        }, "HartuServerListenThread");

        if (!serverListenThread.isAlive()) {
            serverListenThread.start();
            System.out.println("HartuServerBackgroundTask: Server listening thread started.");
        } else {
            System.out.println("HartuServerBackgroundTask: Server listening thread already alive.");
        }
    }

    @Override
    public void runCyclic() {
        if (!hartuServer.isRunning() && serverListenThread.isAlive()) {
            System.err.println("HartuServerBackgroundTask: Server not reporting as running but thread is alive. Investigate!");
        } else if (!serverListenThread.isAlive() && hartuServer.isRunning()) {
            System.err.println("HartuServerBackgroundTask: Server thread died but server reports as running. Investigate!");
        } else if (!serverListenThread.isAlive() && !hartuServer.isRunning()) {
            System.out.println("HartuServerBackgroundTask: Server appears to have stopped. Not restarting cyclically.");
        }
    }

    @Override
    public void dispose() {
        System.out.println("HartuServerBackgroundTask: dispose() called. Stopping HartuServer...");
        if (hartuServer != null && hartuServer.isRunning()) {
            hartuServer.stop();

            try {
                serverListenThread.join(5000);
                if (serverListenThread.isAlive()) {
                    System.err.println("HartuServerBackgroundTask: Server thread did not terminate within timeout.");
                    serverListenThread.interrupt();
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
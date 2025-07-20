package hartu.robot.communication.server;

import hartu.protocols.constants.ProtocolConstants.ListenerType;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerClass implements IClientHandlerCallback
{
    private static final String CONFIG_FILE_PATH = "server_config.properties";

    // Made private for better encapsulation, as getClientNameForIp method is now public
    private final Map<String, String> clientIpToNameMap;
    private final AtomicInteger clientNameCounter;

    private final EnumMap<ListenerType, ServerPortListener> activeListeners;
    private final EnumMap<ListenerType, Thread> listenerThreads;
    private final EnumMap<ListenerType, ClientHandler> activeClientHandlers;

    private volatile boolean isLogClientConnected = false;
    private String robotIp; // Added to store robot's own IP

    public ServerClass() throws IOException
    {
        this.clientIpToNameMap = new ConcurrentHashMap<>();
        this.clientNameCounter = new AtomicInteger(0);
        this.activeListeners = new EnumMap<>(ListenerType.class);
        this.listenerThreads = new EnumMap<>(ListenerType.class);
        this.activeClientHandlers = new EnumMap<>(ListenerType.class);

        loadConfigurationAndInitializeListeners();
        Logger.getInstance().log("SERVER", "Server initialized with dynamic configuration. Robot IP: " + robotIp);
    }

    private void loadConfigurationAndInitializeListeners() throws IOException {
        Properties properties = new Properties();
        try {
            properties.load(getClass().getClassLoader().getResourceAsStream(CONFIG_FILE_PATH));
            Logger.getInstance().log("SERVER", "Loaded server configuration from classpath resource: " + CONFIG_FILE_PATH);
        } catch (IOException e) {
            try (FileInputStream fis = new FileInputStream(CONFIG_FILE_PATH)) {
                properties.load(fis);
                Logger.getInstance().log("SERVER", "Loaded server configuration from file system: " + CONFIG_FILE_PATH);
            } catch (IOException ex) {
                Logger.getInstance().log("SERVER", "Error: Could not load server configuration from " + CONFIG_FILE_PATH + ". Server will not start. " + ex.getMessage());
                throw new IOException("Failed to load server configuration: " + ex.getMessage(), ex);
            }
        }

        // Read robot's own IP
        robotIp = properties.getProperty("robot.ip", "0.0.0.0"); // Default to 0.0.0.0 if not specified
        //wtflolXD
        // Dynamically create listeners based on properties
        for (ListenerType type : ListenerType.values()) {
            String portKey = type.getName() + ".port";
            String portString = properties.getProperty(portKey);
            if (portString != null) {
                try {
                    int port = Integer.parseInt(portString);
                    ServerSocket serverSocket = new ServerSocket(port);
                    ServerPortListener listener = new ServerPortListener(serverSocket, type, this, this);
                    activeListeners.put(type, listener);
                    Logger.getInstance().log("SERVER", "Configured " + type.getName() + " on port " + port);
                } catch (NumberFormatException e) {
                    Logger.getInstance().log("SERVER", "Error: Invalid port number for " + type.getName() + ": " + portString + ". " + e.getMessage());
                } catch (IOException e) {
                    Logger.getInstance().log("SERVER", "Error: Could not open socket for " + type.getName() + " on port " + portString + ": " + e.getMessage());
                    throw e;
                }
            } else {
                Logger.getInstance().log("SERVER", "Warning: Port for " + type.getName() + " not found in configuration. This listener will not be started.");
            }
        }

        if (activeListeners.isEmpty()) {
            Logger.getInstance().log("SERVER", "Error: No listeners configured. Server will not start.");
            throw new IOException("No listeners configured in " + CONFIG_FILE_PATH);
        }
    }

    public void start()
    {
        for (Map.Entry<ListenerType, ServerPortListener> entry : activeListeners.entrySet()) {
            ListenerType type = entry.getKey();
            ServerPortListener listener = entry.getValue();
            Thread listenerThread = new Thread(listener);
            listenerThread.setDaemon(true);
            listenerThread.start();
            listenerThreads.put(type, listenerThread);
        }
        Logger.getInstance().log("SERVER", "All configured server listeners started.");
    }

    public void stop() throws IOException
    {
        Logger.getInstance().log("SERVER", "Stopping all server listeners and client handlers...");

        for (ServerPortListener listener : activeListeners.values()) {
            if (listener != null) {
                listener.stopListening();
            }
        }

        for (Map.Entry<ListenerType, Thread> entry : listenerThreads.entrySet()) {
            ListenerType type = entry.getKey();
            Thread thread = entry.getValue();
            if (thread != null && thread.isAlive()) {
                try {
                    thread.join(2000);
                    if (thread.isAlive()) {
                        Logger.getInstance().log("SERVER", "Warning: " + type.getName() + " thread did not terminate within timeout.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Logger.getInstance().log("SERVER", "Interrupted while waiting for " + type.getName() + " thread to stop: " + e.getMessage());
                }
            }
        }

        if (activeClientHandlers.containsKey(ListenerType.LOG_LISTENER)) {
            Logger.getInstance().setLogClientHandler(null);
        }

        for (ClientHandler handler : activeClientHandlers.values()) {
            if (handler != null) {
                handler.close();
            }
        }
        activeClientHandlers.clear();

        this.isLogClientConnected = false;
        Logger.getInstance().log("SERVER", "Server stopped.");
    }

    public String getClientNameForIp(String clientIp) {
        String clientName = clientIpToNameMap.get(clientIp);
        if (clientName == null) {
            clientName = "Client-" + clientNameCounter.incrementAndGet();
            clientIpToNameMap.put(clientIp, clientName);
        }
        return clientName;
    }

    @Override
    public void onClientConnected(ClientHandler handler, ListenerType listenerType)
    {
        String clientIp = handler.getClientSession().getRemoteAddress();
        String clientName = handler.getClientSession().getClientName();

        activeClientHandlers.put(listenerType, handler);

        if (listenerType == ListenerType.LOG_LISTENER)
        {
            Logger.getInstance().setLogClientHandler(handler);
            this.isLogClientConnected = true;
        }
        Logger.getInstance().log("SERVER", "Client " + clientName + " (" + clientIp + ") connected to " + listenerType.getName());
    }

    public boolean isLogClientConnected()
    {
        return isLogClientConnected;
    }
}

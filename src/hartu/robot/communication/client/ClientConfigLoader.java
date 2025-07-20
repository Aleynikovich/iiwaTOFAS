package hartu.robot.communication.client;

import hartu.robot.communication.server.Logger; // Assuming Logger is accessible
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Utility class to load client-side network configuration from properties files.
 */
public class ClientConfigLoader {

    private static final String CLIENT_CONFIG_FILE_PATH = "client_config.properties";
    private static final String NETWORK_CONFIG_FILE_PATH = "network_config.properties";

    private ClientConfigLoader() {
        // Private constructor to prevent instantiation
    }

    /**
     * Loads the server IP and port for a specific client purpose from configuration files.
     *
     * @param clientPurposeKey The key in client_config.properties that defines the port (e.g., "jointLog.port").
     * @param defaultPort The default port to use if the key is not found or invalid.
     * @return A String array containing [serverIp, serverPortAsString]. Returns defaults if config fails.
     */
    public static String[] loadConnectionConfig(String clientPurposeKey, String defaultPort) {
        Properties clientProps = new Properties();
        Properties networkProps = new Properties();

        // Load client_config.properties
        try {
            clientProps.load(ClientConfigLoader.class.getClassLoader().getResourceAsStream(CLIENT_CONFIG_FILE_PATH));
            Logger.getInstance().log("CONFIG_LOADER", "Loaded client configuration from classpath: " + CLIENT_CONFIG_FILE_PATH);
        } catch (IOException e) {
            try (FileInputStream fis = new FileInputStream(CLIENT_CONFIG_FILE_PATH)) {
                clientProps.load(fis);
                Logger.getInstance().log("CONFIG_LOADER", "Loaded client configuration from file system: " + CLIENT_CONFIG_FILE_PATH);
            } catch (IOException ex) {
                Logger.getInstance().log("CONFIG_LOADER", "Error: Could not load client configuration from " + CLIENT_CONFIG_FILE_PATH + ". " + ex.getMessage());
            }
        }

        // Load network_config.properties
        try {
            networkProps.load(ClientConfigLoader.class.getClassLoader().getResourceAsStream(NETWORK_CONFIG_FILE_PATH));
            Logger.getInstance().log("CONFIG_LOADER", "Loaded network configuration from classpath: " + NETWORK_CONFIG_FILE_PATH);
        } catch (IOException e) {
            try (FileInputStream fis = new FileInputStream(NETWORK_CONFIG_FILE_PATH)) {
                networkProps.load(fis);
                Logger.getInstance().log("CONFIG_LOADER", "Loaded network configuration from file system: " + NETWORK_CONFIG_FILE_PATH);
            } catch (IOException ex) {
                Logger.getInstance().log("CONFIG_LOADER", "Error: Could not load network configuration from " + NETWORK_CONFIG_FILE_PATH + ". " + ex.getMessage());
            }
        }

        String serverIp = networkProps.getProperty("rosPC.ip", "10.66.171.69"); // Default IP
        String serverPortStr = clientProps.getProperty(clientPurposeKey, defaultPort); // Default port

        Logger.getInstance().log("CONFIG_LOADER", "Resolved connection config: IP=" + serverIp + ", Port=" + serverPortStr + " for key " + clientPurposeKey);
        return new String[]{serverIp, serverPortStr};
    }
}

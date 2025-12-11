package hartu.robot.executor;

import com.kuka.generated.ioAccess.Ethercat_x44IOGroup;
import com.kuka.generated.ioAccess.IOFlangeIOGroup;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.deviceModel.Device;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.motionModel.ErrorHandlingAction;
import com.kuka.roboticsAPI.motionModel.IErrorHandler;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import hartu.robot.commands.ParsedCommand;
import hartu.robot.communication.server.CommandQueue;
import hartu.robot.communication.server.CommandResultHolder;
import hartu.robot.communication.server.LogLevel;
import hartu.robot.communication.server.Logger;
import hartu.robot.executor.io.IoExecutor;
import hartu.robot.executor.io.ToolController;
import hartu.robot.executor.motion.MotionExecutor;
import hartu.robot.executor.program.ProgramExecutor;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.kuka.roboticsAPI.motionModel.BasicMotions.ptpHome;

public class CommandExecutor extends RoboticsAPIApplication
{

    @Inject
    private LBR iiwa;
    @Inject
    private IOFlangeIOGroup gimaticIO;
    @Inject
    private Ethercat_x44IOGroup toolControlIO;
    @Inject
    private MediaFlangeIOGroup mediaFlangeIO;

    // Tool registry: maps tool names to Tool objects
    // Tools are loaded from Object Templates during initialization
    private java.util.Map<String, Tool> toolRegistry;

    // Tool ID mapping: maps tool IDs to tool names
    // Tool 0 = Flange, Tool 1 = GimaticCamera, Tool 2 = Vacuum1, etc.
    private ToolMapping toolMapping;

    // Currently attached tool (null if using flange)
    private Tool currentlyAttachedTool = null;
    private String currentlyAttachedToolName = null;

    private RobotConsoleClient consoleClient;
    private Thread consoleClientThread;

    // Executors for different command types
    private MotionExecutor motionExecutor;
    private IoExecutor ioExecutor;
    private ProgramExecutor programExecutor;

    private IErrorHandler moveAsyncErrorHandler;

    @Override
    public void initialize()
    {
        // Start robot console client to receive logs from LoggingServerManager
        // and broadcast them via println (only foreground tasks can println to robot console)
        startRobotConsoleClient();
        Logger.getInstance().setMinimumLogLevel(LogLevel.MEDIUM);
        Logger.getInstance().debug("ROBOT_EXEC", "Initializing CommandExecutor.");

        // Initialize tool ID to name mapping
        toolMapping = new ToolMapping();

        // Initialize tool registry and load all available tools
        // Tools must be defined in Sunrise.Workbench Object Templates
        toolRegistry = new java.util.HashMap<String, Tool>();
        loadAndRegisterTools();

        // Register error handler for asynchronous motion failures
        // This prevents the application from terminating when moveAsync fails
        registerMoveAsyncErrorHandler();

        // Initialize kitting box (reset on each CommandExecutor restart)
        hartu.robot.executor.kitting.KittingBox kittingBox = new hartu.robot.executor.kitting.KittingBox(hartu.robot.executor.kitting.BoxType.STANDARD);
        Logger.getInstance().debug("ROBOT_EXEC", "Initialized kitting box: " + kittingBox.toString());

        // Initialize executors
        ToolController toolController = new ToolController(gimaticIO, toolControlIO, mediaFlangeIO);
        hartu.robot.io.IOList ioList = new hartu.robot.io.IOList(toolControlIO, gimaticIO, mediaFlangeIO);
        hartu.robot.executor.program.ProgramSubroutines programSubroutines = new hartu.robot.executor.program.ProgramSubroutines(iiwa, toolController, this, kittingBox);
        this.motionExecutor = new MotionExecutor(iiwa, this, moveAsyncErrorHandler);
        this.ioExecutor = new IoExecutor(toolController, ioList);
        this.programExecutor = new ProgramExecutor(toolController, programSubroutines);

        // Flush any stale commands from previous runs
        int flushedCount = CommandQueue.flushQueue();
        if (flushedCount > 0)
        {
            Logger.getInstance().debug("ROBOT_EXEC", "Cleared " + flushedCount + " stale command(s) from queue on initialization.");
        }

        // Move robot to home position after flushing queue
        try
        {
            Logger.getInstance().debug("ROBOT_EXEC", "Moving robot to home position...");
            iiwa.move(ptpHome().setJointVelocityRel(0.5));
            Logger.getInstance().debug("ROBOT_EXEC", "Robot successfully moved to home position.");
        } catch (Exception e)
        {
            Logger.getInstance().error("ROBOT_EXEC", "Failed to move robot to home position: " + e.getMessage());
            Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", e);
        }

        Logger.getInstance().debug("ROBOT_EXEC", "Ready to take commands from queue.");
    }

    /**
     * Loads and registers all tools defined in the tool mapping.
     * Tools are loaded from Object Templates but NOT attached yet.
     * Tools will be attached dynamically when commands specify a tool ID.
     */
    private void loadAndRegisterTools()
    {
        int loadedCount = 0;

        // Get all tool mappings and try to load each tool
        for (Map.Entry<Integer, String> entry : toolMapping.getAllMappings().entrySet())
        {
            int toolId = entry.getKey();
            String toolName = entry.getValue();

            try
            {
                Tool tool = getApplicationData().createFromTemplate(toolName);
                if (tool != null)
                {
                    // Store in registry but don't attach yet
                    toolRegistry.put(toolName, tool);
                    Logger.getInstance().debug("ROBOT_EXEC", "Tool ID " + toolId + " ('" + toolName + "') loaded successfully.");
                    loadedCount++;
                }
            } catch (Exception e)
            {
                Logger.getInstance().warn("ROBOT_EXEC", "Tool ID " + toolId + " ('" + toolName + "') defined in mapping but not found in Object Templates.");
            }
        }

        if (loadedCount == 0)
        {
            Logger.getInstance().debug("ROBOT_EXEC", "No tools loaded. Commands with tool ID 0 will use robot flange.");
        } else
        {
            Logger.getInstance().debug("ROBOT_EXEC", "Loaded " + loadedCount + " tool(s). Tools will be attached dynamically based on command tool ID.");
        }
    }

    /**
     * Gets the tool for a given tool ID by looking up the mapping and returning the Tool object.
     * Dynamically attaches the tool if it's different from the currently attached tool.
     *
     * @param toolId The tool ID from the command (0 = GimaticCamera, 1+ = specific tools)
     * @return The Tool object, or null if tool not found
     */
    public Tool getAndAttachToolForId(int toolId)
    {
        // Get tool name from mapping
        String toolName = toolMapping.getToolName(toolId);
        if (toolName == null)
        {
            Logger.getInstance().warn("ROBOT_EXEC", "Tool ID " + toolId + " not found in mapping. Cannot attach tool.");
            return null;
        }

        // Check if this tool is already attached
        if (currentlyAttachedToolName != null && currentlyAttachedToolName.equals(toolName))
        {
            Logger.getInstance().debug("ROBOT_EXEC", "Tool '" + toolName + "' (ID " + toolId + ") already attached. No change needed.");
            return currentlyAttachedTool;
        }

        // Get tool from registry
        Tool tool = toolRegistry.get(toolName);
        if (tool == null)
        {
            Logger.getInstance().warn("ROBOT_EXEC", "Tool '" + toolName + "' (ID " + toolId + ") not found in registry. Cannot attach tool.");
            return null;
        }

        // Detach current tool if any
        if (currentlyAttachedTool != null)
        {
            try
            {
                currentlyAttachedTool.detach();
                Logger.getInstance().debug("ROBOT_EXEC", "Detached previous tool '" + currentlyAttachedToolName + "'.");
            } catch (Exception e)
            {
                Logger.getInstance().error("ROBOT_EXEC", "Failed to detach previous tool '" + currentlyAttachedToolName + "': " + e.getMessage());
            }
        }

        // Attach new tool
        try
        {
            tool.attachTo(iiwa.getFlange());
            currentlyAttachedTool = tool;
            currentlyAttachedToolName = toolName;
            Logger.getInstance().debug("ROBOT_EXEC", "Attached tool '" + toolName + "' (ID " + toolId + ") to robot flange.");
            return tool;
        } catch (Exception e)
        {
            Logger.getInstance().error("ROBOT_EXEC", "Failed to attach tool '" + toolName + "' (ID " + toolId + "): " + e.getMessage());
            currentlyAttachedTool = null;
            currentlyAttachedToolName = null;
            return null;
        }
    }

    /**
     * Registers the error handler for asynchronous motion commands.
     * According to KUKA Sunrise.OS manual section 15.29.3, this is the correct way
     * to handle failed moveAsync commands without terminating the application.
     * <p>
     * The error handler:
     * - Logs the failed motion command details
     * - Logs any canceled motion commands
     * - Flushes the command queue to prevent cascading failures
     * - Returns ErrorHandlingAction.Ignore to continue the application
     */
    private void registerMoveAsyncErrorHandler()
    {
        moveAsyncErrorHandler = new IErrorHandler()
        {
            @Override
            public ErrorHandlingAction handleError(Device device,
                                                   IMotionContainer failedContainer,
                                                   List<IMotionContainer> canceledContainers)
            {
                // Log the failed motion command
                Logger.getInstance().error("ROBOT_EXEC", "Asynchronous motion failed: " + failedContainer.getCommand().toString());

                // Signal the motion executor about the failure
                if (motionExecutor != null)
                {
                    ParsedCommand currentCommand = motionExecutor.getCurrentCommand();
                    if (currentCommand != null)
                    {
                        Logger.getInstance().error("ROBOT_EXEC", "Failed command ID: " + currentCommand.getId());
                        Logger.getInstance().error("ROBOT_EXEC", "This usually means unreachable pose, singularity, joint limits exceeded, or timeout.");
                        motionExecutor.signalCommandFailure();
                    }
                }

                // Log canceled motion commands
                if (canceledContainers != null && !canceledContainers.isEmpty())
                {
                    Logger.getInstance().warn("ROBOT_EXEC", "The following " + canceledContainers.size() + " motion(s) were canceled:");
                    for (int i = 0; i < canceledContainers.size(); i++)
                    {
                        Logger.getInstance().warn("ROBOT_EXEC", "  [" + (i + 1) + "] " + canceledContainers.get(i).getCommand().toString());
                    }
                }

                // Flush the command queue to prevent cascading failures
                Logger.getInstance().warn("ROBOT_EXEC", "Flushing command queue due to motion failure...");
                int flushedCount = CommandQueue.flushQueue();
                if (flushedCount > 0)
                {
                    Logger.getInstance().debug("ROBOT_EXEC", "Flushed " + flushedCount + " pending command(s) from queue after motion failure.");
                }

                // Return Ignore to prevent application termination and allow continued operation
                // This is the recommended approach per KUKA Sunrise.OS manual section 15.29.3
                return ErrorHandlingAction.Ignore;
            }
        };

        // Register the error handler with the application controller
        getApplicationControl().registerMoveAsyncErrorHandler(moveAsyncErrorHandler);
        Logger.getInstance().debug("ROBOT_EXEC", "Registered moveAsync error handler for graceful failure handling.");
    }

    /**
     * Starts the robot console client that connects to LoggingServerManager
     * and broadcasts all logs to robot console via println.
     * Only foreground tasks (like CommandExecutor) can use println to write to robot console.
     */
    private void startRobotConsoleClient()
    {
        consoleClient = new RobotConsoleClient();
        consoleClientThread = new Thread(consoleClient);
        consoleClientThread.setDaemon(true);
        consoleClientThread.start();
    }

    @Override
    public void run()
    {
        // Main execution loop - runs indefinitely until application is stopped
        // All exceptions are caught and logged without terminating the loop
        Logger.getInstance().debug("ROBOT_EXEC", "Starting main execution loop.");
        while (true)
        {
            // Check for intentional shutdown signal at the start of each iteration
            if (Thread.currentThread().isInterrupted())
            {
                Logger.getInstance().debug("ROBOT_EXEC", "Shutdown signal received, exiting execution loop gracefully.");
                break;
            }

            try
            {
                CommandResultHolder resultHolder = CommandQueue.pollCommand(100, TimeUnit.MILLISECONDS);
                mediaFlangeIO.setLedRed(false);
                mediaFlangeIO.setLEDBlue(true);
                mediaFlangeIO.setLedGreen(false);

                if (resultHolder != null)
                {
                    mediaFlangeIO.setLedRed(false);
                    mediaFlangeIO.setLEDBlue(false);
                    mediaFlangeIO.setLedGreen(true);
                    ParsedCommand command = resultHolder.getCommand();
                    Logger.getInstance().debug("ROBOT_EXEC", "Received command ID " + command.getId() + " from queue for execution.");
                    boolean executionSuccess = false;

                    try
                    {
                        switch (command.getCommandCategory())
                        {
                            case MOVEMENT:
                                executionSuccess = motionExecutor.executeMotion(command);
                                break;
                            case IO:
                                executionSuccess = ioExecutor.executeIoCommand(command);
                                // For input reading commands, set custom response data with the input state
                                if (executionSuccess && command.isInputReadCommand())
                                {
                                    int stateValue = ioExecutor.getLastInputState() ? 1 : 0;
                                    resultHolder.setCustomResponseData(String.valueOf(stateValue));
                                }
                                break;
                            case PROGRAM_CALL:
                                executionSuccess = programExecutor.executeProgramCall(command);
                                break;
                            case UNKNOWN:
                            default:
                                Logger.getInstance().warn("ROBOT_EXEC", "Unknown or unsupported primary command category for ID " + command.getId() + ": " + command.getCommandCategory().name());
                                executionSuccess = false;
                                break;
                        }
                    } catch (Throwable t)
                    {
                        Logger.getInstance().error("ROBOT_EXEC", "Exception during command execution for ID " + command.getId() + ": " + t.getClass().getName() + " - " + t.getMessage());
                        Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", t instanceof Exception ? (Exception) t : new Exception("Throwable wrapper", t));
                        executionSuccess = false;
                        // Continue processing - don't let one command failure stop the system
                    } finally
                    {
                        // If command failed, flush the command queue to prevent cascading failures
                        // Note: Motion command failures are already handled by the IErrorHandler which flushes the queue
                        // This catches IO, program call, and other non-motion command failures
                        if (!executionSuccess)
                        {
                            Logger.getInstance().warn("ROBOT_EXEC", "Command ID " + command.getId() + " failed. Flushing command queue to prevent cascading failures.");
                            int flushedCount = CommandQueue.flushQueue();
                            if (flushedCount > 0)
                            {
                                Logger.getInstance().debug("ROBOT_EXEC", "Flushed " + flushedCount + " pending command(s) from queue after failure.");
                            }
                            mediaFlangeIO.setLedRed(true);
                            mediaFlangeIO.setLEDBlue(false);
                            mediaFlangeIO.setLedGreen(false);
                            Thread.sleep(5000);
                            mediaFlangeIO.setLedRed(false);
                            mediaFlangeIO.setLEDBlue(false);
                            mediaFlangeIO.setLedGreen(true);

                        }

                        resultHolder.setSuccess(executionSuccess);
                        resultHolder.getLatch().countDown();
                        Logger.getInstance().debug("ROBOT_EXEC", "Signaled completion for command ID " + command.getId() + ". Success: " + executionSuccess);
                    }
                }
            } catch (Throwable t)
            {
                // Catch ANY exception or error in the main loop itself
                // This is the ultimate safety net - prevents the entire run loop from exiting
                Logger.getInstance().error("ROBOT_EXEC", "Unexpected error in main execution loop: " + t.getClass().getName() + " - " + t.getMessage());
                Logger.getInstance().error("ROBOT_EXEC", "Stack trace:", t instanceof Exception ? (Exception) t : new Exception("Throwable wrapper", t));
                Logger.getInstance().debug("ROBOT_EXEC", "Recovering from error and continuing execution...");
                // Brief pause to prevent tight error loops
                try
                {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException ie)
                {
                    Thread.currentThread().interrupt();
                    Logger.getInstance().debug("ROBOT_EXEC", "Sleep interrupted during error recovery - shutdown signal detected, exiting...");
                    break;
                }
            }
        }
        Logger.getInstance().debug("ROBOT_EXEC", "CommandExecutor run method is exiting.");
    }


    @Override
    public void dispose()
    {
        Logger.getInstance().debug("ROBOT_EXEC", "Disposing CommandExecutor.");

        // Stop robot console client
        if (consoleClient != null)
        {
            consoleClient.stop();
        }
        if (consoleClientThread != null && consoleClientThread.isAlive())
        {
            try
            {
                // Interrupt the thread to break out of blocking operations
                consoleClientThread.interrupt();
                // Wait briefly for graceful shutdown
                consoleClientThread.join(2000);
            } catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }

        super.dispose();
    }

    /**
     * Inner class that connects to LoggingServerManager as a client
     * and broadcasts received log messages to robot console via println.
     * <p>
     * This is necessary because only foreground tasks (like CommandExecutor)
     * can use println to write to the robot's SmartPad console.
     * Background tasks cannot directly write to console.
     */
    private static class RobotConsoleClient implements Runnable
    {
        private static final String LOG_SERVER_HOST = "localhost";
        private static final int LOG_SERVER_PORT = 30002;
        private static final int RECONNECT_DELAY_MS = 5000;

        private volatile boolean running = true;
        private Socket socket;
        private BufferedReader reader;

        @Override
        public void run()
        {
            System.out.println("[RobotConsoleClient] Starting console client to receive logs from LoggingServerManager...");

            while (running)
            {
                try
                {
                    // Connect to logging server
                    socket = new Socket(LOG_SERVER_HOST, LOG_SERVER_PORT);
                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    System.out.println("[RobotConsoleClient] Connected to LoggingServerManager on port " + LOG_SERVER_PORT);

                    // Read and broadcast log messages
                    String line;
                    while (running && (line = reader.readLine()) != null)
                    {
                        // Broadcast to robot console using println
                        // Only foreground tasks can do this
                        System.out.println(line);
                    }

                } catch (IOException e)
                {
                    if (running)
                    {
                        System.out.println("[RobotConsoleClient] Connection error: " + e.getMessage());
                        System.out.println("[RobotConsoleClient] Will retry in " + RECONNECT_DELAY_MS + "ms...");

                        // Wait before reconnecting
                        try
                        {
                            Thread.sleep(RECONNECT_DELAY_MS);
                        } catch (InterruptedException ie)
                        {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } finally
                {
                    // Clean up connection
                    closeConnection();
                }
            }

            System.out.println("[RobotConsoleClient] Console client stopped.");
        }

        public void stop()
        {
            running = false;
            // Close socket first to interrupt blocking readLine()
            // This is critical: closing socket before reader unblocks the read operation
            // The reader will be closed by closeConnection() in the finally block after readLine() throws IOException
            try
            {
                if (socket != null && !socket.isClosed())
                {
                    socket.close();
                }
            } catch (IOException e)
            {
                // Ignore - we're shutting down anyway
            }
        }

        private void closeConnection()
        {
            // Close socket first to interrupt blocking read
            try
            {
                if (socket != null && !socket.isClosed())
                {
                    socket.close();
                }
            } catch (IOException e)
            {
                // Ignore
            }

            // Then close reader (this should be non-blocking now)
            try
            {
                if (reader != null)
                {
                    reader.close();
                }
            } catch (IOException e)
            {
                // Ignore
            }
        }
    }
}
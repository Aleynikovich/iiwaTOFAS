package hartu.robot.hmi;

import com.kuka.roboticsAPI.uiModel.userKeys.IUserKey;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKeyBar;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKeyListener;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyAlignment;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyEvent;
import hartu.robot.communication.server.Logger;
import hartu.robot.executor.CommandExecutor;

/**
 * Handles HMI programmable button events on the KUKA SmartPad.
 * Implements the four side buttons with the following functionality:
 * - Button 1: Toggle open/close tools
 * - Button 2: Lock/unlock Gimatic (requires 2-second press to unlock)
 * - Button 3: Send current robot position to ROS2 clients
 * - Button 4: Reserved for future use
 */
public class HmiButtonHandler implements IUserKeyListener
{
    private final CommandExecutor commandExecutor;
    private final RobotPositionPublisher positionPublisher;
    
    // Track tool state for button 1
    private boolean toolOpen = false;
    
    // Track Gimatic lock state for button 2
    private boolean gimaticLocked = true;
    
    // Track button 2 press time for 2-second hold requirement
    // Volatile ensures visibility across HMI event handler threads
    private volatile long button2PressStartTime = 0;
    private static final long GIMATIC_UNLOCK_HOLD_TIME_MS = 2000;
    
    // User key references
    private IUserKey button1;
    private IUserKey button2;
    private IUserKey button3;
    private IUserKey button4;

    /**
     * Creates a new HMI button handler.
     *
     * @param commandExecutor The command executor for accessing robot state and tools
     * @param positionPublisher The position publisher for sending position data
     */
    public HmiButtonHandler(CommandExecutor commandExecutor, RobotPositionPublisher positionPublisher)
    {
        this.commandExecutor = commandExecutor;
        this.positionPublisher = positionPublisher;
    }

    /**
     * Registers the user keys with the HMI user key bar.
     * This must be called during application initialization.
     *
     * @param keyBar The user key bar from getApplicationUI().createUserKeyBar()
     */
    public void registerUserKeys(IUserKeyBar keyBar)
    {
        try
        {
            // Create user keys aligned to the right side of the SmartPad
            button1 = keyBar.addUserKey(0, this, false);
            button1.setText(UserKeyAlignment.TopMiddle, "Tool Open/Close");
            button1.setEnabled(true);
            Logger.getInstance().debug("HMI", "Registered Button 1: Tool Open/Close");

            button2 = keyBar.addUserKey(1, this, false);
            button2.setText(UserKeyAlignment.TopMiddle, "Gimatic Lock/Unlock");
            button2.setEnabled(true);
            Logger.getInstance().debug("HMI", "Registered Button 2: Gimatic Lock/Unlock (2s hold)");

            button3 = keyBar.addUserKey(2, this, false);
            button3.setText(UserKeyAlignment.TopMiddle, "Send Position");
            button3.setEnabled(true);
            Logger.getInstance().debug("HMI", "Registered Button 3: Send Position");

            button4 = keyBar.addUserKey(3, this, false);
            button4.setText(UserKeyAlignment.TopMiddle, "Cancel Task");
            button4.setEnabled(true);
            Logger.getInstance().debug("HMI", "Registered Button 4: Cancel Task");

            keyBar.publish();
            Logger.getInstance().low("HMI", "HMI programmable buttons initialized successfully");
        } catch (Exception e)
        {
            Logger.getInstance().error("HMI", "Failed to register user keys: " + e.getMessage());
            Logger.getInstance().error("HMI", "Stack trace:", e);
        }
    }

    @Override
    public void onKeyEvent(IUserKey key, UserKeyEvent event)
    {
        try
        {
            if (event == UserKeyEvent.KeyDown)
            {
                handleKeyDown(key);
            } else if (event == UserKeyEvent.KeyUp)
            {
                handleKeyUp(key);
            }
        } catch (Exception e)
        {
            Logger.getInstance().error("HMI", "Error handling key event: " + e.getMessage());
            Logger.getInstance().error("HMI", "Stack trace:", e);
        }
    }

    /**
     * Handles key down events.
     */
    private void handleKeyDown(IUserKey key)
    {
        if (key == button1)
        {
            handleButton1Press();
        } else if (key == button2)
        {
            handleButton2Press();
        } else if (key == button3)
        {
            handleButton3Press();
        } else if (key == button4)
        {
            handleButton4Press();
        }
    }

    /**
     * Handles key up events.
     */
    private void handleKeyUp(IUserKey key)
    {
        if (key == button2)
        {
            handleButton2Release();
        }
    }

    /**
     * Button 1: Toggle open/close tools.
     * Uses the current tool ID detected from MediaFlange digital inputs.
     */
    private void handleButton1Press()
    {
        Logger.getInstance().low("HMI", "Button 1 pressed: " + (toolOpen ? "Closing" : "Opening") + " tool");
        
        try
        {
            // Get the current tool ID from MediaFlange digital inputs
            int currentToolId = commandExecutor.getIoExecutor().getToolController().getCurrentToolId();
            Logger.getInstance().debug("HMI", "Using tool ID " + currentToolId + " for open/close operation");
            
            if (toolOpen)
            {
                // Close the tool (activate suction/grip)
                boolean success = commandExecutor.getIoExecutor().getToolController().closeTool(currentToolId);
                if (success)
                {
                    toolOpen = false;
                    button1.setText(UserKeyAlignment.TopMiddle, "Tool " + currentToolId + ": Closed");
                    Logger.getInstance().low("HMI", "Tool " + currentToolId + " closed successfully via Button 1");
                } else
                {
                    Logger.getInstance().warn("HMI", "Failed to close tool " + currentToolId + " via Button 1");
                }
            } else
            {
                // Open the tool (blow air/release)
                boolean success = commandExecutor.getIoExecutor().getToolController().openTool(currentToolId);
                if (success)
                {
                    toolOpen = true;
                    button1.setText(UserKeyAlignment.TopMiddle, "Tool " + currentToolId + ": Open");
                    Logger.getInstance().low("HMI", "Tool " + currentToolId + " opened successfully via Button 1");
                } else
                {
                    Logger.getInstance().warn("HMI", "Failed to open tool " + currentToolId + " via Button 1");
                }
            }
        } catch (Exception e)
        {
            Logger.getInstance().error("HMI", "Error toggling tool state: " + e.getMessage());
        }
    }

    /**
     * Button 2: Start tracking press time for Gimatic lock/unlock.
     * Requires 2-second hold to unlock (safety feature).
     */
    private void handleButton2Press()
    {
        button2PressStartTime = System.currentTimeMillis();
        Logger.getInstance().debug("HMI", "Button 2 pressed: Starting hold timer");
        
        // Update button text to show hold requirement
        if (gimaticLocked)
        {
            button2.setText(UserKeyAlignment.TopMiddle, "Hold 2s to unlock");
        }
    }

    /**
     * Button 2: Handle release and check if held long enough for unlock.
     */
    private void handleButton2Release()
    {
        long holdDuration = System.currentTimeMillis() - button2PressStartTime;
        Logger.getInstance().debug("HMI", "Button 2 released after " + holdDuration + "ms");
        
        try
        {
            if (gimaticLocked)
            {
                // Unlocking requires 2-second hold
                if (holdDuration >= GIMATIC_UNLOCK_HOLD_TIME_MS)
                {
                    boolean success = commandExecutor.getIoExecutor().getToolController().unlockGimatic();
                    if (success)
                    {
                        gimaticLocked = false;
                        button2.setText(UserKeyAlignment.TopMiddle, "Gimatic: Unlocked");
                        Logger.getInstance().low("HMI", "Gimatic unlocked via Button 2 (held " + holdDuration + "ms)");
                    } else
                    {
                        button2.setText(UserKeyAlignment.TopMiddle, "Unlock Failed");
                        Logger.getInstance().warn("HMI", "Failed to unlock Gimatic via Button 2");
                    }
                } else
                {
                    button2.setText(UserKeyAlignment.TopMiddle, "Hold longer (2s)");
                    Logger.getInstance().debug("HMI", "Button 2 not held long enough to unlock (" + holdDuration + "ms < 2000ms)");
                }
            } else
            {
                // Locking doesn't require hold time
                boolean success = commandExecutor.getIoExecutor().getToolController().lockGimatic();
                if (success)
                {
                    gimaticLocked = true;
                    button2.setText(UserKeyAlignment.TopMiddle, "Gimatic: Locked");
                    Logger.getInstance().low("HMI", "Gimatic locked via Button 2");
                } else
                {
                    button2.setText(UserKeyAlignment.TopMiddle, "Lock Failed");
                    Logger.getInstance().warn("HMI", "Failed to lock Gimatic via Button 2");
                }
            }
        } catch (Exception e)
        {
            Logger.getInstance().error("HMI", "Error handling Gimatic lock/unlock: " + e.getMessage());
        }
        
        button2PressStartTime = 0;
    }

    /**
     * Button 3: Send current robot position to ROS2 clients.
     * Sends position in both joint space and Cartesian space (flange and tool).
     */
    private void handleButton3Press()
    {
        Logger.getInstance().low("HMI", "Button 3 pressed: Sending robot position to ROS2 clients");
        
        try
        {
            positionPublisher.publishCurrentPosition();
            button3.setText(UserKeyAlignment.TopMiddle, "Position Sent!");
            
            // Reset text after brief delay (in a separate thread to avoid blocking)
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        Thread.sleep(1500);
                        button3.setText(UserKeyAlignment.TopMiddle, "Send Position");
                    } catch (InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                    }
                }
            }).start();
            
        } catch (Exception e)
        {
            Logger.getInstance().error("HMI", "Error sending robot position: " + e.getMessage());
            button3.setText(UserKeyAlignment.TopMiddle, "Send Failed");
        }
    }

    /**
     * Button 4: Cancel current task and send success message to ROS2 client.
     * Cancels any ongoing motion, flushes the command queue, and sends a success response.
     * Useful for quick debugging and skipping unnecessary steps.
     */
    private void handleButton4Press()
    {
        Logger.getInstance().low("HMI", "Button 4 pressed: Canceling current task");
        
        try
        {
            // Cancel any ongoing motion
            commandExecutor.getMotionExecutor().cancelCurrentMotion();
            Logger.getInstance().debug("HMI", "Canceled ongoing motion");
            String commandId = commandExecutor.getMotionExecutor().getCurrentCommand().getId();
            // Flush the command queue to clear any pending commands
            int flushedCount = hartu.robot.communication.server.CommandQueue.flushQueue();
            Logger.getInstance().debug("HMI", "Flushed " + flushedCount + " pending command(s) from queue");
            
            // Send success message back to ROS2 client
            hartu.robot.communication.server.Ros2ServerManager serverManager = 
                hartu.robot.communication.server.Ros2ServerManager.getInstance();
            
            if (serverManager != null && serverManager.getTaskServer() != null)
            {
                hartu.robot.communication.server.ClientHandler taskClient = 
                    serverManager.getTaskServer().getClientHandler();
                
                if (taskClient != null)
                {
                    String successMessage = "FREE|" + commandId + "|success" +
                        hartu.protocols.constants.ProtocolConstants.MESSAGE_TERMINATOR;
                    taskClient.sendMessage(successMessage);
                    Logger.getInstance().low("HMI", "Sent task cancellation success message to ROS2 client");
                    
                    button4.setText(UserKeyAlignment.TopMiddle, "Task Canceled!");
                    
                    // Reset text after brief delay
                    new Thread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                Thread.sleep(1500);
                                button4.setText(UserKeyAlignment.TopMiddle, "Cancel Task");
                            } catch (InterruptedException e)
                            {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }).start();
                } else
                {
                    Logger.getInstance().warn("HMI", "No task client connected to send cancellation message");
                    button4.setText(UserKeyAlignment.TopMiddle, "No Client");
                }
            } else
            {
                Logger.getInstance().warn("HMI", "ROS2 server manager not initialized");
                button4.setText(UserKeyAlignment.TopMiddle, "Server Error");
            }
            
        } catch (Exception e)
        {
            Logger.getInstance().error("HMI", "Error canceling task: " + e.getMessage());
            button4.setText(UserKeyAlignment.TopMiddle, "Cancel Failed");
        }
    }
}

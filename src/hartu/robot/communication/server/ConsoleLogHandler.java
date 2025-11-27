package hartu.robot.communication.server;

/**
 * Log handler that writes messages to the robot's console using System.out.
 * This allows logs to be visible on the KUKA SmartPad/Teach Pendant.
 * Works from any thread (foreground tasks and background tasks).
 */
public class ConsoleLogHandler implements LogHandler
{
    private boolean active = true;

    @Override
    public void sendMessage(String formattedMessage)
    {
        if (active)
        {
            // Use println for robot console output
            // The KUKA Sunrise controller displays these messages on the SmartPad
            System.out.println(formattedMessage.trim());
        }
    }

    @Override
    public boolean isActive()
    {
        return active;
    }

    @Override
    public void close()
    {
        active = false;
    }
}

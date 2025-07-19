package hartu.tests;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;
import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.controllerModel.Controller;

// ** IMPORTANT: Add these Log4j2 imports **
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Implementation of a cyclic background task.
 * <p>
 * It provides the {@link RoboticsAPICyclicBackgroundTask#runCyclic} method
 * which will be called cyclically with the specified period.<br>
 * Cycle period and initial delay can be set by calling
 * {@link RoboticsAPICyclicBackgroundTask#initializeCyclic} method in the
 * {@link RoboticsAPIBackgroundTask#initialize()} method of the inheriting
 * class.<br>
 * The cyclic background task can be terminated via
 * {@link RoboticsAPICyclicBackgroundTask#getCyclicFuture()#cancel()} method or
 * stopping of the task.
 * @see UseRoboticsAPIContext
 *
 */
public class MinimalTestTask extends RoboticsAPICyclicBackgroundTask {

    // ** Declare the Logger instance here **
    // 'static final' ensures there's one logger instance per class, initialized once.
    // LogManager.getLogger(MinimalTestTask.class) gets a logger specifically named after this class.
    private static Logger LOGGER = LogManager.getLogger(MinimalTestTask.class);
    @Inject
    Controller kUKA_Sunrise_Cabinet_1;

    @Override
    public void initialize() {

        // initialize your task here
        initializeCyclic(0, 500, TimeUnit.MILLISECONDS,
                CycleBehavior.BestEffort);

        // ** Add your log messages in the initialize method **
        //LOGGER.info("MinimalTestTask: Task initialization complete. Cycle period set to 500ms.");
        //LOGGER.debug("MinimalTestTask: Debug message during initialize(). This will show if log level is DEBUG or TRACE.");
    }

    @Override
    public void runCyclic() {
        // your task execution starts here

        // ** Add your log messages in the runCyclic method **
        // Be mindful of logging too frequently in runCyclic as it runs every 500ms.
        // Use TRACE for very frequent, fine-grained details.
        // INFO is generally for significant events.
        //LOGGER.trace("MinimalTestTask: Entering runCyclic() method.");

        // Example: Log controller name (might be too frequent for INFO)
        // You could uncomment this and change the level to TRACE if needed.
        /*
        if (kUKA_Sunrise_Cabinet_1 != null) {
            LOGGER.trace("MinimalTestTask: Controller name: " + kUKA_Sunrise_Cabinet_1.getName());
        }
        */

        // Example: Simulate some action and log it
        // This log will appear every 500ms if the level is INFO or lower.
        //LOGGER.info("MinimalTestTask: Performing cyclic operation.");

        // Example: Log an error if something goes wrong (e.g., a critical variable is null)
        /*
        if (someCriticalVariable == null) {
            LOGGER.error("MinimalTestTask: Critical variable 'someCriticalVariable' is null!");
        }
        */
    }
}
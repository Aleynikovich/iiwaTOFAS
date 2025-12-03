package application;


import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Tool;

import javax.inject.Inject;
import javax.inject.Named;

import static com.kuka.roboticsAPI.motionModel.BasicMotions.ptp;

/**
 * Implementation of a robot application.
 * <p>
 * The application provides a {@link RoboticsAPITask#initialize()} and a
 * {@link RoboticsAPITask#run()} method, which will be called successively in
 * the application lifecycle. The application will terminate automatically after
 * the {@link RoboticsAPITask#run()} method has finished or after stopping the
 * task. The {@link RoboticsAPITask#dispose()} method will be called, even if an
 * exception is thrown during initialization or run.
 * <p>
 * <b>It is imperative to call <code>super.dispose()</code> when overriding the
 * {@link RoboticsAPITask#dispose()} method.</b>
 *
 * @see UseRoboticsAPIContext
 * @see #initialize()
 * @see #run()
 * @see #dispose()
 */
public class testing extends RoboticsAPIApplication
{
    @Inject
    private LBR iiwa;
    @Inject
    @Named("GimaticCamera")
    private Tool gimatic;

    @Override
    public void initialize()
    {
        // initialize your application here
    }

    @Override
    public void run()
    {
        // your application execution starts here
        gimatic.attachTo(iiwa.getFlange());
        gimatic.move(ptp(getApplicationData().getFrame("/T1Base/P1")));
    }
}
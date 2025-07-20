package hartu.robot.commands.positions;

import com.kuka.roboticsAPI.deviceModel.JointPosition;
import hartu.robot.communication.server.Logger; // Assuming Logger is accessible

/**
 * Represents a robot's joint position with 7 axis values.
 */
public class AxisPosition {
    private final double j1, j2, j3, j4, j5, j6, j7;

    public AxisPosition(double j1, double j2, double j3, double j4, double j5, double j6, double j7) {
        this.j1 = j1;
        this.j2 = j2;
        this.j3 = j3;
        this.j4 = j4;
        this.j5 = j5;
        this.j6 = j6;
        this.j7 = j7;
        Logger.getInstance().log("AXIS_POS", String.format("Created AxisPosition: J1=%.3f, J2=%.3f, J3=%.3f, J4=%.3f, J5=%.3f, J6=%.3f, J7=%.3f", j1, j2, j3, j4, j5, j6, j7));
    }

    public double getJ1() { return j1; }
    public double getJ2() { return j2; }
    public double getJ3() { return j3; }
    public double getJ4() { return j4; }
    public double getJ5() { return j5; }
    public double getJ6() { return j6; }
    public double getJ7() { return j7; }

    /**
     * Converts this AxisPosition object into a KUKA RoboticsAPI JointPosition object.
     * @return A new JointPosition object.
     */
    public JointPosition toJointPosition() {
        return new JointPosition(j1, j2, j3, j4, j5, j6, j7);
    }

    @Override
    public String toString() {
        return String.format("J1:%.3f, J2:%.3f, J3:%.3f, J4:%.3f, J5:%.3f, J6:%.3f, J7:%.3f", j1, j2, j3, j4, j5, j6, j7);
    }
}

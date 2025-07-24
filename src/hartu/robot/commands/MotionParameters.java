package hartu.robot.commands;

import com.kuka.roboticsAPI.deviceModel.JointEnum;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.motionModel.*;
import hartu.robot.communication.server.Logger;
import java.util.Map;

public class MotionParameters {
    private final double speedOverride;
    private final String tool;
    private final String base;
    private final boolean continuous;
    private final int numPoints;
    private final Map<JointEnum, Double> jointVelocityRel;
    private final Map<JointEnum, Double> jointAccelerationRel;
    private final Double blendingRel;

    public MotionParameters(double speedOverride, String tool, String base, boolean continuous, int numPoints,
                            Map<JointEnum, Double> jointVelocityRel, Map<JointEnum, Double> jointAccelerationRel, Double blendingRel) {

        Logger logger = Logger.getInstance();
        if (speedOverride < 0.0) {
            logger.warn("CMD_PARAM", "Warning: Initial speedOverride (" + speedOverride + ") is negative.");
        }

        while (speedOverride > 1.0) {
            logger.warn("CMD_PARAM", "Warning: Initial speedOverride (" + speedOverride + ") is outside 0.0-1.0 range. Clamping by 0.01");
            speedOverride = speedOverride * 0.01;
        }

        this.speedOverride = Math.max(0.0, speedOverride);

        if (numPoints < 0) {
            logger.log("CMD_PARAM", "Error: Number of points cannot be negative.");
            throw new IllegalArgumentException("Number of points cannot be negative.");
        }

        this.tool = tool;
        this.base = base;
        this.continuous = continuous;
        this.numPoints = numPoints;
        this.jointVelocityRel = jointVelocityRel;
        this.jointAccelerationRel = jointAccelerationRel;
        this.blendingRel = blendingRel == null ? 0.05 : blendingRel;
    }

    public double getSpeedOverride() { return speedOverride; }
    public String getTool() { return tool; }
    public String getBase() { return base; }
    public boolean isContinuous() { return continuous; }
    public int getNumPoints() { return numPoints; }
    public Map<JointEnum, Double> getJointVelocityRel() { return jointVelocityRel; }
    public Map<JointEnum, Double> getJointAccelerationRel() { return jointAccelerationRel; }
    public Double getBlendingRel() { return blendingRel; }

    public CartesianPTP createPTPMotion(Frame destination) {
        CartesianPTP ptpMotion = new CartesianPTP(destination);
        ptpMotion.setJointVelocityRel(getSpeedOverride());
        if (getBlendingRel() != null) {
            ptpMotion.setBlendingRel(getBlendingRel());
        }
        return ptpMotion;
    }

    public PTP createPTPJointMotion(JointPosition jointPosition) {
        PTP ptpMotion = new PTP(jointPosition);
        if (getJointVelocityRel() == null) {
            ptpMotion.setJointVelocityRel(getSpeedOverride());
        } else {
            for (Map.Entry<JointEnum, Double> entry : getJointVelocityRel().entrySet()) {
                ptpMotion.setJointVelocityRel(entry.getKey(), entry.getValue());
            }
        }
        if (getJointAccelerationRel() != null) {
            for (Map.Entry<JointEnum, Double> entry : getJointAccelerationRel().entrySet()) {
                ptpMotion.setJointAccelerationRel(entry.getKey(), entry.getValue());
            }
        }
        if (getBlendingRel() != null) {
            ptpMotion.setBlendingRel(getBlendingRel());
        }
        return ptpMotion;
    }

    public LIN createLINMotion(Frame destination) {
        LIN linMotion = new LIN(destination);
        linMotion.setJointVelocityRel(getSpeedOverride());
        if (getBlendingRel() != null) {
            linMotion.setBlendingRel(getBlendingRel());
        }
        return linMotion;
    }

    public CIRC createCircularMotion(Frame auxiliaryFrame, Frame destination) {
        CIRC circMotion = new CIRC(auxiliaryFrame, destination);
        circMotion.setJointVelocityRel(getSpeedOverride());
        if (getBlendingRel() != null) {
            circMotion.setBlendingRel(getBlendingRel());
        }
        return circMotion;
    }
}
package com.kuka.connectivity.smartServo.examples;

import static com.kuka.roboticsAPI.motionModel.BasicMotions.ptp;

import com.kuka.common.StatisticTimer;
import com.kuka.common.StatisticTimer.OneTimeStep;
import com.kuka.common.ThreadUtil;
import com.kuka.connectivity.motionModel.smartServo.ISmartServoRuntime;
import com.kuka.connectivity.motionModel.smartServo.ServoMotion;
import com.kuka.connectivity.motionModel.smartServo.SmartServo;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.LoadData;
import com.kuka.roboticsAPI.geometricModel.ObjectFrame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.math.XyzAbcTransformation;
import com.kuka.roboticsAPI.sensorModel.DataRecorder;
import com.kuka.roboticsAPI.sensorModel.DataRecorder.AngleUnit;

/**
 * This example activates a SmartServo motion in position control mode, sends a sequence of joint specific set points,
 * describing a sine function and evaluates the statistic timing.
 */
public class test_smartServo extends RoboticsAPIApplication
{
	
    
    private LBR _lbr;
    private ISmartServoRuntime _theSmartServoRuntime = null;

    // Tool Data
    private Tool _toolAttachedToLBR;
    private LoadData _loadData;
    private static final String TOOL_FRAME = "toolFrame";
    private static final double[] TRANSLATION_OF_TOOL = { 0, 0, 100 };
    private static final double MASS = 0;
    private static final double[] CENTER_OF_MASS_IN_MILLIMETER = { 0, 0, 100 };

    private int _count = 0;

    private static final int MILLI_SLEEP_TO_EMULATE_COMPUTATIONAL_EFFORT = 60;
    private static final int NUM_RUNS = 1000;
    private static final double AMPLITUDE = 0.2;
    private static final double FREQENCY = 0.1;
    private int _steps = 0;

    @Override
    public void initialize()
    {
        _lbr = getContext().getDeviceFromType(LBR.class);
        

        // Create a Tool by Hand this is the tool we want to move with some mass
        // properties and a TCP-Z-offset of 100.
        _loadData = new LoadData();
        _loadData.setMass(MASS);
        _loadData.setCenterOfMass(
                CENTER_OF_MASS_IN_MILLIMETER[0], CENTER_OF_MASS_IN_MILLIMETER[1],
                CENTER_OF_MASS_IN_MILLIMETER[2]);
        _toolAttachedToLBR = new Tool("Tool", _loadData);

        XyzAbcTransformation trans = XyzAbcTransformation.ofTranslation(
                TRANSLATION_OF_TOOL[0], TRANSLATION_OF_TOOL[1],
                TRANSLATION_OF_TOOL[2]);
        ObjectFrame aTransformation = _toolAttachedToLBR.addChildFrame(TOOL_FRAME
                + "(TCP)", trans);
        _toolAttachedToLBR.setDefaultMotionFrame(aTransformation);
        // Attach tool to the robot
        _toolAttachedToLBR.attachTo(_lbr.getFlange());
    }

    /**
     * Move to an initial Position WARNING: MAKE SURE, THAT the pose is collision free.
     */
    public void moveToInitialPosition()
    {

        _toolAttachedToLBR.move(
                ptp(Math.PI / 180 * -68.08,0.572926 , 6.0415e-05 , 
                		-0.839401 , -9.78274e-05 , 1.73356 , 9.76438e-06).setJointVelocityRel(0.1));
        /* Note: The Validation itself justifies, that in this very time instance, the load parameter setting was
         * sufficient. This does not mean by far, that the parameter setting is valid in the sequel or lifetime of this
         * program */
        try
        {
            if (!ServoMotion.validateForImpedanceMode(_toolAttachedToLBR))
            {
                getLogger().info("Validation of torque model failed - correct your mass property settings");
                getLogger().info("SmartServo will be available for position controlled mode only, until validation is performed");
            }
        }
        catch (IllegalStateException e)
        {
            getLogger().info("Omitting validation failure for this sample\n"
                    + e.getMessage());
        }
    }

    @Override
    public void run()
    {

        moveToInitialPosition();

        boolean doDebugPrints = false;

        JointPosition initialPosition = new JointPosition(
                _lbr.getCurrentJointPosition());
        SmartServo aSmartServoMotion = new SmartServo(initialPosition);

        // Set the motion properties to 20% of systems abilities
        aSmartServoMotion.setJointAccelerationRel(1.0);
        aSmartServoMotion.setJointVelocityRel(0.5);
        aSmartServoMotion.overrideJointAcceleration(10.0);

        aSmartServoMotion.setMinimumTrajectoryExecutionTime(90e-3);
        
        DataRecorder rec = new DataRecorder();
        rec.setFileName("Recording_test.log");
        rec.setSampleInterval(1);
        
      
        rec.addCurrentJointPosition(_lbr, AngleUnit.Degree);
        rec.addCommandedJointPosition(_lbr,AngleUnit.Degree);
        rec.addCurrentCartesianPositionXYZ(_lbr.getDefaultMotionFrame(), _lbr.getRootFrame());
        rec.addCommandedCartesianPositionXYZ(_lbr.getDefaultMotionFrame(),_lbr.getRootFrame());

        
        rec.enable();

        getLogger().info("Starting SmartServo motion in position control mode");
        _toolAttachedToLBR.getDefaultMotionFrame().moveAsync(aSmartServoMotion);

        getLogger().info("Get the runtime of the SmartServo motion");
        _theSmartServoRuntime = aSmartServoMotion.getRuntime();

        // create an JointPosition Instance, to play with
        JointPosition destination = new JointPosition(
                _lbr.getJointCount());
        getLogger().info("Start loop");
        // For Roundtrip time measurement...
        StatisticTimer timing = new StatisticTimer();
        try
        {
            // do a cyclic loop
            // Refer to some timing...
            // in nanosec
            double omega = FREQENCY * 2 * Math.PI * 1e-9;
            long startTimeStamp = System.nanoTime();
            
            
            JointPosition jointPosUp[] = new JointPosition[569];
        	
        	jointPosUp[0] = new JointPosition(-5.07029e-05 , 0.572926 , 6.0415e-05 , -0.839401 , -9.78274e-05 , 1.73356 , 9.76438e-06);
        	jointPosUp[1] = new JointPosition(-5.07029e-05 , 0.572926 , 6.0415e-05 , -0.839401 , -9.78274e-05 , 1.73356 , 9.76438e-06);
        	jointPosUp[2] = new JointPosition(-5.07604e-05 , 0.572278 , 6.04256e-05 , -0.84087 , -9.77868e-05 , 1.73274 , 9.82267e-06);
        	jointPosUp[3] = new JointPosition(-5.08178e-05 , 0.571633 , 6.04362e-05 , -0.842335 , -9.77464e-05 , 1.73192 , 9.88073e-06);
        	jointPosUp[4] = new JointPosition(-5.09324e-05 , 0.570343 , 6.04572e-05 , -0.845261 , -9.76657e-05 , 1.73029 , 9.99653e-06);
        	jointPosUp[5] = new JointPosition(-5.10465e-05 , 0.56906 , 6.04778e-05 , -0.848174 , -9.75855e-05 , 1.72866 , 1.01114e-05);
        	jointPosUp[6] = new JointPosition(-5.11601e-05 , 0.567781 , 6.04983e-05 , -0.851078 , -9.75057e-05 , 1.72703 , 1.02256e-05);
        	jointPosUp[7] = new JointPosition(-5.12733e-05 , 0.566508 , 6.05184e-05 , -0.853972 , -9.74263e-05 , 1.72541 , 1.03391e-05);
        	jointPosUp[8] = new JointPosition(-5.1386e-05 , 0.565239 , 6.05384e-05 , -0.856857 , -9.73472e-05 , 1.7238 , 1.04519e-05);
        	jointPosUp[9] = new JointPosition(-5.14984e-05 , 0.563975 , 6.0558e-05 , -0.859733 , -9.72685e-05 , 1.72218 , 1.0564e-05);
        	jointPosUp[10] = new JointPosition(-5.16103e-05 , 0.562715 , 6.05775e-05 , -0.8626 , -9.71902e-05 , 1.72058 , 1.06754e-05);
        	jointPosUp[11] = new JointPosition(-5.17218e-05 , 0.56146 , 6.05967e-05 , -0.865458 , -9.71122e-05 , 1.71897 , 1.07861e-05);
        	jointPosUp[12] = new JointPosition(-5.18329e-05 , 0.56021 , 6.06156e-05 , -0.868307 , -9.70347e-05 , 1.71737 , 1.08962e-05);
        	jointPosUp[13] = new JointPosition(-5.19436e-05 , 0.558964 , 6.06343e-05 , -0.871147 , -9.69574e-05 , 1.71578 , 1.10056e-05);
        	jointPosUp[14] = new JointPosition(-5.20538e-05 , 0.557722 , 6.06528e-05 , -0.873979 , -9.68806e-05 , 1.71419 , 1.11143e-05);
        	jointPosUp[15] = new JointPosition(-5.21637e-05 , 0.556485 , 6.0671e-05 , -0.876801 , -9.68041e-05 , 1.7126 , 1.12224e-05);
        	jointPosUp[16] = new JointPosition(-5.22732e-05 , 0.555253 , 6.0689e-05 , -0.879616 , -9.67279e-05 , 1.71102 , 1.13298e-05);
        	jointPosUp[17] = new JointPosition(-5.23822e-05 , 0.554024 , 6.07068e-05 , -0.882422 , -9.66521e-05 , 1.70945 , 1.14366e-05);
        	jointPosUp[18] = new JointPosition(-5.24909e-05 , 0.5528 , 6.07244e-05 , -0.885219 , -9.65766e-05 , 1.70787 , 1.15428e-05);
        	jointPosUp[19] = new JointPosition(-5.25992e-05 , 0.551581 , 6.07417e-05 , -0.888008 , -9.65015e-05 , 1.7063 , 1.16483e-05);
        	jointPosUp[20] = new JointPosition(-5.27071e-05 , 0.550365 , 6.07588e-05 , -0.890789 , -9.64268e-05 , 1.70474 , 1.17533e-05);
        	jointPosUp[21] = new JointPosition(-5.28146e-05 , 0.549154 , 6.07757e-05 , -0.893562 , -9.63523e-05 , 1.70318 , 1.18576e-05);
        	jointPosUp[22] = new JointPosition(-5.29217e-05 , 0.547946 , 6.07924e-05 , -0.896326 , -9.62782e-05 , 1.70162 , 1.19613e-05);
        	jointPosUp[23] = new JointPosition(-5.30284e-05 , 0.546743 , 6.08089e-05 , -0.899083 , -9.62045e-05 , 1.70006 , 1.20644e-05);
        	jointPosUp[24] = new JointPosition(-5.31348e-05 , 0.545544 , 6.08251e-05 , -0.901832 , -9.6131e-05 , 1.69852 , 1.21669e-05);
        	jointPosUp[25] = new JointPosition(-5.32408e-05 , 0.544349 , 6.08412e-05 , -0.904573 , -9.60579e-05 , 1.69697 , 1.22688e-05);
        	jointPosUp[26] = new JointPosition(-5.33464e-05 , 0.543158 , 6.0857e-05 , -0.907306 , -9.59852e-05 , 1.69543 , 1.23702e-05);
        	jointPosUp[27] = new JointPosition(-5.34516e-05 , 0.541971 , 6.08726e-05 , -0.910031 , -9.59127e-05 , 1.69389 , 1.24709e-05);
        	jointPosUp[28] = new JointPosition(-5.35565e-05 , 0.540788 , 6.08881e-05 , -0.912749 , -9.58406e-05 , 1.69235 , 1.25711e-05);
        	jointPosUp[29] = new JointPosition(-5.3661e-05 , 0.539609 , 6.09033e-05 , -0.915459 , -9.57688e-05 , 1.69082 , 1.26708e-05);
        	jointPosUp[30] = new JointPosition(-5.37651e-05 , 0.538434 , 6.09183e-05 , -0.918162 , -9.56973e-05 , 1.6893 , 1.27698e-05);
        	jointPosUp[31] = new JointPosition(-5.38689e-05 , 0.537262 , 6.09331e-05 , -0.920857 , -9.56261e-05 , 1.68777 , 1.28684e-05);
        	jointPosUp[32] = new JointPosition(-5.39723e-05 , 0.536095 , 6.09477e-05 , -0.923545 , -9.55553e-05 , 1.68625 , 1.29663e-05);
        	jointPosUp[33] = new JointPosition(-5.40754e-05 , 0.534931 , 6.09622e-05 , -0.926226 , -9.54847e-05 , 1.68473 , 1.30638e-05);
        	jointPosUp[34] = new JointPosition(-5.41781e-05 , 0.533771 , 6.09764e-05 , -0.928899 , -9.54145e-05 , 1.68322 , 1.31607e-05);
        	jointPosUp[35] = new JointPosition(-5.42805e-05 , 0.532614 , 6.09904e-05 , -0.931565 , -9.53446e-05 , 1.68171 , 1.3257e-05);
        	jointPosUp[36] = new JointPosition(-5.43825e-05 , 0.531462 , 6.10043e-05 , -0.934224 , -9.52749e-05 , 1.68021 , 1.33529e-05);
        	jointPosUp[37] = new JointPosition(-5.44841e-05 , 0.530312 , 6.1018e-05 , -0.936876 , -9.52056e-05 , 1.6787 , 1.34482e-05);
        	jointPosUp[38] = new JointPosition(-5.45854e-05 , 0.529167 , 6.10314e-05 , -0.939521 , -9.51366e-05 , 1.6772 , 1.3543e-05);
        	jointPosUp[39] = new JointPosition(-5.46864e-05 , 0.528025 , 6.10447e-05 , -0.942159 , -9.50679e-05 , 1.67571 , 1.36373e-05);
        	jointPosUp[40] = new JointPosition(-5.4787e-05 , 0.526887 , 6.10578e-05 , -0.94479 , -9.49995e-05 , 1.67421 , 1.37311e-05);
        	jointPosUp[41] = new JointPosition(-5.48873e-05 , 0.525752 , 6.10708e-05 , -0.947415 , -9.49313e-05 , 1.67272 , 1.38243e-05);
        	jointPosUp[42] = new JointPosition(-5.49873e-05 , 0.524621 , 6.10835e-05 , -0.950032 , -9.48635e-05 , 1.67124 , 1.39171e-05);
        	jointPosUp[43] = new JointPosition(-5.50869e-05 , 0.523493 , 6.10961e-05 , -0.952643 , -9.4796e-05 , 1.66976 , 1.40094e-05);
        	jointPosUp[44] = new JointPosition(-5.51862e-05 , 0.522369 , 6.11085e-05 , -0.955247 , -9.47287e-05 , 1.66828 , 1.41012e-05);
        	jointPosUp[45] = new JointPosition(-5.52852e-05 , 0.521248 , 6.11207e-05 , -0.957844 , -9.46618e-05 , 1.6668 , 1.41925e-05);
        	jointPosUp[46] = new JointPosition(-5.53838e-05 , 0.520131 , 6.11327e-05 , -0.960435 , -9.45951e-05 , 1.66532 , 1.42834e-05);
        	jointPosUp[47] = new JointPosition(-5.54821e-05 , 0.519017 , 6.11446e-05 , -0.96302 , -9.45287e-05 , 1.66385 , 1.43737e-05);
        	jointPosUp[48] = new JointPosition(-5.55801e-05 , 0.517907 , 6.11563e-05 , -0.965598 , -9.44626e-05 , 1.66239 , 1.44636e-05);
        	jointPosUp[49] = new JointPosition(-5.56777e-05 , 0.516799 , 6.11678e-05 , -0.96817 , -9.43968e-05 , 1.66092 , 1.4553e-05);
        	jointPosUp[50] = new JointPosition(-5.57751e-05 , 0.515695 , 6.11792e-05 , -0.970735 , -9.43312e-05 , 1.65946 , 1.4642e-05);
        	jointPosUp[51] = new JointPosition(-5.58721e-05 , 0.514595 , 6.11904e-05 , -0.973294 , -9.4266e-05 , 1.658 , 1.47305e-05);
        	jointPosUp[52] = new JointPosition(-5.59688e-05 , 0.513497 , 6.12014e-05 , -0.975847 , -9.4201e-05 , 1.65655 , 1.48185e-05);
        	jointPosUp[53] = new JointPosition(-5.60652e-05 , 0.512403 , 6.12122e-05 , -0.978393 , -9.41363e-05 , 1.6551 , 1.49061e-05);
        	jointPosUp[54] = new JointPosition(-5.61612e-05 , 0.511312 , 6.12229e-05 , -0.980934 , -9.40718e-05 , 1.65365 , 1.49933e-05);
        	jointPosUp[55] = new JointPosition(-5.6257e-05 , 0.510225 , 6.12335e-05 , -0.983468 , -9.40076e-05 , 1.6522 , 1.508e-05);
        	jointPosUp[56] = new JointPosition(-5.63525e-05 , 0.50914 , 6.12438e-05 , -0.985996 , -9.39437e-05 , 1.65076 , 1.51663e-05);
        	jointPosUp[57] = new JointPosition(-5.64476e-05 , 0.508059 , 6.12541e-05 , -0.988518 , -9.38801e-05 , 1.64931 , 1.52521e-05);
        	jointPosUp[58] = new JointPosition(-5.65424e-05 , 0.50698 , 6.12641e-05 , -0.991035 , -9.38167e-05 , 1.64788 , 1.53375e-05);
        	jointPosUp[59] = new JointPosition(-5.6637e-05 , 0.505905 , 6.1274e-05 , -0.993545 , -9.37536e-05 , 1.64644 , 1.54225e-05);
        	jointPosUp[60] = new JointPosition(-5.67312e-05 , 0.504833 , 6.12838e-05 , -0.99605 , -9.36908e-05 , 1.64501 , 1.55071e-05);
        	jointPosUp[61] = new JointPosition(-5.68251e-05 , 0.503764 , 6.12934e-05 , -0.998548 , -9.36282e-05 , 1.64358 , 1.55912e-05);
        	jointPosUp[62] = new JointPosition(-5.69188e-05 , 0.502698 , 6.13028e-05 , -1.00104 , -9.35659e-05 , 1.64215 , 1.56749e-05);
        	jointPosUp[63] = new JointPosition(-5.70121e-05 , 0.501635 , 6.13121e-05 , -1.00353 , -9.35039e-05 , 1.64073 , 1.57583e-05);
        	jointPosUp[64] = new JointPosition(-5.71051e-05 , 0.500575 , 6.13212e-05 , -1.00601 , -9.34421e-05 , 1.63931 , 1.58412e-05);
        	jointPosUp[65] = new JointPosition(-5.71979e-05 , 0.499518 , 6.13302e-05 , -1.00849 , -9.33805e-05 , 1.63789 , 1.59237e-05);
        	jointPosUp[66] = new JointPosition(-5.72903e-05 , 0.498464 , 6.13391e-05 , -1.01096 , -9.33193e-05 , 1.63647 , 1.60058e-05);
        	jointPosUp[67] = new JointPosition(-5.73825e-05 , 0.497413 , 6.13478e-05 , -1.01342 , -9.32582e-05 , 1.63506 , 1.60875e-05);
        	jointPosUp[68] = new JointPosition(-5.74744e-05 , 0.496365 , 6.13563e-05 , -1.01588 , -9.31975e-05 , 1.63365 , 1.61688e-05);
        	jointPosUp[69] = new JointPosition(-5.7566e-05 , 0.49532 , 6.13647e-05 , -1.01833 , -9.31369e-05 , 1.63224 , 1.62497e-05);
        	jointPosUp[70] = new JointPosition(-5.76573e-05 , 0.494278 , 6.1373e-05 , -1.02078 , -9.30767e-05 , 1.63083 , 1.63302e-05);
        	jointPosUp[71] = new JointPosition(-5.77483e-05 , 0.493238 , 6.13811e-05 , -1.02322 , -9.30166e-05 , 1.62943 , 1.64104e-05);
        	jointPosUp[72] = new JointPosition(-5.7839e-05 , 0.492201 , 6.13891e-05 , -1.02566 , -9.29569e-05 , 1.62803 , 1.64902e-05);
        	jointPosUp[73] = new JointPosition(-5.79294e-05 , 0.491168 , 6.13969e-05 , -1.02809 , -9.28973e-05 , 1.62663 , 1.65696e-05);
        	jointPosUp[74] = new JointPosition(-5.80196e-05 , 0.490137 , 6.14046e-05 , -1.03052 , -9.28381e-05 , 1.62523 , 1.66486e-05);
        	jointPosUp[75] = new JointPosition(-5.81095e-05 , 0.489109 , 6.14121e-05 , -1.03294 , -9.2779e-05 , 1.62384 , 1.67272e-05);
        	jointPosUp[76] = new JointPosition(-5.81991e-05 , 0.488083 , 6.14196e-05 , -1.03536 , -9.27202e-05 , 1.62245 , 1.68055e-05);
        	jointPosUp[77] = new JointPosition(-5.82885e-05 , 0.487061 , 6.14268e-05 , -1.03777 , -9.26617e-05 , 1.62106 , 1.68834e-05);
        	jointPosUp[78] = new JointPosition(-5.83775e-05 , 0.486041 , 6.1434e-05 , -1.04017 , -9.26034e-05 , 1.61968 , 1.6961e-05);
        	jointPosUp[79] = new JointPosition(-5.84663e-05 , 0.485023 , 6.1441e-05 , -1.04258 , -9.25453e-05 , 1.61829 , 1.70382e-05);
        	jointPosUp[80] = new JointPosition(-5.85548e-05 , 0.484009 , 6.14479e-05 , -1.04497 , -9.24874e-05 , 1.61691 , 1.7115e-05);
        	jointPosUp[81] = new JointPosition(-5.86431e-05 , 0.482997 , 6.14546e-05 , -1.04736 , -9.24298e-05 , 1.61553 , 1.71915e-05);
        	jointPosUp[82] = new JointPosition(-5.8731e-05 , 0.481988 , 6.14613e-05 , -1.04975 , -9.23725e-05 , 1.61416 , 1.72676e-05);
        	jointPosUp[83] = new JointPosition(-5.88188e-05 , 0.480982 , 6.14677e-05 , -1.05213 , -9.23154e-05 , 1.61278 , 1.73434e-05);
        	jointPosUp[84] = new JointPosition(-5.89062e-05 , 0.479978 , 6.14741e-05 , -1.0545 , -9.22585e-05 , 1.61141 , 1.74188e-05);
        	jointPosUp[85] = new JointPosition(-5.89934e-05 , 0.478977 , 6.14803e-05 , -1.05687 , -9.22018e-05 , 1.61004 , 1.74939e-05);
        	jointPosUp[86] = new JointPosition(-5.90803e-05 , 0.477978 , 6.14864e-05 , -1.05924 , -9.21454e-05 , 1.60867 , 1.75687e-05);
        	jointPosUp[87] = new JointPosition(-5.9167e-05 , 0.476982 , 6.14924e-05 , -1.0616 , -9.20892e-05 , 1.60731 , 1.76431e-05);
        	jointPosUp[88] = new JointPosition(-5.92534e-05 , 0.475989 , 6.14982e-05 , -1.06396 , -9.20332e-05 , 1.60594 , 1.77172e-05);
        	jointPosUp[89] = new JointPosition(-5.93395e-05 , 0.474998 , 6.1504e-05 , -1.06631 , -9.19774e-05 , 1.60458 , 1.7791e-05);
        	jointPosUp[90] = new JointPosition(-5.94254e-05 , 0.47401 , 6.15096e-05 , -1.06866 , -9.19219e-05 , 1.60322 , 1.78644e-05);
        	jointPosUp[91] = new JointPosition(-5.9511e-05 , 0.473024 , 6.1515e-05 , -1.071 , -9.18666e-05 , 1.60187 , 1.79375e-05);
        	jointPosUp[92] = new JointPosition(-5.95963e-05 , 0.472041 , 6.15204e-05 , -1.07334 , -9.18116e-05 , 1.60051 , 1.80103e-05);
        	jointPosUp[93] = new JointPosition(-5.96815e-05 , 0.47106 , 6.15256e-05 , -1.07567 , -9.17567e-05 , 1.59916 , 1.80828e-05);
        	jointPosUp[94] = new JointPosition(-5.97663e-05 , 0.470082 , 6.15307e-05 , -1.078 , -9.17021e-05 , 1.59781 , 1.8155e-05);
        	jointPosUp[95] = new JointPosition(-5.98509e-05 , 0.469107 , 6.15357e-05 , -1.08032 , -9.16477e-05 , 1.59646 , 1.82268e-05);
        	jointPosUp[96] = new JointPosition(-5.99353e-05 , 0.468133 , 6.15406e-05 , -1.08264 , -9.15936e-05 , 1.59512 , 1.82983e-05);
        	jointPosUp[97] = new JointPosition(-6.00194e-05 , 0.467163 , 6.15453e-05 , -1.08495 , -9.15396e-05 , 1.59378 , 1.83695e-05);
        	jointPosUp[98] = new JointPosition(-6.01032e-05 , 0.466194 , 6.155e-05 , -1.08726 , -9.14859e-05 , 1.59243 , 1.84405e-05);
        	jointPosUp[99] = new JointPosition(-6.01868e-05 , 0.465228 , 6.15545e-05 , -1.08957 , -9.14324e-05 , 1.59109 , 1.85111e-05);
        	jointPosUp[100] = new JointPosition(-6.02702e-05 , 0.464265 , 6.15589e-05 , -1.09187 , -9.13791e-05 , 1.58976 , 1.85814e-05);
        	jointPosUp[101] = new JointPosition(-6.03533e-05 , 0.463304 , 6.15632e-05 , -1.09417 , -9.1326e-05 , 1.58842 , 1.86514e-05);
        	jointPosUp[102] = new JointPosition(-6.04362e-05 , 0.462345 , 6.15673e-05 , -1.09646 , -9.12731e-05 , 1.58709 , 1.87211e-05);
        	jointPosUp[103] = new JointPosition(-6.05188e-05 , 0.461389 , 6.15714e-05 , -1.09875 , -9.12205e-05 , 1.58576 , 1.87905e-05);
        	jointPosUp[104] = new JointPosition(-6.06012e-05 , 0.460435 , 6.15753e-05 , -1.10103 , -9.1168e-05 , 1.58443 , 1.88596e-05);
        	jointPosUp[105] = new JointPosition(-6.06833e-05 , 0.459483 , 6.15792e-05 , -1.10331 , -9.11158e-05 , 1.5831 , 1.89285e-05);
        	jointPosUp[106] = new JointPosition(-6.07652e-05 , 0.458534 , 6.15829e-05 , -1.10558 , -9.10638e-05 , 1.58177 , 1.8997e-05);
        	jointPosUp[107] = new JointPosition(-6.08469e-05 , 0.457587 , 6.15865e-05 , -1.10785 , -9.1012e-05 , 1.58045 , 1.90653e-05);
        	jointPosUp[108] = new JointPosition(-6.09283e-05 , 0.456642 , 6.159e-05 , -1.11012 , -9.09604e-05 , 1.57913 , 1.91332e-05);
        	jointPosUp[109] = new JointPosition(-6.10095e-05 , 0.455699 , 6.15934e-05 , -1.11238 , -9.0909e-05 , 1.57781 , 1.92009e-05);
        	jointPosUp[110] = new JointPosition(-6.10905e-05 , 0.454759 , 6.15967e-05 , -1.11464 , -9.08579e-05 , 1.57649 , 1.92683e-05);
        	jointPosUp[111] = new JointPosition(-6.11712e-05 , 0.453822 , 6.15998e-05 , -1.1169 , -9.08069e-05 , 1.57517 , 1.93355e-05);
        	jointPosUp[112] = new JointPosition(-6.12517e-05 , 0.452886 , 6.16029e-05 , -1.11914 , -9.07562e-05 , 1.57386 , 1.94023e-05);
        	jointPosUp[113] = new JointPosition(-6.1332e-05 , 0.451953 , 6.16058e-05 , -1.12139 , -9.07056e-05 , 1.57255 , 1.94689e-05);
        	jointPosUp[114] = new JointPosition(-6.1412e-05 , 0.451022 , 6.16087e-05 , -1.12363 , -9.06553e-05 , 1.57124 , 1.95353e-05);
        	jointPosUp[115] = new JointPosition(-6.14918e-05 , 0.450093 , 6.16114e-05 , -1.12587 , -9.06051e-05 , 1.56993 , 1.96013e-05);
        	jointPosUp[116] = new JointPosition(-6.15714e-05 , 0.449166 , 6.16141e-05 , -1.1281 , -9.05552e-05 , 1.56862 , 1.96671e-05);
        	jointPosUp[117] = new JointPosition(-6.16507e-05 , 0.448242 , 6.16166e-05 , -1.13033 , -9.05055e-05 , 1.56732 , 1.97326e-05);
        	jointPosUp[118] = new JointPosition(-6.17298e-05 , 0.447319 , 6.16191e-05 , -1.13256 , -9.0456e-05 , 1.56601 , 1.97979e-05);
        	jointPosUp[119] = new JointPosition(-6.18087e-05 , 0.446399 , 6.16214e-05 , -1.13478 , -9.04066e-05 , 1.56471 , 1.98629e-05);
        	jointPosUp[120] = new JointPosition(-6.18874e-05 , 0.445481 , 6.16236e-05 , -1.137 , -9.03575e-05 , 1.56341 , 1.99276e-05);
        	jointPosUp[121] = new JointPosition(-6.19658e-05 , 0.444566 , 6.16257e-05 , -1.13921 , -9.03086e-05 , 1.56212 , 1.99921e-05);
        	jointPosUp[122] = new JointPosition(-6.20441e-05 , 0.443652 , 6.16278e-05 , -1.14142 , -9.02599e-05 , 1.56082 , 2.00563e-05);
        	jointPosUp[123] = new JointPosition(-6.2122e-05 , 0.442741 , 6.16297e-05 , -1.14363 , -9.02114e-05 , 1.55952 , 2.01203e-05);
        	jointPosUp[124] = new JointPosition(-6.21998e-05 , 0.441831 , 6.16315e-05 , -1.14583 , -9.0163e-05 , 1.55823 , 2.01841e-05);
        	jointPosUp[125] = new JointPosition(-6.22774e-05 , 0.440924 , 6.16332e-05 , -1.14803 , -9.01149e-05 , 1.55694 , 2.02475e-05);
        	jointPosUp[126] = new JointPosition(-6.23547e-05 , 0.440019 , 6.16349e-05 , -1.15022 , -9.0067e-05 , 1.55565 , 2.03108e-05);
        	jointPosUp[127] = new JointPosition(-6.24318e-05 , 0.439116 , 6.16364e-05 , -1.15241 , -9.00193e-05 , 1.55436 , 2.03738e-05);
        	jointPosUp[128] = new JointPosition(-6.25087e-05 , 0.438215 , 6.16378e-05 , -1.1546 , -8.99717e-05 , 1.55308 , 2.04365e-05);
        	jointPosUp[129] = new JointPosition(-6.25854e-05 , 0.437316 , 6.16392e-05 , -1.15678 , -8.99244e-05 , 1.55179 , 2.0499e-05);
        	jointPosUp[130] = new JointPosition(-6.26619e-05 , 0.436419 , 6.16404e-05 , -1.15896 , -8.98773e-05 , 1.55051 , 2.05613e-05);
        	jointPosUp[131] = new JointPosition(-6.27381e-05 , 0.435525 , 6.16415e-05 , -1.16114 , -8.98303e-05 , 1.54923 , 2.06234e-05);
        	jointPosUp[132] = new JointPosition(-6.28142e-05 , 0.434632 , 6.16426e-05 , -1.16331 , -8.97835e-05 , 1.54795 , 2.06852e-05);
        	jointPosUp[133] = new JointPosition(-6.289e-05 , 0.433741 , 6.16435e-05 , -1.16548 , -8.9737e-05 , 1.54667 , 2.07467e-05);
        	jointPosUp[134] = new JointPosition(-6.29656e-05 , 0.432853 , 6.16444e-05 , -1.16764 , -8.96906e-05 , 1.5454 , 2.08081e-05);
        	jointPosUp[135] = new JointPosition(-6.3041e-05 , 0.431966 , 6.16452e-05 , -1.1698 , -8.96444e-05 , 1.54412 , 2.08692e-05);
        	jointPosUp[136] = new JointPosition(-6.31162e-05 , 0.431082 , 6.16458e-05 , -1.17196 , -8.95984e-05 , 1.54285 , 2.09301e-05);
        	jointPosUp[137] = new JointPosition(-6.31912e-05 , 0.430199 , 6.16464e-05 , -1.17411 , -8.95526e-05 , 1.54158 , 2.09907e-05);
        	jointPosUp[138] = new JointPosition(-6.3266e-05 , 0.429319 , 6.16469e-05 , -1.17626 , -8.9507e-05 , 1.54031 , 2.10511e-05);
        	jointPosUp[139] = new JointPosition(-6.33406e-05 , 0.42844 , 6.16473e-05 , -1.17841 , -8.94616e-05 , 1.53904 , 2.11114e-05);
        	jointPosUp[140] = new JointPosition(-6.34149e-05 , 0.427563 , 6.16476e-05 , -1.18056 , -8.94164e-05 , 1.53777 , 2.11713e-05);
        	jointPosUp[141] = new JointPosition(-6.34891e-05 , 0.426689 , 6.16478e-05 , -1.18269 , -8.93713e-05 , 1.53651 , 2.12311e-05);
        	jointPosUp[142] = new JointPosition(-6.3563e-05 , 0.425816 , 6.16479e-05 , -1.18483 , -8.93265e-05 , 1.53524 , 2.12907e-05);
        	jointPosUp[143] = new JointPosition(-6.36368e-05 , 0.424945 , 6.1648e-05 , -1.18696 , -8.92818e-05 , 1.53398 , 2.135e-05);
        	jointPosUp[144] = new JointPosition(-6.37103e-05 , 0.424076 , 6.16479e-05 , -1.18909 , -8.92373e-05 , 1.53272 , 2.14091e-05);
        	jointPosUp[145] = new JointPosition(-6.37837e-05 , 0.42321 , 6.16478e-05 , -1.19122 , -8.9193e-05 , 1.53146 , 2.1468e-05);
        	jointPosUp[146] = new JointPosition(-6.38568e-05 , 0.422345 , 6.16475e-05 , -1.19334 , -8.91489e-05 , 1.5302 , 2.15267e-05);
        	jointPosUp[147] = new JointPosition(-6.39297e-05 , 0.421482 , 6.16472e-05 , -1.19546 , -8.9105e-05 , 1.52895 , 2.15852e-05);
        	jointPosUp[148] = new JointPosition(-6.40025e-05 , 0.420621 , 6.16468e-05 , -1.19758 , -8.90612e-05 , 1.52769 , 2.16435e-05);
        	jointPosUp[149] = new JointPosition(-6.4075e-05 , 0.419761 , 6.16463e-05 , -1.19969 , -8.90177e-05 , 1.52644 , 2.17016e-05);
        	jointPosUp[150] = new JointPosition(-6.41474e-05 , 0.418904 , 6.16457e-05 , -1.2018 , -8.89743e-05 , 1.52519 , 2.17595e-05);
        	jointPosUp[151] = new JointPosition(-6.42195e-05 , 0.418049 , 6.16451e-05 , -1.20391 , -8.89311e-05 , 1.52394 , 2.18171e-05);
        	jointPosUp[152] = new JointPosition(-6.42914e-05 , 0.417195 , 6.16443e-05 , -1.20601 , -8.88881e-05 , 1.52269 , 2.18746e-05);
        	jointPosUp[153] = new JointPosition(-6.43632e-05 , 0.416343 , 6.16435e-05 , -1.20811 , -8.88453e-05 , 1.52144 , 2.19319e-05);
        	jointPosUp[154] = new JointPosition(-6.44347e-05 , 0.415493 , 6.16426e-05 , -1.2102 , -8.88026e-05 , 1.52019 , 2.19889e-05);
        	jointPosUp[155] = new JointPosition(-6.45061e-05 , 0.414645 , 6.16416e-05 , -1.2123 , -8.87601e-05 , 1.51895 , 2.20458e-05);
        	jointPosUp[156] = new JointPosition(-6.45773e-05 , 0.413799 , 6.16405e-05 , -1.21439 , -8.87178e-05 , 1.51771 , 2.21025e-05);
        	jointPosUp[157] = new JointPosition(-6.46482e-05 , 0.412955 , 6.16393e-05 , -1.21647 , -8.86757e-05 , 1.51646 , 2.2159e-05);
        	jointPosUp[158] = new JointPosition(-6.4719e-05 , 0.412112 , 6.16381e-05 , -1.21856 , -8.86338e-05 , 1.51522 , 2.22153e-05);
        	jointPosUp[159] = new JointPosition(-6.47896e-05 , 0.411272 , 6.16367e-05 , -1.22064 , -8.8592e-05 , 1.51398 , 2.22714e-05);
        	jointPosUp[160] = new JointPosition(-6.486e-05 , 0.410433 , 6.16353e-05 , -1.22271 , -8.85505e-05 , 1.51275 , 2.23273e-05);
        	jointPosUp[161] = new JointPosition(-6.49302e-05 , 0.409596 , 6.16338e-05 , -1.22479 , -8.85091e-05 , 1.51151 , 2.2383e-05);
        	jointPosUp[162] = new JointPosition(-6.50002e-05 , 0.408761 , 6.16323e-05 , -1.22686 , -8.84678e-05 , 1.51027 , 2.24386e-05);
        	jointPosUp[163] = new JointPosition(-6.507e-05 , 0.407927 , 6.16306e-05 , -1.22892 , -8.84268e-05 , 1.50904 , 2.24939e-05);
        	jointPosUp[164] = new JointPosition(-6.51397e-05 , 0.407095 , 6.16289e-05 , -1.23099 , -8.83859e-05 , 1.50781 , 2.25491e-05);
        	jointPosUp[165] = new JointPosition(-6.52091e-05 , 0.406265 , 6.16271e-05 , -1.23305 , -8.83452e-05 , 1.50658 , 2.26041e-05);
        	jointPosUp[166] = new JointPosition(-6.52784e-05 , 0.405437 , 6.16252e-05 , -1.23511 , -8.83047e-05 , 1.50535 , 2.26589e-05);
        	jointPosUp[167] = new JointPosition(-6.53475e-05 , 0.404611 , 6.16232e-05 , -1.23716 , -8.82644e-05 , 1.50412 , 2.27135e-05);
        	jointPosUp[168] = new JointPosition(-6.54164e-05 , 0.403786 , 6.16211e-05 , -1.23922 , -8.82242e-05 , 1.50289 , 2.2768e-05);
        	jointPosUp[169] = new JointPosition(-6.54851e-05 , 0.402963 , 6.1619e-05 , -1.24127 , -8.81842e-05 , 1.50166 , 2.28223e-05);
        	jointPosUp[170] = new JointPosition(-6.55536e-05 , 0.402142 , 6.16168e-05 , -1.24331 , -8.81444e-05 , 1.50044 , 2.28764e-05);
        	jointPosUp[171] = new JointPosition(-6.56219e-05 , 0.401323 , 6.16145e-05 , -1.24535 , -8.81047e-05 , 1.49921 , 2.29303e-05);
        	jointPosUp[172] = new JointPosition(-6.56901e-05 , 0.400505 , 6.16122e-05 , -1.24739 , -8.80652e-05 , 1.49799 , 2.29841e-05);
        	jointPosUp[173] = new JointPosition(-6.57581e-05 , 0.399689 , 6.16097e-05 , -1.24943 , -8.80259e-05 , 1.49677 , 2.30377e-05);
        	jointPosUp[174] = new JointPosition(-6.58259e-05 , 0.398875 , 6.16072e-05 , -1.25147 , -8.79868e-05 , 1.49555 , 2.30911e-05);
        	jointPosUp[175] = new JointPosition(-6.58935e-05 , 0.398063 , 6.16046e-05 , -1.2535 , -8.79478e-05 , 1.49433 , 2.31443e-05);
        	jointPosUp[176] = new JointPosition(-6.59609e-05 , 0.397252 , 6.1602e-05 , -1.25553 , -8.79091e-05 , 1.49311 , 2.31974e-05);
        	jointPosUp[177] = new JointPosition(-6.60282e-05 , 0.396443 , 6.15992e-05 , -1.25755 , -8.78704e-05 , 1.4919 , 2.32503e-05);
        	jointPosUp[178] = new JointPosition(-6.60952e-05 , 0.395635 , 6.15964e-05 , -1.25957 , -8.7832e-05 , 1.49068 , 2.33031e-05);
        	jointPosUp[179] = new JointPosition(-6.61621e-05 , 0.39483 , 6.15935e-05 , -1.26159 , -8.77937e-05 , 1.48947 , 2.33557e-05);
        	jointPosUp[180] = new JointPosition(-6.62289e-05 , 0.394026 , 6.15906e-05 , -1.26361 , -8.77556e-05 , 1.48826 , 2.34081e-05);
        	jointPosUp[181] = new JointPosition(-6.62954e-05 , 0.393223 , 6.15875e-05 , -1.26562 , -8.77176e-05 , 1.48704 , 2.34603e-05);
        	jointPosUp[182] = new JointPosition(-6.63618e-05 , 0.392423 , 6.15844e-05 , -1.26764 , -8.76799e-05 , 1.48583 , 2.35125e-05);
        	jointPosUp[183] = new JointPosition(-6.6428e-05 , 0.391623 , 6.15813e-05 , -1.26964 , -8.76423e-05 , 1.48462 , 2.35644e-05);
        	jointPosUp[184] = new JointPosition(-6.6494e-05 , 0.390826 , 6.1578e-05 , -1.27165 , -8.76048e-05 , 1.48342 , 2.36162e-05);
        	jointPosUp[185] = new JointPosition(-6.65598e-05 , 0.39003 , 6.15747e-05 , -1.27365 , -8.75676e-05 , 1.48221 , 2.36678e-05);
        	jointPosUp[186] = new JointPosition(-6.66255e-05 , 0.389236 , 6.15713e-05 , -1.27565 , -8.75305e-05 , 1.481 , 2.37193e-05);
        	jointPosUp[187] = new JointPosition(-6.6691e-05 , 0.388444 , 6.15678e-05 , -1.27765 , -8.74935e-05 , 1.4798 , 2.37706e-05);
        	jointPosUp[188] = new JointPosition(-6.67563e-05 , 0.387653 , 6.15643e-05 , -1.27964 , -8.74568e-05 , 1.47859 , 2.38218e-05);
        	jointPosUp[189] = new JointPosition(-6.68215e-05 , 0.386864 , 6.15607e-05 , -1.28164 , -8.74202e-05 , 1.47739 , 2.38728e-05);
        	jointPosUp[190] = new JointPosition(-6.68864e-05 , 0.386076 , 6.1557e-05 , -1.28362 , -8.73837e-05 , 1.47619 , 2.39237e-05);
        	jointPosUp[191] = new JointPosition(-6.69513e-05 , 0.38529 , 6.15533e-05 , -1.28561 , -8.73475e-05 , 1.47499 , 2.39744e-05);
        	jointPosUp[192] = new JointPosition(-6.70159e-05 , 0.384506 , 6.15494e-05 , -1.28759 , -8.73113e-05 , 1.47379 , 2.4025e-05);
        	jointPosUp[193] = new JointPosition(-6.70804e-05 , 0.383723 , 6.15456e-05 , -1.28958 , -8.72754e-05 , 1.47259 , 2.40755e-05);
        	jointPosUp[194] = new JointPosition(-6.71447e-05 , 0.382942 , 6.15416e-05 , -1.29155 , -8.72396e-05 , 1.4714 , 2.41258e-05);
        	jointPosUp[195] = new JointPosition(-6.72088e-05 , 0.382162 , 6.15376e-05 , -1.29353 , -8.7204e-05 , 1.4702 , 2.41759e-05);
        	jointPosUp[196] = new JointPosition(-6.72728e-05 , 0.381384 , 6.15335e-05 , -1.2955 , -8.71686e-05 , 1.46901 , 2.42259e-05);
        	jointPosUp[197] = new JointPosition(-6.73366e-05 , 0.380608 , 6.15293e-05 , -1.29747 , -8.71333e-05 , 1.46781 , 2.42758e-05);
        	jointPosUp[198] = new JointPosition(-6.74002e-05 , 0.379833 , 6.15251e-05 , -1.29944 , -8.70982e-05 , 1.46662 , 2.43255e-05);
        	jointPosUp[199] = new JointPosition(-6.74637e-05 , 0.37906 , 6.15208e-05 , -1.3014 , -8.70632e-05 , 1.46543 , 2.43751e-05);
        	jointPosUp[200] = new JointPosition(-6.7527e-05 , 0.378288 , 6.15164e-05 , -1.30337 , -8.70284e-05 , 1.46424 , 2.44245e-05);
        	jointPosUp[201] = new JointPosition(-6.75901e-05 , 0.377518 , 6.1512e-05 , -1.30533 , -8.69938e-05 , 1.46305 , 2.44738e-05);
        	jointPosUp[202] = new JointPosition(-6.76531e-05 , 0.37675 , 6.15075e-05 , -1.30728 , -8.69593e-05 , 1.46186 , 2.4523e-05);
        	jointPosUp[203] = new JointPosition(-6.77159e-05 , 0.375983 , 6.1503e-05 , -1.30924 , -8.6925e-05 , 1.46067 , 2.4572e-05);
        	jointPosUp[204] = new JointPosition(-6.77785e-05 , 0.375217 , 6.14983e-05 , -1.31119 , -8.68909e-05 , 1.45948 , 2.46209e-05);
        	jointPosUp[205] = new JointPosition(-6.7841e-05 , 0.374453 , 6.14936e-05 , -1.31314 , -8.68569e-05 , 1.4583 , 2.46697e-05);
        	jointPosUp[206] = new JointPosition(-6.79033e-05 , 0.373691 , 6.14889e-05 , -1.31509 , -8.68231e-05 , 1.45711 , 2.47184e-05);
        	jointPosUp[207] = new JointPosition(-6.79655e-05 , 0.37293 , 6.14841e-05 , -1.31703 , -8.67894e-05 , 1.45593 , 2.47669e-05);
        	jointPosUp[208] = new JointPosition(-6.80275e-05 , 0.372171 , 6.14792e-05 , -1.31897 , -8.67559e-05 , 1.45475 , 2.48153e-05);
        	jointPosUp[209] = new JointPosition(-6.80893e-05 , 0.371413 , 6.14742e-05 , -1.32091 , -8.67225e-05 , 1.45357 , 2.48635e-05);
        	jointPosUp[210] = new JointPosition(-6.8151e-05 , 0.370656 , 6.14692e-05 , -1.32285 , -8.66894e-05 , 1.45239 , 2.49116e-05);
        	jointPosUp[211] = new JointPosition(-6.82125e-05 , 0.369902 , 6.14641e-05 , -1.32478 , -8.66563e-05 , 1.45121 , 2.49596e-05);
        	jointPosUp[212] = new JointPosition(-6.82739e-05 , 0.369148 , 6.1459e-05 , -1.32672 , -8.66235e-05 , 1.45003 , 2.50075e-05);
        	jointPosUp[213] = new JointPosition(-6.83351e-05 , 0.368397 , 6.14538e-05 , -1.32865 , -8.65908e-05 , 1.44885 , 2.50553e-05);
        	jointPosUp[214] = new JointPosition(-6.83962e-05 , 0.367646 , 6.14485e-05 , -1.33057 , -8.65582e-05 , 1.44767 , 2.51029e-05);
        	jointPosUp[215] = new JointPosition(-6.8457e-05 , 0.366898 , 6.14432e-05 , -1.3325 , -8.65258e-05 , 1.4465 , 2.51504e-05);
        	jointPosUp[216] = new JointPosition(-6.85178e-05 , 0.36615 , 6.14378e-05 , -1.33442 , -8.64936e-05 , 1.44532 , 2.51978e-05);
        	jointPosUp[217] = new JointPosition(-6.85784e-05 , 0.365405 , 6.14323e-05 , -1.33634 , -8.64615e-05 , 1.44415 , 2.52451e-05);
        	jointPosUp[218] = new JointPosition(-6.86388e-05 , 0.36466 , 6.14268e-05 , -1.33826 , -8.64296e-05 , 1.44298 , 2.52922e-05);
        	jointPosUp[219] = new JointPosition(-6.8699e-05 , 0.363917 , 6.14212e-05 , -1.34017 , -8.63979e-05 , 1.4418 , 2.53392e-05);
        	jointPosUp[220] = new JointPosition(-6.87592e-05 , 0.363176 , 6.14156e-05 , -1.34208 , -8.63663e-05 , 1.44063 , 2.53861e-05);
        	jointPosUp[221] = new JointPosition(-6.88191e-05 , 0.362436 , 6.14099e-05 , -1.34399 , -8.63348e-05 , 1.43946 , 2.54329e-05);
        	jointPosUp[222] = new JointPosition(-6.88789e-05 , 0.361698 , 6.14041e-05 , -1.3459 , -8.63036e-05 , 1.43829 , 2.54796e-05);
        	jointPosUp[223] = new JointPosition(-6.89386e-05 , 0.360961 , 6.13983e-05 , -1.34781 , -8.62724e-05 , 1.43712 , 2.55262e-05);
        	jointPosUp[224] = new JointPosition(-6.89981e-05 , 0.360225 , 6.13924e-05 , -1.34971 , -8.62415e-05 , 1.43596 , 2.55727e-05);
        	jointPosUp[225] = new JointPosition(-6.90574e-05 , 0.359491 , 6.13865e-05 , -1.35161 , -8.62107e-05 , 1.43479 , 2.5619e-05);
        	jointPosUp[226] = new JointPosition(-6.91166e-05 , 0.358759 , 6.13805e-05 , -1.35351 , -8.618e-05 , 1.43362 , 2.56652e-05);
        	jointPosUp[227] = new JointPosition(-6.91756e-05 , 0.358027 , 6.13744e-05 , -1.3554 , -8.61495e-05 , 1.43246 , 2.57114e-05);
        	jointPosUp[228] = new JointPosition(-6.92345e-05 , 0.357298 , 6.13683e-05 , -1.3573 , -8.61192e-05 , 1.4313 , 2.57574e-05);
        	jointPosUp[229] = new JointPosition(-6.92933e-05 , 0.356569 , 6.13621e-05 , -1.35919 , -8.6089e-05 , 1.43013 , 2.58033e-05);
        	jointPosUp[230] = new JointPosition(-6.93519e-05 , 0.355842 , 6.13558e-05 , -1.36108 , -8.60589e-05 , 1.42897 , 2.58491e-05);
        	jointPosUp[231] = new JointPosition(-6.94103e-05 , 0.355117 , 6.13495e-05 , -1.36297 , -8.60291e-05 , 1.42781 , 2.58948e-05);
        	jointPosUp[232] = new JointPosition(-6.94686e-05 , 0.354393 , 6.13432e-05 , -1.36485 , -8.59993e-05 , 1.42665 , 2.59404e-05);
        	jointPosUp[233] = new JointPosition(-6.95268e-05 , 0.35367 , 6.13368e-05 , -1.36673 , -8.59698e-05 , 1.42549 , 2.59859e-05);
        	jointPosUp[234] = new JointPosition(-6.95848e-05 , 0.352949 , 6.13303e-05 , -1.36861 , -8.59404e-05 , 1.42433 , 2.60312e-05);
        	jointPosUp[235] = new JointPosition(-6.96426e-05 , 0.352229 , 6.13238e-05 , -1.37049 , -8.59111e-05 , 1.42317 , 2.60765e-05);
        	jointPosUp[236] = new JointPosition(-6.97003e-05 , 0.351511 , 6.13172e-05 , -1.37237 , -8.5882e-05 , 1.42201 , 2.61217e-05);
        	jointPosUp[237] = new JointPosition(-6.97579e-05 , 0.350794 , 6.13105e-05 , -1.37424 , -8.5853e-05 , 1.42086 , 2.61668e-05);
        	jointPosUp[238] = new JointPosition(-6.98153e-05 , 0.350078 , 6.13038e-05 , -1.37611 , -8.58242e-05 , 1.4197 , 2.62117e-05);
        	jointPosUp[239] = new JointPosition(-6.98726e-05 , 0.349364 , 6.1297e-05 , -1.37798 , -8.57956e-05 , 1.41855 , 2.62566e-05);
        	jointPosUp[240] = new JointPosition(-6.99297e-05 , 0.348651 , 6.12902e-05 , -1.37985 , -8.57671e-05 , 1.41739 , 2.63014e-05);
        	jointPosUp[241] = new JointPosition(-6.99867e-05 , 0.34794 , 6.12833e-05 , -1.38171 , -8.57388e-05 , 1.41624 , 2.63461e-05);
        	jointPosUp[242] = new JointPosition(-7.00435e-05 , 0.34723 , 6.12764e-05 , -1.38357 , -8.57106e-05 , 1.41509 , 2.63907e-05);
        	jointPosUp[243] = new JointPosition(-7.01002e-05 , 0.346521 , 6.12694e-05 , -1.38543 , -8.56826e-05 , 1.41394 , 2.64352e-05);
        	jointPosUp[244] = new JointPosition(-7.01568e-05 , 0.345814 , 6.12624e-05 , -1.38729 , -8.56547e-05 , 1.41278 , 2.64796e-05);
        	jointPosUp[245] = new JointPosition(-7.02132e-05 , 0.345108 , 6.12553e-05 , -1.38915 , -8.5627e-05 , 1.41163 , 2.65239e-05);
        	jointPosUp[246] = new JointPosition(-7.02694e-05 , 0.344404 , 6.12481e-05 , -1.391 , -8.55994e-05 , 1.41048 , 2.65681e-05);
        	jointPosUp[247] = new JointPosition(-7.03256e-05 , 0.343701 , 6.12409e-05 , -1.39285 , -8.5572e-05 , 1.40934 , 2.66122e-05);
        	jointPosUp[248] = new JointPosition(-7.03815e-05 , 0.342999 , 6.12337e-05 , -1.3947 , -8.55447e-05 , 1.40819 , 2.66563e-05);
        	jointPosUp[249] = new JointPosition(-7.04374e-05 , 0.342299 , 6.12263e-05 , -1.39655 , -8.55176e-05 , 1.40704 , 2.67002e-05);
        	jointPosUp[250] = new JointPosition(-7.04931e-05 , 0.341599 , 6.1219e-05 , -1.3984 , -8.54906e-05 , 1.4059 , 2.6744e-05);
        	jointPosUp[251] = new JointPosition(-7.05487e-05 , 0.340902 , 6.12115e-05 , -1.40024 , -8.54638e-05 , 1.40475 , 2.67878e-05);
        	jointPosUp[252] = new JointPosition(-7.06041e-05 , 0.340205 , 6.12041e-05 , -1.40208 , -8.54371e-05 , 1.4036 , 2.68315e-05);
        	jointPosUp[253] = new JointPosition(-7.06594e-05 , 0.33951 , 6.11965e-05 , -1.40392 , -8.54106e-05 , 1.40246 , 2.68751e-05);
        	jointPosUp[254] = new JointPosition(-7.07145e-05 , 0.338817 , 6.11889e-05 , -1.40576 , -8.53843e-05 , 1.40132 , 2.69186e-05);
        	jointPosUp[255] = new JointPosition(-7.07695e-05 , 0.338125 , 6.11813e-05 , -1.40759 , -8.53581e-05 , 1.40017 , 2.6962e-05);
        	jointPosUp[256] = new JointPosition(-7.08244e-05 , 0.337434 , 6.11736e-05 , -1.40942 , -8.5332e-05 , 1.39903 , 2.70053e-05);
        	jointPosUp[257] = new JointPosition(-7.08791e-05 , 0.336744 , 6.11658e-05 , -1.41126 , -8.53061e-05 , 1.39789 , 2.70486e-05);
        	jointPosUp[258] = new JointPosition(-7.09337e-05 , 0.336056 , 6.1158e-05 , -1.41308 , -8.52803e-05 , 1.39675 , 2.70918e-05);
        	jointPosUp[259] = new JointPosition(-7.09882e-05 , 0.335369 , 6.11502e-05 , -1.41491 , -8.52547e-05 , 1.39561 , 2.71349e-05);
        	jointPosUp[260] = new JointPosition(-7.10425e-05 , 0.334683 , 6.11423e-05 , -1.41674 , -8.52293e-05 , 1.39447 , 2.71779e-05);
        	jointPosUp[261] = new JointPosition(-7.10967e-05 , 0.333999 , 6.11343e-05 , -1.41856 , -8.5204e-05 , 1.39333 , 2.72208e-05);
        	jointPosUp[262] = new JointPosition(-7.11507e-05 , 0.333316 , 6.11263e-05 , -1.42038 , -8.51788e-05 , 1.3922 , 2.72637e-05);
        	jointPosUp[263] = new JointPosition(-7.12046e-05 , 0.332634 , 6.11182e-05 , -1.4222 , -8.51538e-05 , 1.39106 , 2.73064e-05);
        	jointPosUp[264] = new JointPosition(-7.12584e-05 , 0.331954 , 6.11101e-05 , -1.42401 , -8.5129e-05 , 1.38992 , 2.73491e-05);
        	jointPosUp[265] = new JointPosition(-7.13121e-05 , 0.331274 , 6.1102e-05 , -1.42583 , -8.51043e-05 , 1.38879 , 2.73918e-05);
        	jointPosUp[266] = new JointPosition(-7.13656e-05 , 0.330597 , 6.10937e-05 , -1.42764 , -8.50797e-05 , 1.38765 , 2.74343e-05);
        	jointPosUp[267] = new JointPosition(-7.1419e-05 , 0.32992 , 6.10855e-05 , -1.42945 , -8.50553e-05 , 1.38652 , 2.74768e-05);
        	jointPosUp[268] = new JointPosition(-7.14722e-05 , 0.329245 , 6.10771e-05 , -1.43126 , -8.50311e-05 , 1.38539 , 2.75192e-05);
        	jointPosUp[269] = new JointPosition(-7.15253e-05 , 0.328571 , 6.10688e-05 , -1.43307 , -8.5007e-05 , 1.38425 , 2.75615e-05);
        	jointPosUp[270] = new JointPosition(-7.15783e-05 , 0.327899 , 6.10604e-05 , -1.43487 , -8.4983e-05 , 1.38312 , 2.76038e-05);
        	jointPosUp[271] = new JointPosition(-7.16312e-05 , 0.327227 , 6.10519e-05 , -1.43668 , -8.49592e-05 , 1.38199 , 2.76459e-05);
        	jointPosUp[272] = new JointPosition(-7.16839e-05 , 0.326557 , 6.10434e-05 , -1.43848 , -8.49356e-05 , 1.38086 , 2.7688e-05);
        	jointPosUp[273] = new JointPosition(-7.17365e-05 , 0.325889 , 6.10348e-05 , -1.44027 , -8.49121e-05 , 1.37973 , 2.77301e-05);
        	jointPosUp[274] = new JointPosition(-7.1789e-05 , 0.325221 , 6.10262e-05 , -1.44207 , -8.48887e-05 , 1.3786 , 2.77721e-05);
        	jointPosUp[275] = new JointPosition(-7.18413e-05 , 0.324555 , 6.10175e-05 , -1.44387 , -8.48655e-05 , 1.37747 , 2.7814e-05);
        	jointPosUp[276] = new JointPosition(-7.18935e-05 , 0.32389 , 6.10088e-05 , -1.44566 , -8.48424e-05 , 1.37634 , 2.78558e-05);
        	jointPosUp[277] = new JointPosition(-7.19456e-05 , 0.323227 , 6.1e-05 , -1.44745 , -8.48195e-05 , 1.37521 , 2.78976e-05);
        	jointPosUp[278] = new JointPosition(-7.19975e-05 , 0.322565 , 6.09912e-05 , -1.44924 , -8.47968e-05 , 1.37409 , 2.79393e-05);
        	jointPosUp[279] = new JointPosition(-7.20493e-05 , 0.321903 , 6.09823e-05 , -1.45103 , -8.47742e-05 , 1.37296 , 2.79809e-05);
        	jointPosUp[280] = new JointPosition(-7.2101e-05 , 0.321244 , 6.09734e-05 , -1.45281 , -8.47517e-05 , 1.37183 , 2.80225e-05);
        	jointPosUp[281] = new JointPosition(-7.21526e-05 , 0.320585 , 6.09644e-05 , -1.4546 , -8.47294e-05 , 1.37071 , 2.8064e-05);
        	jointPosUp[282] = new JointPosition(-7.2204e-05 , 0.319928 , 6.09554e-05 , -1.45638 , -8.47072e-05 , 1.36958 , 2.81054e-05);
        	jointPosUp[283] = new JointPosition(-7.22553e-05 , 0.319272 , 6.09463e-05 , -1.45816 , -8.46852e-05 , 1.36846 , 2.81468e-05);
        	jointPosUp[284] = new JointPosition(-7.23065e-05 , 0.318617 , 6.09372e-05 , -1.45994 , -8.46633e-05 , 1.36734 , 2.81881e-05);
        	jointPosUp[285] = new JointPosition(-7.23575e-05 , 0.317964 , 6.0928e-05 , -1.46171 , -8.46416e-05 , 1.36621 , 2.82294e-05);
        	jointPosUp[286] = new JointPosition(-7.24085e-05 , 0.317312 , 6.09188e-05 , -1.46349 , -8.462e-05 , 1.36509 , 2.82706e-05);
        	jointPosUp[287] = new JointPosition(-7.24593e-05 , 0.316661 , 6.09096e-05 , -1.46526 , -8.45986e-05 , 1.36397 , 2.83117e-05);
        	jointPosUp[288] = new JointPosition(-7.251e-05 , 0.316011 , 6.09003e-05 , -1.46703 , -8.45773e-05 , 1.36285 , 2.83528e-05);
        	jointPosUp[289] = new JointPosition(-7.25605e-05 , 0.315363 , 6.08909e-05 , -1.4688 , -8.45562e-05 , 1.36173 , 2.83938e-05);
        	jointPosUp[290] = new JointPosition(-7.26109e-05 , 0.314715 , 6.08815e-05 , -1.47057 , -8.45352e-05 , 1.36061 , 2.84348e-05);
        	jointPosUp[291] = new JointPosition(-7.26612e-05 , 0.31407 , 6.08721e-05 , -1.47233 , -8.45144e-05 , 1.35949 , 2.84757e-05);
        	jointPosUp[292] = new JointPosition(-7.27114e-05 , 0.313425 , 6.08626e-05 , -1.4741 , -8.44937e-05 , 1.35837 , 2.85166e-05);
        	jointPosUp[293] = new JointPosition(-7.27615e-05 , 0.312781 , 6.0853e-05 , -1.47586 , -8.44732e-05 , 1.35725 , 2.85574e-05);
        	jointPosUp[294] = new JointPosition(-7.28114e-05 , 0.312139 , 6.08434e-05 , -1.47762 , -8.44528e-05 , 1.35613 , 2.85981e-05);
        	jointPosUp[295] = new JointPosition(-7.28612e-05 , 0.311498 , 6.08338e-05 , -1.47938 , -8.44325e-05 , 1.35502 , 2.86388e-05);
        	jointPosUp[296] = new JointPosition(-7.29109e-05 , 0.310858 , 6.08241e-05 , -1.48113 , -8.44125e-05 , 1.3539 , 2.86795e-05);
        	jointPosUp[297] = new JointPosition(-7.29605e-05 , 0.31022 , 6.08144e-05 , -1.48289 , -8.43925e-05 , 1.35278 , 2.87201e-05);
        	jointPosUp[298] = new JointPosition(-7.30099e-05 , 0.309582 , 6.08046e-05 , -1.48464 , -8.43727e-05 , 1.35167 , 2.87606e-05);
        	jointPosUp[299] = new JointPosition(-7.30593e-05 , 0.308946 , 6.07948e-05 , -1.48639 , -8.43531e-05 , 1.35055 , 2.88011e-05);
        	jointPosUp[300] = new JointPosition(-7.31085e-05 , 0.308311 , 6.07849e-05 , -1.48814 , -8.43336e-05 , 1.34944 , 2.88415e-05);
        	jointPosUp[301] = new JointPosition(-7.31575e-05 , 0.307678 , 6.0775e-05 , -1.48989 , -8.43142e-05 , 1.34832 , 2.88819e-05);
        	jointPosUp[302] = new JointPosition(-7.32065e-05 , 0.307045 , 6.07651e-05 , -1.49164 , -8.4295e-05 , 1.34721 , 2.89222e-05);
        	jointPosUp[303] = new JointPosition(-7.32554e-05 , 0.306414 , 6.07551e-05 , -1.49338 , -8.42759e-05 , 1.3461 , 2.89625e-05);
        	jointPosUp[304] = new JointPosition(-7.33041e-05 , 0.305784 , 6.0745e-05 , -1.49512 , -8.4257e-05 , 1.34499 , 2.90028e-05);
        	jointPosUp[305] = new JointPosition(-7.33527e-05 , 0.305155 , 6.07349e-05 , -1.49686 , -8.42383e-05 , 1.34387 , 2.9043e-05);
        	jointPosUp[306] = new JointPosition(-7.34012e-05 , 0.304528 , 6.07248e-05 , -1.4986 , -8.42197e-05 , 1.34276 , 2.90831e-05);
        	jointPosUp[307] = new JointPosition(-7.34496e-05 , 0.303901 , 6.07146e-05 , -1.50034 , -8.42012e-05 , 1.34165 , 2.91233e-05);
        	jointPosUp[308] = new JointPosition(-7.34978e-05 , 0.303276 , 6.07044e-05 , -1.50207 , -8.41829e-05 , 1.34054 , 2.91633e-05);
        	jointPosUp[309] = new JointPosition(-7.35459e-05 , 0.302652 , 6.06941e-05 , -1.50381 , -8.41647e-05 , 1.33943 , 2.92033e-05);
        	jointPosUp[310] = new JointPosition(-7.3594e-05 , 0.302029 , 6.06838e-05 , -1.50554 , -8.41467e-05 , 1.33832 , 2.92433e-05);
        	jointPosUp[311] = new JointPosition(-7.36419e-05 , 0.301408 , 6.06734e-05 , -1.50727 , -8.41288e-05 , 1.33721 , 2.92833e-05);
        	jointPosUp[312] = new JointPosition(-7.36896e-05 , 0.300787 , 6.0663e-05 , -1.509 , -8.41111e-05 , 1.3361 , 2.93232e-05);
        	jointPosUp[313] = new JointPosition(-7.37373e-05 , 0.300168 , 6.06525e-05 , -1.51073 , -8.40935e-05 , 1.335 , 2.9363e-05);
        	jointPosUp[314] = new JointPosition(-7.37849e-05 , 0.29955 , 6.0642e-05 , -1.51245 , -8.4076e-05 , 1.33389 , 2.94028e-05);
        	jointPosUp[315] = new JointPosition(-7.38323e-05 , 0.298933 , 6.06315e-05 , -1.51418 , -8.40588e-05 , 1.33278 , 2.94426e-05);
        	jointPosUp[316] = new JointPosition(-7.38796e-05 , 0.298318 , 6.06209e-05 , -1.5159 , -8.40416e-05 , 1.33168 , 2.94824e-05);
        	jointPosUp[317] = new JointPosition(-7.39268e-05 , 0.297703 , 6.06103e-05 , -1.51762 , -8.40246e-05 , 1.33057 , 2.95221e-05);
        	jointPosUp[318] = new JointPosition(-7.39739e-05 , 0.29709 , 6.05996e-05 , -1.51934 , -8.40078e-05 , 1.32946 , 2.95617e-05);
        	jointPosUp[319] = new JointPosition(-7.40209e-05 , 0.296478 , 6.05889e-05 , -1.52105 , -8.39911e-05 , 1.32836 , 2.96013e-05);
        	jointPosUp[320] = new JointPosition(-7.40678e-05 , 0.295867 , 6.05781e-05 , -1.52277 , -8.39745e-05 , 1.32725 , 2.96409e-05);
        	jointPosUp[321] = new JointPosition(-7.41145e-05 , 0.295258 , 6.05673e-05 , -1.52448 , -8.39581e-05 , 1.32615 , 2.96805e-05);
        	jointPosUp[322] = new JointPosition(-7.41612e-05 , 0.294649 , 6.05565e-05 , -1.5262 , -8.39419e-05 , 1.32505 , 2.972e-05);
        	jointPosUp[323] = new JointPosition(-7.42077e-05 , 0.294042 , 6.05456e-05 , -1.52791 , -8.39258e-05 , 1.32394 , 2.97595e-05);
        	jointPosUp[324] = new JointPosition(-7.42541e-05 , 0.293436 , 6.05347e-05 , -1.52962 , -8.39098e-05 , 1.32284 , 2.9799e-05);
        	jointPosUp[325] = new JointPosition(-7.43004e-05 , 0.292831 , 6.05237e-05 , -1.53132 , -8.3894e-05 , 1.32174 , 2.98384e-05);
        	jointPosUp[326] = new JointPosition(-7.43466e-05 , 0.292227 , 6.05127e-05 , -1.53303 , -8.38783e-05 , 1.32064 , 2.98778e-05);
        	jointPosUp[327] = new JointPosition(-7.43926e-05 , 0.291624 , 6.05016e-05 , -1.53473 , -8.38628e-05 , 1.31953 , 2.99171e-05);
        	jointPosUp[328] = new JointPosition(-7.44386e-05 , 0.291023 , 6.04905e-05 , -1.53644 , -8.38475e-05 , 1.31843 , 2.99565e-05);
        	jointPosUp[329] = new JointPosition(-7.44845e-05 , 0.290422 , 6.04794e-05 , -1.53814 , -8.38322e-05 , 1.31733 , 2.99958e-05);
        	jointPosUp[330] = new JointPosition(-7.45302e-05 , 0.289823 , 6.04682e-05 , -1.53984 , -8.38172e-05 , 1.31623 , 3.0035e-05);
        	jointPosUp[331] = new JointPosition(-7.45758e-05 , 0.289225 , 6.0457e-05 , -1.54153 , -8.38022e-05 , 1.31513 , 3.00743e-05);
        	jointPosUp[332] = new JointPosition(-7.46214e-05 , 0.288629 , 6.04457e-05 , -1.54323 , -8.37875e-05 , 1.31403 , 3.01135e-05);
        	jointPosUp[333] = new JointPosition(-7.46668e-05 , 0.288033 , 6.04344e-05 , -1.54493 , -8.37728e-05 , 1.31293 , 3.01527e-05);
        	jointPosUp[334] = new JointPosition(-7.47121e-05 , 0.287438 , 6.0423e-05 , -1.54662 , -8.37583e-05 , 1.31183 , 3.01919e-05);
        	jointPosUp[335] = new JointPosition(-7.47573e-05 , 0.286845 , 6.04116e-05 , -1.54831 , -8.3744e-05 , 1.31074 , 3.0231e-05);
        	jointPosUp[336] = new JointPosition(-7.48024e-05 , 0.286253 , 6.04002e-05 , -1.55 , -8.37298e-05 , 1.30964 , 3.02701e-05);
        	jointPosUp[337] = new JointPosition(-7.48473e-05 , 0.285662 , 6.03887e-05 , -1.55169 , -8.37158e-05 , 1.30854 , 3.03092e-05);
        	jointPosUp[338] = new JointPosition(-7.48922e-05 , 0.285072 , 6.03772e-05 , -1.55338 , -8.37019e-05 , 1.30744 , 3.03483e-05);
        	jointPosUp[339] = new JointPosition(-7.4937e-05 , 0.284483 , 6.03656e-05 , -1.55506 , -8.36882e-05 , 1.30635 , 3.03873e-05);
        	jointPosUp[340] = new JointPosition(-7.49816e-05 , 0.283896 , 6.0354e-05 , -1.55675 , -8.36746e-05 , 1.30525 , 3.04263e-05);
        	jointPosUp[341] = new JointPosition(-7.50262e-05 , 0.283309 , 6.03424e-05 , -1.55843 , -8.36611e-05 , 1.30415 , 3.04653e-05);
        	jointPosUp[342] = new JointPosition(-7.50706e-05 , 0.282724 , 6.03307e-05 , -1.56011 , -8.36478e-05 , 1.30306 , 3.05043e-05);
        	jointPosUp[343] = new JointPosition(-7.51149e-05 , 0.28214 , 6.0319e-05 , -1.56179 , -8.36347e-05 , 1.30196 , 3.05432e-05);
        	jointPosUp[344] = new JointPosition(-7.51591e-05 , 0.281557 , 6.03072e-05 , -1.56347 , -8.36217e-05 , 1.30087 , 3.05822e-05);
        	jointPosUp[345] = new JointPosition(-7.52033e-05 , 0.280975 , 6.02954e-05 , -1.56514 , -8.36088e-05 , 1.29977 , 3.06211e-05);
        	jointPosUp[346] = new JointPosition(-7.52473e-05 , 0.280395 , 6.02836e-05 , -1.56682 , -8.35961e-05 , 1.29868 , 3.066e-05);
        	jointPosUp[347] = new JointPosition(-7.52912e-05 , 0.279815 , 6.02717e-05 , -1.56849 , -8.35836e-05 , 1.29759 , 3.06989e-05);
        	jointPosUp[348] = new JointPosition(-7.5335e-05 , 0.279237 , 6.02597e-05 , -1.57016 , -8.35712e-05 , 1.29649 , 3.07377e-05);
        	jointPosUp[349] = new JointPosition(-7.53787e-05 , 0.278659 , 6.02478e-05 , -1.57183 , -8.35589e-05 , 1.2954 , 3.07766e-05);
        	jointPosUp[350] = new JointPosition(-7.54223e-05 , 0.278083 , 6.02358e-05 , -1.5735 , -8.35468e-05 , 1.29431 , 3.08154e-05);
        	jointPosUp[351] = new JointPosition(-7.54658e-05 , 0.277508 , 6.02237e-05 , -1.57517 , -8.35349e-05 , 1.29321 , 3.08542e-05);
        	jointPosUp[352] = new JointPosition(-7.55091e-05 , 0.276934 , 6.02116e-05 , -1.57683 , -8.35231e-05 , 1.29212 , 3.0893e-05);
        	jointPosUp[353] = new JointPosition(-7.55524e-05 , 0.276362 , 6.01995e-05 , -1.5785 , -8.35114e-05 , 1.29103 , 3.09318e-05);
        	jointPosUp[354] = new JointPosition(-7.55956e-05 , 0.27579 , 6.01873e-05 , -1.58016 , -8.34999e-05 , 1.28994 , 3.09706e-05);
        	jointPosUp[355] = new JointPosition(-7.56387e-05 , 0.27522 , 6.01751e-05 , -1.58182 , -8.34885e-05 , 1.28885 , 3.10093e-05);
        	jointPosUp[356] = new JointPosition(-7.56816e-05 , 0.27465 , 6.01629e-05 , -1.58348 , -8.34773e-05 , 1.28776 , 3.10481e-05);
        	jointPosUp[357] = new JointPosition(-7.57245e-05 , 0.274082 , 6.01506e-05 , -1.58514 , -8.34663e-05 , 1.28667 , 3.10868e-05);
        	jointPosUp[358] = new JointPosition(-7.57673e-05 , 0.273515 , 6.01382e-05 , -1.5868 , -8.34554e-05 , 1.28558 , 3.11255e-05);
        	jointPosUp[359] = new JointPosition(-7.58099e-05 , 0.272949 , 6.01259e-05 , -1.58846 , -8.34446e-05 , 1.28449 , 3.11642e-05);
        	jointPosUp[360] = new JointPosition(-7.58525e-05 , 0.272384 , 6.01135e-05 , -1.59011 , -8.3434e-05 , 1.2834 , 3.12029e-05);
        	jointPosUp[361] = new JointPosition(-7.58949e-05 , 0.271821 , 6.0101e-05 , -1.59176 , -8.34235e-05 , 1.28231 , 3.12416e-05);
        	jointPosUp[362] = new JointPosition(-7.59373e-05 , 0.271258 , 6.00885e-05 , -1.59341 , -8.34132e-05 , 1.28122 , 3.12803e-05);
        	jointPosUp[363] = new JointPosition(-7.59796e-05 , 0.270697 , 6.0076e-05 , -1.59506 , -8.34031e-05 , 1.28013 , 3.1319e-05);
        	jointPosUp[364] = new JointPosition(-7.60217e-05 , 0.270136 , 6.00634e-05 , -1.59671 , -8.3393e-05 , 1.27904 , 3.13577e-05);
        	jointPosUp[365] = new JointPosition(-7.60638e-05 , 0.269577 , 6.00508e-05 , -1.59836 , -8.33832e-05 , 1.27795 , 3.13963e-05);
        	jointPosUp[366] = new JointPosition(-7.61057e-05 , 0.269019 , 6.00382e-05 , -1.60001 , -8.33735e-05 , 1.27687 , 3.1435e-05);
        	jointPosUp[367] = new JointPosition(-7.61476e-05 , 0.268462 , 6.00255e-05 , -1.60165 , -8.33639e-05 , 1.27578 , 3.14736e-05);
        	jointPosUp[368] = new JointPosition(-7.61893e-05 , 0.267906 , 6.00128e-05 , -1.60329 , -8.33545e-05 , 1.27469 , 3.15123e-05);
        	jointPosUp[369] = new JointPosition(-7.6231e-05 , 0.267351 , 6e-05 , -1.60493 , -8.33453e-05 , 1.27361 , 3.15509e-05);
        	jointPosUp[370] = new JointPosition(-7.62725e-05 , 0.266798 , 5.99872e-05 , -1.60657 , -8.33361e-05 , 1.27252 , 3.15896e-05);
        	jointPosUp[371] = new JointPosition(-7.6314e-05 , 0.266245 , 5.99744e-05 , -1.60821 , -8.33272e-05 , 1.27143 , 3.16282e-05);
        	jointPosUp[372] = new JointPosition(-7.63554e-05 , 0.265694 , 5.99615e-05 , -1.60985 , -8.33184e-05 , 1.27035 , 3.16669e-05);
        	jointPosUp[373] = new JointPosition(-7.63966e-05 , 0.265144 , 5.99486e-05 , -1.61149 , -8.33097e-05 , 1.26926 , 3.17055e-05);
        	jointPosUp[374] = new JointPosition(-7.64378e-05 , 0.264594 , 5.99357e-05 , -1.61312 , -8.33012e-05 , 1.26818 , 3.17441e-05);
        	jointPosUp[375] = new JointPosition(-7.64788e-05 , 0.264046 , 5.99227e-05 , -1.61475 , -8.32929e-05 , 1.26709 , 3.17828e-05);
        	jointPosUp[376] = new JointPosition(-7.65198e-05 , 0.263499 , 5.99097e-05 , -1.61639 , -8.32847e-05 , 1.26601 , 3.18214e-05);
        	jointPosUp[377] = new JointPosition(-7.65607e-05 , 0.262953 , 5.98966e-05 , -1.61802 , -8.32766e-05 , 1.26492 , 3.186e-05);
        	jointPosUp[378] = new JointPosition(-7.66014e-05 , 0.262409 , 5.98835e-05 , -1.61965 , -8.32687e-05 , 1.26384 , 3.18987e-05);
        	jointPosUp[379] = new JointPosition(-7.66421e-05 , 0.261865 , 5.98704e-05 , -1.62127 , -8.3261e-05 , 1.26275 , 3.19373e-05);
        	jointPosUp[380] = new JointPosition(-7.66827e-05 , 0.261323 , 5.98572e-05 , -1.6229 , -8.32534e-05 , 1.26167 , 3.1976e-05);
        	jointPosUp[381] = new JointPosition(-7.67232e-05 , 0.260781 , 5.9844e-05 , -1.62452 , -8.3246e-05 , 1.26059 , 3.20146e-05);
        	jointPosUp[382] = new JointPosition(-7.67636e-05 , 0.260241 , 5.98307e-05 , -1.62615 , -8.32387e-05 , 1.2595 , 3.20533e-05);
        	jointPosUp[383] = new JointPosition(-7.68039e-05 , 0.259702 , 5.98174e-05 , -1.62777 , -8.32316e-05 , 1.25842 , 3.20919e-05);
        	jointPosUp[384] = new JointPosition(-7.68441e-05 , 0.259164 , 5.98041e-05 , -1.62939 , -8.32246e-05 , 1.25734 , 3.21306e-05);
        	jointPosUp[385] = new JointPosition(-7.68842e-05 , 0.258626 , 5.97907e-05 , -1.63101 , -8.32178e-05 , 1.25625 , 3.21692e-05);
        	jointPosUp[386] = new JointPosition(-7.69242e-05 , 0.258091 , 5.97773e-05 , -1.63263 , -8.32111e-05 , 1.25517 , 3.22079e-05);
        	jointPosUp[387] = new JointPosition(-7.69641e-05 , 0.257556 , 5.97639e-05 , -1.63425 , -8.32046e-05 , 1.25409 , 3.22466e-05);
        	jointPosUp[388] = new JointPosition(-7.70039e-05 , 0.257022 , 5.97504e-05 , -1.63586 , -8.31982e-05 , 1.25301 , 3.22853e-05);
        	jointPosUp[389] = new JointPosition(-7.70436e-05 , 0.256489 , 5.97369e-05 , -1.63748 , -8.3192e-05 , 1.25193 , 3.2324e-05);
        	jointPosUp[390] = new JointPosition(-7.70832e-05 , 0.255958 , 5.97233e-05 , -1.63909 , -8.31859e-05 , 1.25084 , 3.23627e-05);
        	jointPosUp[391] = new JointPosition(-7.71228e-05 , 0.255427 , 5.97097e-05 , -1.6407 , -8.318e-05 , 1.24976 , 3.24014e-05);
        	jointPosUp[392] = new JointPosition(-7.71622e-05 , 0.254898 , 5.96961e-05 , -1.64231 , -8.31743e-05 , 1.24868 , 3.24401e-05);
        	jointPosUp[393] = new JointPosition(-7.72016e-05 , 0.25437 , 5.96824e-05 , -1.64392 , -8.31687e-05 , 1.2476 , 3.24789e-05);
        	jointPosUp[394] = new JointPosition(-7.72408e-05 , 0.253843 , 5.96687e-05 , -1.64553 , -8.31632e-05 , 1.24652 , 3.25176e-05);
        	jointPosUp[395] = new JointPosition(-7.728e-05 , 0.253317 , 5.9655e-05 , -1.64713 , -8.31579e-05 , 1.24544 , 3.25564e-05);
        	jointPosUp[396] = new JointPosition(-7.73191e-05 , 0.252792 , 5.96412e-05 , -1.64874 , -8.31528e-05 , 1.24436 , 3.25951e-05);
        	jointPosUp[397] = new JointPosition(-7.7358e-05 , 0.252268 , 5.96274e-05 , -1.65034 , -8.31478e-05 , 1.24328 , 3.26339e-05);
        	jointPosUp[398] = new JointPosition(-7.73969e-05 , 0.251745 , 5.96135e-05 , -1.65195 , -8.3143e-05 , 1.2422 , 3.26727e-05);
        	jointPosUp[399] = new JointPosition(-7.74357e-05 , 0.251224 , 5.95997e-05 , -1.65355 , -8.31383e-05 , 1.24112 , 3.27115e-05);
        	jointPosUp[400] = new JointPosition(-7.74744e-05 , 0.250703 , 5.95857e-05 , -1.65515 , -8.31338e-05 , 1.24004 , 3.27504e-05);
        	jointPosUp[401] = new JointPosition(-7.7513e-05 , 0.250183 , 5.95718e-05 , -1.65675 , -8.31295e-05 , 1.23896 , 3.27892e-05);
        	jointPosUp[402] = new JointPosition(-7.75515e-05 , 0.249665 , 5.95578e-05 , -1.65834 , -8.31253e-05 , 1.23788 , 3.28281e-05);
        	jointPosUp[403] = new JointPosition(-7.759e-05 , 0.249148 , 5.95437e-05 , -1.65994 , -8.31212e-05 , 1.2368 , 3.2867e-05);
        	jointPosUp[404] = new JointPosition(-7.76283e-05 , 0.248632 , 5.95297e-05 , -1.66153 , -8.31173e-05 , 1.23573 , 3.29059e-05);
        	jointPosUp[405] = new JointPosition(-7.76666e-05 , 0.248116 , 5.95156e-05 , -1.66313 , -8.31136e-05 , 1.23465 , 3.29448e-05);
        	jointPosUp[406] = new JointPosition(-7.77047e-05 , 0.247602 , 5.95014e-05 , -1.66472 , -8.311e-05 , 1.23357 , 3.29837e-05);
        	jointPosUp[407] = new JointPosition(-7.77428e-05 , 0.247089 , 5.94872e-05 , -1.66631 , -8.31066e-05 , 1.23249 , 3.30227e-05);
        	jointPosUp[408] = new JointPosition(-7.77808e-05 , 0.246578 , 5.9473e-05 , -1.6679 , -8.31034e-05 , 1.23141 , 3.30616e-05);
        	jointPosUp[409] = new JointPosition(-7.78187e-05 , 0.246067 , 5.94588e-05 , -1.66949 , -8.31003e-05 , 1.23033 , 3.31006e-05);
        	jointPosUp[410] = new JointPosition(-7.78565e-05 , 0.245557 , 5.94445e-05 , -1.67108 , -8.30973e-05 , 1.22926 , 3.31397e-05);
        	jointPosUp[411] = new JointPosition(-7.78942e-05 , 0.245048 , 5.94302e-05 , -1.67266 , -8.30945e-05 , 1.22818 , 3.31787e-05);
        	jointPosUp[412] = new JointPosition(-7.79318e-05 , 0.244541 , 5.94158e-05 , -1.67425 , -8.30919e-05 , 1.2271 , 3.32178e-05);
        	jointPosUp[413] = new JointPosition(-7.79693e-05 , 0.244034 , 5.94014e-05 , -1.67583 , -8.30894e-05 , 1.22602 , 3.32568e-05);
        	jointPosUp[414] = new JointPosition(-7.80068e-05 , 0.243529 , 5.9387e-05 , -1.67742 , -8.30871e-05 , 1.22495 , 3.3296e-05);
        	jointPosUp[415] = new JointPosition(-7.80441e-05 , 0.243025 , 5.93725e-05 , -1.679 , -8.3085e-05 , 1.22387 , 3.33351e-05);
        	jointPosUp[416] = new JointPosition(-7.80814e-05 , 0.242522 , 5.9358e-05 , -1.68058 , -8.3083e-05 , 1.22279 , 3.33742e-05);
        	jointPosUp[417] = new JointPosition(-7.81186e-05 , 0.24202 , 5.93435e-05 , -1.68216 , -8.30811e-05 , 1.22172 , 3.34134e-05);
        	jointPosUp[418] = new JointPosition(-7.81557e-05 , 0.241519 , 5.93289e-05 , -1.68373 , -8.30794e-05 , 1.22064 , 3.34526e-05);
        	jointPosUp[419] = new JointPosition(-7.81927e-05 , 0.241019 , 5.93143e-05 , -1.68531 , -8.30779e-05 , 1.21956 , 3.34919e-05);
        	jointPosUp[420] = new JointPosition(-7.82296e-05 , 0.24052 , 5.92996e-05 , -1.68688 , -8.30766e-05 , 1.21849 , 3.35311e-05);
        	jointPosUp[421] = new JointPosition(-7.82665e-05 , 0.240022 , 5.92849e-05 , -1.68846 , -8.30754e-05 , 1.21741 , 3.35704e-05);
        	jointPosUp[422] = new JointPosition(-7.83032e-05 , 0.239525 , 5.92702e-05 , -1.69003 , -8.30743e-05 , 1.21633 , 3.36098e-05);
        	jointPosUp[423] = new JointPosition(-7.83399e-05 , 0.23903 , 5.92555e-05 , -1.6916 , -8.30734e-05 , 1.21526 , 3.36491e-05);
        	jointPosUp[424] = new JointPosition(-7.83765e-05 , 0.238535 , 5.92407e-05 , -1.69317 , -8.30727e-05 , 1.21418 , 3.36885e-05);
        	jointPosUp[425] = new JointPosition(-7.8413e-05 , 0.238042 , 5.92258e-05 , -1.69474 , -8.30722e-05 , 1.21311 , 3.37279e-05);
        	jointPosUp[426] = new JointPosition(-7.84494e-05 , 0.237549 , 5.9211e-05 , -1.69631 , -8.30718e-05 , 1.21203 , 3.37674e-05);
        	jointPosUp[427] = new JointPosition(-7.84857e-05 , 0.237058 , 5.91961e-05 , -1.69788 , -8.30715e-05 , 1.21096 , 3.38068e-05);
        	jointPosUp[428] = new JointPosition(-7.85219e-05 , 0.236568 , 5.91811e-05 , -1.69944 , -8.30715e-05 , 1.20988 , 3.38463e-05);
        	jointPosUp[429] = new JointPosition(-7.85581e-05 , 0.236079 , 5.91662e-05 , -1.70101 , -8.30715e-05 , 1.20881 , 3.38859e-05);
        	jointPosUp[430] = new JointPosition(-7.85941e-05 , 0.235591 , 5.91512e-05 , -1.70257 , -8.30718e-05 , 1.20773 , 3.39255e-05);
        	jointPosUp[431] = new JointPosition(-7.86301e-05 , 0.235104 , 5.91361e-05 , -1.70413 , -8.30722e-05 , 1.20666 , 3.39651e-05);
        	jointPosUp[432] = new JointPosition(-7.8666e-05 , 0.234618 , 5.9121e-05 , -1.70569 , -8.30728e-05 , 1.20558 , 3.40047e-05);
        	jointPosUp[433] = new JointPosition(-7.87018e-05 , 0.234133 , 5.91059e-05 , -1.70725 , -8.30735e-05 , 1.20451 , 3.40444e-05);
        	jointPosUp[434] = new JointPosition(-7.87376e-05 , 0.233649 , 5.90908e-05 , -1.70881 , -8.30744e-05 , 1.20343 , 3.40841e-05);
        	jointPosUp[435] = new JointPosition(-7.87732e-05 , 0.233166 , 5.90756e-05 , -1.71037 , -8.30755e-05 , 1.20236 , 3.41239e-05);
        	jointPosUp[436] = new JointPosition(-7.88088e-05 , 0.232685 , 5.90604e-05 , -1.71192 , -8.30767e-05 , 1.20128 , 3.41637e-05);
        	jointPosUp[437] = new JointPosition(-7.88443e-05 , 0.232204 , 5.90451e-05 , -1.71348 , -8.30781e-05 , 1.20021 , 3.42035e-05);
        	jointPosUp[438] = new JointPosition(-7.88797e-05 , 0.231725 , 5.90298e-05 , -1.71503 , -8.30797e-05 , 1.19913 , 3.42434e-05);
        	jointPosUp[439] = new JointPosition(-7.8915e-05 , 0.231247 , 5.90145e-05 , -1.71659 , -8.30814e-05 , 1.19806 , 3.42833e-05);
        	jointPosUp[440] = new JointPosition(-7.89502e-05 , 0.230769 , 5.89992e-05 , -1.71814 , -8.30833e-05 , 1.19698 , 3.43232e-05);
        	jointPosUp[441] = new JointPosition(-7.89854e-05 , 0.230293 , 5.89838e-05 , -1.71969 , -8.30853e-05 , 1.19591 , 3.43632e-05);
        	jointPosUp[442] = new JointPosition(-7.90204e-05 , 0.229818 , 5.89683e-05 , -1.72124 , -8.30875e-05 , 1.19484 , 3.44032e-05);
        	jointPosUp[443] = new JointPosition(-7.90554e-05 , 0.229344 , 5.89529e-05 , -1.72279 , -8.30899e-05 , 1.19376 , 3.44433e-05);
        	jointPosUp[444] = new JointPosition(-7.90903e-05 , 0.228871 , 5.89374e-05 , -1.72433 , -8.30924e-05 , 1.19269 , 3.44834e-05);
        	jointPosUp[445] = new JointPosition(-7.91252e-05 , 0.228399 , 5.89218e-05 , -1.72588 , -8.30952e-05 , 1.19161 , 3.45236e-05);
        	jointPosUp[446] = new JointPosition(-7.91599e-05 , 0.227928 , 5.89063e-05 , -1.72742 , -8.3098e-05 , 1.19054 , 3.45638e-05);
        	jointPosUp[447] = new JointPosition(-7.91946e-05 , 0.227459 , 5.88907e-05 , -1.72897 , -8.31011e-05 , 1.18947 , 3.4604e-05);
        	jointPosUp[448] = new JointPosition(-7.92291e-05 , 0.22699 , 5.8875e-05 , -1.73051 , -8.31043e-05 , 1.18839 , 3.46443e-05);
        	jointPosUp[449] = new JointPosition(-7.92636e-05 , 0.226522 , 5.88593e-05 , -1.73205 , -8.31077e-05 , 1.18732 , 3.46847e-05);
        	jointPosUp[450] = new JointPosition(-7.92981e-05 , 0.226056 , 5.88436e-05 , -1.73359 , -8.31112e-05 , 1.18624 , 3.47251e-05);
        	jointPosUp[451] = new JointPosition(-7.93324e-05 , 0.22559 , 5.88279e-05 , -1.73513 , -8.31149e-05 , 1.18517 , 3.47655e-05);
        	jointPosUp[452] = new JointPosition(-7.93667e-05 , 0.225126 , 5.88121e-05 , -1.73667 , -8.31188e-05 , 1.1841 , 3.4806e-05);
        	jointPosUp[453] = new JointPosition(-7.94009e-05 , 0.224663 , 5.87963e-05 , -1.73821 , -8.31229e-05 , 1.18302 , 3.48465e-05);
        	jointPosUp[454] = new JointPosition(-7.9435e-05 , 0.224201 , 5.87804e-05 , -1.73974 , -8.31271e-05 , 1.18195 , 3.48871e-05);
        	jointPosUp[455] = new JointPosition(-7.9469e-05 , 0.223739 , 5.87645e-05 , -1.74128 , -8.31315e-05 , 1.18088 , 3.49277e-05);
        	jointPosUp[456] = new JointPosition(-7.95029e-05 , 0.223279 , 5.87486e-05 , -1.74281 , -8.3136e-05 , 1.1798 , 3.49684e-05);
        	jointPosUp[457] = new JointPosition(-7.95368e-05 , 0.22282 , 5.87327e-05 , -1.74434 , -8.31408e-05 , 1.17873 , 3.50091e-05);
        	jointPosUp[458] = new JointPosition(-7.95706e-05 , 0.222363 , 5.87167e-05 , -1.74587 , -8.31457e-05 , 1.17766 , 3.50499e-05);
        	jointPosUp[459] = new JointPosition(-7.96043e-05 , 0.221906 , 5.87006e-05 , -1.7474 , -8.31507e-05 , 1.17658 , 3.50907e-05);
        	jointPosUp[460] = new JointPosition(-7.96379e-05 , 0.22145 , 5.86846e-05 , -1.74893 , -8.3156e-05 , 1.17551 , 3.51316e-05);
        	jointPosUp[461] = new JointPosition(-7.96715e-05 , 0.220995 , 5.86685e-05 , -1.75046 , -8.31614e-05 , 1.17444 , 3.51725e-05);
        	jointPosUp[462] = new JointPosition(-7.97049e-05 , 0.220542 , 5.86524e-05 , -1.75199 , -8.3167e-05 , 1.17336 , 3.52135e-05);
        	jointPosUp[463] = new JointPosition(-7.97383e-05 , 0.220089 , 5.86362e-05 , -1.75351 , -8.31727e-05 , 1.17229 , 3.52546e-05);
        	jointPosUp[464] = new JointPosition(-7.97717e-05 , 0.219638 , 5.862e-05 , -1.75504 , -8.31786e-05 , 1.17122 , 3.52957e-05);
        	jointPosUp[465] = new JointPosition(-7.98049e-05 , 0.219187 , 5.86038e-05 , -1.75656 , -8.31847e-05 , 1.17014 , 3.53369e-05);
        	jointPosUp[466] = new JointPosition(-7.98381e-05 , 0.218738 , 5.85875e-05 , -1.75808 , -8.3191e-05 , 1.16907 , 3.53781e-05);
        	jointPosUp[467] = new JointPosition(-7.98712e-05 , 0.21829 , 5.85712e-05 , -1.75961 , -8.31975e-05 , 1.168 , 3.54193e-05);
        	jointPosUp[468] = new JointPosition(-7.99042e-05 , 0.217843 , 5.85548e-05 , -1.76113 , -8.32041e-05 , 1.16692 , 3.54607e-05);
        	jointPosUp[469] = new JointPosition(-7.99371e-05 , 0.217397 , 5.85385e-05 , -1.76265 , -8.32109e-05 , 1.16585 , 3.55021e-05);
        	jointPosUp[470] = new JointPosition(-7.997e-05 , 0.216952 , 5.8522e-05 , -1.76416 , -8.32178e-05 , 1.16478 , 3.55435e-05);
        	jointPosUp[471] = new JointPosition(-8.00027e-05 , 0.216508 , 5.85056e-05 , -1.76568 , -8.3225e-05 , 1.1637 , 3.5585e-05);
        	jointPosUp[472] = new JointPosition(-8.00354e-05 , 0.216065 , 5.84891e-05 , -1.7672 , -8.32323e-05 , 1.16263 , 3.56266e-05);
        	jointPosUp[473] = new JointPosition(-8.00681e-05 , 0.215624 , 5.84726e-05 , -1.76871 , -8.32398e-05 , 1.16155 , 3.56682e-05);
        	jointPosUp[474] = new JointPosition(-8.01006e-05 , 0.215183 , 5.8456e-05 , -1.77023 , -8.32474e-05 , 1.16048 , 3.57099e-05);
        	jointPosUp[475] = new JointPosition(-8.01331e-05 , 0.214743 , 5.84395e-05 , -1.77174 , -8.32553e-05 , 1.15941 , 3.57517e-05);
        	jointPosUp[476] = new JointPosition(-8.01655e-05 , 0.214305 , 5.84228e-05 , -1.77325 , -8.32633e-05 , 1.15833 , 3.57935e-05);
        	jointPosUp[477] = new JointPosition(-8.01978e-05 , 0.213868 , 5.84062e-05 , -1.77476 , -8.32715e-05 , 1.15726 , 3.58354e-05);
        	jointPosUp[478] = new JointPosition(-8.02301e-05 , 0.213431 , 5.83895e-05 , -1.77627 , -8.32799e-05 , 1.15619 , 3.58773e-05);
        	jointPosUp[479] = new JointPosition(-8.02623e-05 , 0.212996 , 5.83728e-05 , -1.77778 , -8.32884e-05 , 1.15511 , 3.59194e-05);
        	jointPosUp[480] = new JointPosition(-8.02944e-05 , 0.212562 , 5.8356e-05 , -1.77929 , -8.32972e-05 , 1.15404 , 3.59614e-05);
        	jointPosUp[481] = new JointPosition(-8.03264e-05 , 0.212129 , 5.83392e-05 , -1.7808 , -8.33061e-05 , 1.15297 , 3.60036e-05);
        	jointPosUp[482] = new JointPosition(-8.03584e-05 , 0.211697 , 5.83224e-05 , -1.7823 , -8.33151e-05 , 1.15189 , 3.60458e-05);
        	jointPosUp[483] = new JointPosition(-8.03902e-05 , 0.211266 , 5.83055e-05 , -1.78381 , -8.33244e-05 , 1.15082 , 3.60881e-05);
        	jointPosUp[484] = new JointPosition(-8.0422e-05 , 0.210836 , 5.82886e-05 , -1.78531 , -8.33339e-05 , 1.14974 , 3.61305e-05);
        	jointPosUp[485] = new JointPosition(-8.04538e-05 , 0.210407 , 5.82716e-05 , -1.78681 , -8.33435e-05 , 1.14867 , 3.61729e-05);
        	jointPosUp[486] = new JointPosition(-8.04854e-05 , 0.20998 , 5.82547e-05 , -1.78832 , -8.33533e-05 , 1.1476 , 3.62154e-05);
        	jointPosUp[487] = new JointPosition(-8.0517e-05 , 0.209553 , 5.82377e-05 , -1.78982 , -8.33633e-05 , 1.14652 , 3.62579e-05);
        	jointPosUp[488] = new JointPosition(-8.05485e-05 , 0.209128 , 5.82206e-05 , -1.79132 , -8.33734e-05 , 1.14545 , 3.63006e-05);
        	jointPosUp[489] = new JointPosition(-8.058e-05 , 0.208703 , 5.82035e-05 , -1.79281 , -8.33838e-05 , 1.14437 , 3.63433e-05);
        	jointPosUp[490] = new JointPosition(-8.06114e-05 , 0.20828 , 5.81864e-05 , -1.79431 , -8.33943e-05 , 1.1433 , 3.63861e-05);
        	jointPosUp[491] = new JointPosition(-8.06426e-05 , 0.207858 , 5.81693e-05 , -1.79581 , -8.3405e-05 , 1.14223 , 3.64289e-05);
        	jointPosUp[492] = new JointPosition(-8.06739e-05 , 0.207437 , 5.81521e-05 , -1.7973 , -8.34159e-05 , 1.14115 , 3.64718e-05);
        	jointPosUp[493] = new JointPosition(-8.0705e-05 , 0.207017 , 5.81349e-05 , -1.7988 , -8.3427e-05 , 1.14008 , 3.65148e-05);
        	jointPosUp[494] = new JointPosition(-8.07361e-05 , 0.206598 , 5.81176e-05 , -1.80029 , -8.34382e-05 , 1.139 , 3.65579e-05);
        	jointPosUp[495] = new JointPosition(-8.07671e-05 , 0.20618 , 5.81003e-05 , -1.80178 , -8.34497e-05 , 1.13793 , 3.66011e-05);
        	jointPosUp[496] = new JointPosition(-8.0798e-05 , 0.205763 , 5.8083e-05 , -1.80328 , -8.34613e-05 , 1.13685 , 3.66443e-05);
        	jointPosUp[497] = new JointPosition(-8.08289e-05 , 0.205347 , 5.80656e-05 , -1.80477 , -8.34731e-05 , 1.13578 , 3.66876e-05);
        	jointPosUp[498] = new JointPosition(-8.08597e-05 , 0.204933 , 5.80482e-05 , -1.80626 , -8.34851e-05 , 1.1347 , 3.6731e-05);
        	jointPosUp[499] = new JointPosition(-8.08904e-05 , 0.204519 , 5.80308e-05 , -1.80774 , -8.34973e-05 , 1.13363 , 3.67745e-05);
        	jointPosUp[500] = new JointPosition(-8.09211e-05 , 0.204107 , 5.80133e-05 , -1.80923 , -8.35097e-05 , 1.13255 , 3.6818e-05);
        	jointPosUp[501] = new JointPosition(-8.09517e-05 , 0.203696 , 5.79958e-05 , -1.81072 , -8.35222e-05 , 1.13148 , 3.68617e-05);
        	jointPosUp[502] = new JointPosition(-8.09822e-05 , 0.203285 , 5.79783e-05 , -1.8122 , -8.3535e-05 , 1.1304 , 3.69054e-05);
        	jointPosUp[503] = new JointPosition(-8.10126e-05 , 0.202876 , 5.79607e-05 , -1.81369 , -8.35479e-05 , 1.12933 , 3.69492e-05);
        	jointPosUp[504] = new JointPosition(-8.1043e-05 , 0.202468 , 5.79431e-05 , -1.81517 , -8.3561e-05 , 1.12825 , 3.69931e-05);
        	jointPosUp[505] = new JointPosition(-8.10733e-05 , 0.202061 , 5.79255e-05 , -1.81665 , -8.35743e-05 , 1.12718 , 3.7037e-05);
        	jointPosUp[506] = new JointPosition(-8.11035e-05 , 0.201655 , 5.79078e-05 , -1.81814 , -8.35878e-05 , 1.1261 , 3.70811e-05);
        	jointPosUp[507] = new JointPosition(-8.11337e-05 , 0.201251 , 5.78901e-05 , -1.81962 , -8.36015e-05 , 1.12503 , 3.71252e-05);
        	jointPosUp[508] = new JointPosition(-8.11638e-05 , 0.200847 , 5.78723e-05 , -1.8211 , -8.36154e-05 , 1.12395 , 3.71694e-05);
        	jointPosUp[509] = new JointPosition(-8.11938e-05 , 0.200444 , 5.78545e-05 , -1.82257 , -8.36295e-05 , 1.12287 , 3.72137e-05);
        	jointPosUp[510] = new JointPosition(-8.12237e-05 , 0.200043 , 5.78367e-05 , -1.82405 , -8.36437e-05 , 1.1218 , 3.72581e-05);
        	jointPosUp[511] = new JointPosition(-8.12536e-05 , 0.199643 , 5.78188e-05 , -1.82553 , -8.36582e-05 , 1.12072 , 3.73026e-05);
        	jointPosUp[512] = new JointPosition(-8.12834e-05 , 0.199243 , 5.78009e-05 , -1.827 , -8.36728e-05 , 1.11964 , 3.73471e-05);
        	jointPosUp[513] = new JointPosition(-8.13132e-05 , 0.198845 , 5.7783e-05 , -1.82848 , -8.36877e-05 , 1.11857 , 3.73918e-05);
        	jointPosUp[514] = new JointPosition(-8.13428e-05 , 0.198448 , 5.7765e-05 , -1.82995 , -8.37027e-05 , 1.11749 , 3.74365e-05);
        	jointPosUp[515] = new JointPosition(-8.13724e-05 , 0.198052 , 5.7747e-05 , -1.83143 , -8.37179e-05 , 1.11641 , 3.74814e-05);
        	jointPosUp[516] = new JointPosition(-8.1402e-05 , 0.197657 , 5.7729e-05 , -1.8329 , -8.37333e-05 , 1.11534 , 3.75263e-05);
        	jointPosUp[517] = new JointPosition(-8.14314e-05 , 0.197263 , 5.77109e-05 , -1.83437 , -8.37489e-05 , 1.11426 , 3.75713e-05);
        	jointPosUp[518] = new JointPosition(-8.14608e-05 , 0.196871 , 5.76928e-05 , -1.83584 , -8.37647e-05 , 1.11318 , 3.76164e-05);
        	jointPosUp[519] = new JointPosition(-8.14902e-05 , 0.196479 , 5.76747e-05 , -1.83731 , -8.37807e-05 , 1.11211 , 3.76616e-05);
        	jointPosUp[520] = new JointPosition(-8.15194e-05 , 0.196089 , 5.76565e-05 , -1.83877 , -8.37969e-05 , 1.11103 , 3.77069e-05);
        	jointPosUp[521] = new JointPosition(-8.15486e-05 , 0.195699 , 5.76383e-05 , -1.84024 , -8.38133e-05 , 1.10995 , 3.77523e-05);
        	jointPosUp[522] = new JointPosition(-8.15778e-05 , 0.195311 , 5.762e-05 , -1.84171 , -8.38299e-05 , 1.10887 , 3.77978e-05);
        	jointPosUp[523] = new JointPosition(-8.16068e-05 , 0.194924 , 5.76017e-05 , -1.84317 , -8.38467e-05 , 1.10779 , 3.78434e-05);
        	jointPosUp[524] = new JointPosition(-8.16358e-05 , 0.194538 , 5.75834e-05 , -1.84464 , -8.38637e-05 , 1.10672 , 3.78891e-05);
        	jointPosUp[525] = new JointPosition(-8.16648e-05 , 0.194153 , 5.7565e-05 , -1.8461 , -8.38808e-05 , 1.10564 , 3.79349e-05);
        	jointPosUp[526] = new JointPosition(-8.16936e-05 , 0.193769 , 5.75466e-05 , -1.84756 , -8.38982e-05 , 1.10456 , 3.79808e-05);
        	jointPosUp[527] = new JointPosition(-8.17224e-05 , 0.193387 , 5.75282e-05 , -1.84902 , -8.39158e-05 , 1.10348 , 3.80268e-05);
        	jointPosUp[528] = new JointPosition(-8.17511e-05 , 0.193005 , 5.75097e-05 , -1.85048 , -8.39336e-05 , 1.1024 , 3.80729e-05);
        	jointPosUp[529] = new JointPosition(-8.17798e-05 , 0.192625 , 5.74912e-05 , -1.85194 , -8.39516e-05 , 1.10132 , 3.8119e-05);
        	jointPosUp[530] = new JointPosition(-8.18084e-05 , 0.192245 , 5.74726e-05 , -1.8534 , -8.39697e-05 , 1.10024 , 3.81653e-05);
        	jointPosUp[531] = new JointPosition(-8.18369e-05 , 0.191867 , 5.7454e-05 , -1.85486 , -8.39881e-05 , 1.09916 , 3.82117e-05);
        	jointPosUp[532] = new JointPosition(-8.18654e-05 , 0.19149 , 5.74354e-05 , -1.85632 , -8.40067e-05 , 1.09808 , 3.82582e-05);
        	jointPosUp[533] = new JointPosition(-8.18938e-05 , 0.191114 , 5.74167e-05 , -1.85777 , -8.40255e-05 , 1.097 , 3.83048e-05);
        	jointPosUp[534] = new JointPosition(-8.19221e-05 , 0.190739 , 5.7398e-05 , -1.85923 , -8.40445e-05 , 1.09592 , 3.83515e-05);
        	jointPosUp[535] = new JointPosition(-8.19504e-05 , 0.190365 , 5.73793e-05 , -1.86068 , -8.40637e-05 , 1.09484 , 3.83983e-05);
        	jointPosUp[536] = new JointPosition(-8.19786e-05 , 0.189992 , 5.73605e-05 , -1.86213 , -8.40831e-05 , 1.09376 , 3.84453e-05);
        	jointPosUp[537] = new JointPosition(-8.20067e-05 , 0.189621 , 5.73417e-05 , -1.86359 , -8.41027e-05 , 1.09268 , 3.84923e-05);
        	jointPosUp[538] = new JointPosition(-8.20348e-05 , 0.18925 , 5.73229e-05 , -1.86504 , -8.41225e-05 , 1.0916 , 3.85394e-05);
        	jointPosUp[539] = new JointPosition(-8.20628e-05 , 0.188881 , 5.7304e-05 , -1.86649 , -8.41425e-05 , 1.09052 , 3.85867e-05);
        	jointPosUp[540] = new JointPosition(-8.20907e-05 , 0.188513 , 5.72851e-05 , -1.86794 , -8.41628e-05 , 1.08944 , 3.8634e-05);
        	jointPosUp[541] = new JointPosition(-8.21186e-05 , 0.188146 , 5.72661e-05 , -1.86939 , -8.41832e-05 , 1.08836 , 3.86815e-05);
        	jointPosUp[542] = new JointPosition(-8.21464e-05 , 0.18778 , 5.72471e-05 , -1.87083 , -8.42038e-05 , 1.08728 , 3.87291e-05);
        	jointPosUp[543] = new JointPosition(-8.21742e-05 , 0.187415 , 5.72281e-05 , -1.87228 , -8.42247e-05 , 1.0862 , 3.87768e-05);
        	jointPosUp[544] = new JointPosition(-8.22018e-05 , 0.187051 , 5.7209e-05 , -1.87373 , -8.42457e-05 , 1.08511 , 3.88246e-05);
        	jointPosUp[545] = new JointPosition(-8.22294e-05 , 0.186689 , 5.71899e-05 , -1.87517 , -8.4267e-05 , 1.08403 , 3.88725e-05);
        	jointPosUp[546] = new JointPosition(-8.2257e-05 , 0.186327 , 5.71708e-05 , -1.87661 , -8.42885e-05 , 1.08295 , 3.89205e-05);
        	jointPosUp[547] = new JointPosition(-8.22845e-05 , 0.185967 , 5.71516e-05 , -1.87806 , -8.43102e-05 , 1.08187 , 3.89687e-05);
        	jointPosUp[548] = new JointPosition(-8.23119e-05 , 0.185608 , 5.71323e-05 , -1.8795 , -8.43321e-05 , 1.08078 , 3.90169e-05);
        	jointPosUp[549] = new JointPosition(-8.23393e-05 , 0.18525 , 5.71131e-05 , -1.88094 , -8.43542e-05 , 1.0797 , 3.90653e-05);
        	jointPosUp[550] = new JointPosition(-8.23666e-05 , 0.184893 , 5.70938e-05 , -1.88238 , -8.43765e-05 , 1.07862 , 3.91138e-05);
        	jointPosUp[551] = new JointPosition(-8.23938e-05 , 0.184537 , 5.70744e-05 , -1.88382 , -8.43991e-05 , 1.07753 , 3.91625e-05);
        	jointPosUp[552] = new JointPosition(-8.2421e-05 , 0.184182 , 5.70551e-05 , -1.88526 , -8.44218e-05 , 1.07645 , 3.92112e-05);
        	jointPosUp[553] = new JointPosition(-8.24481e-05 , 0.183829 , 5.70357e-05 , -1.8867 , -8.44448e-05 , 1.07537 , 3.92601e-05);
        	jointPosUp[554] = new JointPosition(-8.24751e-05 , 0.183477 , 5.70162e-05 , -1.88813 , -8.4468e-05 , 1.07428 , 3.9309e-05);
        	jointPosUp[555] = new JointPosition(-8.25021e-05 , 0.183125 , 5.69967e-05 , -1.88957 , -8.44914e-05 , 1.0732 , 3.93581e-05);
        	jointPosUp[556] = new JointPosition(-8.2529e-05 , 0.182775 , 5.69772e-05 , -1.891 , -8.4515e-05 , 1.07211 , 3.94074e-05);
        	jointPosUp[557] = new JointPosition(-8.25559e-05 , 0.182426 , 5.69576e-05 , -1.89244 , -8.45388e-05 , 1.07103 , 3.94567e-05);
        	jointPosUp[558] = new JointPosition(-8.25827e-05 , 0.182078 , 5.6938e-05 , -1.89387 , -8.45629e-05 , 1.06994 , 3.95062e-05);
        	jointPosUp[559] = new JointPosition(-8.26094e-05 , 0.181732 , 5.69184e-05 , -1.8953 , -8.45872e-05 , 1.06886 , 3.95558e-05);
        	jointPosUp[560] = new JointPosition(-8.26361e-05 , 0.181386 , 5.68987e-05 , -1.89673 , -8.46117e-05 , 1.06777 , 3.96056e-05);
        	jointPosUp[561] = new JointPosition(-8.26627e-05 , 0.181042 , 5.68789e-05 , -1.89817 , -8.46364e-05 , 1.06668 , 3.96554e-05);
        	jointPosUp[562] = new JointPosition(-8.26892e-05 , 0.180699 , 5.68592e-05 , -1.89959 , -8.46613e-05 , 1.0656 , 3.97054e-05);
        	jointPosUp[563] = new JointPosition(-8.27157e-05 , 0.180357 , 5.68394e-05 , -1.90102 , -8.46865e-05 , 1.06451 , 3.97555e-05);
        	jointPosUp[564] = new JointPosition(-8.27421e-05 , 0.180016 , 5.68195e-05 , -1.90245 , -8.47119e-05 , 1.06342 , 3.98058e-05);
        	jointPosUp[565] = new JointPosition(-8.27685e-05 , 0.179676 , 5.67996e-05 , -1.90388 , -8.47375e-05 , 1.06234 , 3.98562e-05);
        	jointPosUp[566] = new JointPosition(-8.27948e-05 , 0.179337 , 5.67797e-05 , -1.9053 , -8.47633e-05 , 1.06125 , 3.99067e-05);
        	jointPosUp[567] = new JointPosition(-8.2821e-05 , 0.179 , 5.67598e-05 , -1.90673 , -8.47893e-05 , 1.06016 , 3.99573e-05);
        	jointPosUp[568] = new JointPosition(-8.28389e-05 , 0.178769 , 5.6746e-05 , -1.90771 , -8.48074e-05 , 1.05942 , 3.99921e-05);


            rec.startRecording();
            
            
            long cT;
            long pT;
            cT = _theSmartServoRuntime.updateWithRealtimeSystem();
            pT = cT;
            long deltaT;
            
            for (_steps = 3; _steps < 4; _steps = _steps + 1) //NUM_RUNS
            {
                // Timing - draw one step
                OneTimeStep aStep = timing.newTimeStep();

                ThreadUtil.milliSleep(MILLI_SLEEP_TO_EMULATE_COMPUTATIONAL_EFFORT);
                _theSmartServoRuntime.updateWithRealtimeSystem();
                // Get the measured position
                JointPosition curMsrJntPose = _theSmartServoRuntime
                        .getAxisQMsrOnController();

                //double curTime = System.nanoTime() - startTimeStamp;
                //double sinArgument = omega * curTime;
                
                /*JointPosition jointPositionsUP[] = new JointPosition[1];
                
                double d2r = Math.PI / 180;

            	jointPositionsUP[0] = new JointPosition(0. , 30.31*d2r , 0. , -65.28*d2r , 0. , 84.4*d2r , 0.);

                for (int k = 0; k < destination.getAxisCount(); ++k)
                {
                    destination.set(k, Math.sin(sinArgument)
                            * AMPLITUDE + initialPosition.get(k));
                    if (k > 5)
                    {
                        destination.set(k, initialPosition.get(k));
                    }
                }
                destination = jointPositionsUP[0];
                */
                
               
                
                
                // TRIAL II
                
                JointPosition currentPosition = new JointPosition(
                        _lbr.getCurrentJointPosition());
                double d2r = Math.PI / 180;
                //JointPosition endPosition = new JointPosition(0. , 31.66*d2r , 0. , -71.45*d2r , 0. , 76.91*d2r , 0.);
                //JointPosition endPosition = new JointPosition(0. , 7*d2r , 0. , -116.52*d2r , 0. , 56.44*d2r , 0.);
               
                //JointPosition jointVel = new JointPosition(0. , 0. , 0. , 0. , 0. , 0. , 0.);

                /*      	
                double deltaP = -AMPLITUDE*0.04/(endPosition.get(1) - Math.PI / 180 * 30.);
                if (endPosition.get(1) - currentPosition.get(1) > 0.001)
                {
                	deltaP = 0.;
                }
 
            	double p1 = deltaP*(endPosition.get(1) - currentPosition.get(1));
            	double p3 = deltaP*(endPosition.get(3) - currentPosition.get(3));
            	double p5 = deltaP*(endPosition.get(5) - currentPosition.get(5));
				*/
                destination.set(0, jointPosUp[_steps].get(0)+ Math.PI / 180 * -68.08);
                destination.set(1, jointPosUp[_steps].get(1));
                destination.set(2, jointPosUp[_steps].get(2));
                destination.set(3, jointPosUp[_steps].get(3));
                destination.set(4, jointPosUp[_steps].get(4));
                destination.set(5, jointPosUp[_steps].get(5));
                destination.set(6, jointPosUp[_steps].get(6));
                

				// TIME EACH CYCLE
				cT = _theSmartServoRuntime.updateWithRealtimeSystem();
				deltaT = cT - pT;
				pT = cT;
				getLogger().info("delta Time : " +deltaT );

				cT = _theSmartServoRuntime.updateWithRealtimeSystem();
                JointPosition cPosition = new JointPosition(
                        _lbr.getCurrentJointPosition());
                
                JointPosition nPosition = new JointPosition(
                        _lbr.getCurrentJointPosition());
				
                _theSmartServoRuntime.setDestination(destination);
                
                while(nPosition.equals(cPosition))
                {
    				pT = _theSmartServoRuntime.updateWithRealtimeSystem();
    				nPosition = new JointPosition(
                            _lbr.getCurrentJointPosition());	
                }
                deltaT = pT - cT;
				getLogger().info("delta time TO start : " +deltaT );
                
             
                //ThreadUtil.milliSleep(10);


                // ////////////////////////////////////////////////////////////
                if (doDebugPrints)
                {
                    getLogger().info("Step " + _steps + " New Goal "
                            + destination);
                    getLogger().info("Fine ipo finished " + _theSmartServoRuntime.isDestinationReached());
                    if (_theSmartServoRuntime.isDestinationReached())
                    {
                        _count++;
                    }
                    getLogger().info("Ipo state " + _theSmartServoRuntime.getFineIpoState());
                    getLogger().info("Remaining time " + _theSmartServoRuntime.getRemainingTime());
                    getLogger().info("LBR Position "
                            + _lbr.getCurrentJointPosition());
                    getLogger().info(" Measured LBR Position "
                            + curMsrJntPose);
                    if (_steps % 100 == 0)
                    {
                        // Some internal values, which can be displayed
                        getLogger().info("Simple Joint Test - step " + _steps + _theSmartServoRuntime.toString());

                    }
                }
                aStep.end();

            }
            rec.stopRecording();
        }
        catch (Exception e)
        {
            getLogger().info(e.getLocalizedMessage());
            e.printStackTrace();
        }
        ThreadUtil.milliSleep(1000);

        //Print statistics and parameters of the motion
        getLogger().info("Displaying final states after loop ");
        getLogger().info(getClass().getName() + _theSmartServoRuntime.toString());
        // Stop the motion

        getLogger().info("Stop the SmartServo motion");
        _theSmartServoRuntime.stopMotion();

        getLogger().info(_count + " times was the destination reached.");
        getLogger().info("Statistic Timing of Overall Loop " + timing);
        if (timing.getMeanTimeMillis() > 150)
        {
            getLogger().info("Statistic Timing is unexpected slow, you should try to optimize TCP/IP Transfer");
            getLogger().info("Under Windows, you should play with the registry, see the e.g. the SmartServo Class javaDoc for details");
        }
    }

    /**
     * Main routine, which starts the application.
     * 
     * @param args
     *            arguments
     */
    public static void main(String[] args)
    {
    	test_smartServo app = new
    			test_smartServo();
        app.runApplication();
    }
}

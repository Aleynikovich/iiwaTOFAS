package hartuTofas;

import com.kuka.generated.ioAccess.Ethercat_x44IOGroup;
import com.kuka.generated.ioAccess.IOFlangeIOGroup;
import com.kuka.roboticsAPI.deviceModel.LBR;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.World;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

public class MessageHandler {

	private LBR robot;
	private Tool flexTool;

	@Inject
	private IOFlangeIOGroup gimatic; // Out 7 True=Unlock False = Lock
	@Inject
	private Ethercat_x44IOGroup IOs; // Out 1 = Pick Out 2 = Place [raise]

	// Action types
	// <ACTION_TYPE>|<NUM_POINTS>|<POINTS>|<OUTPUT_POINTS>|<OUTPUT_PINS>|<OUTPUT_STATES>|<TOOL_ID>|<BASE_ID>|<OVERRIDE>|<UUID>#
	public static final int PTP_AXIS = 0;
	public static final int PTP_FRAME = 1;
	public static final int LIN_AXIS = 2;
	public static final int LIN_FRAME = 3;
	public static final int CIRC_AXIS = 4;
	public static final int CIRC_FRAME = 5;
	public static final int PTP_AXIS_C = 6;
	public static final int PTP_FRAME_C = 7;
	public static final int LIN_FRAME_C = 8;
	public static final int ACTIVATE_IO = 9;
	public static final int LIN_REL_TOOL = 10;
	public static final int LIN_REL_BASE = 11;

	// 100+ = program call with ID actiontype-100

	public MessageHandler(LBR robot) {
		this.robot = robot;
	}

	public class Command {
		public int actionType;
		public int numPoints;
		public int ioPoint;
		public int ioPin;
		public boolean ioState;
		public int tool;
		public int base;
		public double speedOverride;

		public boolean programCall;
		public String targetPoints;
		public String id;

		private String get(String[] parts, int index, String defaultValue) {
			return (index < parts.length && parts[index] != null && !parts[index]
					.trim().isEmpty()) ? parts[index].trim() : defaultValue;
		}

		public Command(String[] parts) {
			System.out.println("Generating command");

			try {
				int rawActionType = Integer.parseInt(get(parts, 0, "999"));
				this.programCall = rawActionType > 100;
				this.actionType = this.programCall ? rawActionType - 100
						: rawActionType;
				this.numPoints = Integer.parseInt(get(parts, 1, "0"));
				this.targetPoints = get(parts, 2, "");
				this.ioPoint = Integer.parseInt(get(parts, 3, "0"));
				this.ioPin = Integer.parseInt(get(parts, 4, "0"));
				this.ioState = Boolean.parseBoolean(get(parts, 5, "false"));
				this.tool = Integer.parseInt(get(parts, 6, "0"));
				this.base = Integer.parseInt(get(parts, 7, "0"));
				this.speedOverride = Double.parseDouble(get(parts, 8, "100.0")) / 100.0;
				this.id = get(parts, 9, "N/A");

			} catch (NumberFormatException e) {
				throw new IllegalArgumentException(
						"Invalid number format in input: " + e.getMessage());
			}

			printCommand();
		}

		public void printCommand() {
			System.out.println("Command{" + "actionType=" + actionType
					+ ", programCall=" + programCall + ", numPoints="
					+ numPoints + ", ioPoint=" + ioPoint + ", ioPin=" + ioPin
					+ ", ioState=" + ioState + ", tool=" + tool + ", base="
					+ base + ", speedOverride=" + speedOverride
					+ ", targetPoints='" + targetPoints + '\'' + ", id='" + id
					+ '\'' + '}');
		}
	}

	public String handleMessage(String message) {
		if (!message.endsWith("#")) {
			System.out.println("Message does not end with '#'");
			return "Invalid message format";
		}

		message = message.substring(0, message.length() - 1);
		String[] parts = message.split("\\|");

		System.out.println("Parsed parts: " + Arrays.toString(parts));

		try {
			Command cmd = new Command(parts);
			cmd.printCommand();
			if (!cmd.programCall) {
				System.out.println("Entered Movetype");
				switch (cmd.actionType) {
				case PTP_AXIS:
				case PTP_AXIS_C:
					System.out.println("Entered PTP AXIS");
					return handlePTPAxis(cmd);
				case PTP_FRAME:
				case PTP_FRAME_C:
					return handlePTPFrame(cmd);
				case LIN_FRAME:
				case LIN_FRAME_C:
					return handleLINFrame(cmd);
				case LIN_REL_TOOL:
				case LIN_REL_BASE:

				case ACTIVATE_IO:
					System.out.println("Entered Activate IO");
					return handleActivateIO(cmd);
				default:
					System.out.println("Unknown move type: " + cmd.actionType);
					return "Unknown move type: " + cmd.actionType;
				}
			} else {
				System.out.println("Entered Programcall");
				switch (cmd.actionType) {

				case 1:
					return "Program 1 called";
				case 2:
					return "Program 2 called";
				default:
					return "Program " + cmd.actionType + " not found.";
				}
			}

		} catch (Exception e) {
			return "Failed to parse command: " + e.getMessage();
		}
	}

	private String handlePTPAxis(Command cmd) {
		// Process the PTP_AXIS or PTP_AXIS_C command
		List<String> jointPositionGroups = Arrays.asList(cmd.targetPoints
				.split(","));

		if (jointPositionGroups.size() != cmd.numPoints) {
			System.out.println("Invalid number of point groups for ID: "
					+ cmd.id);
			return "Invalid number of point groups";
		}

		try {
			for (String jointPositions : jointPositionGroups) {
				List<String> jointValuesStr = Arrays.asList(jointPositions
						.split(";"));

				if (jointValuesStr.size() != 7) {
					System.out
							.println("Invalid number of joint positions in a group for ID: "
									+ cmd.id);
					return "Invalid number of joint positions";
				}

				double[] jointValues = new double[7];
				for (int i = 0; i < jointValuesStr.size(); i++) {
					double jointValueDeg = Double.parseDouble(jointValuesStr
							.get(i));
					double jointValueRad = Math.toRadians(jointValueDeg); // Convert
																			// degrees
																			// to
																			// radians

					if (!isWithinLimits(i, jointValueRad)) {
						System.out.println("Joint " + (i + 1)
								+ " value out of limits: " + jointValueRad
								+ " for ID: " + cmd.id);
						return "Joint " + (i + 1) + " value out of limits";
					}

					jointValues[i] = jointValueRad;
				}

				if (cmd.actionType == PTP_AXIS) {
					// Execute synchronous PTP motion
					robot.move(ptp(jointValues).setJointVelocityRel(
							cmd.speedOverride));
				} else if (cmd.actionType == PTP_AXIS_C) {
					// Execute asynchronous PTP motion for each point
					robot.moveAsync(ptp(jointValues).setJointVelocityRel(
							cmd.speedOverride).setBlendingRel(0.5));
				}
			}
		} catch (NumberFormatException e) {
			System.out.println("Invalid joint position values for ID: "
					+ cmd.id);
			return "Invalid joint position values";
		}

		if (cmd.actionType == PTP_AXIS) {
			System.out.println("PTP_AXIS command executed for ID: " + cmd.id);
			return "PTP_AXIS command executed";
		} else {
			System.out.println("PTP_AXIS_C command executed for ID: " + cmd.id);
			return "PTP_AXIS_C command executed";
		}
	}

	private String handlePTPFrame(Command cmd) {
		// Process the PTP_FRAME command
		List<String> points = Arrays.asList(cmd.targetPoints.split(","));
		try {
			for (int i = 0; i < points.size(); i++) {
				String point = points.get(i);
				List<String> coordinates = Arrays.asList(point.split(";"));
				double x = Double.parseDouble(coordinates.get(0));
				double y = Double.parseDouble(coordinates.get(1));
				double z = Double.parseDouble(coordinates.get(2));
				double roll = Math.toRadians(Double.parseDouble(coordinates
						.get(3))); // Convert degrees to radians
				double pitch = Math.toRadians(Double.parseDouble(coordinates
						.get(4)));
				double yaw = Math.toRadians(Double.parseDouble(coordinates
						.get(5)));
				Frame targetFrameVirgin = new Frame(
						World.Current.getRootFrame(), x, y, z, roll, pitch, yaw);
				if (cmd.actionType == PTP_FRAME) {
					robot.move(ptp(targetFrameVirgin).setJointVelocityRel(
							cmd.speedOverride));
				} else if (cmd.actionType == PTP_FRAME_C) {
					// Execute asynchronous PTP motion for each point
					robot.moveAsync(ptp(targetFrameVirgin).setBlendingRel(0.5)
							.setJointVelocityRel(cmd.speedOverride));
				}
			}
		} catch (NumberFormatException e) {
			System.out.println("Invalid coordinate values for ID: " + cmd.id);
			return "Invalid coordinate values";
		} catch (IndexOutOfBoundsException e) {
			System.out.println("Coordinate values are incomplete for ID: "
					+ cmd.id);
			return "Coordinate values are incomplete";
		}
		System.out.println("PTP_FRAME command executed for ID: " + cmd.id);
		return "PTP_FRAME command executed";
	}

	private String handleLINFrame(Command cmd) {
		// Process the LIN_FRAME commands
		List<String> points = Arrays.asList(cmd.targetPoints.split(","));
		try {
			for (int i = 0; i < points.size(); i++) {
				String point = points.get(i);
				List<String> coordinates = Arrays.asList(point.split(";"));
				double x = Double.parseDouble(coordinates.get(0));
				double y = Double.parseDouble(coordinates.get(1));
				double z = Double.parseDouble(coordinates.get(2));
				double roll = Math.toRadians(Double.parseDouble(coordinates
						.get(3))); // Convert degrees to radians
				double pitch = Math.toRadians(Double.parseDouble(coordinates
						.get(4)));
				double yaw = Math.toRadians(Double.parseDouble(coordinates
						.get(5)));
				Frame targetFrameVirgin = new Frame(
						World.Current.getRootFrame(), x, y, z, roll, pitch, yaw);
				if (cmd.actionType == LIN_FRAME) {
					robot.move(lin(targetFrameVirgin).setJointVelocityRel(
							cmd.speedOverride));
				} else if (cmd.actionType == LIN_FRAME_C) {
					robot.moveAsync(lin(targetFrameVirgin).setJointVelocityRel(
							cmd.speedOverride).setBlendingRel(0.5));
				}

			}
		} catch (NumberFormatException e) {
			System.out.println("Invalid coordinate values for ID: " + cmd.id);
			return "Invalid coordinate values";
		} catch (IndexOutOfBoundsException e) {
			System.out.println("Coordinate values are incomplete for ID: "
					+ cmd.id);
			return "Coordinate values are incomplete";
		}
		System.out.println("LIN_FRAME command executed for ID: " + cmd.id);
		return "LIN_FRAME command executed";
	}

	private String handleLINREL(Command cmd) {
		List<String> relFrames = Arrays.asList(cmd.targetPoints.split(","));
		for (int i = 0; i < relFrames.size(); i++) {
			String point = relFrames.get(i);
			List<String> coordinates = Arrays.asList(point.split(";"));
			double x = Double.parseDouble(coordinates.get(0));
			double y = Double.parseDouble(coordinates.get(1));
			double z = Double.parseDouble(coordinates.get(2));
			double roll = Math
					.toRadians(Double.parseDouble(coordinates.get(3))); // Convert
																		// degrees
																		// to
																		// radians
			double pitch = Math
					.toRadians(Double.parseDouble(coordinates.get(4)));
			double yaw = Math.toRadians(Double.parseDouble(coordinates.get(5)));
			if (cmd.actionType == LIN_REL_TOOL) {
				flexTool.attachTo(robot.getFlange());
				// flexTool.
				// robot.move(linRel(x,y,z,roll,pitch,yaw,getApplicationData().getBase));
			} else if (cmd.actionType == LIN_REL_BASE) {
				// robot.move(lin());
			}
		}
		System.out.println("LIN_REL command executed for ID: " + cmd.id);
		return "LIN_REL command executed for ID: " + cmd.id;

	}

	private String handleActivateIO(Command cmd) {
		switch (cmd.ioPin) {
		case 1:
			gimatic.setDO_Flange7(cmd.ioState);
		case 2:
			IOs.setOutput1(cmd.ioState);
		case 3:
			IOs.setOutput2(cmd.ioState);

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return "ACTIVATE_IO command executed for ID: " + cmd.id;
	}

	private boolean isWithinLimits(int jointIndex, double jointValue) {
		switch (jointIndex) {
		case 0:
			return jointValue >= Math.toRadians(-170)
					&& jointValue <= Math.toRadians(170);
		case 1:
			return jointValue >= Math.toRadians(-120)
					&& jointValue <= Math.toRadians(120);
		case 2:
			return jointValue >= Math.toRadians(-170)
					&& jointValue <= Math.toRadians(170);
		case 3:
			return jointValue >= Math.toRadians(-120)
					&& jointValue <= Math.toRadians(120);
		case 4:
			return jointValue >= Math.toRadians(-170)
					&& jointValue <= Math.toRadians(170);
		case 5:
			return jointValue >= Math.toRadians(-120)
					&& jointValue <= Math.toRadians(120);
		case 6:
			return jointValue >= Math.toRadians(-175)
					&& jointValue <= Math.toRadians(175);
		default:
			return false;
		}
	}
}

import socket
import uuid
import math

ROBOT_IP = "10.66.171.147"
TASK_PORT = 30001
JOINT_STATE_PORT = 30002
TERMINATOR = "#"


def generate_command_id():
    return str(uuid.uuid4())


def send_command(sock, command):
    if not command.endswith(TERMINATOR):
        command += TERMINATOR
    sock.sendall(command.encode())
    print(f"> Sent: {command}")


def receive_response(sock):
    data = b""
    while not data.endswith(TERMINATOR.encode()):
        chunk = sock.recv(1024)
        if not chunk:
            raise ConnectionError("Connection closed by robot.")
        data += chunk
    return data.decode()


def determine_action_code(motion_type, continuous, joint_mode):
    if motion_type == "1":  # PTP
        if joint_mode:
            return "6" if continuous else "0"  # PTP_AXIS_C or PTP_AXIS
        else:
            return "7" if continuous else "1"  # PTP_FRAME_C or PTP_FRAME
    elif motion_type == "2":  # LIN
        if joint_mode:
            return "2"  # LIN_AXIS
        else:
            return "8" if continuous else "3"  # LIN_FRAME_C or LIN_FRAME
    elif motion_type == "3":  # CIRC
        if joint_mode:
            return "4"  # CIRC_AXIS
        else:
            return "5"  # CIRC_FRAME
    return "0"  # fallback


def movement_menu(sock):
    print("\n-- Movement Type --")
    print("1. PTP")
    print("2. LIN")
    print("3. CIRC")
    motion_type = input("Select motion type: ").strip()
    continuous = input("Should motion be continuous? (y/n): ").strip().lower() == 'y'

    points = []
    joint_mode = None

    while True:
        raw = input("Enter motion point (6 or 7 values): ").strip()
        values = raw.split()

        if len(values) == 6:
            joint_mode = False
            points.append(";".join(values))
        elif len(values) == 7:
            joint_mode = True
            points.append(";".join(values))
        else:
            print("❌ Invalid number of values. Expected 6 or 7. Try again.")
            continue

        cont = input("Add another point? (y/n): ").strip().lower()
        if cont != 'y':
            break

    id = generate_command_id()
    tool = ""
    base = ""
    speed = 0.25
    num_points = len(points)

    action_code = determine_action_code(motion_type, continuous, joint_mode)
    joined_points = ",".join(points)
    command = f"{action_code}|{num_points}|{joined_points}|{tool}|{base}|{speed}|0|0|25|{id}"
    send_command(sock, command)
    print("< Response:", receive_response(sock))


def io_menu(sock):
    pin = input("Enter IO pin number (e.g., 1, 2, 3): ").strip()
    state = input("Enter state (true/false): ").strip().lower()
    id = generate_command_id()
    command = f"9|0|||{pin}|{state}|0|0|0|{id}"
    send_command(sock, command)
    print("< Response:", receive_response(sock))


def subroutine_menu(sock):
    program_id = input("Enter program ID to call: ").strip()
    id = generate_command_id()
    command = f"41|||0.5|0|||{program_id}|{id}"
    send_command(sock, command)
    print("< Response:", receive_response(sock))


def get_current_joint_state():
    """
    Connects to the joint state publishing port, reads the current joint state,
    displays it, and optionally saves it in XML format.
    """
    print("\n-- Get Current Joint State --")
    print(f"Connecting to joint state publisher at {ROBOT_IP}:{JOINT_STATE_PORT}...")
    
    try:
        with socket.create_connection((ROBOT_IP, JOINT_STATE_PORT), timeout=5) as sock:
            print("✅ Connected to joint state publisher.")
            
            # Read joint state data (format: j1;j2;j3;j4;j5;j6;j7#)
            data = b""
            while not data.endswith(TERMINATOR.encode()):
                chunk = sock.recv(1024)
                if not chunk:
                    raise ConnectionError("Connection closed by robot.")
                data += chunk
            
            message = data.decode().rstrip(TERMINATOR)
            joint_values_rad = [float(val) for val in message.split(";")]
            
            if len(joint_values_rad) != 7:
                print(f"❌ Invalid joint state data. Expected 7 values, got {len(joint_values_rad)}")
                return
            
            # Convert from radians to degrees
            joints_deg = [math.degrees(val) for val in joint_values_rad]
            
            # Display the joint state
            print("\n📊 Current Joint State (in degrees):")
            for i, deg in enumerate(joints_deg, 1):
                print(f"  J{i}: {deg:.6f}°")
            
            # Ask if user wants to save
            save = input("\nDo you want to save this joint state? (y/n): ").strip().lower()
            if save == 'y':
                name = input("Enter a name for this joint state: ").strip()
                if not name:
                    print("❌ Name cannot be empty. Joint state not saved.")
                    return
                
                # Format in the specified XML format with the specified order
                xml_output = (
                    f'<Joints name="{name}" '
                    f'j6="{joints_deg[5]}" j7="{joints_deg[6]}" '
                    f'j1="{joints_deg[0]}" j3="{joints_deg[2]}" '
                    f'j5="{joints_deg[4]}" j2="{joints_deg[1]}" '
                    f'j4="{joints_deg[3]}"/>'
                )
                
                print("\n✅ Joint state saved in XML format:")
                print(xml_output)
            else:
                print("Joint state not saved.")
                
    except socket.timeout:
        print(f"❌ Connection timeout. Could not connect to {ROBOT_IP}:{JOINT_STATE_PORT}")
    except ConnectionRefusedError:
        print(f"❌ Connection refused. Is the robot server running on {ROBOT_IP}:{JOINT_STATE_PORT}?")
    except Exception as e:
        print(f"❌ Error: {e}")


def main():
    print(f"Connecting to robot at {ROBOT_IP}:{TASK_PORT}...")
    try:
        with socket.create_connection((ROBOT_IP, TASK_PORT)) as sock:
            print("✅ Connected. Ready.")
            while True:
                print("\n-- Main Menu --")
                print("1. Move")
                print("2. Activate IO")
                print("3. Call Subroutine")
                print("4. Get Current Joint State")
                print("5. Exit")
                choice = input("Select an option: ").strip()
                if choice == "1":
                    movement_menu(sock)
                elif choice == "2":
                    io_menu(sock)
                elif choice == "3":
                    subroutine_menu(sock)
                elif choice == "4":
                    get_current_joint_state()
                elif choice == "5":
                    print("Goodbye.")
                    break
                else:
                    print("Invalid choice.")
    except Exception as e:
        print(f"[ERROR] {e}")


if __name__ == "__main__":
    main()
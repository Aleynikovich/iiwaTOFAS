import socket
import uuid

ROBOT_IP = "10.66.171.147"
TASK_PORT = 30001
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
                print("4. Exit")
                choice = input("Select an option: ").strip()
                if choice == "1":
                    movement_menu(sock)
                elif choice == "2":
                    io_menu(sock)
                elif choice == "3":
                    subroutine_menu(sock)
                elif choice == "4":
                    print("Goodbye.")
                    break
                else:
                    print("Invalid choice.")
    except Exception as e:
        print(f"[ERROR] {e}")


if __name__ == "__main__":
    main()
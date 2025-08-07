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

def input_floats(prompt, count):
    while True:
        try:
            values = input(prompt).strip().split()
            if len(values) != count:
                print(f"Please enter exactly {count} values.")
                continue
            return [float(v) for v in values]
        except ValueError:
            print("Invalid number. Try again.")

def movement_menu(sock):
    print("\n-- Movement Type --")
    print("1. PTP")
    print("2. LIN")
    print("3. CIRC")
    motion_type = input("Select motion type: ").strip()

    continuous = input("Should motion be continuous? (y/n): ").strip().lower() == 'y'

    points = []
    while True:
        mode = input("Enter point as: [1] Joints (7 values) or [2] Cartesian (X Y Z A B C)? ").strip()
        if mode == "1":
            joints = input_floats("Enter J1-J7 (degrees): ", 7)
            joints_rad = [str(j * 3.141592 / 180.0) for j in joints]
            points.append(";".join(joints_rad))
        elif mode == "2":
            cart = input_floats("Enter X Y Z A B C: ", 6)
            cart_rad = cart[:3] + [str(a * 3.141592 / 180.0) for a in cart[3:]]
            points.append(";".join(str(c) for c in cart_rad))
        else:
            print("Invalid input.")
            continue

        cont = input("Add another point? (y/n): ").strip().lower()
        if cont != 'y':
            break

    id = generate_command_id()
    tool = "testTool"
    base = "testBase"
    speed = 0.5
    num_points = len(points)
    action_code = "1" if motion_type == "1" else "8" if motion_type == "2" else "11"
    joined = "|".join([action_code, tool, base, str(speed), str(num_points), "#MULTI#".join(points), id])
    send_command(sock, joined)
    print("< Response:", receive_response(sock))

def io_menu(sock):
    pin = input("Enter IO pin number (e.g., 1, 2, 3): ").strip()
    state = input("Enter state (true/false): ").strip().lower()
    id = generate_command_id()
    command = f"20|tool|base|0.5|1|true|{pin}|{state}|{id}"
    send_command(sock, command)
    print("< Response:", receive_response(sock))

def subroutine_menu(sock):
    program_id = input("Enter program ID to call: ").strip()
    id = generate_command_id()
    command = f"40|tool|base|0.5|0|||{program_id}|{id}"
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

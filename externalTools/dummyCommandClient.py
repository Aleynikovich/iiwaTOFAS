import socket
import time

ROBOT_IP = '10.66.171.147' # KUKA iiwa robot IP address
ROBOT_COMMAND_PORT = 30001

# Example command string
# ACTION_TYPE(0)|NUM_POINTS(1)|TARGET_POINTS(2)|IO_POINT(3)|IO_PIN(4)|IO_STATE(5)|TOOL(6)|BASE(7)|SPEED_OVERRIDE(8)|ID(9)
# Example: PTP_FRAME (1), 2 points, coordinates, IO_POINT 2, IO_PIN 2, IO_STATE true, TOOL magnet, BASE table, SPEED 0.6, ID 3h2h5
DUMMY_COMMAND_STRING = "1|2|10,10,10,10,10,10,10;23,32,53,34,23,53,23|2|2|true|magnet|table|0.6|3h2h5|"

def send_command_to_robot(command_string):
    """Connects to the robot, sends a command, and waits for a response."""
    try:
        # Create a TCP/IP socket
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
            # Connect to the robot
            print(f"Client: Connecting to robot at {ROBOT_IP}:{ROBOT_COMMAND_PORT}...")
            sock.connect((ROBOT_IP, ROBOT_COMMAND_PORT))
            print("Client: Connected.")

            # Send the command string, followed by a newline (important for readLine() on Java side)
            full_command = command_string + "\n"
            print(f"Client: Sending command: '{full_command.strip()}'")
            sock.sendall(full_command.encode('utf-8'))

            # Wait for response from the robot
            response = sock.recv(1024).decode('utf-8').strip()
            print(f"Client: Received response: '{response}'")
            return response

    except ConnectionRefusedError:
        print(f"Client: Connection refused. Is the robot server running on {ROBOT_IP}:{ROBOT_COMMAND_PORT}?")
        return None
    except socket.timeout:
        print("Client: Connection timed out.")
        return None
    except Exception as e:
        print(f"Client: An error occurred: {e}")
        return None

if __name__ == "__main__":
    print("--- Robot Command Client ---")
    print(f"Default command to send: '{DUMMY_COMMAND_STRING}'")
    print("\nOptions:")
    print("1. Send default command")
    print("2. Enter a custom command")
    print("3. Exit")

    while True:
        choice = input("Enter your choice (1, 2, or 3): ")

        if choice == '1':
            send_command_to_robot(DUMMY_COMMAND_STRING)
        elif choice == '2':
            custom_command = input("Enter the custom command string: ")
            send_command_to_robot(custom_command)
        elif choice == '3':
            print("Client: Exiting.")
            break
        else:
            print("Invalid choice. Please enter 1, 2, or 3.")
        print("-" * 30)
        time.sleep(1) # Small delay before next prompt

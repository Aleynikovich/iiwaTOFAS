# --- log_client.py ---
import socket
import time
import re # Import the regular expression module

SERVER_IP = '10.66.171.147'
SERVER_PORT = 30002 # Log client port

RECONNECT_DELAY_SECONDS = 5 # How long to wait before attempting to reconnect

# ANSI escape codes for coloring
COLOR_RESET = "\033[0m"
COLOR_GREEN = "\033[92m"   # Light Green (for success messages)
COLOR_YELLOW = "\033[93m"  # Light Yellow (for warnings)
COLOR_RED = "\033[91m"     # Light Red (for errors)
COLOR_CYAN = "\033[96m"    # Light Cyan (for general received/sent messages)
COLOR_BLUE = "\033[94m"    # Blue (for command type/ID labels)
COLOR_WHITE = "\033[97m"   # White (for general labels/headers)

# Special delimiters from Java ParsedCommand.java
VALUE_START_DELIMITER = "\u001A" # Substitute (SUB) character
VALUE_END_DELIMITER = "\u001B"   # Escape (ESC) character

# Specific colors for individual joint/Cartesian coordinates (used cyclically)
JOINT_COLORS = [
    "\033[38;5;208m", # Orange
    "\033[38;5;198m", # Pink
    "\033[38;5;220m", # Gold
    "\033[38;5;118m", # Light Green
    "\033[38;5;153m", # Light Blue
    "\033[38;5;214m", # Orange-Yellow
    "\033[38;5;170m"  # Purple-Blue
]

# Specific colors for individual motion parameter values (used cyclically)
PARAM_COLORS = [
    "\033[38;5;46m",  # Bright Green
    "\033[38;5;226m", # Bright Yellow
    "\033[38;5;201m", # Deep Pink
    "\033[38;5;39m",  # Deep Sky Blue
    "\033[38;5;160m", # Red Orange
    "\033[38;5;105m", # Light Purple
    "\033[38;5;123m"  # Light Cyan
]


def colorize_parsed_command(message_content):
    """
    Applies detailed coloring to the ParsedCommand output using embedded delimiters.
    """
    lines = message_content.split('\n')
    colored_lines = []

    # Keep track of the index for cyclic coloring of joints/coords and params
    joint_coord_color_idx = 0
    param_color_idx = 0

    for line in lines:
        temp_line = line

        # Split the line by the delimiters
        # The regex (VALUE_START_DELIMITER + '(.*?)' + VALUE_END_DELIMITER)
        # captures the content between the delimiters.
        # re.split will return a list where odd indices are the captured groups
        # (the values we want to color), and even indices are the text outside.
        parts = re.split(f'{re.escape(VALUE_START_DELIMITER)}(.*?){re.escape(VALUE_END_DELIMITER)}', temp_line)

        colored_parts = []
        for i, part in enumerate(parts):
            if i % 2 == 1: # This is a value that was inside the delimiters
                value_to_color = part

                # Determine which color to apply based on the content/context
                color_to_apply = COLOR_WHITE # Default for values

                # Check for specific patterns to apply distinct colors
                if any(f"J{j}=" in value_to_color for j in range(1, 8)) or \
                        any(f"{coord}=" in value_to_color for coord in ['X', 'Y', 'Z', 'A', 'B', 'C']):
                    color_to_apply = JOINT_COLORS[joint_coord_color_idx % len(JOINT_COLORS)]
                    joint_coord_color_idx += 1
                elif any(label in line for label in ["Speed Override:", "Tool:", "Base:", "Continuous:", "Num Points:", "IO Point:", "IO Pin:", "IO State:", "Program ID:"]):
                    color_to_apply = PARAM_COLORS[param_color_idx % len(PARAM_COLORS)]
                    param_color_idx += 1

                colored_parts.append(f"{color_to_apply}{value_to_color}{COLOR_RESET}")
            else: # This is text outside the delimiters (labels, static text)
                # Apply colors to specific labels/headers
                part = part.replace("ActionType:", f"{COLOR_BLUE}ActionType:{COLOR_RESET}")
                part = part.replace("ID:", f"{COLOR_BLUE}ID:{COLOR_RESET}")
                part = part.replace("--- Movement Command ---", f"{COLOR_WHITE}--- Movement Command ---{COLOR_RESET}")
                part = part.replace("--- IO Command ---", f"{COLOR_WHITE}--- IO Command ---{COLOR_RESET}")
                part = part.replace("--- Program Call ---", f"{COLOR_WHITE}--- Program Call ---{COLOR_RESET}")
                part = part.replace("Axis Target Points", f"{COLOR_WHITE}Axis Target Points{COLOR_RESET}")
                part = part.replace("Cartesian Target Points", f"{COLOR_WHITE}Cartesian Target Points{COLOR_RESET}")
                part = part.replace("Motion Parameters:", f"{COLOR_WHITE}Motion Parameters:{COLOR_RESET}")
                part = part.replace("IO Data:", f"{COLOR_WHITE}IO Data:{COLOR_RESET}")
                part = part.replace("Program ID:", f"{COLOR_WHITE}Program ID:{COLOR_RESET}")
                part = re.sub(r'(Point \d+:)', f"{COLOR_WHITE}\\1{COLOR_RESET}", part) # Color "Point X:"

                # Color labels for motion parameters and IO data
                part = part.replace("Speed Override: ", f"{COLOR_WHITE}Speed Override: {COLOR_RESET}")
                part = part.replace("Tool: ", f"{COLOR_WHITE}Tool: {COLOR_RESET}")
                part = part.replace("Base: ", f"{COLOR_WHITE}Base: {COLOR_RESET}")
                part = part.replace("Continuous: ", f"{COLOR_WHITE}Continuous: {COLOR_RESET}")
                part = part.replace("Num Points: ", f"{COLOR_WHITE}Num Points: {COLOR_RESET}")
                part = part.replace("IO Point: ", f"{COLOR_WHITE}IO Point: {COLOR_RESET}")
                part = part.replace("IO Pin: ", f"{COLOR_WHITE}IO Pin: {COLOR_RESET}")
                part = part.replace("IO State: ", f"{COLOR_WHITE}IO State: {COLOR_RESET}")

                colored_parts.append(part)

        colored_lines.append("".join(colored_parts))

    return "\n".join(colored_lines) + COLOR_RESET

def run_log_client():
    """
    Runs the log client, attempting to connect to the server and
    reconnecting automatically if the connection is lost or refused.
    """
    print("Starting log client...")
    while True: # Outer loop for continuous reconnection attempts
        client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        try:
            print(f"Attempting to connect to server at {SERVER_IP}:{SERVER_PORT}...")
            client_socket.connect((SERVER_IP, SERVER_PORT))
            print(f"Connected to server at {SERVER_IP}:{SERVER_PORT}\n")

            # Inner loop for receiving data while connected
            while True:
                try:
                    data = client_socket.recv(4096) # Increased buffer size for potentially larger log messages
                    if not data:
                        print("Server closed the connection. Attempting to reconnect...\n")
                        break # Break inner loop to trigger reconnection

                    received_message = data.decode()

                    colored_message = received_message # Default to no color

                    # Apply general coloring first
                    if "Error:" in received_message:
                        colored_message = f"{COLOR_RED}{received_message}{COLOR_RESET}"
                    elif "Warning:" in received_message:
                        colored_message = f"{COLOR_YELLOW}{received_message}{COLOR_RESET}"
                    elif "Successfully parsed command:" in received_message:
                        colored_message = colorize_parsed_command(received_message) # Call helper function for detailed coloring
                    elif "Received:" in received_message or "Sent response:" in received_message:
                        colored_message = f"{COLOR_CYAN}{received_message}{COLOR_RESET}"

                    # Add a final reset for other messages that might not have one
                    if not colored_message.endswith(COLOR_RESET):
                        colored_message += COLOR_RESET

                    print(f"{colored_message}")

                except BlockingIOError:
                    pass
                except ConnectionResetError:
                    print("Server forcibly closed the connection. Attempting to reconnect...\n")
                    break
                except Exception as e:
                    print(f"An error occurred during communication: {e}. Attempting to reconnect...\n")
                    break

        except ConnectionRefusedError:
            print(f"Connection refused. Is the server running on {SERVER_IP}:{SERVER_PORT}? Retrying in {RECONNECT_DELAY_SECONDS} seconds...\n")
        except Exception as e:
            print(f"An unexpected error occurred: {e}. Retrying in {RECONNECT_DELAY_SECONDS} seconds...\n")
        finally:
            if client_socket:
                client_socket.close()

        time.sleep(RECONNECT_DELAY_SECONDS)

if __name__ == '__main__':
    run_log_client()

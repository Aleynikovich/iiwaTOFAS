# --- log_client.py ---
import socket
import time
import json # Import the json module
import re   # Still needed for general log message parsing

SERVER_IP = '10.66.171.147'
SERVER_PORT = 30002 # Log client port

RECONNECT_DELAY_SECONDS = 5 # How long to wait before attempting to reconnect

# ANSI escape codes for coloring
COLOR_RESET = "\033[0m"
COLOR_GREEN = "\033[92m"   # Light Green (for success messages)
COLOR_YELLOW = "\033[93m"  # Light Yellow (for warnings)
COLOR_RED = "\033[91m"     # Light Red (for errors)
COLOR_CYAN = "\033[96m"    # Light Cyan (for general received/sent messages)
COLOR_BLUE = "\033[94m"    # Blue (for JSON keys/labels)
COLOR_MAGENTA = "\033[95m" # Magenta (for JSON string values)
COLOR_WHITE = "\033[97m"   # White (for JSON numeric/boolean values)
COLOR_LIGHT_BLUE = "\033[38;5;111m" # A softer blue for some values

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
    "\033[38;5;46m",  # Bright Green (Speed Override)
    "\033[38;5;226m", # Bright Yellow (Tool)
    "\033[38;5;201m", # Deep Pink (Base)
    "\033[38;5;39m",  # Deep Sky Blue (Continuous)
    "\033[38;5;160m", # Red Orange (Num Points)
    "\033[38;5;105m", # Light Purple (IO Point)
    "\033[38;5;123m"  # Light Cyan (IO Pin)
]

# Global index for cyclic coloring (reset per ParsedCommand)
_joint_coord_color_idx = 0
_param_color_idx = 0

def format_json_with_colors(data, indent=0):
    """
    Recursively formats and colors JSON data for terminal output.
    """
    global _joint_coord_color_idx, _param_color_idx # Use global indices

    indent_str = "  " * indent
    output = []

    if isinstance(data, dict):
        output.append(f"{indent_str}{{")
        for i, (key, value) in enumerate(data.items()):
            # Reset indices for new command block (top level)
            if indent == 0 and key == "actionType":
                _joint_coord_color_idx = 0
                _param_color_idx = 0

            # Color keys (labels)
            colored_key = f"{COLOR_BLUE}\"{key}\"{COLOR_RESET}"

            # Handle specific values with their own colors
            if key in ["actionType", "id", "actionValue", "commandType", "programId"]:
                colored_value = f"{COLOR_GREEN}{json.dumps(value)}{COLOR_RESET}"
            elif key in ["J1", "J2", "J3", "J4", "J5", "J6", "J7", "X", "Y", "Z", "A", "B", "C"]:
                color = JOINT_COLORS[_joint_coord_color_idx % len(JOINT_COLORS)]
                colored_value = f"{color}{json.dumps(value)}{COLOR_RESET}"
                _joint_coord_color_idx += 1
            elif key in ["speedOverride", "tool", "base", "continuous", "numPoints", "ioPoint", "ioPin", "ioState"]:
                color = PARAM_COLORS[_param_color_idx % len(PARAM_COLORS)]
                colored_value = f"{color}{json.dumps(value)}{COLOR_RESET}"
                _param_color_idx += 1
            elif isinstance(value, (int, float, bool)):
                colored_value = f"{COLOR_WHITE}{json.dumps(value)}{COLOR_RESET}"
            elif isinstance(value, str):
                colored_value = f"{COLOR_MAGENTA}{json.dumps(value)}{COLOR_RESET}"
            else:
                colored_value = format_json_with_colors(value, indent + 1) # Recurse for nested dicts/lists

            output.append(f"{indent_str}  {colored_key}: {colored_value}{',' if i < len(data) - 1 else ''}")
        output.append(f"{indent_str}}}")
    elif isinstance(data, list):
        output.append(f"{indent_str}[")
        for i, item in enumerate(data):
            output.append(f"{indent_str}  {format_json_with_colors(item, indent + 1)}{',' if i < len(data) - 1 else ''}")
        output.append(f"{indent_str}]")
    else:
        # For primitive types not handled above (e.g., if a top-level value is not dict/list)
        if isinstance(data, (int, float, bool)):
            return f"{COLOR_WHITE}{json.dumps(data)}{COLOR_RESET}"
        elif isinstance(data, str):
            return f"{COLOR_MAGENTA}{json.dumps(data)}{COLOR_RESET}"
        else:
            return json.dumps(data) # Fallback

    return "\n".join(output)

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

                    # Check if the message is a "Successfully parsed command:" log containing JSON
                    if "Successfully parsed command:" in received_message:
                        # Extract the JSON part
                        json_start_index = received_message.find("{")
                        if json_start_index != -1:
                            log_prefix = received_message[:json_start_index]
                            json_string = received_message[json_start_index:]

                            try:
                                parsed_json = json.loads(json_string)
                                # Colorize the log prefix
                                colored_prefix = f"{COLOR_GREEN}{log_prefix}{COLOR_RESET}"
                                # Format and colorize the JSON data
                                colored_json = format_json_with_colors(parsed_json)
                                colored_message = f"{colored_prefix}\n{colored_json}"
                            except json.JSONDecodeError:
                                # Not valid JSON, fall back to simple coloring
                                colored_message = f"{COLOR_GREEN}{received_message}{COLOR_RESET}"
                        else:
                            # "Successfully parsed command:" but no JSON found, simple color
                            colored_message = f"{COLOR_GREEN}{received_message}{COLOR_RESET}"
                    elif "Error:" in received_message:
                        colored_message = f"{COLOR_RED}{received_message}{COLOR_RESET}"
                    elif "Warning:" in received_message:
                        colored_message = f"{COLOR_YELLOW}{received_message}{COLOR_RESET}"
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

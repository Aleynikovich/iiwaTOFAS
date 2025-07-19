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
COLOR_HEADER = "\033[92m"   # Light Green for "Successfully parsed command:"
COLOR_ERROR = "\033[91m"    # Light Red for errors
COLOR_WARNING = "\033[93m"  # Light Yellow for warnings
COLOR_INFO = "\033[96m"     # Light Cyan for general info (Received/Sent)

# Colors for JSON keys (labels) and general values
COLOR_JSON_KEY = "\033[94m" # Blue for all JSON keys
COLOR_STRING_VALUE_DEFAULT = "\033[95m" # Magenta for string values (if no specific color)
COLOR_NUMBER_VALUE_DEFAULT = "\033[97m" # White for number/boolean values (if no specific color)

# Explicit mapping of JSON field names to their desired colors
FIELD_VALUE_COLORS = {
    # Top-level Command Info
    "actionType": "\033[38;5;46m",    # Bright Green
    "actionValue": "\033[38;5;46m",   # Bright Green
    "id": "\033[38;5;111m",           # Light Blue
    "commandType": "\033[38;5;153m",  # Light Cyan

    # Joint Coordinates (AxisPosition)
    "J1": "\033[38;5;208m", # Orange
    "J2": "\033[38;5;198m", # Pink
    "J3": "\033[38;5;220m", # Gold
    "J4": "\033[38;5;118m", # Light Green
    "J5": "\033[38;5;153m", # Light Blue
    "J6": "\033[38;5;214m", # Orange-Yellow
    "J7": "\033[38;5;170m", # Purple-Blue

    # Cartesian Coordinates (CartesianPosition)
    "X": "\033[38;5;208m", # Orange
    "Y": "\033[38;5;198m", # Pink
    "Z": "\033[38;5;220m", # Gold
    "A": "\033[38;5;118m", # Light Green
    "B": "\033[38;5;153m", # Light Blue
    "C": "\033[38;5;214m", # Orange-Yellow

    # Motion Parameters
    "speedOverride": "\033[38;5;46m", # Bright Green
    "tool": "\033[38;5;226m",        # Bright Yellow
    "base": "\033[38;5;201m",        # Deep Pink
    "continuous": "\033[38;5;39m",   # Deep Sky Blue
    "numPoints": "\033[38;5;160m",   # Red Orange

    # IO Command Data
    "ioPoint": "\033[38;5;105m",     # Light Purple
    "ioPin": "\033[38;5;123m",       # Light Cyan
    "ioState": "\033[38;5;46m",      # Bright Green (reusing)

    # Program Call
    "programId": "\033[38;5;153m"    # Light Blue (reusing)
}


def format_json_with_colors(data, indent=0):
    """
    Recursively formats and colors JSON data for terminal output based on FIELD_VALUE_COLORS.
    """
    indent_str = "  " * indent
    output = []

    if isinstance(data, dict):
        output.append(f"{indent_str}{{")
        for i, (key, value) in enumerate(data.items()):
            # Color key (label)
            colored_key = f"{COLOR_JSON_KEY}\"{key}\"{COLOR_RESET}"

            # Determine color for value based on the key, falling back to type-based defaults
            value_color = FIELD_VALUE_COLORS.get(key)

            # Prepare value string using json.dumps first. json.dumps handles quotes for strings.
            # This ensures that the raw_value_str includes JSON-specific formatting (e.g., quotes around strings).
            raw_value_str = json.dumps(value)

            colored_value = ""
            if isinstance(value, dict) or isinstance(value, list):
                # Recursively format nested structures
                colored_value = format_json_with_colors(value, indent + 1)
            else:
                # Apply specific color from FIELD_VALUE_COLORS if defined, else type-based default
                if value_color:
                    colored_value = f"{value_color}{raw_value_str}{COLOR_RESET}"
                elif isinstance(value, str):
                    colored_value = f"{COLOR_STRING_VALUE_DEFAULT}{raw_value_str}{COLOR_RESET}"
                elif isinstance(value, (int, float, bool)) or value is None:
                    colored_value = f"{COLOR_NUMBER_VALUE_DEFAULT}{raw_value_str}{COLOR_RESET}"
                else:
                    colored_value = raw_value_str # Fallback for other types

            output.append(f"{indent_str}  {colored_key}: {colored_value}{',' if i < len(data) - 1 else ''}")
        output.append(f"{indent_str}}}")
    elif isinstance(data, list):
        output.append(f"{indent_str}[")
        for i, item in enumerate(data):
            # Recursively format each item in the list
            output.append(f"{indent_str}  {format_json_with_colors(item, indent + 1)}{',' if i < len(data) - 1 else ''}")
        output.append(f"{indent_str}]")
    else:
        # Handle top-level primitive types if they ever occur
        if isinstance(data, str):
            return f"{COLOR_STRING_VALUE_DEFAULT}{json.dumps(data)}{COLOR_RESET}"
        elif isinstance(data, (int, float, bool)) or data is None:
            return f"{COLOR_NUMBER_VALUE_DEFAULT}{json.dumps(data)}{COLOR_RESET}"
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
            print(f"Connected to server at {SERVER_IP}:{SERVER_PORT}")

            # Inner loop for receiving data while connected
            while True:
                try:
                    data = client_socket.recv(4096)
                    if not data:
                        print("Server closed the connection.")
                        break

                    received_message = data.decode()

                    colored_output = received_message # Default to raw message

                    # Check if the message is a "Successfully parsed command:" log
                    if "Successfully parsed command:" in received_message:
                        # Extract the log prefix (timestamp, client info)
                        log_prefix_match = re.match(r'^(.*?Successfully parsed command:)', received_message)
                        log_prefix = log_prefix_match.group(1) if log_prefix_match else ""

                        # The JSON string starts immediately after the prefix
                        json_string = received_message[len(log_prefix):].strip()

                        try:
                            parsed_json = json.loads(json_string)
                            # Colorize the log prefix
                            colored_prefix = f"{COLOR_HEADER}{log_prefix}{COLOR_RESET}"
                            # Format and colorize the JSON data
                            colored_json_data = format_json_with_colors(parsed_json)
                            colored_output = f"{colored_prefix}\n{colored_json_data}"
                        except json.JSONDecodeError as e:
                            # If it's not valid JSON, log the error and print the raw message
                            colored_output = f"{COLOR_HEADER}{received_message}{COLOR_RESET}"
                            print(f"{COLOR_ERROR}JSON Decode Error: {e} - Raw JSON string: {json_string}{COLOR_RESET}")
                        except Exception as e:
                            # Catch other potential errors during coloring/formatting
                            colored_output = f"{COLOR_ERROR}Error during JSON formatting: {e}\nRaw message: {received_message}{COLOR_RESET}"
                    elif "Error:" in received_message:
                        colored_output = f"{COLOR_ERROR}{received_message}{COLOR_RESET}"
                    elif "Warning:" in received_message:
                        colored_output = f"{COLOR_WARNING}{received_message}{COLOR_RESET}"
                    elif "Received:" in received_message or "Sent response:" in received_message:
                        colored_output = f"{COLOR_INFO}{received_message}{COLOR_RESET}"

                    # Print the final colored output
                    print(f"{colored_output}")

                except BlockingIOError:
                    pass
                except ConnectionResetError:
                    print("Server forcibly closed the connection.")
                    break
                except Exception as e:
                    print(f"An error occurred during communication: {e}.")
                    break

        except ConnectionRefusedError:
            print(f"Connection refused. Is the server running on {SERVER_IP}:{SERVER_PORT}? Retrying in {RECONNECT_DELAY_SECONDS} seconds...")
        except Exception as e:
            print(f"An unexpected error occurred: {e}. Retrying in {RECONNECT_DELAY_SECONDS} seconds...")
        finally:
            if client_socket:
                client_socket.close()

        time.sleep(RECONNECT_DELAY_SECONDS)

if __name__ == '__main__':
    run_log_client()

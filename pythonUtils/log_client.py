# --- log_client.py ---
import socket
import time

SERVER_IP = '10.66.171.147'
SERVER_PORT = 30002 # Log client port

RECONNECT_DELAY_SECONDS = 5 # How long to wait before attempting to reconnect

# ANSI escape codes for coloring
# You can find more colors here: https://en.wikipedia.org/wiki/ANSI_escape_code#Colors
COLOR_RESET = "\033[0m"
COLOR_GREEN = "\033[92m" # Light Green (for success messages)
COLOR_YELLOW = "\033[93m" # Light Yellow (for warnings)
COLOR_RED = "\033[91m"   # Light Red (for errors)
COLOR_CYAN = "\033[96m"  # Light Cyan (for general received/sent messages)
COLOR_BLUE = "\033[94m"  # Blue (for command type/ID)
COLOR_MAGENTA = "\033[95m" # Magenta (for parameters/points)
COLOR_WHITE = "\033[97m"  # White (for labels)

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
                        # For parsed commands, apply more detailed coloring
                        colored_message = received_message.replace("Successfully parsed command:", f"{COLOR_GREEN}Successfully parsed command:{COLOR_RESET}")

                        # Apply colors to specific parts of the ParsedCommand output
                        colored_message = colored_message.replace("ActionType:", f"{COLOR_BLUE}ActionType:{COLOR_RESET}")
                        colored_message = colored_message.replace("ID:", f"{COLOR_BLUE}ID:{COLOR_RESET}")
                        colored_message = colored_message.replace("--- Movement Command ---", f"{COLOR_WHITE}--- Movement Command ---{COLOR_RESET}")
                        colored_message = colored_message.replace("--- IO Command ---", f"{COLOR_WHITE}--- IO Command ---{COLOR_RESET}")
                        colored_message = colored_message.replace("--- Program Call ---", f"{COLOR_WHITE}--- Program Call ---{COLOR_RESET}")
                        colored_message = colored_message.replace("Axis Target Points", f"{COLOR_WHITE}Axis Target Points{COLOR_RESET}")
                        colored_message = colored_message.replace("Cartesian Target Points", f"{COLOR_WHITE}Cartesian Target Points{COLOR_RESET}")
                        colored_message = colored_message.replace("Motion Parameters:", f"{COLOR_WHITE}Motion Parameters:{COLOR_RESET}")
                        colored_message = colored_message.replace("IO Data:", f"{COLOR_WHITE}IO Data:{COLOR_RESET}")
                        colored_message = colored_message.replace("Program ID:", f"{COLOR_WHITE}Program ID:{COLOR_RESET}")

                        # Color the values themselves (e.g., J1=..., X=..., Speed Override: ...)
                        # This is a more complex regex approach if values need specific coloring,
                        # but for now, the labels are colored, making values stand out by contrast.

                        # Example for specific value coloring (can be expanded)
                        # Speed Override value
                        colored_message = colored_message.replace("Speed Override: ", f"{COLOR_WHITE}Speed Override: {COLOR_MAGENTA}")
                        # Tool value
                        colored_message = colored_message.replace("Tool: ", f"{COLOR_WHITE}Tool: {COLOR_MAGENTA}")
                        # Base value
                        colored_message = colored_message.replace("Base: ", f"{COLOR_WHITE}Base: {COLOR_MAGENTA}")
                        # Continuous value
                        colored_message = colored_message.replace("Continuous: ", f"{COLOR_WHITE}Continuous: {COLOR_MAGENTA}")
                        # Num Points value
                        colored_message = colored_message.replace("Num Points: ", f"{COLOR_WHITE}Num Points: {COLOR_MAGENTA}")
                        # IO Point/Pin/State values
                        colored_message = colored_message.replace("IO Point: ", f"{COLOR_WHITE}IO Point: {COLOR_MAGENTA}")
                        colored_message = colored_message.replace("IO Pin: ", f"{COLOR_WHITE}IO Pin: {COLOR_MAGENTA}")
                        colored_message = colored_message.replace("IO State: ", f"{COLOR_WHITE}IO State: {COLOR_MAGENTA}")

                        # Ensure reset after each colored segment for multi-line output
                        colored_message = colored_message.replace("\n", f"{COLOR_RESET}\n")
                        # Add final reset in case the message ends abruptly
                        colored_message += COLOR_RESET


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

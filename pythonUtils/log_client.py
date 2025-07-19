# --- log_client.py ---
import socket
import time

SERVER_IP = '10.66.171.147'
SERVER_PORT = 30002 # Log client port

RECONNECT_DELAY_SECONDS = 5 # How long to wait before attempting to reconnect

# ANSI escape codes for coloring
# You can find more colors here: https://en.wikipedia.org/wiki/ANSI_escape_code#Colors
COLOR_RESET = "\033[0m"
COLOR_GREEN = "\033[92m" # Light Green
COLOR_YELLOW = "\033[93m" # Light Yellow
COLOR_RED = "\033[91m"   # Light Red
COLOR_CYAN = "\033[96m"  # Light Cyan
COLOR_BLUE = "\033[94m"  # Blue

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

                    # Apply coloring based on message content (simple example)
                    # You can expand this logic for more granular coloring
                    if "Error:" in received_message:
                        colored_message = f"{COLOR_RED}{received_message}{COLOR_RESET}"
                    elif "Warning:" in received_message:
                        colored_message = f"{COLOR_YELLOW}{received_message}{COLOR_RESET}"
                    elif "Successfully parsed command:" in received_message:
                        colored_message = f"{COLOR_GREEN}{received_message}{COLOR_RESET}"
                    elif "Received:" in received_message or "Sent response:" in received_message:
                        colored_message = f"{COLOR_CYAN}{received_message}{COLOR_RESET}"
                    else:
                        colored_message = received_message # Default color (no change)

                    print(f"{colored_message}") # MODIFIED: Removed the trailing \n

                    # No sleep here, as logs might come in rapidly.
                    # The server's heartbeat already has a delay.
                except BlockingIOError:
                    # This should not typically happen with blocking sockets unless explicitly set
                    pass
                except ConnectionResetError:
                    print("Server forcibly closed the connection. Attempting to reconnect...\n")
                    break # Break inner loop to trigger reconnection
                except Exception as e:
                    print(f"An error occurred during communication: {e}. Attempting to reconnect...\n")
                    break # Break inner loop to trigger reconnection

        except ConnectionRefusedError:
            print(f"Connection refused. Is the server running on {SERVER_IP}:{SERVER_PORT}? Retrying in {RECONNECT_DELAY_SECONDS} seconds...\n")
        except Exception as e:
            print(f"An unexpected error occurred: {e}. Retrying in {RECONNECT_DELAY_SECONDS} seconds...\n")
        finally:
            # Ensure the socket is closed before the next reconnection attempt
            if client_socket:
                client_socket.close()
                # print("Client socket closed.\n") # Removed for cleaner output during rapid retries

        # Wait before attempting to reconnect
        time.sleep(RECONNECT_DELAY_SECONDS)

if __name__ == '__main__':
    run_log_client()

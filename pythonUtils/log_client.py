import socket
import time

SERVER_IP = '10.66.171.147'
SERVER_PORT = 30002 # Log client port

RECONNECT_DELAY_SECONDS = 5 # How long to wait before attempting to reconnect

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
                    data = client_socket.recv(4096) # Increased buffer size for potentially larger log messages
                    if not data:
                        print("Server closed the connection.")
                        break # Break inner loop to trigger reconnection

                    received_message = data.decode()

                    # Print the received message WITHOUT adding an extra newline
                    # The 'end=""' argument tells print not to add its default newline.
                    print(f"{received_message}", end="")

                except BlockingIOError:
                    pass
                except ConnectionResetError:
                    print("Server forcibly closed the connection.")
                    break # Break inner loop to trigger reconnection
                except Exception as e:
                    print(f"An error occurred during communication: {e}")
                    break # Break inner loop to trigger reconnection

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

import socket
import sys
import threading
import time

# --- Configuration ---
ROBOT_IP = "10.66.171.147"  # The IP address of your KUKA iiwa robot
LOG_SERVER_PORT = 30003    # The port your LogServer is listening on
LOG_FILE_NAME = "robot_activity.log" # Name of the file to write logs to

# --- Client Functionality ---
def receive_logs(sock, log_file):
    """Continuously receives and prints log messages from the server, and writes them to a log file."""
    buffer = ""
    try:
        while True:
            # Receive data in chunks
            # Adjust buffer size as needed, 4096 bytes is a common default
            data = sock.recv(4096).decode('utf-8')
            if not data:
                # Server closed the connection
                print("\n[INFO] Server disconnected.")
                break

            buffer += data
            # Process messages line by line
            while "\n" in buffer:
                line, buffer = buffer.split("\n", 1)
                formatted_line = f"[ROBOT LOG] {line.strip()}"
                print(formatted_line)
                if log_file:
                    log_file.write(formatted_line + "\n")
                    log_file.flush() # Ensure data is written to disk immediately

    except ConnectionResetError:
        print("\n[ERROR] Connection reset by peer. Server might have closed unexpectedly.")
    except UnicodeDecodeError:
        print("\n[ERROR] Failed to decode received data. Check encoding.")
    except Exception as e:
        print(f"\n[ERROR] An unexpected error occurred during reception: {e}")
    finally:
        # Ensure the socket is closed if the loop breaks
        sock.close()

def main():
    """Main function to set up and run the log client."""
    client_socket = None
    log_file = None # Initialize log_file to None
    try:
        # Open the log file in append mode
        log_file = open(LOG_FILE_NAME, 'a', encoding='utf-8')
        print(f"Opened log file: {LOG_FILE_NAME}")

        # Create a TCP/IP socket
        client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

        # Connect the socket to the server's address and port
        client_socket.connect((ROBOT_IP, LOG_SERVER_PORT))
        print(f"Successfully connected to Log Server at {ROBOT_IP}:{LOG_SERVER_PORT}")
        print("Waiting for log messages from the robot...")
        print("Press Ctrl+C to exit.")

        # Start a thread to continuously receive messages, passing the log_file object
        receive_thread = threading.Thread(target=receive_logs, args=(client_socket, log_file))
        receive_thread.daemon = True  # Allow the main program to exit even if this thread is running
        receive_thread.start()

        # Keep the main thread alive to allow the receive_thread to run
        # This loop will run indefinitely until Ctrl+C is pressed,
        # or the receive_thread signals a disconnection.
        while receive_thread.is_alive():
            time.sleep(1) # Sleep to avoid busy-waiting

    except ConnectionRefusedError:
        print(f"[ERROR] Connection refused. Is the Log Server running on {ROBOT_IP}:{LOG_SERVER_PORT}?")
    except socket.timeout:
        print("[ERROR] Connection timed out. Check IP address and network connectivity.")
    except IOError as e:
        print(f"[ERROR] Could not open/write to log file {LOG_FILE_NAME}: {e}")
    except Exception as e:
        print(f"[ERROR] An unexpected error occurred: {e}")
    finally:
        if client_socket:
            client_socket.close()
            print("[INFO] Socket closed.")
        if log_file:
            log_file.close() # Ensure the log file is closed
            print(f"[INFO] Log file '{LOG_FILE_NAME}' closed.")
        print("[INFO] Log client exited.")

if __name__ == "__main__":
    main()

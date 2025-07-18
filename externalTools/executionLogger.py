import socket
import threading
import time
import sys

# Configuration for the Execution Log Server
HOST = '127.0.0.1'  # The IP address of the KUKA controller (or your PC if testing locally)
PORT = 30004        # The port of the Execution Log Server
RECONNECT_DELAY = 5 # Seconds to wait before attempting to reconnect

# Event to signal a disconnection
disconnected_event = threading.Event()

def receive_messages(sock):
    """Continuously receives and prints messages from the socket."""
    try:
        while True:
            # Receive data in chunks. Adjust buffer size if messages are very long.
            message = sock.recv(4096).decode('utf-8').strip()
            if not message:
                print("Server disconnected gracefully.")
                break # Exit loop if no data is received (server closed connection)
            print(f"[EXECUTION LOG] {message}")
    except socket.timeout:
        print("Socket timed out while receiving. Connection might be lost.")
    except OSError as e:
        # Check for common "socket closed" or "connection reset" errors
        if "Bad file descriptor" in str(e) or "Connection reset by peer" in str(e) or "Broken pipe" in str(e):
            print(f"Connection lost unexpectedly: {e}")
        else:
            print(f"An OS error occurred while receiving: {e}")
    except Exception as e:
        print(f"An unexpected error occurred while receiving: {e}")
    finally:
        print("Stopped receiving messages. Signalling main thread to reconnect.")
        disconnected_event.set() # Set the event to signal disconnection

def connect_to_server():
    """Attempts to establish a connection to the server."""
    client_socket = None
    try:
        client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        client_socket.settimeout(10) # Set a timeout for connection attempts
        print(f"Attempting to connect to Execution Log Server at {HOST}:{PORT}...")
        client_socket.connect((HOST, PORT))
        client_socket.settimeout(None) # Remove timeout after successful connection
        print(f"Successfully connected to Execution Log Server at {HOST}:{PORT}.")
        return client_socket
    except ConnectionRefusedError:
        print(f"Connection refused. Is the Execution Log Server running on {HOST}:{PORT}?")
    except socket.timeout:
        print(f"Connection timed out. Could not connect to {HOST}:{PORT}.")
    except OSError as e:
        print(f"OS error during connection: {e}")
    except Exception as e:
        print(f"An unexpected error occurred during connection: {e}")
    finally:
        if client_socket and not disconnected_event.is_set(): # Only close if not successfully connected
            if not client_socket._closed and not client_socket.fileno() == -1: # Check if socket is still open
                try:
                    client_socket.shutdown(socket.SHUT_RDWR) # Attempt graceful shutdown
                    client_socket.close()
                except OSError:
                    pass # Already closed or not connected
                except Exception as e:
                    print(f"Error closing socket during failed connection attempt: {e}")

    return None # Return None on failure

def main():
    current_socket = None
    receive_thread = None

    while True:
        if current_socket is None or disconnected_event.is_set():
            if disconnected_event.is_set():
                print(f"Disconnected. Attempting to reconnect in {RECONNECT_DELAY} seconds...")
                time.sleep(RECONNECT_DELAY) # Wait before retrying
                disconnected_event.clear() # Clear the event for the next attempt

            current_socket = connect_to_server()

            if current_socket:
                # If a previous receive thread exists, ensure it's not running
                if receive_thread and receive_thread.is_alive():
                    # This scenario should ideally not happen if disconnected_event is set correctly,
                    # but as a safeguard, you might want to try joining/stopping it.
                    # For simplicity, we'll assume the old thread will die soon after its socket is closed.
                    pass

                receive_thread = threading.Thread(target=receive_messages, args=(current_socket,))
                receive_thread.daemon = True
                receive_thread.start()
            else:
                # If connection failed, ensure disconnected_event is set to trigger retry logic
                disconnected_event.set()

        # Keep the main thread alive, but allow it to be interrupted (e.g., Ctrl+C)
        try:
            time.sleep(1) # Small sleep to avoid busy-waiting
        except KeyboardInterrupt:
            print("\nKeyboardInterrupt detected. Exiting.")
            break
        except Exception as e:
            print(f"Main loop error: {e}")
            break

    # Cleanup before exiting
    if current_socket:
        try:
            current_socket.shutdown(socket.SHUT_RDWR)
            current_socket.close()
            print("Client socket closed.")
        except OSError:
            pass # Socket already closed or not connected
        except Exception as e:
            print(f"Error during final socket close: {e}")
    print("Execution Log Client terminated.")

if __name__ == "__main__":
    main()
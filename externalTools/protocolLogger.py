import socket
import threading
import time
import sys

# Configuration for the Protocol Log Server
HOST = '10.66.171.147' # The IP address of the KUKA controller
PORT = 30003        # The port of the Protocol Log Server
RECONNECT_DELAY = 5 # Seconds to wait before attempting to reconnect

# Event to signal a disconnection
disconnected_event = threading.Event()

def receive_messages(sock):
    """Continuously receives and prints messages from the socket."""
    try:
        while True:
            # Receive data in chunks. Adjust buffer size if messages are very long.
            # recv() will return an empty bytes object if the peer gracefully closes the connection.
            message = sock.recv(4096)
            if not message:
                print("Server disconnected gracefully.")
                break # Exit loop if no data is received (server closed connection)
            print(f"[PROTOCOL LOG] {message.decode('utf-8').strip()}")
    except socket.timeout:
        print("Socket timed out while receiving. Connection might be lost.")
    except OSError as e:
        # Check for common "socket closed" or "connection reset" errors
        # Errno 9 (Bad file descriptor) often means the socket was closed by another thread or the peer.
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
        client_socket.settimeout(10) # Set a timeout for connection attempts (e.g., 10 seconds)
        print(f"Attempting to connect to Protocol Log Server at {HOST}:{PORT}...")
        client_socket.connect((HOST, PORT))
        client_socket.settimeout(None) # Remove timeout after successful connection for blocking recv()
        print(f"Successfully connected to Protocol Log Server at {HOST}:{PORT}.")
        return client_socket # Return the successfully connected socket
    except Exception as e: # Catch all exceptions during connection
        print(f"Connection attempt failed: {e}")
        if client_socket: # If socket was created, close it before returning None
            try:
                client_socket.close()
            except Exception as close_e:
                print(f"Error closing socket after failed connection: {close_e}")
        return None # Return None on failure

def main():
    current_socket = None
    receive_thread = None

    while True:
        # If not connected or a disconnection event occurred, attempt to reconnect
        if current_socket is None or disconnected_event.is_set():
            if disconnected_event.is_set():
                print(f"Disconnected. Attempting to reconnect in {RECONNECT_DELAY} seconds...")
                time.sleep(RECONNECT_DELAY) # Wait before retrying
                disconnected_event.clear() # Clear the event for the next attempt

            current_socket = connect_to_server()

            if current_socket:
                # If a new socket was successfully connected, start a new receive thread for it
                # Ensure previous thread is not active (though it should have died and set the event)
                if receive_thread and receive_thread.is_alive():
                    print("Warning: Old receive thread is still alive. This should not happen if disconnection was handled correctly.")
                    # In a more complex scenario, you might need to explicitly stop the old thread.
                    # For now, we assume it will die soon as its socket is closed.

                receive_thread = threading.Thread(target=receive_messages, args=(current_socket,))
                receive_thread.daemon = True # Daemon threads exit when the main program exits
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
            # Attempt graceful shutdown of the socket
            current_socket.shutdown(socket.SHUT_RDWR)
            current_socket.close()
            print("Client socket closed.")
        except OSError:
            pass # Socket might already be closed or not connected
        except Exception as e:
            print(f"Error during final socket close: {e}")
    print("Protocol Log Client terminated.")

if __name__ == "__main__":
    main()
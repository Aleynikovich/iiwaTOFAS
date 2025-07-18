import socket
import threading
import datetime
import os

HOST = '0.0.0.0'  # Listen on all available interfaces
PORT = 30003
LOG_FILE = 'robot_logs.log' # Name of the log file

# Global file object for logging
log_file_handle = None
log_file_lock = threading.Lock() # To ensure thread-safe writing to the log file

def write_to_log_file(message):
    """Writes a timestamped message to the global log file."""
    global log_file_handle
    with log_file_lock:
        if log_file_handle:
            timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S.%f")[:-3] # Milliseconds precision
            log_file_handle.write(f"[{timestamp}] {message}\n")
            log_file_handle.flush() # Ensure data is written to disk immediately

def handle_client(client_socket, addr):
    """Handles communication with a single client."""
    log_message = f"UbuntuLogServer: Client connected from {addr}"
    print(log_message)
    write_to_log_file(log_message)

    try:
        while True:
            # Read data sent by the client, up to 1024 bytes
            data = client_socket.recv(1024)
            if not data:
                # No data means the client has closed the connection
                break
            # Decode bytes to string, assuming UTF-8
            log_message = data.decode('utf-8').strip()
            print(f"RECEIVED LOG from {addr}: {log_message}")
            write_to_log_file(f"RECEIVED LOG from {addr}: {log_message}")
    except Exception as e:
        error_message = f"UbuntuLogServer: ClientHandler error for {addr}: {e}"
        print(error_message)
        write_to_log_file(error_message)
    finally:
        client_socket.close()
        disconnect_message = f"UbuntuLogServer: Client disconnected: {addr}"
        print(disconnect_message)
        write_to_log_file(disconnect_message)

def main():
    """Starts the main TCP server."""
    global log_file_handle
    server_socket = None
    try:
        # Open the log file in append mode
        log_file_handle = open(LOG_FILE, 'a')
        initial_message = f"UbuntuLogServer: Log file '{LOG_FILE}' opened."
        print(initial_message)
        write_to_log_file(initial_message)

        # Create a TCP/IP socket
        server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        # Allow reuse of the address (useful for quick restarts)
        server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        # Bind the socket to the port
        server_socket.bind((HOST, PORT))
        # Listen for incoming connections (up to 5 queued connections)
        server_socket.listen(5)

        start_message = f"UbuntuLogServer: Starting on {HOST}:{PORT}..."
        print(start_message)
        write_to_log_file(start_message)

        wait_message = "UbuntuLogServer: Waiting for client connection..."
        print(wait_message)
        write_to_log_file(wait_message)

        while True:
            # Wait for a connection
            client_socket, addr = server_socket.accept()
            # Start a new thread to handle the client connection
            client_handler = threading.Thread(target=handle_client, args=(client_socket, addr))
            client_handler.start()

    except Exception as e:
        error_message = f"UbuntuLogServer: Could not start server: {e}"
        print(error_message)
        write_to_log_file(error_message)
    finally:
        if server_socket:
            server_socket.close()
            close_socket_message = "UbuntuLogServer: Server socket closed."
            print(close_socket_message)
            write_to_log_file(close_socket_message)
        if log_file_handle:
            log_file_handle.close()
            print(f"UbuntuLogServer: Log file '{LOG_FILE}' closed.")

if __name__ == "__main__":
    main()

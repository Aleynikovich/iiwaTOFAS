# --- log_client.py ---
import socket
import time

SERVER_IP = '10.66.171.147'
SERVER_PORT = 30002 # Log client port - Corrected to 30002

def run_log_client():
    client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        client_socket.connect((SERVER_IP, SERVER_PORT))
        print(f"Connected to server at {SERVER_IP}:{SERVER_PORT}\n")

        while True:
            try:
                # Attempt to receive data (logs/heartbeats from server)
                data = client_socket.recv(1024)
                if not data:
                    print("Server closed the connection.\n")
                    break
                print(f"{data.decode().strip()}\n")
                time.sleep(1) # Small delay to avoid busy-waiting too aggressively
            except BlockingIOError:
                # No data available, continue loop
                pass
            except ConnectionResetError:
                print("Server forcibly closed the connection.\n")
                break
            except Exception as e:
                print(f"An error occurred during communication: {e}\n")
                break
    except ConnectionRefusedError:
        print(f"Connection refused. Is the server running on {SERVER_IP}:{SERVER_PORT}?\n")
    except Exception as e:
        print(f"An error occurred during connection: {e}\n")
    finally:
        print("Closing client socket.\n")
        client_socket.close()

if __name__ == '__main__':
    run_log_client()

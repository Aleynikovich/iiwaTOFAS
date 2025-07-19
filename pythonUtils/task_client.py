# --- task_client.py ---
import socket
import time

SERVER_IP = '10.66.171.147'
SERVER_PORT = 30001

def run_task_client():
    client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        client_socket.connect((SERVER_IP, SERVER_PORT))
        print(f"Connected to server at {SERVER_IP}:{SERVER_PORT}")

        while True:
            try:
                # Attempt to receive data (heartbeats from server)
                data = client_socket.recv(1024)
                if not data:
                    print("Server closed the connection.")
                    break
                print(f"Received from server: {data.decode().strip()}")
                time.sleep(1) # Small delay to avoid busy-waiting too aggressively
            except BlockingIOError:
                # No data available, continue loop
                pass
            except ConnectionResetError:
                print("Server forcibly closed the connection.")
                break
            except Exception as e:
                print(f"An error occurred during communication: {e}")
                break
    except ConnectionRefusedError:
        print(f"Connection refused. Is the server running on {SERVER_IP}:{SERVER_PORT}?")
    except Exception as e:
        print(f"An error occurred during connection: {e}")
    finally:
        print("Closing client socket.")
        client_socket.close()

if __name__ == '__main__':
    run_task_client()

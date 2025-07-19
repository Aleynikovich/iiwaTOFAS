# --- task_client.py ---
import socket

SERVER_IP = '10.66.171.147'
SERVER_PORT = 30001 # Task client port

def run_task_client():
    client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        client_socket.connect((SERVER_IP, SERVER_PORT))
        client_socket.sendall(b"Hello from task client!\n")
        response = client_socket.recv(1024) # Try to receive, but server will close quickly
        print(f"Received from server: {response.decode().strip()}")
    except ConnectionRefusedError:
        print(f"Connection refused. Is the server running on {SERVER_IP}:{SERVER_PORT}?")
    except Exception as e:
        print(f"An error occurred: {e}")
    finally:
        client_socket.close()

if __name__ == '__main__':
    run_task_client()

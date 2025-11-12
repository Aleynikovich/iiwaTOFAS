import socket
import time
import re

SERVER_IP = '10.66.171.147'
SERVER_PORT = 30002  # Log client port

RECONNECT_DELAY_SECONDS = 5  # How long to wait before attempting to reconnect

# ANSI color codes for terminal output
COLORS = {
    'INFO': '\033[92m',    # Green
    'WARN': '\033[93m',    # Yellow
    'ERROR': '\033[91m',   # Red
    'RESET': '\033[0m',    # Reset to default
    'TIMESTAMP': '\033[90m',  # Dark gray
    'TAG': '\033[96m'      # Cyan
}

# Regex pattern to parse log messages: [timestamp] [LEVEL] [tag] message
LOG_PATTERN = re.compile(r'\[([\d:\.]+)\] \[(\w+)\] \[([\w_]+)\] (.+)')


def colorize_log(log_message):
    """
    Parse a log message and return it with ANSI color codes.
    Format: [timestamp] [LEVEL] [tag] message
    """
    match = LOG_PATTERN.match(log_message.strip())
    if match:
        timestamp, level, tag, message = match.groups()
        
        # Choose color based on level
        level_color = COLORS.get(level, COLORS['RESET'])
        
        # Format with colors: timestamp (gray), level (colored), tag (cyan), message (colored)
        colored = (
            f"{COLORS['TIMESTAMP']}[{timestamp}]{COLORS['RESET']} "
            f"{level_color}[{level}]{COLORS['RESET']} "
            f"{COLORS['TAG']}[{tag}]{COLORS['RESET']} "
            f"{level_color}{message}{COLORS['RESET']}"
        )
        return colored
    
    # If format doesn't match, return as-is
    return log_message.strip()


def run_log_client():
    """
    Runs the log client, attempting to connect to the server and
    reconnecting automatically if the connection is lost or refused.
    Displays logs with color-coding based on severity level.
    """
    print("Starting log client with color-coded output...")
    print(f"  {COLORS['INFO']}INFO{COLORS['RESET']} = Green")
    print(f"  {COLORS['WARN']}WARN{COLORS['RESET']} = Yellow")
    print(f"  {COLORS['ERROR']}ERROR{COLORS['RESET']} = Red")
    print()
    
    while True:  # Outer loop for continuous reconnection attempts
        client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        try:
            print(f"Attempting to connect to server at {SERVER_IP}:{SERVER_PORT}...")
            client_socket.connect((SERVER_IP, SERVER_PORT))
            print(f"{COLORS['INFO']}Connected to server at {SERVER_IP}:{SERVER_PORT}{COLORS['RESET']}")
            print("-" * 80)

            buffer = ""  # Buffer to handle partial messages
            
            # Inner loop for receiving data while connected
            while True:
                try:
                    data = client_socket.recv(4096)
                    if not data:
                        print(f"\n{COLORS['WARN']}Server closed the connection.{COLORS['RESET']}")
                        break  # Break inner loop to trigger reconnection

                    # Decode and add to buffer
                    buffer += data.decode('utf-8')
                    
                    # Process complete lines (ending with \n)
                    while '\n' in buffer:
                        line, buffer = buffer.split('\n', 1)
                        if line.strip():  # Only process non-empty lines
                            colored_line = colorize_log(line)
                            print(colored_line)

                except BlockingIOError:
                    pass
                except ConnectionResetError:
                    print(f"\n{COLORS['ERROR']}Server forcibly closed the connection.{COLORS['RESET']}")
                    break  # Break inner loop to trigger reconnection
                except UnicodeDecodeError as e:
                    print(f"{COLORS['ERROR']}Error decoding message: {e}{COLORS['RESET']}")
                    buffer = ""  # Clear buffer on decode error
                except Exception as e:
                    print(f"{COLORS['ERROR']}An error occurred during communication: {e}{COLORS['RESET']}")
                    break  # Break inner loop to trigger reconnection

        except ConnectionRefusedError:
            print(f"{COLORS['ERROR']}Connection refused. Is the server running on {SERVER_IP}:{SERVER_PORT}?{COLORS['RESET']}")
            print(f"Retrying in {RECONNECT_DELAY_SECONDS} seconds...")
        except Exception as e:
            print(f"{COLORS['ERROR']}An unexpected error occurred: {e}{COLORS['RESET']}")
            print(f"Retrying in {RECONNECT_DELAY_SECONDS} seconds...")
        finally:
            if client_socket:
                client_socket.close()

        time.sleep(RECONNECT_DELAY_SECONDS)

if __name__ == '__main__':
    run_log_client()

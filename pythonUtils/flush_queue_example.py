#!/usr/bin/env python3
"""
Example script demonstrating the flush queue command.
This command clears all pending commands and moves the robot to home position.
"""

import socket
import sys

# Configuration
ROBOT_IP = "10.66.171.147"  # Update with your robot's IP
TASK_PORT = 30001
TERMINATOR = "#"


def send_flush_queue_command():
    """
    Sends a flush queue command to the robot.
    Command format: 14|0||0.0|0.0|0.0|flush_cmd#
    
    This will:
    1. Clear all pending commands from the queue
    2. Move the robot to home position (all joints at 0 degrees)
    """
    try:
        # Connect to robot
        print(f"Connecting to robot at {ROBOT_IP}:{TASK_PORT}...")
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect((ROBOT_IP, TASK_PORT))
        print("Connected!")
        
        # Wait for initial "FREE|0#" message
        data = b""
        while not data.endswith(TERMINATOR.encode()):
            chunk = sock.recv(1024)
            if not chunk:
                print("Connection closed by robot.")
                return
            data += chunk
        initial_response = data.decode()
        print(f"< Received: {initial_response}")
        
        # Send flush queue command
        # Format: ACTION_TYPE|NUM_POINTS|POINT_DATA|VELOCITY|ACCELERATION|JERK|ID#
        command = "14|0||0.0|0.0|0.0|flush_cmd" + TERMINATOR
        print(f"> Sending: {command}")
        sock.sendall(command.encode())
        
        # Wait for response
        data = b""
        while not data.endswith(TERMINATOR.encode()):
            chunk = sock.recv(1024)
            if not chunk:
                print("Connection closed by robot.")
                return
            data += chunk
        response = data.decode()
        print(f"< Received: {response}")
        
        if "success" in response:
            print("✓ Flush queue command executed successfully!")
            print("  - All pending commands have been cleared")
            print("  - Robot is moving to home position (all joints at 0°)")
        else:
            print("✗ Flush queue command failed!")
        
        # Close connection
        sock.close()
        print("Connection closed.")
        
    except socket.error as e:
        print(f"Socket error: {e}")
        sys.exit(1)
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)


if __name__ == "__main__":
    print("=" * 60)
    print("Flush Queue Command Example")
    print("=" * 60)
    print()
    print("This script sends a flush queue command to the robot.")
    print("The command will:")
    print("  1. Clear all pending commands from the queue")
    print("  2. Move the robot to home position (all joints at 0°)")
    print()
    
    response = input("Do you want to proceed? (y/n): ").strip().lower()
    if response == 'y':
        send_flush_queue_command()
    else:
        print("Aborted.")

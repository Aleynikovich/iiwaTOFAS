#!/usr/bin/env python3
"""
Joint State Client for KUKA iiwa Robot

This client connects to the robot's joint state server (port 30003) and
receives real-time joint position data at 100Hz.

The robot broadcasts joint positions in the format:
J1;J2;J3;J4;J5;J6;J7#

Where J1-J7 are joint angles in degrees.
"""

import socket
import sys
import signal

# Configuration
SERVER_IP = "10.66.171.147"  # Update this to match your robot's IP
JOINT_STATE_PORT = 30003
BUFFER_SIZE = 1024

# Flag for graceful shutdown
running = True


def signal_handler(sig, frame):
    """Handle Ctrl+C for graceful shutdown"""
    global running
    print("\nShutting down joint state client...")
    running = False


def parse_joint_data(data):
    """Parse joint data string into a list of floats"""
    try:
        joint_values = [float(x) for x in data.split(';')]
        return joint_values
    except ValueError as e:
        print(f"Error parsing joint data: {e}")
        return None


def main():
    global running
    
    # Set up signal handler for Ctrl+C
    signal.signal(signal.SIGINT, signal_handler)
    
    print(f"Connecting to KUKA iiwa Joint State Server at {SERVER_IP}:{JOINT_STATE_PORT}...")
    
    try:
        # Create socket and connect to server
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(5.0)  # 5 second timeout for connection
        sock.connect((SERVER_IP, JOINT_STATE_PORT))
        sock.settimeout(1.0)  # 1 second timeout for receiving data
        print("Connected successfully!")
        print("Receiving joint state data (press Ctrl+C to stop)...")
        print("-" * 80)
        
        message_count = 0
        buffer = ""
        
        while running:
            try:
                # Receive data from server
                chunk = sock.recv(BUFFER_SIZE).decode('utf-8')
                
                if not chunk:
                    print("Connection closed by server")
                    break
                
                buffer += chunk
                
                # Process complete messages (terminated by '#')
                while '#' in buffer:
                    message, buffer = buffer.split('#', 1)
                    
                    if message:
                        joint_values = parse_joint_data(message)
                        
                        if joint_values and len(joint_values) == 7:
                            message_count += 1
                            
                            # Display every 10th message to avoid flooding the console
                            if message_count % 10 == 0:
                                print(f"[{message_count:6d}] Joints: " + 
                                      " ".join([f"J{i+1}:{v:7.2f}°" for i, v in enumerate(joint_values)]))
                        else:
                            print(f"Invalid joint data: {message}")
                            
            except socket.timeout:
                # Timeout is normal, just continue
                continue
            except Exception as e:
                print(f"Error receiving data: {e}")
                break
                
    except socket.timeout:
        print(f"Connection timeout. Make sure the robot is running and reachable at {SERVER_IP}")
        return 1
    except ConnectionRefusedError:
        print(f"Connection refused. Make sure the Joint State Server is running on the robot.")
        return 1
    except Exception as e:
        print(f"Error: {e}")
        return 1
    finally:
        try:
            sock.close()
            print("Socket closed.")
        except:
            pass
    
    print("Joint state client terminated.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

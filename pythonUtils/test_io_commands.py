#!/usr/bin/env python3
"""
Test script for IO commands (IDs 9, 12, 13)

This script demonstrates the three types of IO commands:
- Command 9 (ACTIVATE_IO): Set a digital output
- Command 12 (DIGITAL_INPUT): Read a digital input
- Command 13 (ANALOG_INPUT): Read an analog input (not yet implemented)

Usage:
    python3 test_io_commands.py [robot_ip]

Default robot IP is 10.66.171.147
"""

import socket
import sys
import uuid

# Configuration
DEFAULT_ROBOT_IP = "10.66.171.147"
TASK_PORT = 30001
TERMINATOR = "#"


def generate_command_id():
    """Generate a unique command ID"""
    return str(uuid.uuid4())


def send_command(sock, command):
    """Send a command to the robot"""
    if not command.endswith(TERMINATOR):
        command += TERMINATOR
    sock.sendall(command.encode())
    print(f"> Sent: {command}")


def receive_response(sock):
    """Receive response from the robot"""
    data = b""
    while not data.endswith(TERMINATOR.encode()):
        chunk = sock.recv(1024)
        if not chunk:
            raise ConnectionError("Connection closed by robot.")
        data += chunk
    response = data.decode()
    print(f"< Response: {response}")
    return response


def test_activate_io(sock, pin, state):
    """
    Test Command 9: ACTIVATE_IO
    Format: 9|0|0|0|PIN|STATE|0|0|0|ID#
    
    Args:
        sock: Socket connection to robot
        pin: IO pin number (1-88)
        state: Output state (true/false)
    """
    print(f"\n=== Testing ACTIVATE_IO (Command 9) ===")
    print(f"Setting output pin {pin} to {state}")

    cmd_id = generate_command_id()
    # Format: ACTION_TYPE|NUM_POINTS|TARGET_POINTS|IO_POINT|IO_PIN|IO_STATE|TOOL|BASE|SPEED_OVERRIDE|ID
    command = f"9|0|0|0|{pin}|{state}|0|0|0|{cmd_id}"

    send_command(sock, command)
    response = receive_response(sock)

    # Expected response: FREE|id|success# or FREE|id|failure#
    if "|success#" in response:
        print("✅ Command executed successfully")
    else:
        print("❌ Command failed")

    return response


def test_digital_input(sock, pin):
    """
    Test Command 12: DIGITAL_INPUT
    Format: 12|0|0|0|PIN|0|0|0|0|ID#
    
    Args:
        sock: Socket connection to robot
        pin: Input pin number (1-86)
    
    Returns:
        The input state (0 or 1)
    """
    print(f"\n=== Testing DIGITAL_INPUT (Command 12) ===")
    print(f"Reading digital input pin {pin}")

    cmd_id = generate_command_id()
    # Format: ACTION_TYPE|NUM_POINTS|TARGET_POINTS|IO_POINT|IO_PIN|IO_STATE|TOOL|BASE|SPEED_OVERRIDE|ID
    command = f"12|0|0|0|{pin}|0|0|0|0|{cmd_id}"

    send_command(sock, command)
    response = receive_response(sock)

    # Expected response: FREE|id|STATE# where STATE is 0 or 1
    if "|1#" in response:
        print("✅ Input state: HIGH (1)")
        return 1
    elif "|0#" in response:
        print("✅ Input state: LOW (0)")
        return 0
    else:
        print("❌ Unexpected response format")
        return None


def test_analog_input(sock, pin):
    """
    Test Command 13: ANALOG_INPUT (not yet implemented)
    Format: 13|0|0|0|PIN|0|0|0|0|ID#
    
    Args:
        sock: Socket connection to robot
        pin: Analog input pin number
    """
    print(f"\n=== Testing ANALOG_INPUT (Command 13) ===")
    print(f"Reading analog input pin {pin} (NOT IMPLEMENTED YET)")

    cmd_id = generate_command_id()
    # Format: ACTION_TYPE|NUM_POINTS|TARGET_POINTS|IO_POINT|IO_PIN|IO_STATE|TOOL|BASE|SPEED_OVERRIDE|ID
    command = f"13|0|0|0|{pin}|0|0|0|0|{cmd_id}"

    send_command(sock, command)
    response = receive_response(sock)

    # Expected response: FREE|id|failure# (since not implemented)
    if "|failure#" in response:
        print("⚠️  Command returned failure (expected - not implemented)")
    else:
        print(f"Response: {response}")

    return response


def run_comprehensive_test(sock):
    """Run a comprehensive test of all IO commands"""
    print("\n" + "=" * 60)
    print("COMPREHENSIVE IO COMMAND TEST")
    print("=" * 60)

    # Test 1: Set output
    print("\nTest 1: Set a digital output (pin 65)")
    test_activate_io(sock, 65, "true")

    # Test 2: Clear output
    print("\nTest 2: Clear the same digital output (pin 65)")
    test_activate_io(sock, 65, "false")

    # Test 3: Read digital input
    print("\nTest 3: Read digital input (pin 65)")
    test_digital_input(sock, 65)

    # Test 4: Try analog input (should fail)
    print("\nTest 4: Try reading analog input (pin 1) - should fail")
    test_analog_input(sock, 1)

    # Test 5: Virtual marks
    print("\nTest 5: Set a virtual mark (pin 1)")
    test_activate_io(sock, 1, "true")

    print("\nTest 6: Read virtual mark (pin 1)")
    test_digital_input(sock, 1)

    print("\n" + "=" * 60)
    print("TEST COMPLETE")
    print("=" * 60)


def interactive_mode(sock):
    """Interactive mode for testing specific commands"""
    while True:
        print("\n" + "=" * 60)
        print("IO COMMAND TEST MENU")
        print("=" * 60)
        print("1. Test ACTIVATE_IO (Command 9) - Set digital output")
        print("2. Test DIGITAL_INPUT (Command 12) - Read digital input")
        print("3. Test ANALOG_INPUT (Command 13) - Read analog input")
        print("4. Run comprehensive test")
        print("5. Exit")
        print()

        choice = input("Select option: ").strip()

        if choice == "1":
            pin = input("Enter output pin (1-88): ").strip()
            state = input("Enter state (true/false): ").strip().lower()
            test_activate_io(sock, pin, state)

        elif choice == "2":
            pin = input("Enter input pin (1-86): ").strip()
            test_digital_input(sock, pin)

        elif choice == "3":
            pin = input("Enter analog input pin: ").strip()
            test_analog_input(sock, pin)

        elif choice == "4":
            run_comprehensive_test(sock)

        elif choice == "5":
            print("Goodbye!")
            break

        else:
            print("Invalid choice. Please try again.")


def main():
    """Main function"""
    # Get robot IP from command line or use default
    robot_ip = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_ROBOT_IP

    print(f"Connecting to robot at {robot_ip}:{TASK_PORT}...")

    try:
        with socket.create_connection((robot_ip, TASK_PORT), timeout=10) as sock:
            print("✅ Connected successfully!")

            # Receive initial response
            initial = receive_response(sock)
            print(f"Initial response: {initial}")

            # Run interactive mode
            interactive_mode(sock)

    except socket.timeout:
        print(f"❌ Connection timeout. Make sure robot is reachable at {robot_ip}:{TASK_PORT}")
        sys.exit(1)
    except ConnectionRefusedError:
        print(f"❌ Connection refused. Make sure the robot server is running on {robot_ip}:{TASK_PORT}")
        sys.exit(1)
    except Exception as e:
        print(f"❌ Error: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()

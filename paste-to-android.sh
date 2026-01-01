#!/bin/bash

# paste-to-android.sh
# Script to send text from Linux terminal to Android device

# Default values
ANDROID_IP=""
PORT="8080"
TEXT=""

# Display usage
usage() {
    echo "Usage: $0 -i <android_ip> [-p <port>] -t <text>"
    echo ""
    echo "Options:"
    echo "  -i    IP address of the Android device (required)"
    echo "  -p    Port number (default: 8080)"
    echo "  -t    Text to send (required)"
    echo ""
    echo "Examples:"
    echo "  $0 -i 192.168.1.100 -t \"Hello Android\""
    echo "  $0 -i 192.168.1.100 -p 8080 -t \"Your text here\""
    echo ""
    echo "Shorthand:"
    echo "  curl -X POST http://192.168.1.100:8080/paste -d \"Your text here\""
    exit 1
}

# Parse command line arguments
while getopts "i:p:t:h" opt; do
    case $opt in
        i) ANDROID_IP="$OPTARG" ;;
        p) PORT="$OPTARG" ;;
        t) TEXT="$OPTARG" ;;
        h) usage ;;
        \?) usage ;;
    esac
done

# Validate required arguments
if [ -z "$ANDROID_IP" ] || [ -z "$TEXT" ]; then
    echo "Error: Android IP and text are required"
    usage
fi

# Send the text
echo "Sending text to Android device at $ANDROID_IP:$PORT..."
response=$(curl -s -X POST "http://$ANDROID_IP:$PORT/paste" -d "$TEXT")

if [ $? -eq 0 ]; then
    echo "Success: $response"
else
    echo "Error: Failed to send text to Android device"
    echo "Make sure:"
    echo "  1. The Android app is running and server is started"
    echo "  2. Both devices are on the same network"
    echo "  3. The IP address is correct"
    exit 1
fi

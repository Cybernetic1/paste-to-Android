# Paste to Android

Send short text strings between Linux and Android over your local network.

## Overview

This project provides a simple solution for sending text between a Linux machine and an Android device. It consists of:
- An Android app that runs an HTTP server to receive text from Linux
- An Android app that can send clipboard text to Linux over SSH/SCP
- A Linux bash script (or direct curl commands) to send text to Android

## First-Time Setup Checklist

### Linux → Android
- Build and install the Android app
- Start the server in the app
- Copy the Android IP address shown in the UI
- Send a test message with `paste-to-android.sh` or `curl`

### Android → Linux
- Make sure the Linux machine has SSH running
- Create or choose an SSH key pair
- Add the public key to `~/.ssh/authorized_keys`
- Import the private key into the Android app
- Add a Linux destination in the app
- Copy text on Android and tap **Send Clipboard to Linux**

## Features

- ✅ Simple HTTP-based communication
- ✅ No external services or cloud dependencies
- ✅ Works over local network (WiFi)
- ✅ Real-time text display on Android
- ✅ Easy to use command-line interface
- ✅ Foreground service keeps server running
- ✅ Android clipboard can be sent to Linux as a `.txt` file

## Prerequisites

### Android Device
- Android 7.0 (API level 24) or higher
- WiFi connection

### Linux Machine
- `curl` installed (usually pre-installed)
- SSH server running for Android -> Linux sends
- Connected to the same network as Android device

## Installation

### Android App

1. **Build the APK:**
   ```bash
   ./gradlew assembleDebug
   ```
   The APK will be at `app/build/outputs/apk/debug/app-debug.apk`

2. **Install on your Android device:**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```
   Or transfer the APK to your device and install manually.

3. **Launch the app** and press "Start Server"

4. **Note the IP address** displayed in the app (e.g., 192.168.1.100)

### Linux Script

1. **Make the script executable:**
   ```bash
   chmod +x paste-to-android.sh
   ```

2. **Optionally, copy to your PATH:**
   ```bash
   sudo cp paste-to-android.sh /usr/local/bin/paste-to-android
   ```

### Android -> Linux Setup

1. **Make sure SSH is enabled on Linux**
   - The Linux machine must be running an SSH server
   - You should be able to log in from another machine with `ssh user@host`

2. **Create a dedicated SSH key pair on Linux**
   ```bash
   ssh-keygen -t rsa -f ~/paste_key
   ```
   When prompted for a passphrase, press Enter for no passphrase.

3. **Add the public key to Linux**
   ```bash
   cat ~/paste_key.pub >> ~/.ssh/authorized_keys
   chmod 700 ~/.ssh
   chmod 600 ~/.ssh/authorized_keys
   ```

4. **Copy the private key to Android**
   - The Android app needs the **private** key file (`~/paste_key`)
   - Do **not** import the `.pub` file

5. **Add a destination in the app**
   - Tap **Setup SSH Key** and import the private key
   - Tap **Manage Destinations**
   - Add your Linux machine details, for example:
     - Name: `Home PC`
     - User: `yky`
     - Host: `192.168.0.100`
     - Port: `22`
     - Directory: `/home/yky/Downloads`

## Usage

### Method 1: Using the Script

```bash
# Basic usage
./paste-to-android.sh -i 192.168.1.100 -t "Hello Android"

# With custom port
./paste-to-android.sh -i 192.168.1.100 -p 8080 -t "Your text here"

# If installed to PATH
paste-to-android -i 192.168.1.100 -t "Message from Linux"
```

### Method 2: Direct curl Command

```bash
# Simple POST request
curl -X POST http://192.168.1.100:8080/paste -d "Your text here"

# With verbose output
curl -v -X POST http://192.168.1.100:8080/paste -d "Your text here"
```

### Script Options

```
-i    IP address of the Android device (required)
-p    Port number (default: 8080)
-t    Text to send (required)
-h    Show help message
```

## How It Works

1. The Android app starts an HTTP server on port 8080
2. The server listens for POST requests at the `/paste` endpoint
3. Linux sends text via HTTP POST using curl
4. Android receives and displays the text immediately
5. A foreground service keeps the server running even when the app is minimized

## Network Configuration

Both devices must be on the same local network (e.g., same WiFi). The Android device's IP address can be found:
- In the app interface after starting the server
- Android Settings → Network & Internet → WiFi → [Your Network] → Advanced
- Or use an IP scanner app

## Troubleshooting

### "Connection refused" error
- Ensure the Android app is running and the server is started
- Check that both devices are on the same WiFi network
- Verify the IP address is correct
- Check if a firewall is blocking port 8080

### "No route to host" error
- Confirm both devices are on the same network
- Try pinging the Android device: `ping 192.168.1.100`
- Some routers isolate devices (AP isolation) - check router settings

### Text not appearing on Android
- Check the app is in foreground or check the notification
- Verify the server is running (notification should be visible)
- Check Android logs: `adb logcat | grep Paste`

### Android -> Linux send fails
- Confirm the Linux host IP is correct in the saved destination
- Make sure the SSH server is running on the Linux machine
- Make sure the imported key is the **private** key, not `*.pub`
- Make sure the destination directory exists and is writable
- If the key was generated with a passphrase, remove it or generate a new key without one

### Permission issues
- The app requests necessary permissions on first run
- Ensure INTERNET permission is granted
- For Android 13+, notification permission may be needed

## Security Considerations

⚠️ **Important:** This app is designed for use on trusted local networks only.

- The HTTP server is **not encrypted** (no HTTPS)
- There is **no authentication** - anyone on the network can send text
- Do not use on public or untrusted networks
- Consider implementing authentication if needed for your use case

## Development

### Building from Source

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing configuration)
./gradlew assembleRelease

# Run on connected device
./gradlew installDebug
```

### Project Structure

```
paste-to-Android/
├── app/
│   ├── src/main/
│   │   ├── java/com/paste/android/
│   │   │   ├── MainActivity.java          # Main UI activity
│   │   │   └── HttpServerService.java     # HTTP server service
│   │   ├── res/                            # Android resources
│   │   └── AndroidManifest.xml            # App manifest
│   └── build.gradle                        # App build configuration
├── paste-to-android.sh                     # Linux script
├── build.gradle                            # Project build configuration
└── README.md                               # This file
```

## Contributing

Contributions are welcome! Feel free to:
- Report bugs
- Suggest features
- Submit pull requests

## License

See [LICENSE](LICENSE) file for details.

## Alternatives Considered

Other methods for sending text to Android:
- **Firebase Cloud Messaging (FCM):** More complex, requires Google account
- **WebSocket:** More setup, bidirectional (not needed here)
- **Third-party services:** External dependencies, privacy concerns
- **SSH/ADB:** Requires USB debugging, more complex setup

This solution was chosen for its simplicity and minimal dependencies.

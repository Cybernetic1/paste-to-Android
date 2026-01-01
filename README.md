# Paste to Android

Send short text strings from your Linux terminal to an Android device over your local network.

## Overview

This project provides a simple solution for sending text from a Linux command line to an Android device. It consists of:
- An Android app that runs an HTTP server to receive text
- A Linux bash script (or direct curl commands) to send text

## Features

- ✅ Simple HTTP-based communication
- ✅ No external services or cloud dependencies
- ✅ Works over local network (WiFi)
- ✅ Real-time text display on Android
- ✅ Easy to use command-line interface
- ✅ Foreground service keeps server running

## Prerequisites

### Android Device
- Android 7.0 (API level 24) or higher
- WiFi connection

### Linux Machine
- `curl` installed (usually pre-installed)
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

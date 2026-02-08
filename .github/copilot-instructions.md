# Copilot Instructions for Paste to Android

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug

# Clean build artifacts
./gradlew clean
```

APK output location: `app/build/outputs/apk/debug/app-debug.apk`

## Testing the Application

No automated tests exist. To test manually:

### Testing Linux → Android (HTTP Server)
1. Build and install: `./gradlew installDebug`
2. Launch app on device and start server
3. Send test message from Linux:
   ```bash
   curl -X POST http://<device-ip>:8080/paste -d "Test message"
   ```
4. Verify text appears in app UI

### Testing Android → Linux (SSH/SCP)
1. Ensure Linux machine has SSH server running
2. Generate dedicated SSH key pair on Linux: `ssh-keygen -t rsa -f ~/paste_key`
3. Add public key to authorized_keys: `cat ~/paste_key.pub >> ~/.ssh/authorized_keys`
4. In Android app: tap "Setup SSH Key" and select the private key file
5. Add a destination with Linux host IP, username, port, and target directory
6. Copy text to Android clipboard
7. Tap "Send Clipboard to Linux" and verify .txt file appears in target directory

## Architecture

This is a bidirectional clipboard sync application with two components:

### Android App

#### Linux → Android (Receive)

- **MainActivity**: UI activity that controls server lifecycle and displays received text
  - Manages server start/stop via buttons
  - Binds to HttpServerService using ServiceConnection
  - Displays device IP address and connection instructions
  - Provides clipboard copy functionality
  
- **HttpServerService**: Foreground service that runs the HTTP server
  - Runs on port 8080 (hardcoded constant)
  - Listens for POST requests at `/paste` endpoint
  - Uses plain Java ServerSocket (not OkHttp or other libraries)
  - Each client connection spawns a new thread for handling
  - Implements custom HTTP request parsing (headers and body)
  - Notifies MainActivity via OnTextReceivedListener callback
  - Content size limit: 64KB (MAX_CONTENT_LENGTH constant)

#### Android → Linux (Send)

- **SSHClient**: Helper class for SSH/SCP operations using JSch library
  - Transfers files via SFTP protocol
  - Uses SSH key authentication (no password support)
  - Runs in background thread with callback interface
  - Generates timestamped filenames: `clipboard_YYYYMMDD_HHMMSS.txt`
  - 30-second connection timeout
  - Disables strict host key checking (for local network use)

- **LinuxDestination**: Data model for Linux targets
  - Properties: name, user, host, port, directory
  - Multiple destinations stored in SharedPreferences as JSON

- **SettingsManager**: Persists destinations and SSH key status
  - Uses SharedPreferences with JSON serialization
  - Stores selected destination index
  - Tracks SSH key import status

- **DestinationDialog**: UI for adding/editing destinations
  - Validates port numbers (1-65535)
  - Requires all fields (name, user, host, port, directory)

- **ManageDestinationsDialog**: List view for managing destinations
  - Edit/delete existing destinations
  - Custom adapter with inline edit/delete buttons

### Linux Script (Client Side)

- **paste-to-android.sh**: Bash wrapper around curl
  - Parses command line arguments (-i for IP, -p for port, -t for text)
  - Sends HTTP POST with text as request body

### Communication Flow

#### Linux → Android
1. MainActivity starts HttpServerService as a foreground service (required for Android 8.0+)
2. Service creates ServerSocket on port 8080 in background thread
3. Service accepts connections and spawns handler threads
4. Handler parses HTTP request manually (not using any HTTP library)
5. Service calls listener callback on main thread via Handler
6. MainActivity updates UI with received text

#### Android → Linux
1. MainActivity reads clipboard content from ClipboardManager
2. User selects destination from RadioGroup (persisted in SharedPreferences)
3. SSHClient.sendClipboardToLinux() spawns background thread
4. JSch establishes SFTP connection using private key from app-private storage
5. Clipboard content uploaded as timestamped .txt file to target directory
6. Success/error callback updates UI via runOnUiThread()

## Key Conventions

### Service Management

- HttpServerService uses `START_STICKY` to restart automatically if killed
- Service must run as foreground service with persistent notification
- MainActivity binds to service using `BIND_AUTO_CREATE` to register listener
- Service uses synchronized blocks when accessing listener (thread safety)

### Threading

- HTTP server runs in dedicated thread (`serverThread`)
- Each client connection handled in separate spawned thread
- SSH operations run in new Thread (spawned in SSHClient)
- Listener callbacks posted to main thread using `mainHandler.post()`
- Always use `runOnUiThread()` or Handler when updating UI from service/background thread

### HTTP Server Implementation

- Custom HTTP parsing (no HTTP library dependencies)
- Request format: `POST /paste` with text in body
- Response always includes `Content-Length` and `Connection: close` headers
- Returns 413 for payloads exceeding MAX_CONTENT_LENGTH
- Non-POST or non-/paste requests return usage instructions

### SSH/SCP Implementation

- Uses JSch 0.1.55 library (pure Java SSH implementation)
- SSH private key stored in app-private directory: `/data/data/com.paste.android/files/id_rsa`
- File permissions set to 600 (read/write owner only)
- StrictHostKeyChecking disabled for local network convenience
- Filename format: `clipboard_YYYYMMDD_HHMMSS.txt`
- Each transfer uses dedicated thread with callback interface

### Settings Persistence

- Destinations stored as JSON array in SharedPreferences
- Selected destination index persisted separately
- SSH key file stored in getFilesDir() (app-private storage)
- Settings survive app restarts but not uninstalls

### IP Address Display

- IP obtained from WifiManager, converted from int to dotted notation
- IP format conversion: `(ipInt & 0xff).(ipInt >> 8 & 0xff).(ipInt >> 16 & 0xff).(ipInt >> 24 & 0xff)`
- IP shown on MainActivity with usage example

### Package and Versioning

- Package name: `com.paste.android`
- Namespace: `com.paste.android`
- minSdk 26 (Android 8.0), targetSdk 34 (Android 14)
- Current version: 1.0 (versionCode 1)
- Dependencies: JSch 0.1.55, AndroidX libraries

## Security Notes

### HTTP Server (Linux → Android)
- **No authentication**: Anyone on the network can send text
- **No encryption**: Plain HTTP, not HTTPS
- This is intentional for simplicity - designed for trusted local networks only

### SSH Client (Android → Linux)
- **SSH key authentication only**: No password support
- **StrictHostKeyChecking disabled**: Accepts any host key (local network use)
- **Private key stored unencrypted**: In app-private directory
- Recommend using dedicated SSH key pair for this app (not your main SSH key)
- SSH provides encryption for data in transit

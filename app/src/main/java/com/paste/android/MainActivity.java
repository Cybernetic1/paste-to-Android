package com.paste.android;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private TextView statusText;
    private TextView receivedText;
    private TextView ipText;
    private Button startButton;
    private Button stopButton;
    private Button copyButton;
    private Button setupSSHKeyButton;
    private Button manageDestinationsButton;
    private Button sendClipboardButton;
    private RadioGroup destinationRadioGroup;
    
    private boolean serverRunning = false;
    private HttpServerService service;
    private boolean bound = false;
    private String lastReceivedText = "";
    
    private SettingsManager settingsManager;
    private ActivityResultLauncher<String> sshKeyPickerLauncher;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            HttpServerService.LocalBinder localBinder = (HttpServerService.LocalBinder) binder;
            service = localBinder.getService();
            bound = true;
            registerListener();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
            service = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settingsManager = new SettingsManager(this);

        statusText = findViewById(R.id.statusText);
        receivedText = findViewById(R.id.receivedText);
        ipText = findViewById(R.id.ipText);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        copyButton = findViewById(R.id.copyButton);
        setupSSHKeyButton = findViewById(R.id.setupSSHKeyButton);
        manageDestinationsButton = findViewById(R.id.manageDestinationsButton);
        sendClipboardButton = findViewById(R.id.sendClipboardButton);
        destinationRadioGroup = findViewById(R.id.destinationRadioGroup);

        startButton.setOnClickListener(v -> startServer());
        stopButton.setOnClickListener(v -> stopServer());
        copyButton.setOnClickListener(v -> copyToClipboard());
        setupSSHKeyButton.setOnClickListener(v -> setupSSHKey());
        manageDestinationsButton.setOnClickListener(v -> manageDestinations());
        sendClipboardButton.setOnClickListener(v -> sendClipboardToLinux());

        // Setup SSH key picker
        sshKeyPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    importSSHKey(uri);
                }
            }
        );

        updateUI();
        showIPAddress();
        loadDestinations();
        updateSendButtonState();
    }

    private void startServer() {
        Intent serviceIntent = new Intent(this, HttpServerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
        serverRunning = true;
        updateUI();
    }

    private void stopServer() {
        if (bound) {
            unbindService(connection);
            bound = false;
        }
        Intent serviceIntent = new Intent(this, HttpServerService.class);
        stopService(serviceIntent);
        serverRunning = false;
        updateUI();
    }

    private void updateUI() {
        if (serverRunning) {
            statusText.setText("Server Status: Running");
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
        } else {
            statusText.setText("Server Status: Stopped");
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        }
    }

    private void showIPAddress() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();
        
        // Convert IP address from int to string format
        String ipAddress = String.format("%d.%d.%d.%d",
            (ipInt & 0xff),
            (ipInt >> 8 & 0xff),
            (ipInt >> 16 & 0xff),
            (ipInt >> 24 & 0xff));
        
        ipText.setText("Device IP: " + ipAddress + "\nPort: 8080\n\n" +
                "To send text from Linux terminal:\n" +
                "curl -X POST http://" + ipAddress + ":8080/paste -d \"Your text here\"");
    }

    private void registerListener() {
        if (service != null) {
            service.setListener(text -> runOnUiThread(() -> {
                lastReceivedText = text;
                receivedText.setText("Received: " + text);
                copyButton.setEnabled(true);
            }));
        }
    }

    private void copyToClipboard() {
        if (lastReceivedText.isEmpty()) {
            Toast.makeText(this, "No text to copy", Toast.LENGTH_SHORT).show();
            return;
        }

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Received Text", lastReceivedText);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bound) {
            registerListener();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bound) {
            unbindService(connection);
            bound = false;
        }
    }

    private void setupSSHKey() {
        new AlertDialog.Builder(this)
                .setTitle("Setup SSH Key")
                .setMessage("Select your SSH private key file.\n\n" +
                        "IMPORTANT: The key must be in PEM format (not OpenSSH format).\n\n" +
                        "Generate a compatible key on Linux:\n" +
                        "ssh-keygen -t rsa -m PEM -f ~/paste_key\n\n" +
                        "Then add public key to Linux:\n" +
                        "cat ~/paste_key.pub >> ~/.ssh/authorized_keys\n\n" +
                        "Transfer the PRIVATE key (paste_key) to Android.")
                .setPositiveButton("Select File", (dialog, which) -> {
                    sshKeyPickerLauncher.launch("*/*");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void importSSHKey(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder keyContent = new StringBuilder();
            String line;
            String firstLine = null;
            while ((line = reader.readLine()) != null) {
                if (firstLine == null) {
                    firstLine = line;
                }
                keyContent.append(line).append("\n");
            }
            reader.close();

            // Validate key format
            if (firstLine == null || firstLine.isEmpty()) {
                Toast.makeText(this, "Empty key file", Toast.LENGTH_LONG).show();
                return;
            }

            if (firstLine.contains("BEGIN OPENSSH PRIVATE KEY")) {
                // OpenSSH format not supported by JSch
                new AlertDialog.Builder(this)
                        .setTitle("Unsupported Key Format")
                        .setMessage("This key is in OpenSSH format, which is not supported.\n\n" +
                                "Please convert it using:\n" +
                                "ssh-keygen -p -m PEM -f ~/paste_key\n\n" +
                                "Or generate a new key in PEM format:\n" +
                                "ssh-keygen -t rsa -m PEM -f ~/paste_key")
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }

            if (firstLine.contains("ENCRYPTED")) {
                Toast.makeText(this, "Encrypted keys are not supported. Use a key without passphrase.", Toast.LENGTH_LONG).show();
                return;
            }

            if (!firstLine.contains("BEGIN") || !firstLine.contains("PRIVATE KEY")) {
                Toast.makeText(this, "Invalid key file format", Toast.LENGTH_LONG).show();
                return;
            }

            // Save to app-private storage
            File keyFile = new File(getFilesDir(), "id_rsa");
            FileOutputStream fos = new FileOutputStream(keyFile);
            fos.write(keyContent.toString().getBytes());
            fos.close();

            // Set file permissions to 600 (read/write owner only)
            keyFile.setReadable(false, false);
            keyFile.setReadable(true, true);
            keyFile.setWritable(false, false);
            keyFile.setWritable(true, true);
            keyFile.setExecutable(false, false);

            settingsManager.setHasSSHKey(true);
            updateSendButtonState();
            Toast.makeText(this, "SSH key imported successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error importing SSH key: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void manageDestinations() {
        ManageDestinationsDialog.show(this, settingsManager, () -> {
            loadDestinations();
            updateSendButtonState();
        });
    }

    private void loadDestinations() {
        destinationRadioGroup.removeAllViews();
        List<LinuxDestination> destinations = settingsManager.getDestinations();
        int selectedIndex = settingsManager.getSelectedIndex();

        for (int i = 0; i < destinations.size(); i++) {
            LinuxDestination dest = destinations.get(i);
            RadioButton radioButton = new RadioButton(this);
            radioButton.setText(dest.toString());
            radioButton.setId(i);
            destinationRadioGroup.addView(radioButton);

            if (i == selectedIndex) {
                radioButton.setChecked(true);
            }
        }

        destinationRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            settingsManager.setSelectedIndex(checkedId);
        });
    }

    private void updateSendButtonState() {
        boolean hasKey = settingsManager.hasSSHKey();
        boolean hasDestinations = !settingsManager.getDestinations().isEmpty();
        sendClipboardButton.setEnabled(hasKey && hasDestinations);
    }

    private void sendClipboardToLinux() {
        // Get clipboard content
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (!clipboard.hasPrimaryClip()) {
            Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
        String clipboardText = item.getText().toString();

        if (clipboardText.isEmpty()) {
            Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get selected destination
        List<LinuxDestination> destinations = settingsManager.getDestinations();
        int selectedIndex = settingsManager.getSelectedIndex();
        
        if (selectedIndex < 0 || selectedIndex >= destinations.size()) {
            Toast.makeText(this, "Please select a destination", Toast.LENGTH_SHORT).show();
            return;
        }

        LinuxDestination destination = destinations.get(selectedIndex);
        File keyFile = new File(getFilesDir(), "id_rsa");

        Toast.makeText(this, "Sending clipboard to " + destination.getName() + "...", Toast.LENGTH_SHORT).show();

        SSHClientHelper.sendClipboardToLinux(destination, keyFile.getAbsolutePath(), clipboardText, 
            new SSHClientHelper.TransferCallback() {
                @Override
                public void onSuccess(String filename) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, 
                            "Sent successfully: " + filename, Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, 
                            "Failed to send: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        );
    }
}

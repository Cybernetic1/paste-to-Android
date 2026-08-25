package com.paste.android;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContentValues;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.tabs.TabLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private TextView statusText;
    private TextView receivedText;
    private TextView errorText;
    private TextView ipText;
    private Button startButton;
    private Button stopButton;
    private Button copyButton;
    private Button saveFileButton;
    private Button setupSSHKeyButton;
    private Button manageDestinationsButton;
    private Button sendClipboardButton;
    private Button sendFileButton;
    private RadioGroup destinationRadioGroup;
    private TabLayout tabLayout;
    private View receiveLayout;
    private View sendLayout;
    
    private boolean serverRunning = false;
    private HttpServerService service;
    private boolean bound = false;
    private String lastReceivedText = "";
    
    private SettingsManager settingsManager;
    private ActivityResultLauncher<String> sshKeyPickerLauncher;
    private ActivityResultLauncher<String> filePickerLauncher;

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
        errorText = findViewById(R.id.errorText);
        ipText = findViewById(R.id.ipText);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        copyButton = findViewById(R.id.copyButton);
        saveFileButton = findViewById(R.id.saveFileButton);
        setupSSHKeyButton = findViewById(R.id.setupSSHKeyButton);
        manageDestinationsButton = findViewById(R.id.manageDestinationsButton);
        sendClipboardButton = findViewById(R.id.sendClipboardButton);
        sendFileButton = findViewById(R.id.sendFileButton);
        destinationRadioGroup = findViewById(R.id.destinationRadioGroup);
        tabLayout = findViewById(R.id.tabLayout);
        receiveLayout = findViewById(R.id.receiveLayout);
        sendLayout = findViewById(R.id.sendLayout);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                boolean receiveSelected = tab.getPosition() == 0;
                receiveLayout.setVisibility(receiveSelected ? View.VISIBLE : View.GONE);
                sendLayout.setVisibility(receiveSelected ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        startButton.setOnClickListener(v -> startServer());
        stopButton.setOnClickListener(v -> stopServer());
        copyButton.setOnClickListener(v -> copyToClipboard());
        saveFileButton.setOnClickListener(v -> saveReceivedToFile());
        setupSSHKeyButton.setOnClickListener(v -> setupSSHKey());
        manageDestinationsButton.setOnClickListener(v -> manageDestinations());
        sendClipboardButton.setOnClickListener(v -> sendClipboardToLinux());
        sendFileButton.setOnClickListener(v -> sendFileToLinux());

        // Setup SSH key picker
        sshKeyPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    importSSHKey(uri);
                }
            }
        );

        // Setup file picker for sending a text file
        filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        sendTextToLinux(readUriText(uri));
                    } catch (Exception e) {
                        reportSendError("Could not read file: " + e.getMessage());
                    }
                }
            }
        );

        updateUI();
        showIPAddress();
        clearLatestError();
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
                "curl -X POST http://" + ipAddress + ":8080/paste -d \"Your text here\"\n" +
                "curl -X POST http://" + ipAddress + ":8080/paste -d @file");
    }

    private void registerListener() {
        if (service != null) {
            service.setListener(text -> runOnUiThread(() -> {
                lastReceivedText = text;
                receivedText.setText("Received: " + text);
                copyButton.setEnabled(true);
                saveFileButton.setEnabled(true);
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

    private void saveReceivedToFile() {
        if (lastReceivedText.isEmpty()) {
            Toast.makeText(this, "No text to save", Toast.LENGTH_SHORT).show();
            return;
        }
        String filename = "received_" + System.currentTimeMillis() + ".txt";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, filename);
                values.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) {
                    throw new IOException("Could not create file in Downloads");
                }
                try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                    os.write(lastReceivedText.getBytes("UTF-8"));
                }
            } else {
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!dir.exists() && !dir.mkdirs()) {
                    throw new IOException("Could not create Downloads directory");
                }
                File outFile = new File(dir, filename);
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    fos.write(lastReceivedText.getBytes("UTF-8"));
                }
            }
            Toast.makeText(this, "Saved to Downloads: " + filename, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error saving file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void clearLatestError() {
        errorText.setText("No errors yet");
    }

    private void showLatestError(String error) {
        errorText.setText(error);
    }

    private void reportSendError(String error) {
        showLatestError(error);
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
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
                        "Transfer the PRIVATE key (paste_key) to Android.\n\n" +
                        "If the key is passphrase-protected, you will be prompted for it each time you send.")
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
        boolean enabled = hasKey && hasDestinations;
        sendClipboardButton.setEnabled(enabled);
        sendFileButton.setEnabled(enabled);
    }

    private void sendClipboardToLinux() {
        // Get clipboard content
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (!clipboard.hasPrimaryClip()) {
            reportSendError("Clipboard is empty");
            return;
        }

        ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
        String clipboardText = item.getText().toString();

        if (clipboardText.isEmpty()) {
            reportSendError("Clipboard is empty");
            return;
        }

        sendTextToLinux(clipboardText);
    }

    private void sendFileToLinux() {
        filePickerLauncher.launch("*/*");
    }

    private void sendTextToLinux(String text) {
        if (text.isEmpty()) {
            reportSendError("Nothing to send");
            return;
        }

        // Get selected destination
        List<LinuxDestination> destinations = settingsManager.getDestinations();
        int selectedIndex = settingsManager.getSelectedIndex();

        if (selectedIndex < 0 || selectedIndex >= destinations.size()) {
            reportSendError("Please select a destination");
            return;
        }

        LinuxDestination destination = destinations.get(selectedIndex);
        File keyFile = new File(getFilesDir(), "id_rsa");

        if (isKeyEncrypted(keyFile)) {
            EditText input = new EditText(this);
            input.setHint("Key passphrase");
            input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                    | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            new AlertDialog.Builder(this)
                    .setTitle("SSH Key Passphrase")
                    .setView(input)
                    .setPositiveButton("Send", (dialog, which) ->
                            sendClipboard(destination, keyFile, input.getText().toString(), text))
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            sendClipboard(destination, keyFile, "", text);
        }
    }

    private String readUriText(Uri uri) throws Exception {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getContentResolver().openInputStream(uri)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    private boolean isKeyEncrypted(File keyFile) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(keyFile)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("ENCRYPTED")) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private void sendClipboard(LinuxDestination destination, File keyFile, String passphrase, String clipboardText) {
        Toast.makeText(this, "Sending clipboard to " + destination.getName() + "...", Toast.LENGTH_SHORT).show();

        SSHClientHelper.sendClipboardToLinux(destination, keyFile.getAbsolutePath(),
                passphrase, clipboardText,
            new SSHClientHelper.TransferCallback() {
                @Override
                public void onSuccess(String filename) {
                    runOnUiThread(() -> {
                        clearLatestError();
                        Toast.makeText(MainActivity.this,
                            "Sent successfully: " + filename, Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        reportSendError(error);
                    });
                }
            }
        );
    }
}

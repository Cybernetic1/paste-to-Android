package com.paste.android;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.format.Formatter;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private TextView statusText;
    private TextView receivedText;
    private TextView ipText;
    private Button startButton;
    private Button stopButton;
    private boolean serverRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        receivedText = findViewById(R.id.receivedText);
        ipText = findViewById(R.id.ipText);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);

        startButton.setOnClickListener(v -> startServer());
        stopButton.setOnClickListener(v -> stopServer());

        updateUI();
        showIPAddress();

        HttpServerService.setOnTextReceivedListener(text -> runOnUiThread(() -> {
            receivedText.setText("Received: " + text);
        }));
    }

    private void startServer() {
        Intent serviceIntent = new Intent(this, HttpServerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        serverRunning = true;
        updateUI();
    }

    private void stopServer() {
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
        int ip = wifiInfo.getIpAddress();
        
        @SuppressWarnings("deprecation")
        String ipAddress = Formatter.formatIpAddress(ip);
        
        ipText.setText("Device IP: " + ipAddress + "\nPort: 8080\n\n" +
                "To send text from Linux terminal:\n" +
                "curl -X POST http://" + ipAddress + ":8080/paste -d \"Your text here\"");
    }

    @Override
    protected void onResume() {
        super.onResume();
        HttpServerService.setOnTextReceivedListener(text -> runOnUiThread(() -> {
            receivedText.setText("Received: " + text);
        }));
    }
}

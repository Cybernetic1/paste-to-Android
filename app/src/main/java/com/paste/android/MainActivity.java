package com.paste.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
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
    private HttpServerService service;
    private boolean bound = false;

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

        statusText = findViewById(R.id.statusText);
        receivedText = findViewById(R.id.receivedText);
        ipText = findViewById(R.id.ipText);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);

        startButton.setOnClickListener(v -> startServer());
        stopButton.setOnClickListener(v -> stopServer());

        updateUI();
        showIPAddress();
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
                receivedText.setText("Received: " + text);
            }));
        }
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
}

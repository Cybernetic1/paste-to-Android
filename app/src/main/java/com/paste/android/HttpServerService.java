package com.paste.android;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServerService extends Service {
    private static final String CHANNEL_ID = "HttpServerChannel";
    private static final int PORT = 8080;
    private static final int MAX_CONTENT_LENGTH = 65536; // 64KB max
    private ServerSocket serverSocket;
    private Thread serverThread;
    private boolean isRunning = false;
    private OnTextReceivedListener listener;
    private Handler mainHandler;

    public interface OnTextReceivedListener {
        void onTextReceived(String text);
    }

    public void setListener(OnTextReceivedListener listener) {
        synchronized (this) {
            this.listener = listener;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, createNotification("Server running on port " + PORT));
        startServer();
        return START_STICKY;
    }

    private void startServer() {
        isRunning = true;
        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                while (isRunning) {
                    try {
                        Socket client = serverSocket.accept();
                        handleClient(client);
                    } catch (IOException e) {
                        if (isRunning) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        serverThread.start();
    }

    private void handleClient(Socket client) {
        new Thread(() -> {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);

                // Read the HTTP request
                String line;
                String method = "";
                String path = "";
                int contentLength = 0;
                
                // Parse request line
                line = in.readLine();
                if (line != null && !line.isEmpty()) {
                    String[] parts = line.split(" ");
                    if (parts.length >= 2) {
                        method = parts[0];
                        path = parts[1];
                    }
                }

                // Read headers
                while ((line = in.readLine()) != null && !line.isEmpty()) {
                    if (line.toLowerCase().startsWith("content-length:")) {
                        contentLength = Integer.parseInt(line.substring(15).trim());
                    }
                }

                // Validate content length
                if (contentLength > MAX_CONTENT_LENGTH) {
                    String response = "Content too large. Maximum " + MAX_CONTENT_LENGTH + " bytes allowed.";
                    out.println("HTTP/1.1 413 Payload Too Large");
                    out.println("Content-Type: text/plain");
                    out.println("Content-Length: " + response.length());
                    out.println("Connection: close");
                    out.println();
                    out.println(response);
                    out.flush();
                    client.close();
                    return;
                }

                // Handle POST request to /paste
                if ("POST".equals(method) && path.startsWith("/paste")) {
                    // Read the body
                    StringBuilder body = new StringBuilder();
                    if (contentLength > 0) {
                        char[] buffer = new char[contentLength];
                        int read = in.read(buffer, 0, contentLength);
                        if (read > 0) {
                            body.append(buffer, 0, read);
                        }
                    }

                    String receivedText = body.toString();

                    // Notify listener on main thread
                    synchronized (this) {
                        if (listener != null) {
                            mainHandler.post(() -> listener.onTextReceived(receivedText));
                        }
                    }

                    // Send HTTP response
                    String response = "Text received successfully";
                    out.println("HTTP/1.1 200 OK");
                    out.println("Content-Type: text/plain");
                    out.println("Content-Length: " + response.length());
                    out.println("Connection: close");
                    out.println();
                    out.println(response);
                } else {
                    // Send usage info for other requests
                    String response = "Send POST request to /paste with text in body";
                    out.println("HTTP/1.1 200 OK");
                    out.println("Content-Type: text/plain");
                    out.println("Content-Length: " + response.length());
                    out.println("Connection: close");
                    out.println();
                    out.println(response);
                }

                out.flush();
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (serverThread != null) {
            serverThread.interrupt();
        }
        synchronized (this) {
            listener = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    public class LocalBinder extends android.os.Binder {
        public HttpServerService getService() {
            return HttpServerService.this;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "HTTP Server Service",
                NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification(String text) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Paste Server")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .build();
    }
}

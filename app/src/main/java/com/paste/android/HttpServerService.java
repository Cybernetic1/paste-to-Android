package com.paste.android;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.util.concurrent.Executors;

public class HttpServerService extends Service {
    private static final String CHANNEL_ID = "HttpServerChannel";
    private static final int PORT = 8080;
    private HttpServer server;
    private static HttpServerService instance;

    public interface OnTextReceivedListener {
        void onTextReceived(String text);
    }

    private OnTextReceivedListener listener;

    public static void setOnTextReceivedListener(OnTextReceivedListener listener) {
        if (instance != null) {
            instance.listener = listener;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, createNotification("Server running on port " + PORT));
        startServer();
        return START_STICKY;
    }

    private void startServer() {
        new Thread(() -> {
            try {
                server = HttpServer.create(new InetSocketAddress(PORT), 0);
                server.createContext("/paste", new PasteHandler());
                server.setExecutor(Executors.newFixedThreadPool(4));
                server.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    class PasteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(exchange.getRequestBody()));
                StringBuilder text = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    text.append(line);
                }
                
                String receivedText = text.toString();
                
                // Notify listener
                if (listener != null) {
                    listener.onTextReceived(receivedText);
                }

                // Send response
                String response = "Text received successfully";
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                String response = "Send POST request to /paste with text in body";
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (server != null) {
            server.stop(0);
        }
        instance = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
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

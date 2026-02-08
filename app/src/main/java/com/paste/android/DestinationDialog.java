package com.paste.android;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class DestinationDialog {
    
    public interface OnDestinationSavedListener {
        void onDestinationSaved(LinuxDestination destination);
    }

    public static void showAddDestinationDialog(Context context, OnDestinationSavedListener listener) {
        showDestinationDialog(context, null, listener);
    }

    public static void showEditDestinationDialog(Context context, LinuxDestination existing, OnDestinationSavedListener listener) {
        showDestinationDialog(context, existing, listener);
    }

    private static void showDestinationDialog(Context context, LinuxDestination existing, OnDestinationSavedListener listener) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_destination, null);
        
        EditText nameInput = dialogView.findViewById(R.id.destinationName);
        EditText userInput = dialogView.findViewById(R.id.destinationUser);
        EditText hostInput = dialogView.findViewById(R.id.destinationHost);
        EditText portInput = dialogView.findViewById(R.id.destinationPort);
        EditText directoryInput = dialogView.findViewById(R.id.destinationDirectory);

        // Pre-fill if editing
        if (existing != null) {
            nameInput.setText(existing.getName());
            userInput.setText(existing.getUser());
            hostInput.setText(existing.getHost());
            portInput.setText(String.valueOf(existing.getPort()));
            directoryInput.setText(existing.getDirectory());
        } else {
            portInput.setText("22");
        }

        new AlertDialog.Builder(context)
                .setTitle(existing == null ? "Add Destination" : "Edit Destination")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    String user = userInput.getText().toString().trim();
                    String host = hostInput.getText().toString().trim();
                    String portStr = portInput.getText().toString().trim();
                    String directory = directoryInput.getText().toString().trim();

                    if (name.isEmpty() || user.isEmpty() || host.isEmpty() || portStr.isEmpty() || directory.isEmpty()) {
                        Toast.makeText(context, "All fields are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int port;
                    try {
                        port = Integer.parseInt(portStr);
                        if (port < 1 || port > 65535) {
                            throw new NumberFormatException();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(context, "Invalid port number", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    LinuxDestination destination = new LinuxDestination(name, user, host, port, directory);
                    listener.onDestinationSaved(destination);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
